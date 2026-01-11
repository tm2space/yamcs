package org.yamcs.booking.provider;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ISOCS-GSaaS provider client implementation for Dhruva Space
 */
public class IsocsProviderClient implements GsProviderClient {

    private static final Logger log = LoggerFactory.getLogger(IsocsProviderClient.class);
    private static final Gson gson = new Gson();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final String baseUrl;
    private final String email;
    private final String password;
    private final HttpClient httpClient;

    private String accessToken;
    private long tokenExpiry;

    public IsocsProviderClient(String baseUrl, String email, String password) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.email = email;
        this.password = password;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String getProviderType() {
        return "dhruva";
    }

    @Override
    public synchronized void connect() throws IOException, InterruptedException {
        log.info("Logging in to ISOCS API at {}", baseUrl);

        JsonObject loginBody = new JsonObject();
        loginBody.addProperty("sm_email", email);
        loginBody.addProperty("sm_password", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/userservice/dev/api/v1/user/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(loginBody)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("ISOCS login failed: " + response.statusCode() + " - " + response.body());
        }

        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
        this.accessToken = responseBody.get("accessToken").getAsString();
        this.tokenExpiry = System.currentTimeMillis() + (23 * 60 * 60 * 1000); // 23 hours

        log.info("Successfully logged in to ISOCS API");
    }

    private synchronized void ensureAuthenticated() throws IOException, InterruptedException {
        if (accessToken == null || System.currentTimeMillis() > tokenExpiry) {
            connect();
        }
    }

