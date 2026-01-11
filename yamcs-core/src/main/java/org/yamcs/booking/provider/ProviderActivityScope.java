package org.yamcs.booking.provider;

/**
 * Activity scope for satellite-GS communication (ISOCS specific but abstracted)
 */
public class ProviderActivityScope {
    private String gsabracId;       // GS Activity Scope ID
    private String spbasId;         // Satellite Pass Booking Activity Scope ID
    private String satelliteId;
    private String activityScope;   // e.g., "TM" for Telemetry
    private String taskName;        // e.g., "Telemetry"
    private String communicationBand; // e.g., "UHF"

    public String getGsabracId() { return gsabracId; }
    public void setGsabracId(String gsabracId) { this.gsabracId = gsabracId; }

    public String getSpbasId() { return spbasId; }
    public void setSpbasId(String spbasId) { this.spbasId = spbasId; }

    public String getSatelliteId() { return satelliteId; }
    public void setSatelliteId(String satelliteId) { this.satelliteId = satelliteId; }

    public String getActivityScope() { return activityScope; }
    public void setActivityScope(String activityScope) { this.activityScope = activityScope; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getCommunicationBand() { return communicationBand; }
    public void setCommunicationBand(String communicationBand) { this.communicationBand = communicationBand; }
}
