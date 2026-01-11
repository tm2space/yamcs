package org.yamcs.booking.provider;

/**
 * Common satellite model used across all providers
 */
public class ProviderSatellite {
    private String id;           // Provider's satellite ID
    private String name;         // Satellite name
    private String noradId;      // NORAD catalog ID (if available)
    private String provider;     // Provider type

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getNoradId() { return noradId; }
    public void setNoradId(String noradId) { this.noradId = noradId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
