package org.yamcs.http.api;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(BookingApi.class);

    @Override
    public void getEnumValues(Context ctx, Empty request, Observer<GetEnumValuesResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getEnumValues - Request received from user: {}", user);

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

            GetEnumValuesResponse response = responseBuilder.build();
            log.info("[API] getEnumValues - Response: providerTypes={}, ruleTypes={}, statusTypes={}, passTypes={}, purposeTypes={}, gsStatusTypes={}",
                    response.getProviderTypesCount(), response.getRuleTypesCount(), response.getStatusTypesCount(),
                    response.getPassTypesCount(), response.getPurposeTypesCount(), response.getGsStatusTypesCount());

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getEnumValues - Error: {}", e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getProviders(Context ctx, Empty request, Observer<GetProvidersResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getProviders - Request received from user: {}", user);

        try {
            BookingDatabase database = getBookingDatabase();
            List<GSProvider> providers = database.getAllProviders();

            GetProvidersResponse.Builder responseBuilder = GetProvidersResponse.newBuilder();
            for (GSProvider provider : providers) {
                responseBuilder.addProviders(toProviderInfo(provider));
            }

            GetProvidersResponse response = responseBuilder.build();
            log.info("[API] getProviders - Response: {} providers returned", response.getProvidersCount());
            if (log.isDebugEnabled()) {
                for (ProviderInfo p : response.getProvidersList()) {
                    log.debug("[API] getProviders - Provider: id={}, name={}, type={}, active={}",
                            p.getId(), p.getName(), p.getType(), p.getIsActive());
                }
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getProviders - Error: {}", e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getBookings(Context ctx, Empty request, Observer<GetBookingsResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getBookings - Request received from user: {}", user);

        try {
            BookingDatabase database = getBookingDatabase();
            List<GSBooking> bookings = database.getAllBookings();

            GetBookingsResponse.Builder responseBuilder = GetBookingsResponse.newBuilder();
            for (GSBooking booking : bookings) {
                responseBuilder.addBookings(toBookingInfo(booking));
            }

            GetBookingsResponse response = responseBuilder.build();
            log.info("[API] getBookings - Response: {} bookings returned", response.getBookingsCount());
            if (log.isDebugEnabled()) {
                for (BookingInfo b : response.getBookingsList()) {
                    log.debug("[API] getBookings - Booking: id={}, provider={}, satellite={}, status={}, gsStatus={}",
                            b.getId(), b.getProvider(), b.getSatelliteId(), b.getStatus(), b.getGsStatus());
                }
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getBookings - Error: {}", e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void createBooking(Context ctx, CreateBookingRequest request, Observer<BookingInfo> observer) {
        String userName = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] createBooking - Request received from user: {}", userName);
        log.info("[API] createBooking - Request payload: provider={}, satelliteId={}, startTime={}, durationMinutes={}, passType={}, purpose={}, ruleType={}, frequencyDays={}, notes={}",
                request.hasProvider() ? request.getProvider() : "N/A",
                request.hasSatelliteId() ? request.getSatelliteId() : "N/A",
                request.hasStartTime() ? request.getStartTime() : "N/A",
                request.hasDurationMinutes() ? request.getDurationMinutes() : "default(15)",
                request.hasPassType() ? request.getPassType() : "default(both)",
                request.getPurpose(),
                request.getRuleType(),
                request.hasFrequencyDays() ? request.getFrequencyDays() : "N/A",
                request.hasNotes() ? request.getNotes() : "N/A");

        try {
            User user = ctx.user;
            if (user == null) {
                log.warn("[API] createBooking - Authentication required, no user in context");
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
            BookingInfo response = toBookingInfo(createdBooking);

            log.info("[API] createBooking - Success: bookingId={}, provider={}, satellite={}, startTime={}, endTime={}, status={}",
                    response.getId(), response.getProvider(), response.getSatelliteId(),
                    response.getStartTime(), response.getEndTime(), response.getStatus());

            observer.complete(response);

        } catch (Exception e) {
            log.error("[API] createBooking - Error: {}", e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getPendingBookings(Context ctx, Empty request, Observer<GetBookingsResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getPendingBookings - Request received from user: {}", user);

        try {
            BookingDatabase database = getBookingDatabase();
            List<GSBooking> bookings = database.getPendingBookings();

            GetBookingsResponse.Builder responseBuilder = GetBookingsResponse.newBuilder();
            for (GSBooking booking : bookings) {
                responseBuilder.addBookings(toBookingInfo(booking));
            }

            GetBookingsResponse response = responseBuilder.build();
            log.info("[API] getPendingBookings - Response: {} pending bookings returned", response.getBookingsCount());
            if (log.isDebugEnabled()) {
                for (BookingInfo b : response.getBookingsList()) {
                    log.debug("[API] getPendingBookings - Pending booking: id={}, provider={}, satellite={}, requestedBy={}",
                            b.getId(), b.getProvider(), b.getSatelliteId(), b.getRequestedBy());
                }
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getPendingBookings - Error: {}", e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void approveBooking(Context ctx, ApprovalRequest request, Observer<BookingInfo> observer) {
        String userName = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] approveBooking - Request received from user: {}", userName);
        log.info("[API] approveBooking - Request payload: bookingId={}, comments={}",
                request.getId(), request.hasComments() ? request.getComments() : "N/A");

        try {
            User user = ctx.user;
            if (user == null) {
                log.warn("[API] approveBooking - Authentication required, no user in context");
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
                BookingInfo response = builder.build();
                log.info("[API] approveBooking - Success: bookingId={} approved by {}", request.getId(), user.getName());
                observer.complete(response);
            } else {
                log.warn("[API] approveBooking - Failed: bookingId={} not found or already processed", request.getId());
                observer.completeExceptionally(new RuntimeException("Booking not found or already processed"));
            }

        } catch (Exception e) {
            log.error("[API] approveBooking - Error for bookingId={}: {}", request.getId(), e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void rejectBooking(Context ctx, ApprovalRequest request, Observer<BookingInfo> observer) {
        String userName = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] rejectBooking - Request received from user: {}", userName);
        log.info("[API] rejectBooking - Request payload: bookingId={}, reason={}",
                request.getId(), request.hasComments() ? request.getComments() : "N/A");

        try {
            User user = ctx.user;
            if (user == null) {
                log.warn("[API] rejectBooking - Authentication required, no user in context");
                observer.completeExceptionally(new RuntimeException("Authentication required"));
                return;
            }

            if (request.getComments() == null || request.getComments().trim().isEmpty()) {
                log.warn("[API] rejectBooking - Rejection reason is required for bookingId={}", request.getId());
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
                BookingInfo response = builder.build();
                log.info("[API] rejectBooking - Success: bookingId={} rejected by {} with reason: {}",
                        request.getId(), user.getName(), request.getComments());
                observer.complete(response);
            } else {
                log.warn("[API] rejectBooking - Failed: bookingId={} not found or already processed", request.getId());
                observer.completeExceptionally(new RuntimeException("Booking not found or already processed"));
            }

        } catch (Exception e) {
            log.error("[API] rejectBooking - Error for bookingId={}: {}", request.getId(), e.getMessage(), e);
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
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getProviderSatellites - Request received from user: {}", user);
        log.info("[API] getProviderSatellites - Request payload: provider={}", request.getProvider());

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

            GetSatellitesResponse response = responseBuilder.build();
            log.info("[API] getProviderSatellites - Response: {} satellites returned for provider={}",
                    response.getSatellitesCount(), request.getProvider());
            if (log.isDebugEnabled()) {
                for (SatelliteInfo sat : response.getSatellitesList()) {
                    log.debug("[API] getProviderSatellites - Satellite: id={}, name={}, noradId={}",
                            sat.getId(), sat.getName(), sat.getNoradId());
                }
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getProviderSatellites - Error for provider={}: {}", request.getProvider(), e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getProviderGroundStations(Context ctx, GetProviderDataRequest request, Observer<GetGroundStationsResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getProviderGroundStations - Request received from user: {}", user);
        log.info("[API] getProviderGroundStations - Request payload: provider={}", request.getProvider());

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

            GetGroundStationsResponse response = responseBuilder.build();
            log.info("[API] getProviderGroundStations - Response: {} ground stations returned for provider={}",
                    response.getGroundStationsCount(), request.getProvider());
            if (log.isDebugEnabled()) {
                for (GroundStationInfo gs : response.getGroundStationsList()) {
                    log.debug("[API] getProviderGroundStations - GroundStation: id={}, name={}, city={}, country={}",
                            gs.getId(), gs.getName(), gs.getCity(), gs.getCountry());
                }
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getProviderGroundStations - Error for provider={}: {}", request.getProvider(), e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getActivityScopes(Context ctx, GetActivityScopeRequest request, Observer<GetActivityScopesResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getActivityScopes - Request received from user: {}", user);
        log.info("[API] getActivityScopes - Request payload: provider={}, satelliteId={}",
                request.getProvider(), request.getSatelliteId());

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

            GetActivityScopesResponse response = responseBuilder.build();
            log.info("[API] getActivityScopes - Response: {} activity scopes returned for provider={}, satelliteId={}",
                    response.getActivityScopesCount(), request.getProvider(), request.getSatelliteId());
            if (log.isDebugEnabled()) {
                for (ActivityScopeInfo scope : response.getActivityScopesList()) {
                    log.debug("[API] getActivityScopes - Scope: gsabracId={}, activityScope={}, taskName={}, band={}",
                            scope.getGsabracId(), scope.getActivityScope(), scope.getTaskName(), scope.getCommunicationBand());
                }
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getActivityScopes - Error for provider={}, satelliteId={}: {}",
                    request.getProvider(), request.getSatelliteId(), e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getProviderContacts(Context ctx, GetContactsRequest request, Observer<GetContactsResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getProviderContacts - Request received from user: {}", user);
        log.info("[API] getProviderContacts - Request payload: provider={}, gsId={}, satelliteId={}, spbasId={}, startDate={}, endDate={}",
                request.getProvider(), request.getGsId(), request.getSatelliteId(),
                request.getSpbasId(), request.getStartDate(), request.getEndDate());

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

            GetContactsResponse response = responseBuilder.build();
            log.info("[API] getProviderContacts - Response: {} contacts returned for provider={}, gsId={}, satelliteId={}, dateRange={} to {}",
                    response.getContactsCount(), request.getProvider(), request.getGsId(),
                    request.getSatelliteId(), request.getStartDate(), request.getEndDate());
            if (log.isDebugEnabled()) {
                for (ContactInfo contact : response.getContactsList()) {
                    log.debug("[API] getProviderContacts - Contact: visibilityId={}, gsName={}, passStart={}, passEnd={}, maxElev={}, status={}",
                            contact.getGsVisibilityId(), contact.getGroundStationName(),
                            contact.getPassStart(), contact.getPassEnd(), contact.getMaxElevation(), contact.getStatus());
                }
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getProviderContacts - Error for provider={}, gsId={}, satelliteId={}: {}",
                    request.getProvider(), request.getGsId(), request.getSatelliteId(), e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void reserveContact(Context ctx, ReserveContactRequest request, Observer<ProviderBookingInfo> observer) {
        String userName = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] reserveContact - Request received from user: {}", userName);
        log.info("[API] reserveContact - Request payload: provider={}, gsId={}, satelliteId={}, gsVisibilityId={}, gsabracId={}, satelliteName={}, purpose={}, notes={}",
                request.getProvider(), request.getGsId(), request.getSatelliteId(),
                request.getGsVisibilityId(), request.getGsabracId(),
                request.hasSatelliteName() ? request.getSatelliteName() : "N/A",
                request.hasPurpose() ? request.getPurpose() : "N/A",
                request.hasNotes() ? request.getNotes() : "N/A");

        try {
            User user = ctx.user;
            if (user == null) {
                log.warn("[API] reserveContact - Authentication required, no user in context");
                observer.completeExceptionally(new RuntimeException("Authentication required"));
                return;
            }

            GsProviderClient client = getProviderClient(request.getProvider());

            log.info("[API] reserveContact - Calling provider API to reserve contact for provider={}, gsVisibilityId={}",
                    request.getProvider(), request.getGsVisibilityId());

            ProviderBooking booking = client.reserveContact(
                    request.getGsId(),
                    request.getSatelliteId(),
                    request.getGsVisibilityId(),
                    request.getGsabracId()
            );

            log.info("[API] reserveContact - Provider API response: bookingId={}, gsName={}, startTime={}, endTime={}, status={}",
                    booking.getSatellitePassBookingId(), booking.getGroundStationName(),
                    booking.getStartDateTime(), booking.getEndDateTime(), booking.getStatus());

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

            ProviderBookingInfo response = builder.build();
            log.info("[API] reserveContact - Success: providerBookingId={}, gsName={}, noradId={}, maxElevation={}, status={}",
                    response.getSatellitePassBookingId(), response.getGroundStationName(),
                    response.getNoradId(), response.getMaxElevation(), response.getStatus());

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] reserveContact - Error for provider={}, gsVisibilityId={}: {}",
                    request.getProvider(), request.getGsVisibilityId(), e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void cancelProviderBooking(Context ctx, CancelBookingRequest request, Observer<CancelBookingResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] cancelProviderBooking - Request received from user: {}", user);
        log.info("[API] cancelProviderBooking - Request payload: provider={}, satellitePassBookingId={}",
                request.getProvider(), request.getSatellitePassBookingId());

        try {
            GsProviderClient client = getProviderClient(request.getProvider());

            log.info("[API] cancelProviderBooking - Calling provider API to cancel booking for provider={}, bookingId={}",
                    request.getProvider(), request.getSatellitePassBookingId());

            boolean success = client.cancelReservation(request.getSatellitePassBookingId());

            CancelBookingResponse.Builder builder = CancelBookingResponse.newBuilder()
                    .setSuccess(success)
                    .setMessage(success ? "Booking cancelled successfully" : "Failed to cancel booking");

            CancelBookingResponse response = builder.build();
            if (success) {
                log.info("[API] cancelProviderBooking - Success: provider={}, bookingId={} cancelled successfully",
                        request.getProvider(), request.getSatellitePassBookingId());
            } else {
                log.warn("[API] cancelProviderBooking - Failed: provider={}, bookingId={} cancellation failed",
                        request.getProvider(), request.getSatellitePassBookingId());
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] cancelProviderBooking - Error for provider={}, bookingId={}: {}",
                    request.getProvider(), request.getSatellitePassBookingId(), e.getMessage(), e);
            observer.completeExceptionally(e);
        }
    }

    @Override
    public void getProviderBookings(Context ctx, GetProviderDataRequest request, Observer<GetProviderBookingsResponse> observer) {
        String user = ctx.user != null ? ctx.user.getName() : "anonymous";
        log.info("[API] getProviderBookings - Request received from user: {}", user);
        log.info("[API] getProviderBookings - Request payload: provider={}", request.getProvider());

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

            GetProviderBookingsResponse response = responseBuilder.build();
            log.info("[API] getProviderBookings - Response: {} bookings returned for provider={}",
                    response.getBookingsCount(), request.getProvider());
            if (log.isDebugEnabled()) {
                for (ProviderBookingInfo b : response.getBookingsList()) {
                    log.debug("[API] getProviderBookings - Booking: id={}, gsName={}, noradId={}, startTime={}, status={}",
                            b.getSatellitePassBookingId(), b.getGroundStationName(),
                            b.getNoradId(), b.getStartDateTime(), b.getStatus());
                }
            }

            observer.complete(response);
        } catch (Exception e) {
            log.error("[API] getProviderBookings - Error for provider={}: {}", request.getProvider(), e.getMessage(), e);
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

            GSBooking createdBooking = database.createBooking(gsBooking);
            log.info("[API] storeProviderBooking - Booking stored in database: dbBookingId={}, providerBookingId={}, provider={}, satellite={}, startTime={}, endTime={}",
                    createdBooking.getId(), booking.getSatellitePassBookingId(), request.getProvider(),
                    gsBooking.getSatelliteId(), booking.getStartDateTime(), booking.getEndDateTime());

        } catch (Exception e) {
            // Log but don't fail the reservation
            log.error("[API] storeProviderBooking - Failed to store booking in database for providerBookingId={}: {}",
                    booking.getSatellitePassBookingId(), e.getMessage(), e);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}