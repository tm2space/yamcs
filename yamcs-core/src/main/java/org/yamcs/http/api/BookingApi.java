package org.yamcs.http.api;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import org.yamcs.api.Observer;
import org.yamcs.booking.BookingService;
import org.yamcs.booking.db.BookingDatabase;
import org.yamcs.booking.model.*;
import org.yamcs.http.Context;
import org.yamcs.protobuf.*;
import org.yamcs.security.User;

import java.sql.SQLException;
import java.time.Instant;
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
        return service.getDatabase();
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
}