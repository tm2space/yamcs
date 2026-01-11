package org.yamcs.booking.provider;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Abstract interface for Ground Station provider clients.
 * Each provider (Dhruva/ISOCS, Leafspace, ISRO) implements this interface.
 */
public interface GsProviderClient {

    /**
     * Get the provider type
     */
    String getProviderType();

    /**
     * Initialize/login to the provider API
     */
    void connect() throws IOException, InterruptedException;

    /**
     * List available satellites from this provider
     */
    List<ProviderSatellite> listSatellites() throws IOException, InterruptedException;

    /**
     * List available ground stations from this provider
     */
    List<ProviderGroundStation> listGroundStations() throws IOException, InterruptedException;

    /**
     * List activity scopes/configurations for a satellite (provider-specific setup)
     */
    List<ProviderActivityScope> listActivityScopes(String satelliteId) throws IOException, InterruptedException;

    /**
     * List available contact windows (passes) for a satellite at a ground station
     */
    List<ProviderContact> listContacts(String gsId, String satelliteId, String spbasId,
                                        LocalDate startDate, LocalDate endDate) throws IOException, InterruptedException;

    /**
     * Reserve a contact (book a pass)
     * @param gsId ground station ID
     * @param satelliteId satellite ID
     * @param gsVisibilityId visibility/contact ID
     * @param gsabracId activity scope ID
     */
    ProviderBooking reserveContact(String gsId, String satelliteId, String gsVisibilityId, String gsabracId)
            throws IOException, InterruptedException;

    /**
     * Cancel a reserved contact
     */
    boolean cancelReservation(String satellitePassBookingId) throws IOException, InterruptedException;

    /**
     * List existing bookings
     */
    List<ProviderBooking> listBookings() throws IOException, InterruptedException;
}
