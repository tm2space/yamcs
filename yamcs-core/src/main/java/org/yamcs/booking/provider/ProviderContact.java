package org.yamcs.booking.provider;

import java.time.Instant;

/**
 * Contact/pass window model - represents an available visibility window
 */
public class ProviderContact {
    private String gsVisibilityId;  // Contact/visibility ID
    private String gsId;
    private String groundStationName;
    private String satelliteId;
    private Instant passStart;
    private Instant passEnd;
    private double maxElevation;
    private String status;          // e.g., "slot_available", "booked"
    private int durationSeconds;

    public String getGsVisibilityId() { return gsVisibilityId; }
    public void setGsVisibilityId(String gsVisibilityId) { this.gsVisibilityId = gsVisibilityId; }

    public String getGsId() { return gsId; }
    public void setGsId(String gsId) { this.gsId = gsId; }

    public String getGroundStationName() { return groundStationName; }
    public void setGroundStationName(String groundStationName) { this.groundStationName = groundStationName; }

    public String getSatelliteId() { return satelliteId; }
    public void setSatelliteId(String satelliteId) { this.satelliteId = satelliteId; }

    public Instant getPassStart() { return passStart; }
    public void setPassStart(Instant passStart) { this.passStart = passStart; }

    public Instant getPassEnd() { return passEnd; }
    public void setPassEnd(Instant passEnd) { this.passEnd = passEnd; }

    public double getMaxElevation() { return maxElevation; }
    public void setMaxElevation(double maxElevation) { this.maxElevation = maxElevation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
}
