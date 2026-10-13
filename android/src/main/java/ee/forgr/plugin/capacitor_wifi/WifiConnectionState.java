package ee.forgr.plugin.capacitor_wifi;

/** Network identity and completion policy, confined to the plugin's connection handler. */
final class WifiConnectionState<N> {

    enum Verification {
        UNAVAILABLE,
        MISMATCH,
        MATCH
    }

    private final String expectedSsid;
    private final long deadline;
    private N network;
    private String ssid;
    private boolean pending = true;
    private boolean released;

    WifiConnectionState(String expectedSsid) {
        this(expectedSsid, Long.MAX_VALUE);
    }

    WifiConnectionState(String expectedSsid, long deadline) {
        this.expectedSsid = expectedSsid;
        this.deadline = deadline;
    }

    boolean hasExpired(long now) {
        return isPending() && now >= deadline;
    }

    boolean isPending() {
        return pending && !released;
    }

    boolean isReleased() {
        return released;
    }

    N getNetwork() {
        return network;
    }

    String expectedSsid() {
        return expectedSsid;
    }

    boolean onAvailable(N availableNetwork) {
        if (!isPending()) {
            return false;
        }
        if (network != null) {
            return false;
        }
        network = availableNetwork;
        return true;
    }

    boolean onCapabilities(N observedNetwork, String observedSsid) {
        if (released || network == null || !network.equals(observedNetwork)) {
            return false;
        }
        ssid = normalizeSsid(observedSsid);
        return true;
    }

    Verification verify(String fallbackSsid, N fallbackNetwork) {
        if (released) {
            return Verification.UNAVAILABLE;
        }
        String observed = ssid;
        // A fallback must independently establish the identity of the requested network.
        if (observed == null && network != null && network.equals(fallbackNetwork)) {
            observed = normalizeSsid(fallbackSsid);
        }
        if (observed == null) {
            return Verification.UNAVAILABLE;
        }
        return expectedSsid.equals(observed) ? Verification.MATCH : Verification.MISMATCH;
    }

    boolean finish() {
        if (!isPending()) {
            return false;
        }
        pending = false;
        return true;
    }

    boolean release() {
        if (released) {
            return false;
        }
        pending = false;
        released = true;
        return true;
    }

    static String normalizeSsid(String value) {
        if (value != null && value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1);
        }
        return value == null || value.isEmpty() || "<unknown ssid>".equalsIgnoreCase(value) ? null : value;
    }

    static boolean isValidTimeout(Integer timeoutMs) {
        return timeoutMs != null && timeoutMs > 0;
    }
}
