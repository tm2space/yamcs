package org.yamcs.booking.provider;

import java.time.Instant;

/**
 * Booking result from provider - represents a reserved contact
 * Contains all timing and metadata needed for triggering telecommands
 */
public class ProviderBooking {
    // Core booking identifiers
    private String satellitePassBookingId;  // Provider's booking ID
    private String gsId;
    private String satelliteId;
    private String gsVisibilityId;

    // Ground station info
    private String groundStationName;
    private String gsapName;            // GS provider name (e.g., "Dhruva")

    // Satellite info
    private int noradId;

    // Timing - critical for triggering telecommands
    private Instant startDateTime;
    private Instant endDateTime;
    private long bookingStartEpochMs;   // Epoch milliseconds
    private long bookingEndEpochMs;     // Epoch milliseconds
    private Instant recordingStartTime;
    private Instant recordingEndTime;

    // Pass characteristics
    private double maxElevation;
    private String activityScope;       // e.g., "TM" for Telemetry

    // Metadata
    private String spbasId;
    private String gsaId;
    private String gsapId;
    private String orgId;
    private String status;
    private Instant createdDateTime;
    private Instant updatedDateTime;

    // Getters and Setters
    public String getSatellitePassBookingId() { return satellitePassBookingId; }
    public void setSatellitePassBookingId(String satellitePassBookingId) { this.satellitePassBookingId = satellitePassBookingId; }

    public String getGsId() { return gsId; }
    public void setGsId(String gsId) { this.gsId = gsId; }

    public String getSatelliteId() { return satelliteId; }
    public void setSatelliteId(String satelliteId) { this.satelliteId = satelliteId; }

    public String getGsVisibilityId() { return gsVisibilityId; }
    public void setGsVisibilityId(String gsVisibilityId) { this.gsVisibilityId = gsVisibilityId; }

    public String getGroundStationName() { return groundStationName; }
    public void setGroundStationName(String groundStationName) { this.groundStationName = groundStationName; }

    public String getGsapName() { return gsapName; }
    public void setGsapName(String gsapName) { this.gsapName = gsapName; }

    public int getNoradId() { return noradId; }
    public void setNoradId(int noradId) { this.noradId = noradId; }

    public Instant getStartDateTime() { return startDateTime; }
    public void setStartDateTime(Instant startDateTime) { this.startDateTime = startDateTime; }

    public Instant getEndDateTime() { return endDateTime; }
    public void setEndDateTime(Instant endDateTime) { this.endDateTime = endDateTime; }

    public long getBookingStartEpochMs() { return bookingStartEpochMs; }
    public void setBookingStartEpochMs(long bookingStartEpochMs) { this.bookingStartEpochMs = bookingStartEpochMs; }

    public long getBookingEndEpochMs() { return bookingEndEpochMs; }
    public void setBookingEndEpochMs(long bookingEndEpochMs) { this.bookingEndEpochMs = bookingEndEpochMs; }

    public Instant getRecordingStartTime() { return recordingStartTime; }
    public void setRecordingStartTime(Instant recordingStartTime) { this.recordingStartTime = recordingStartTime; }

    public Instant getRecordingEndTime() { return recordingEndTime; }
    public void setRecordingEndTime(Instant recordingEndTime) { this.recordingEndTime = recordingEndTime; }

    public double getMaxElevation() { return maxElevation; }
    public void setMaxElevation(double maxElevation) { this.maxElevation = maxElevation; }

    public String getActivityScope() { return activityScope; }
    public void setActivityScope(String activityScope) { this.activityScope = activityScope; }

    public String getSpbasId() { return spbasId; }
    public void setSpbasId(String spbasId) { this.spbasId = spbasId; }

    public String getGsaId() { return gsaId; }
    public void setGsaId(String gsaId) { this.gsaId = gsaId; }

    public String getGsapId() { return gsapId; }
    public void setGsapId(String gsapId) { this.gsapId = gsapId; }

    public String getOrgId() { return orgId; }
    public void setOrgId(String orgId) { this.orgId = orgId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedDateTime() { return createdDateTime; }
    public void setCreatedDateTime(Instant createdDateTime) { this.createdDateTime = createdDateTime; }

    public Instant getUpdatedDateTime() { return updatedDateTime; }
    public void setUpdatedDateTime(Instant updatedDateTime) { this.updatedDateTime = updatedDateTime; }
}
