package org.yamcs.booking.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamcs.YConfiguration;
import org.yamcs.booking.model.*;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Database access layer for Ground Station Booking System
 */
public class BookingDatabase {

    private static final Logger log = LoggerFactory.getLogger(BookingDatabase.class);

    private final HikariDataSource dataSource;

    public BookingDatabase(YConfiguration config) {
        log.info("Initializing Booking Database");

        // Get database configuration from environment or config
        String host = System.getenv("POSTGRES_HOST");
        String port = System.getenv("POSTGRES_PORT");
        String database = System.getenv("POSTGRES_DB");
        String username = System.getenv("POSTGRES_USER");
        String password = System.getenv("POSTGRES_PASSWORD");

        if (host == null) host = "localhost";
        if (port == null) port = "5432";
        if (database == null) database = "mcc";
        if (username == null) username = "mcc_dbadmin";
        if (password == null) password = "mcc_dbadmin";

        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, database);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        // Don't fail on initialization if database is not available
        // This allows the service to start without PostgreSQL and retry later
        hikariConfig.setInitializationFailTimeout(-1);

        this.dataSource = new HikariDataSource(hikariConfig);

        log.info("Database connection pool initialized for {}", jdbcUrl);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool closed");
        }
    }


    // Provider methods
    public List<GSProvider> getAllProviders() throws SQLException {
        List<GSProvider> providers = new ArrayList<>();
        String sql = "SELECT * FROM gs_providers WHERE is_active = true ORDER BY name";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                providers.add(mapProviderFromResultSet(rs));
            }
        }

        return providers;
    }

    public GSProvider getProviderById(int id) throws SQLException {
        String sql = "SELECT * FROM gs_providers WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapProviderFromResultSet(rs);
                }
            }
        }

        return null;
    }

    // Booking methods
    public List<GSBooking> getAllBookings() throws SQLException {
        List<GSBooking> bookings = new ArrayList<>();
        String sql = """
            SELECT b.*
            FROM gs_bookings b
            ORDER BY b.start_time DESC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                bookings.add(mapBookingFromResultSet(rs));
            }
        }

        return bookings;
    }

    public List<GSBooking> getPendingBookings() throws SQLException {
        List<GSBooking> bookings = new ArrayList<>();
        String sql = """
            SELECT b.*
            FROM gs_bookings b
            WHERE b.status = 'pending'
            ORDER BY b.start_time ASC
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                bookings.add(mapBookingFromResultSet(rs));
            }
        }

        return bookings;
    }

    public GSBooking createBooking(GSBooking booking) throws SQLException {
        String sql = """
            INSERT INTO gs_bookings (provider, satellite_id, start_time, duration_minutes,
                                   pass_type, purpose, rule_type, frequency_days,
                                   notes, requested_by)
            VALUES (?, ?, ?, ?, ?::pass_type, ?::purpose_type, ?::booking_rule_type, ?, ?, ?)
            RETURNING id, end_time
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, booking.getProvider());
            stmt.setString(2, booking.getSatelliteId());
            stmt.setTimestamp(3, Timestamp.valueOf(booking.getStartTime()));
            stmt.setInt(4, booking.getDurationMinutes());
            stmt.setString(5, booking.getPassType());
            stmt.setString(6, booking.getPurpose());
            stmt.setString(7, booking.getRuleType());
            stmt.setObject(8, booking.getFrequencyDays());
            stmt.setString(9, booking.getNotes());
            stmt.setString(10, booking.getRequestedBy());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    booking.setId(rs.getInt("id"));
                    booking.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                }
            }
        }

        return booking;
    }

    public boolean approveBooking(int bookingId, String approver, String comments) throws SQLException {
        String updateSql = """
            UPDATE gs_bookings
            SET status = 'approved', approved_by = ?, approved_at = CURRENT_TIMESTAMP
            WHERE id = ? AND status = 'pending'
            """;

        String logSql = """
            INSERT INTO booking_approvals (booking_id, approver, action, comments)
            VALUES (?, ?, 'approved', ?)
            """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                 PreparedStatement logStmt = conn.prepareStatement(logSql)) {

                // Update booking status
                updateStmt.setString(1, approver);
                updateStmt.setInt(2, bookingId);
                int updated = updateStmt.executeUpdate();

                if (updated > 0) {
                    // Log approval
                    logStmt.setInt(1, bookingId);
                    logStmt.setString(2, approver);
                    logStmt.setString(3, comments);
                    logStmt.executeUpdate();

                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public boolean rejectBooking(int bookingId, String approver, String reason) throws SQLException {
        String updateSql = """
            UPDATE gs_bookings
            SET status = 'rejected', rejection_reason = ?
            WHERE id = ? AND status = 'pending'
            """;

        String logSql = """
            INSERT INTO booking_approvals (booking_id, approver, action, comments)
            VALUES (?, ?, 'rejected', ?)
            """;

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                 PreparedStatement logStmt = conn.prepareStatement(logSql)) {

                // Update booking status
                updateStmt.setString(1, reason);
                updateStmt.setInt(2, bookingId);
                int updated = updateStmt.executeUpdate();

                if (updated > 0) {
                    // Log rejection
                    logStmt.setInt(1, bookingId);
                    logStmt.setString(2, approver);
                    logStmt.setString(3, reason);
                    logStmt.executeUpdate();

                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private GSProvider mapProviderFromResultSet(ResultSet rs) throws SQLException {
        GSProvider provider = new GSProvider();
        provider.setId(rs.getInt("id"));
        provider.setName(rs.getString("name"));
        provider.setType(ProviderType.valueOf(rs.getString("type")));
        provider.setContactEmail(rs.getString("contact_email"));
        provider.setContactPhone(rs.getString("contact_phone"));
        provider.setApiEndpoint(rs.getString("api_endpoint"));
        provider.setActive(rs.getBoolean("is_active"));
        provider.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        provider.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return provider;
    }

    private GSBooking mapBookingFromResultSet(ResultSet rs) throws SQLException {
        GSBooking booking = new GSBooking();
        booking.setId(rs.getInt("id"));
        booking.setProvider(rs.getString("provider"));
        booking.setSatelliteId(rs.getString("satellite_id"));
        booking.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
        booking.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
        booking.setDurationMinutes(rs.getInt("duration_minutes"));
        booking.setPassType(rs.getString("pass_type"));
        booking.setPurpose(rs.getString("purpose"));
        booking.setRuleType(rs.getString("rule_type"));
        booking.setFrequencyDays(rs.getObject("frequency_days", Integer.class));
        booking.setStatus(rs.getString("status"));
        booking.setGsStatus(rs.getString("gs_status"));
        booking.setRequestedBy(rs.getString("requested_by"));
        booking.setApprovedBy(rs.getString("approved_by"));
        if (rs.getTimestamp("approved_at") != null) {
            booking.setApprovedAt(rs.getTimestamp("approved_at").toLocalDateTime());
        }
        booking.setRejectionReason(rs.getString("rejection_reason"));
        booking.setNotes(rs.getString("notes"));
        booking.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        booking.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        // Additional fields from JOIN - removed since we don't use provider name/type joins anymore

        return booking;
    }

    // Enum methods
    public List<String> getPassTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT unnest(enum_range(NULL::pass_type))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                types.add(rs.getString(1));
            }
        }

        return types;
    }

    public List<String> getPurposeTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT unnest(enum_range(NULL::purpose_type))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                types.add(rs.getString(1));
            }
        }

        return types;
    }

    public List<String> getRuleTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT unnest(enum_range(NULL::booking_rule_type))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                types.add(rs.getString(1));
            }
        }

        return types;
    }

    public List<String> getStatusTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT unnest(enum_range(NULL::booking_status))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                types.add(rs.getString(1));
            }
        }

        return types;
    }

    public List<String> getGsStatusTypes() throws SQLException {
        List<String> types = new ArrayList<>();
        String sql = "SELECT unnest(enum_range(NULL::gs_status))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                types.add(rs.getString(1));
            }
        }

        return types;
    }

    // For now, return hardcoded provider types since we don't have gs_providers table populated
    public List<String> getProviderTypes() throws SQLException {
        return List.of("leafspace", "dhruva", "isro");
    }
}