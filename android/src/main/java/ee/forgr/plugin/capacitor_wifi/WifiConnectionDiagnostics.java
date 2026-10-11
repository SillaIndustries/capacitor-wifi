package ee.forgr.plugin.capacitor_wifi;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded, identifier-free diagnostics. Access is confined to the connection handler. */
final class WifiConnectionDiagnostics {

    static final int EVENT_LIMIT = 64;

    enum EventType {
        ATTEMPT_STARTED("attemptStarted"),
        PERMISSION_REQUESTED("permissionRequested"),
        REQUEST_STARTED("requestStarted"),
        REQUEST_COMPLETED("requestCompleted"),
        NETWORK_AVAILABLE("networkAvailable"),
        CAPABILITIES_CHANGED("capabilitiesChanged"),
        SSID_VERIFICATION("ssidVerification"),
        BINDING_RESULT("bindingResult"),
        IP_READINESS("ipReadiness"),
        CONNECTION_SUCCEEDED("connectionSucceeded"),
        CONNECTION_FAILED("connectionFailed"),
        CONNECTION_CANCELLED("connectionCancelled"),
        NETWORK_LOST("networkLost"),
        CLEANUP("cleanup");

        final String value;

        EventType(String value) {
            this.value = value;
        }
    }

    final long id;
    private final long startedAt;
    private final long startedElapsed;
    private long updatedAt;
    private long sequence;
    private String outcome = "pending";
    private String stage = "validation";
    private String routing;
    private String ssidVerification = "unavailable";
    private String ipReadiness = "unknown";
    private Long ipObservedAt;
    private Long ssidObservedAt;
    private Boolean cleanupSucceeded;
    private Map<String, Object> error;
    private final List<Map<String, Object>> events = new ArrayList<>();

    WifiConnectionDiagnostics(long id, long timestamp, long elapsedClock, boolean autoRouteTraffic) {
        this.id = id;
        startedAt = timestamp;
        updatedAt = timestamp;
        startedElapsed = elapsedClock;
        routing = autoRouteTraffic ? "pending" : "notRequested";
        record(EventType.ATTEMPT_STARTED, "validation", timestamp, elapsedClock);
    }

    void record(EventType type, String stage, long timestamp, long elapsedClock) {
        this.stage = stage;
        updatedAt = timestamp;
        Map<String, Object> event = state(elapsedClock);
        event.remove("id");
        event.remove("startedAt");
        event.remove("updatedAt");
        event.put("attemptId", id);
        event.put("sequence", ++sequence);
        event.put("timestamp", timestamp);
        event.put("type", type.value);
        events.add(event);
        if (events.size() > EVENT_LIMIT) {
            events.remove(0);
        }
    }

    void observeSsid(String value, long timestamp, long elapsedClock) {
        boolean changed = ssidObservedAt == null || !ssidVerification.equals(value);
        ssidVerification = value;
        ssidObservedAt = timestamp;
        updatedAt = timestamp;
        if (changed) {
            record(EventType.SSID_VERIFICATION, stage, timestamp, elapsedClock);
        }
    }

    void observeIp(String value, long timestamp, long elapsedClock) {
        boolean changed = ipObservedAt == null || !ipReadiness.equals(value);
        ipReadiness = value;
        ipObservedAt = timestamp;
        updatedAt = timestamp;
        if (changed) {
            record(EventType.IP_READINESS, stage, timestamp, elapsedClock);
        }
    }

    void binding(boolean succeeded, long timestamp, long elapsedClock) {
        routing = succeeded ? "bound" : "failed";
        record(EventType.BINDING_RESULT, "routing", timestamp, elapsedClock);
    }

    void succeed(long timestamp, long elapsedClock) {
        outcome = "connected";
        record(EventType.CONNECTION_SUCCEEDED, "connected", timestamp, elapsedClock);
    }

    void fail(String code, String failureStage, Integer nativeCode, boolean cancelled, long timestamp, long elapsedClock) {
        outcome = cancelled ? "cancelled" : "failed";
        error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("stage", failureStage);
        if (nativeCode != null) {
            error.put("nativeCode", nativeCode.toString());
        }
        record(cancelled ? EventType.CONNECTION_CANCELLED : EventType.CONNECTION_FAILED, failureStage, timestamp, elapsedClock);
    }

    void cleanup(boolean succeeded, long timestamp, long elapsedClock) {
        cleanupSucceeded = succeeded;
        if (succeeded && !routing.equals("notRequested")) {
            routing = "released";
        }
        observeIp("unknown", timestamp, elapsedClock);
        record(EventType.CLEANUP, "cleanup", timestamp, elapsedClock);
    }

    boolean isPending() {
        return outcome.equals("pending");
    }

    Map<String, Object> snapshot(long elapsedClock) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attempt", state(elapsedClock));
        List<Map<String, Object>> history = new ArrayList<>();
        for (Map<String, Object> event : events) {
            Map<String, Object> copy = new LinkedHashMap<>(event);
            if (event.containsKey("error")) {
                copy.put("error", new LinkedHashMap<>((Map<?, ?>) event.get("error")));
            }
            history.add(copy);
        }
        result.put("events", history);
        result.put("droppedEventCount", Math.max(0, sequence - events.size()));
        return result;
    }

    private Map<String, Object> state(long elapsedClock) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("startedAt", startedAt);
        result.put("updatedAt", updatedAt);
        result.put("elapsedMs", Math.max(0, elapsedClock - startedElapsed));
        result.put("outcome", outcome);
        result.put("stage", stage);
        result.put("routing", routing);
        result.put("ssidVerification", ssidVerification);
        result.put("ipReadiness", ipReadiness);
        if (ipObservedAt != null) {
            result.put("ipObservedAt", ipObservedAt);
        }
        if (ssidObservedAt != null) {
            result.put("ssidObservedAt", ssidObservedAt);
        }
        if (cleanupSucceeded != null) {
            result.put("cleanupSucceeded", cleanupSucceeded);
        }
        if (error != null) {
            result.put("error", new LinkedHashMap<>(error));
        }
        return result;
    }

    static String classifyAddresses(List<InetAddress> addresses) {
        if (addresses == null) {
            return "unknown";
        }
        boolean linkLocal = false;
        boolean ipv6 = false;
        for (InetAddress address : addresses) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isMulticastAddress()) {
                continue;
            }
            if (address.isLinkLocalAddress()) {
                linkLocal = true;
            } else if (address instanceof Inet4Address) {
                int first = address.getAddress()[0] & 0xff;
                if (first > 0 && first < 224) {
                    return "ipv4Assigned";
                }
            } else if (address instanceof Inet6Address) {
                ipv6 = true;
            }
        }
        return ipv6 ? "ipv6Only" : linkLocal ? "linkLocalOnly" : "missing";
    }
}
