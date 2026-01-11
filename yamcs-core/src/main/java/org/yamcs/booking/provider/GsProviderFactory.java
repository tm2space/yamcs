package org.yamcs.booking.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating Ground Station provider clients.
 * Manages singleton instances of provider clients.
 */
public class GsProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(GsProviderFactory.class);

    private static final Map<String, GsProviderClient> clients = new HashMap<>();

    // Default ISOCS configuration - can be overridden via environment variables
    private static final String ISOCS_BASE_URL = System.getenv("ISOCS_BASE_URL") != null
            ? System.getenv("ISOCS_BASE_URL")
            : "https://demoapi.astraview.in";
    private static final String ISOCS_EMAIL = System.getenv("ISOCS_EMAIL") != null
            ? System.getenv("ISOCS_EMAIL")
            : "demotakeme2space@dhruvaspace.com";
    private static final String ISOCS_PASSWORD = System.getenv("ISOCS_PASSWORD") != null
            ? System.getenv("ISOCS_PASSWORD")
            : "T@keme2$p@ce_216";

    /**
     * Get a provider client for the specified provider type.
     * @param providerType One of: "dhruva", "leafspace", "isro"
     * @return The provider client, or null if not supported
     */
    public static synchronized GsProviderClient getClient(String providerType) {
        if (providerType == null) {
            return null;
        }

        String key = providerType.toLowerCase();

        if (clients.containsKey(key)) {
            return clients.get(key);
        }

        GsProviderClient client = createClient(key);
        if (client != null) {
            clients.put(key, client);
        }

        return client;
    }

    private static GsProviderClient createClient(String providerType) {
        switch (providerType) {
            case "dhruva":
                log.info("Creating ISOCS provider client for Dhruva Space");
                return new IsocsProviderClient(ISOCS_BASE_URL, ISOCS_EMAIL, ISOCS_PASSWORD);

            case "leafspace":
                log.warn("Leafspace provider not yet implemented");
                return null;

            case "isro":
                log.warn("ISRO provider not yet implemented");
                return null;

            default:
                log.warn("Unknown provider type: {}", providerType);
                return null;
        }
    }

    /**
     * Check if a provider type is supported
     */
    public static boolean isSupported(String providerType) {
        if (providerType == null) {
            return false;
        }
        String key = providerType.toLowerCase();
        return key.equals("dhruva"); // Add more as implemented
    }

    /**
     * Clear all cached clients (useful for testing or reconfiguration)
     */
    public static synchronized void clearClients() {
        clients.clear();
    }
}
