package org.yamcs.booking.model;

import java.time.LocalDateTime;

/**
 * Ground Station Booking model
 */
public class GSBooking {
    private int id;
    private String provider;
    private String satelliteId;
    private LocalDateTime startTime;
    private LocalDateTime endTime; // computed field
    private int durationMinutes;
    private String passType; // enum from database
    private String purpose; // enum from database
    private String ruleType; // enum from database
    private Integer frequencyDays;
    private String notes;
    private String status; // enum from database
    private String gsStatus; // enum from database
    private String requestedBy;
    private String approvedBy;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Generic provider fields (works for any provider: Dhruva, Leafspace, ISRO, etc.)
    private String providerSatelliteId;
    private String providerGsId;
    private String providerContactId;      // visibility/contact window ID
    private String providerBookingId;      // booking ID from provider
    private String providerMetadata;       // JSON for provider-specific data (activity scopes, etc.)
    private Double maxElevation;

    public GSBooking() {
        this.status = "pending";
        this.gsStatus = "scheduled";
        this.ruleType = "one_time";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getSatelliteId() { return satelliteId; }
    public void setSatelliteId(String satelliteId) { this.satelliteId = satelliteId; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getPassType() { return passType; }
    public void setPassType(String passType) { this.passType = passType; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public Integer getFrequencyDays() { return frequencyDays; }
    public void setFrequencyDays(Integer frequencyDays) { this.frequencyDays = frequencyDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGsStatus() { return gsStatus; }
    public void setGsStatus(String gsStatus) { this.gsStatus = gsStatus; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Generic provider getters and setters
    public String getProviderSatelliteId() { return providerSatelliteId; }
    public void setProviderSatelliteId(String providerSatelliteId) { this.providerSatelliteId = providerSatelliteId; }

    public String getProviderGsId() { return providerGsId; }
    public void setProviderGsId(String providerGsId) { this.providerGsId = providerGsId; }

    public String getProviderContactId() { return providerContactId; }
    public void setProviderContactId(String providerContactId) { this.providerContactId = providerContactId; }

    public String getProviderBookingId() { return providerBookingId; }
    public void setProviderBookingId(String providerBookingId) { this.providerBookingId = providerBookingId; }

    public String getProviderMetadata() { return providerMetadata; }
    public void setProviderMetadata(String providerMetadata) { this.providerMetadata = providerMetadata; }

    public Double getMaxElevation() { return maxElevation; }
    public void setMaxElevation(Double maxElevation) { this.maxElevation = maxElevation; }

    @Override
    public String toString() {
        return "GSBooking{" +
                "id=" + id +
                ", provider='" + provider + '\'' +
                ", satelliteId='" + satelliteId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", durationMinutes=" + durationMinutes +
                ", purpose='" + purpose + '\'' +
                ", status='" + status + '\'' +
                ", requestedBy='" + requestedBy + '\'' +
                '}';
    }
}