    @Override
    public List<ProviderSatellite> listSatellites() throws IOException, InterruptedException {
        ensureAuthenticated();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/centralsatellite/dev/portal/api/v1/central/gsaas/satellites"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to list satellites: " + response.statusCode() + " - " + response.body());
        }

        List<ProviderSatellite> satellites = new ArrayList<>();
        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray results = responseBody.getAsJsonObject("data").getAsJsonArray("result");

        for (int i = 0; i < results.size(); i++) {
            JsonObject sat = results.get(i).getAsJsonObject();
            ProviderSatellite satellite = new ProviderSatellite();
            satellite.setId(sat.get("satellite_id").getAsString());
            satellite.setName(sat.has("satellite_name") ? sat.get("satellite_name").getAsString() : null);
            satellite.setNoradId(sat.has("norad_id") ? String.valueOf(sat.get("norad_id").getAsInt()) : null);
            satellites.add(satellite);
        }

        return satellites;
    }

    @Override
    public List<ProviderGroundStation> listGroundStations() throws IOException, InterruptedException {
        ensureAuthenticated();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/centralgroundstation/dev/portal/api/v1/central/gsaas/groundstations"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to list ground stations: " + response.statusCode() + " - " + response.body());
        }

        List<ProviderGroundStation> groundStations = new ArrayList<>();
        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray data = responseBody.getAsJsonArray("data");

        for (int i = 0; i < data.size(); i++) {
            JsonObject gs = data.get(i).getAsJsonObject();
            ProviderGroundStation groundStation = new ProviderGroundStation();
            groundStation.setId(gs.get("gs_id").getAsString());
            groundStation.setName(gs.has("ground_station_name") ? gs.get("ground_station_name").getAsString() : null);
            groundStation.setCity(gs.has("city") ? gs.get("city").getAsString() : null);
            groundStation.setState(gs.has("state_name") ? gs.get("state_name").getAsString() : null);
            groundStation.setCountry(gs.has("country_name") ? gs.get("country_name").getAsString() : null);
            groundStation.setLatitude(gs.has("latitude") ? Double.parseDouble(gs.get("latitude").getAsString()) : 0);
            groundStation.setLongitude(gs.has("longitude") ? Double.parseDouble(gs.get("longitude").getAsString()) : 0);
            groundStations.add(groundStation);
        }

        return groundStations;
    }

    @Override
    public List<ProviderActivityScope> listActivityScopes(String satelliteId) throws IOException, InterruptedException {
        ensureAuthenticated();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/centralgroundstation/dev/portal/api/v1/central/gsaas/activityscopes/" + satelliteId))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to get activity scopes: " + response.statusCode() + " - " + response.body());
        }

        List<ProviderActivityScope> activityScopes = new ArrayList<>();
        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray data = responseBody.getAsJsonArray("data");

        for (int i = 0; i < data.size(); i++) {
            JsonObject scope = data.get(i).getAsJsonObject();
            ProviderActivityScope activityScope = new ProviderActivityScope();
            activityScope.setGsabracId(scope.get("gsabrac_id").getAsString());
            activityScope.setSpbasId(scope.get("spbas_id").getAsString());
            activityScope.setSatelliteId(scope.get("satellite_id").getAsString());
            activityScope.setActivityScope(scope.has("activity_scope") ? scope.get("activity_scope").getAsString() : null);
            activityScope.setTaskName(scope.has("task_name") ? scope.get("task_name").getAsString() : null);
            activityScope.setCommunicationBand(scope.has("communication_band_name") ? scope.get("communication_band_name").getAsString() : null);
            activityScopes.add(activityScope);
        }

        return activityScopes;
    }

    @Override
    public List<ProviderContact> listContacts(String gsId, String satelliteId, String spbasId,
                                               LocalDate startDate, LocalDate endDate) throws IOException, InterruptedException {
        ensureAuthenticated();

        String url = String.format("%s/centralgroundstation/dev/portal/api/v1/central/visibility/%s/%s/%s?start=%s&end=%s",
                baseUrl, gsId, satelliteId, spbasId,
                startDate.format(DATE_FORMAT), endDate.format(DATE_FORMAT));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to list contacts: " + response.statusCode() + " - " + response.body());
        }

        List<ProviderContact> contacts = new ArrayList<>();
        Set<String> seenIds = new HashSet<>(); // To deduplicate

        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray data = responseBody.getAsJsonArray("data");

        for (int i = 0; i < data.size(); i++) {
            JsonObject contact = data.get(i).getAsJsonObject();
            String visibilityId = contact.get("gs_visibility_id").getAsString();

            // Skip duplicates
            if (seenIds.contains(visibilityId)) {
                continue;
            }
            seenIds.add(visibilityId);

            ProviderContact c = new ProviderContact();
            c.setGsVisibilityId(visibilityId);
            c.setGsId(contact.get("gs_id").getAsString());
            c.setGroundStationName(contact.has("ground_station_name") ? contact.get("ground_station_name").getAsString() : null);
            c.setSatelliteId(contact.get("satellite_id").getAsString());

            if (contact.has("pass_start")) {
                c.setPassStart(Instant.parse(contact.get("pass_start").getAsString()));
            }
            if (contact.has("pass_end")) {
                c.setPassEnd(Instant.parse(contact.get("pass_end").getAsString()));
            }
            if (contact.has("maxel_elevation")) {
                c.setMaxElevation(Double.parseDouble(contact.get("maxel_elevation").getAsString()));
            }
            c.setStatus(contact.has("is_active_status") ? contact.get("is_active_status").getAsString() : null);

            // Calculate duration
            if (c.getPassStart() != null && c.getPassEnd() != null) {
                c.setDurationSeconds((int) Duration.between(c.getPassStart(), c.getPassEnd()).getSeconds());
            }

            contacts.add(c);
        }

        return contacts;
    }

    @Override
    public ProviderBooking reserveContact(String gsId, String satelliteId, String gsVisibilityId, String gsabracId)
            throws IOException, InterruptedException {
        ensureAuthenticated();

        JsonObject bookingBody = new JsonObject();
        bookingBody.addProperty("gs_id", gsId);
        bookingBody.addProperty("satellite_id", satelliteId);
        bookingBody.addProperty("gs_visibility_id", gsVisibilityId);
        bookingBody.addProperty("gsabrac_id", gsabracId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/centralgroundstation/dev/portal/api/v1/central/bookings"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(bookingBody)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new IOException("Failed to reserve contact: " + response.statusCode() + " - " + response.body());
        }

        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
        ProviderBooking booking = new ProviderBooking();

        if (responseBody.has("data")) {
            JsonObject data = responseBody.getAsJsonObject("data");

            // Core identifiers
            booking.setSatellitePassBookingId(getStringOrNull(data, "satellite_pass_booking_id"));
            booking.setGsId(getStringOrNull(data, "gs_id"));
            booking.setSatelliteId(getStringOrNull(data, "satellite_id"));
            booking.setGsVisibilityId(getStringOrNull(data, "gs_visibility_id"));

            // Ground station info
            booking.setGroundStationName(getStringOrNull(data, "ground_station_name"));
            booking.setGsapName(getStringOrNull(data, "gsap_name"));

            // Satellite info
            if (data.has("norad_id")) {
                booking.setNoradId(data.get("norad_id").getAsInt());
            }

            // Timing - critical for triggering telecommands
            if (data.has("start_date_time")) {
                booking.setStartDateTime(Instant.parse(data.get("start_date_time").getAsString()));
            }
            if (data.has("end_date_time")) {
                booking.setEndDateTime(Instant.parse(data.get("end_date_time").getAsString()));
            }
            if (data.has("booking_start_date_time")) {
                booking.setBookingStartEpochMs(Long.parseLong(data.get("booking_start_date_time").getAsString()));
            }
            if (data.has("booking_end_date_time")) {
                booking.setBookingEndEpochMs(Long.parseLong(data.get("booking_end_date_time").getAsString()));
            }
            if (data.has("recording_start_date_time")) {
                booking.setRecordingStartTime(Instant.parse(data.get("recording_start_date_time").getAsString()));
            }
            if (data.has("recording_end_date_time")) {
                booking.setRecordingEndTime(Instant.parse(data.get("recording_end_date_time").getAsString()));
            }

            // Pass characteristics
            if (data.has("maxel_elevation")) {
                booking.setMaxElevation(Double.parseDouble(data.get("maxel_elevation").getAsString()));
            }
            booking.setActivityScope(getStringOrNull(data, "activity_scope"));

            // Metadata
            booking.setSpbasId(getStringOrNull(data, "spbas_id"));
            booking.setGsaId(getStringOrNull(data, "gsa_id"));
            booking.setGsapId(getStringOrNull(data, "gsap_id"));
            booking.setOrgId(getStringOrNull(data, "org_id"));

            if (data.has("created_date_time")) {
                booking.setCreatedDateTime(Instant.parse(data.get("created_date_time").getAsString()));
            }
            if (data.has("updated_date_time")) {
                booking.setUpdatedDateTime(Instant.parse(data.get("updated_date_time").getAsString()));
            }
        }

        booking.setStatus("booking_received");
        log.info("Successfully reserved contact with ISOCS: {} from {} to {}",
                booking.getSatellitePassBookingId(),
                booking.getStartDateTime(),
                booking.getEndDateTime());
        return booking;
    }

    private String getStringOrNull(JsonObject obj, String field) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : null;
    }

    @Override
    public boolean cancelReservation(String satellitePassBookingId) throws IOException, InterruptedException {
        ensureAuthenticated();

        JsonObject cancelBody = new JsonObject();
        cancelBody.addProperty("satellite_pass_booking_id", satellitePassBookingId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/centralgroundstation/dev/portal/api/v1/central/bookings/cancel"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(cancelBody)))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Failed to cancel reservation: {} - {}", response.statusCode(), response.body());
            return false;
        }

        log.info("Successfully cancelled reservation: {}", satellitePassBookingId);
        return true;
    }

    @Override
    public List<ProviderBooking> listBookings() throws IOException, InterruptedException {
        ensureAuthenticated();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/centralgroundstation/dev/portal/api/v1/central/gsaas/bookings"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Failed to list bookings: " + response.statusCode() + " - " + response.body());
        }

        List<ProviderBooking> bookings = new ArrayList<>();
        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray results = responseBody.getAsJsonObject("data").getAsJsonArray("result");

        for (int i = 0; i < results.size(); i++) {
            JsonObject b = results.get(i).getAsJsonObject();
            ProviderBooking booking = new ProviderBooking();
            booking.setSatellitePassBookingId(b.get("satellite_pass_booking_id").getAsString());
            booking.setGsId(b.get("gs_id").getAsString());
            booking.setGroundStationName(b.has("ground_station_name") ? b.get("ground_station_name").getAsString() : null);
            booking.setGsVisibilityId(b.has("gs_visibility_id") ? b.get("gs_visibility_id").getAsString() : null);
            booking.setNoradId(b.has("norad_id") ? b.get("norad_id").getAsInt() : 0);

            if (b.has("start_date_time")) {
                booking.setStartDateTime(Instant.parse(b.get("start_date_time").getAsString()));
            }
            if (b.has("end_date_time")) {
                booking.setEndDateTime(Instant.parse(b.get("end_date_time").getAsString()));
            }
            booking.setStatus(b.has("is_active_status") ? b.get("is_active_status").getAsString() : null);

            bookings.add(booking);
        }

        return bookings;
    }
}
