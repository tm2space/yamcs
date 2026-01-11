package org.yamcs.http.api;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import org.yamcs.api.Observer;
import org.yamcs.booking.BookingService;
import org.yamcs.booking.db.BookingDatabase;
import org.yamcs.booking.model.*;
import org.yamcs.booking.provider.*;
import org.yamcs.http.Context;
import org.yamcs.protobuf.*;
import org.yamcs.security.User;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class BookingApi extends AbstractBookingApi<Context> {

    @Override
    public void getEnumValues(Context ctx, Empty request, Observer<GetEnumValuesResponse> observer) {
        try {
            BookingDatabase database = getBookingDatabase();

            GetEnumValuesResponse.Builder responseBuilder = GetEnumValuesResponse.newBuilder();

            // Get enum values from database
            responseBuilder.addAllProviderTypes(database.getProviderTypes());
            responseBuilder.addAllRuleTypes(database.getRuleTypes());
            responseBuilder.addAllStatusTypes(database.getStatusTypes());
            responseBuilder.addAllPassTypes(database.getPassTypes());
            responseBuilder.addAllPurposeTypes(database.getPurposeTypes());
            responseBuilder.addAllGsStatusTypes(database.getGsStatusTypes());

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getProviders(Context ctx, Empty request, Observer<GetProvidersResponse> observer) {
        try {
            BookingDatabase database = getBookingDatabase();
            List<GSProvider> providers = database.getAllProviders();

            GetProvidersResponse.Builder responseBuilder = GetProvidersResponse.newBuilder();
            for (GSProvider provider : providers) {
                responseBuilder.addProviders(toProviderInfo(provider));
            }

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getBookings(Context ctx, Empty request, Observer<GetBookingsResponse> observer) {
        try {
            BookingDatabase database = getBookingDatabase();
            List<GSBooking> bookings = database.getAllBookings();

            GetBookingsResponse.Builder responseBuilder = GetBookingsResponse.newBuilder();
            for (GSBooking booking : bookings) {
                responseBuilder.addBookings(toBookingInfo(booking));
            }

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void createBooking(Context ctx, CreateBookingRequest request, Observer<BookingInfo> observer) {
        try {
            User user = ctx.user;
            if (user == null) {
                observer.completeExceptionally(new RuntimeException("Authentication required"));
                return;
            }

            BookingDatabase database = getBookingDatabase();

            GSBooking booking = new GSBooking();

            // Map the fields from the new protobuf to the database schema
            if (request.hasProvider()) {
                booking.setProvider(request.getProvider());
            }
            if (request.hasSatelliteId()) {
                booking.setSatelliteId(request.getSatelliteId());
            }

            booking.setStartTime(toLocalDateTime(request.getStartTime()));

            // Use duration directly from request
            if (request.hasDurationMinutes()) {
                booking.setDurationMinutes(request.getDurationMinutes());
            } else {
                booking.setDurationMinutes(15); // default
            }

            if (request.hasPassType()) {
                booking.setPassType(request.getPassType());
            } else {
                booking.setPassType("both"); // default
            }

            booking.setPurpose(request.getPurpose());
            booking.setRuleType(request.getRuleType());

            if (request.hasFrequencyDays()) {
                booking.setFrequencyDays(request.getFrequencyDays());
            }
            if (request.hasNotes() && !request.getNotes().isEmpty()) {
                booking.setNotes(request.getNotes());
            }
            booking.setRequestedBy(user.getName());

            GSBooking createdBooking = database.createBooking(booking);
            observer.complete(toBookingInfo(createdBooking));

        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getPendingBookings(Context ctx, Empty request, Observer<GetBookingsResponse> observer) {
        try {
            BookingDatabase database = getBookingDatabase();
            List<GSBooking> bookings = database.getPendingBookings();

            GetBookingsResponse.Builder responseBuilder = GetBookingsResponse.newBuilder();
            for (GSBooking booking : bookings) {
                responseBuilder.addBookings(toBookingInfo(booking));
            }

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void approveBooking(Context ctx, ApprovalRequest request, Observer<BookingInfo> observer) {
        try {
            User user = ctx.user;
            if (user == null) {
                observer.completeExceptionally(new RuntimeException("Authentication required"));
                return;
            }

            BookingDatabase database = getBookingDatabase();
            boolean success = database.approveBooking(
                request.getId(),
                user.getName(),
                request.getComments()
            );

            if (success) {
                BookingInfo.Builder builder = BookingInfo.newBuilder()
                    .setId(request.getId())
                    .setStatus("approved");
                observer.complete(builder.build());
            } else {
                observer.completeExceptionally(new RuntimeException("Booking not found or already processed"));
            }

        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void rejectBooking(Context ctx, ApprovalRequest request, Observer<BookingInfo> observer) {
        try {
            User user = ctx.user;
            if (user == null) {
                observer.completeExceptionally(new RuntimeException("Authentication required"));
                return;
            }

            if (request.getComments() == null || request.getComments().trim().isEmpty()) {
                observer.completeExceptionally(new RuntimeException("Rejection reason is required"));
                return;
            }

            BookingDatabase database = getBookingDatabase();
            boolean success = database.rejectBooking(
                request.getId(),
                user.getName(),
                request.getComments()
            );

            if (success) {
                BookingInfo.Builder builder = BookingInfo.newBuilder()
                    .setId(request.getId())
                    .setStatus("rejected")
                    .setRejectionReason(request.getComments());
                observer.complete(builder.build());
            } else {
                observer.completeExceptionally(new RuntimeException("Booking not found or already processed"));
            }

        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    private BookingDatabase getBookingDatabase() throws SQLException {
        BookingService service = BookingService.getInstance();
        if (service == null) {
            throw new RuntimeException("BookingService not available");
        }
        BookingDatabase database = service.getDatabase();
        if (database == null) {
            throw new RuntimeException("Booking database not available. Please ensure PostgreSQL is running and configured properly.");
        }
        return database;
    }

    private ProviderInfo toProviderInfo(GSProvider provider) {
        ProviderInfo.Builder builder = ProviderInfo.newBuilder()
            .setId(provider.getId())
            .setName(provider.getName())
            .setType(provider.getType().toString())
            .setIsActive(provider.isActive());

        if (provider.getContactEmail() != null) {
            builder.setContactEmail(provider.getContactEmail());
        }
        if (provider.getContactPhone() != null) {
            builder.setContactPhone(provider.getContactPhone());
        }
        if (provider.getApiEndpoint() != null) {
            builder.setApiEndpoint(provider.getApiEndpoint());
        }
        if (provider.getCreatedAt() != null) {
            builder.setCreatedAt(toTimestamp(provider.getCreatedAt()));
        }
        if (provider.getUpdatedAt() != null) {
            builder.setUpdatedAt(toTimestamp(provider.getUpdatedAt()));
        }

        return builder.build();
    }

    private BookingInfo toBookingInfo(GSBooking booking) {
        BookingInfo.Builder builder = BookingInfo.newBuilder()
            .setId(booking.getId())
            .setProvider(booking.getProvider())
            .setSatelliteId(booking.getSatelliteId())
            .setStartTime(toTimestamp(booking.getStartTime()))
            .setEndTime(toTimestamp(booking.getEndTime()))
            .setDurationMinutes(booking.getDurationMinutes())
            .setPassType(booking.getPassType())
            .setPurpose(booking.getPurpose())
            .setRuleType(booking.getRuleType())
            .setStatus(booking.getStatus())
            .setGsStatus(booking.getGsStatus())
            .setRequestedBy(booking.getRequestedBy());

        if (booking.getCreatedAt() != null) {
            builder.setCreatedAt(toTimestamp(booking.getCreatedAt()));
        }
        if (booking.getUpdatedAt() != null) {
            builder.setUpdatedAt(toTimestamp(booking.getUpdatedAt()));
        }

        if (booking.getFrequencyDays() != null) {
            builder.setFrequencyDays(booking.getFrequencyDays());
        }
        if (booking.getApprovedBy() != null) {
            builder.setApprovedBy(booking.getApprovedBy());
        }
        if (booking.getApprovedAt() != null) {
            builder.setApprovedAt(toTimestamp(booking.getApprovedAt()));
        }
        if (booking.getRejectionReason() != null) {
            builder.setRejectionReason(booking.getRejectionReason());
        }
        if (booking.getNotes() != null) {
            builder.setNotes(booking.getNotes());
        }
        // Provider-specific fields
        if (booking.getProviderBookingId() != null) {
            builder.setProviderBookingId(booking.getProviderBookingId());
        }
        if (booking.getProviderSatelliteId() != null) {
            builder.setProviderSatelliteId(booking.getProviderSatelliteId());
        }
        if (booking.getMaxElevation() != null) {
            builder.setMaxElevation(booking.getMaxElevation());
        }

        return builder.build();
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder()
            .setSeconds(instant.getEpochSecond())
            .setNanos(instant.getNano())
            .build();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) return null;
        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    // ==================== Provider API Endpoints ====================

    @Override
    public void getProviderSatellites(Context ctx, GetProviderDataRequest request, Observer<GetSatellitesResponse> observer) {
        try {
            GsProviderClient client = getProviderClient(request.getProvider());

            List<ProviderSatellite> satellites = client.listSatellites();

            GetSatellitesResponse.Builder responseBuilder = GetSatellitesResponse.newBuilder();
            for (ProviderSatellite sat : satellites) {
                SatelliteInfo.Builder satBuilder = SatelliteInfo.newBuilder();
                if (sat.getId() != null) satBuilder.setId(sat.getId());
                if (sat.getName() != null) satBuilder.setName(sat.getName());
                if (sat.getNoradId() != null) satBuilder.setNoradId(sat.getNoradId());
                responseBuilder.addSatellites(satBuilder.build());
            }

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getProviderGroundStations(Context ctx, GetProviderDataRequest request, Observer<GetGroundStationsResponse> observer) {
        try {
            GsProviderClient client = getProviderClient(request.getProvider());

            List<ProviderGroundStation> groundStations = client.listGroundStations();

            GetGroundStationsResponse.Builder responseBuilder = GetGroundStationsResponse.newBuilder();
            for (ProviderGroundStation gs : groundStations) {
                GroundStationInfo.Builder gsBuilder = GroundStationInfo.newBuilder();
                if (gs.getId() != null) gsBuilder.setId(gs.getId());
                if (gs.getName() != null) gsBuilder.setName(gs.getName());
                if (gs.getCity() != null) gsBuilder.setCity(gs.getCity());
                if (gs.getState() != null) gsBuilder.setState(gs.getState());
                if (gs.getCountry() != null) gsBuilder.setCountry(gs.getCountry());
                gsBuilder.setLatitude(gs.getLatitude());
                gsBuilder.setLongitude(gs.getLongitude());
                responseBuilder.addGroundStations(gsBuilder.build());
            }

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getActivityScopes(Context ctx, GetActivityScopeRequest request, Observer<GetActivityScopesResponse> observer) {
        try {
            GsProviderClient client = getProviderClient(request.getProvider());

            List<ProviderActivityScope> scopes = client.listActivityScopes(request.getSatelliteId());

            GetActivityScopesResponse.Builder responseBuilder = GetActivityScopesResponse.newBuilder();
            for (ProviderActivityScope scope : scopes) {
                ActivityScopeInfo.Builder scopeBuilder = ActivityScopeInfo.newBuilder();
                if (scope.getGsabracId() != null) scopeBuilder.setGsabracId(scope.getGsabracId());
                if (scope.getSpbasId() != null) scopeBuilder.setSpbasId(scope.getSpbasId());
                if (scope.getSatelliteId() != null) scopeBuilder.setSatelliteId(scope.getSatelliteId());
                if (scope.getActivityScope() != null) scopeBuilder.setActivityScope(scope.getActivityScope());
                if (scope.getTaskName() != null) scopeBuilder.setTaskName(scope.getTaskName());
                if (scope.getCommunicationBand() != null) scopeBuilder.setCommunicationBand(scope.getCommunicationBand());
                responseBuilder.addActivityScopes(scopeBuilder.build());
            }

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getProviderContacts(Context ctx, GetContactsRequest request, Observer<GetContactsResponse> observer) {
        try {
            GsProviderClient client = getProviderClient(request.getProvider());

            LocalDate startDate = LocalDate.parse(request.getStartDate());
            LocalDate endDate = LocalDate.parse(request.getEndDate());

            List<ProviderContact> contacts = client.listContacts(
                    request.getGsId(),
                    request.getSatelliteId(),
                    request.getSpbasId(),
                    startDate,
                    endDate
            );

            GetContactsResponse.Builder responseBuilder = GetContactsResponse.newBuilder();
            for (ProviderContact contact : contacts) {
                ContactInfo.Builder contactBuilder = ContactInfo.newBuilder();
                if (contact.getGsVisibilityId() != null) contactBuilder.setGsVisibilityId(contact.getGsVisibilityId());
                if (contact.getGsId() != null) contactBuilder.setGsId(contact.getGsId());
                if (contact.getGroundStationName() != null) contactBuilder.setGroundStationName(contact.getGroundStationName());
                if (contact.getSatelliteId() != null) contactBuilder.setSatelliteId(contact.getSatelliteId());
                if (contact.getPassStart() != null) contactBuilder.setPassStart(toTimestamp(contact.getPassStart()));
                if (contact.getPassEnd() != null) contactBuilder.setPassEnd(toTimestamp(contact.getPassEnd()));
                contactBuilder.setMaxElevation(contact.getMaxElevation());
                if (contact.getStatus() != null) contactBuilder.setStatus(contact.getStatus());
                contactBuilder.setDurationSeconds(contact.getDurationSeconds());
                responseBuilder.addContacts(contactBuilder.build());
            }

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void reserveContact(Context ctx, ReserveContactRequest request, Observer<ProviderBookingInfo> observer) {
        try {
            User user = ctx.user;
            if (user == null) {
                observer.completeExceptionally(new RuntimeException("Authentication required"));
                return;
            }

            GsProviderClient client = getProviderClient(request.getProvider());

            ProviderBooking booking = client.reserveContact(
                    request.getGsId(),
                    request.getSatelliteId(),
                    request.getGsVisibilityId(),
                    request.getGsabracId()
            );

            // Store booking in our database for command triggering process
            storeProviderBooking(booking, request, user.getName());

            ProviderBookingInfo.Builder builder = ProviderBookingInfo.newBuilder();
            if (booking.getSatellitePassBookingId() != null) builder.setSatellitePassBookingId(booking.getSatellitePassBookingId());
            if (booking.getGsId() != null) builder.setGsId(booking.getGsId());
            if (booking.getGroundStationName() != null) builder.setGroundStationName(booking.getGroundStationName());
            if (booking.getGsVisibilityId() != null) builder.setGsVisibilityId(booking.getGsVisibilityId());
            builder.setNoradId(booking.getNoradId());
            if (booking.getStartDateTime() != null) builder.setStartDateTime(toTimestamp(booking.getStartDateTime()));
            if (booking.getEndDateTime() != null) builder.setEndDateTime(toTimestamp(booking.getEndDateTime()));
            if (booking.getStatus() != null) builder.setStatus(booking.getStatus());
            builder.setMaxElevation(booking.getMaxElevation());

            observer.complete(builder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void cancelProviderBooking(Context ctx, CancelBookingRequest request, Observer<CancelBookingResponse> observer) {
        try {
            GsProviderClient client = getProviderClient(request.getProvider());

            boolean success = client.cancelReservation(request.getSatellitePassBookingId());

            CancelBookingResponse.Builder builder = CancelBookingResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ? "Booking cancelled successfully" : "Failed to cancel booking");

            observer.complete(builder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getProviderBookings(Context ctx, GetProviderDataRequest request, Observer<GetProviderBookingsResponse> observer) {
        try {
            GsProviderClient client = getProviderClient(request.getProvider());

            List<ProviderBooking> bookings = client.listBookings();

            GetProviderBookingsResponse.Builder responseBuilder = GetProviderBookingsResponse.newBuilder();
            for (ProviderBooking booking : bookings) {
                ProviderBookingInfo.Builder bookingBuilder = ProviderBookingInfo.newBuilder();
                if (booking.getSatellitePassBookingId() != null) bookingBuilder.setSatellitePassBookingId(booking.getSatellitePassBookingId());
                if (booking.getGsId() != null) bookingBuilder.setGsId(booking.getGsId());
                if (booking.getGroundStationName() != null) bookingBuilder.setGroundStationName(booking.getGroundStationName());
                if (booking.getGsVisibilityId() != null) bookingBuilder.setGsVisibilityId(booking.getGsVisibilityId());
                bookingBuilder.setNoradId(booking.getNoradId());
                if (booking.getStartDateTime() != null) bookingBuilder.setStartDateTime(toTimestamp(booking.getStartDateTime()));
                if (booking.getEndDateTime() != null) bookingBuilder.setEndDateTime(toTimestamp(booking.getEndDateTime()));
                if (booking.getStatus() != null) bookingBuilder.setStatus(booking.getStatus());
                bookingBuilder.setMaxElevation(booking.getMaxElevation());
                responseBuilder.addBookings(bookingBuilder.build());
            }

            observer.complete(responseBuilder.build());
        } catch (Exception e) {
            observer.completeExceptionally(e);
        }
    }

    // ==================== Helper Methods ====================

    private GsProviderClient getProviderClient(String provider) throws Exception {
        if (!GsProviderFactory.isSupported(provider)) {
            throw new RuntimeException("Provider not supported: " + provider);
        }

        GsProviderClient client = GsProviderFactory.getClient(provider);
        if (client == null) {
            throw new RuntimeException("Failed to get provider client for: " + provider);
        }

        return client;
    }

    private Timestamp toTimestamp(Instant instant) {
        if (instant == null) return null;
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private void storeProviderBooking(ProviderBooking booking, ReserveContactRequest request, String requestedBy) {
        try {
            BookingDatabase database = getBookingDatabase();

            GSBooking gsBooking = new GSBooking();
            gsBooking.setProvider(request.getProvider());
            // Use the human-readable satellite name, fallback to ID if name not provided
            gsBooking.setSatelliteId(request.hasSatelliteName() && !request.getSatelliteName().isEmpty()
                    ? request.getSatelliteName()
                    : (booking.getSatelliteId() != null ? booking.getSatelliteId() : request.getSatelliteId()));

            // Timing - critical for triggering telecommands
            if (booking.getStartDateTime() != null) {
                gsBooking.setStartTime(LocalDateTime.ofInstant(booking.getStartDateTime(), ZoneOffset.UTC));
            }
            if (booking.getEndDateTime() != null) {
                gsBooking.setEndTime(LocalDateTime.ofInstant(booking.getEndDateTime(), ZoneOffset.UTC));
            }

            // Calculate duration
            if (booking.getStartDateTime() != null && booking.getEndDateTime() != null) {
                long durationMinutes = java.time.Duration.between(booking.getStartDateTime(), booking.getEndDateTime()).toMinutes();
                gsBooking.setDurationMinutes((int) durationMinutes);
            }

            gsBooking.setPurpose(request.hasPurpose() ? request.getPurpose() : "telemetry");
            gsBooking.setPassType("both");
            gsBooking.setRuleType("one_time");
            gsBooking.setNotes(request.hasNotes() ? request.getNotes() : null);
            gsBooking.setRequestedBy(requestedBy);
            gsBooking.setStatus("approved"); // Auto-approve provider bookings
            gsBooking.setGsStatus("confirmed");

            // Store generic provider IDs (works for any provider)
            gsBooking.setProviderSatelliteId(booking.getSatelliteId());
            gsBooking.setProviderGsId(booking.getGsId());
            gsBooking.setProviderContactId(booking.getGsVisibilityId());
            gsBooking.setProviderBookingId(booking.getSatellitePassBookingId());
            gsBooking.setMaxElevation(booking.getMaxElevation());

            // Store ALL provider-specific metadata as JSON for telecommand triggering
            StringBuilder metadata = new StringBuilder("{");
            metadata.append("\"noradId\":").append(booking.getNoradId());
            metadata.append(",\"satelliteName\":\"").append(nullSafe(request.getSatelliteName())).append("\"");
            metadata.append(",\"groundStationName\":\"").append(nullSafe(booking.getGroundStationName())).append("\"");
            metadata.append(",\"gsapName\":\"").append(nullSafe(booking.getGsapName())).append("\"");
            metadata.append(",\"activityScope\":\"").append(nullSafe(booking.getActivityScope())).append("\"");
            metadata.append(",\"gsabracId\":\"").append(nullSafe(request.getGsabracId())).append("\"");
            metadata.append(",\"spbasId\":\"").append(nullSafe(booking.getSpbasId())).append("\"");
            metadata.append(",\"gsaId\":\"").append(nullSafe(booking.getGsaId())).append("\"");
            metadata.append(",\"gsapId\":\"").append(nullSafe(booking.getGsapId())).append("\"");

            // Timing data in multiple formats for flexibility
            if (booking.getBookingStartEpochMs() > 0) {
                metadata.append(",\"bookingStartEpochMs\":").append(booking.getBookingStartEpochMs());
            }
            if (booking.getBookingEndEpochMs() > 0) {
                metadata.append(",\"bookingEndEpochMs\":").append(booking.getBookingEndEpochMs());
            }
            if (booking.getRecordingStartTime() != null) {
                metadata.append(",\"recordingStartTime\":\"").append(booking.getRecordingStartTime()).append("\"");
            }
            if (booking.getRecordingEndTime() != null) {
                metadata.append(",\"recordingEndTime\":\"").append(booking.getRecordingEndTime()).append("\"");
            }
            metadata.append("}");

            gsBooking.setProviderMetadata(metadata.toString());

            database.createBooking(gsBooking);
            System.out.println("Stored booking in database: " + booking.getSatellitePassBookingId() +
                    " from " + booking.getStartDateTime() + " to " + booking.getEndDateTime());

        } catch (Exception e) {
            // Log but don't fail the reservation
            System.err.println("Failed to store booking in database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}