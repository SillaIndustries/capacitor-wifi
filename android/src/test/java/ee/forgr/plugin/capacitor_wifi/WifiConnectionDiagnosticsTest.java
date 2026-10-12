package ee.forgr.plugin.capacitor_wifi;

import static org.junit.Assert.*;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class WifiConnectionDiagnosticsTest {

    private List<InetAddress> addresses(String... values) throws Exception {
        List<InetAddress> result = new ArrayList<>();
        for (String value : values) {
            result.add(InetAddress.getByName(value));
        }
        return result;
    }

    @Test
    public void historyIsBoundedAndReportsTruncationInSequenceOrder() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(7, 1000, 100, false);
        for (int i = 0; i < 100; i++) {
            diagnostic.record(WifiConnectionDiagnostics.EventType.CAPABILITIES_CHANGED, "verification", 1001 + i, 101 + i);
        }
        Map<String, Object> snapshot = diagnostic.snapshot(300);
        List<?> events = (List<?>) snapshot.get("events");
        assertEquals(64, events.size());
        assertEquals(37L, snapshot.get("droppedEventCount"));
        assertEquals(38L, ((Map<?, ?>) events.get(0)).get("sequence"));
        assertEquals(101L, ((Map<?, ?>) events.get(63)).get("sequence"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void callerMutationCannotChangeStoredHistoryOrErrors() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(1, 1000, 100, true);
        diagnostic.fail("CONNECTION_TIMEOUT", "verification", 42, false, 1100, 200);
        Map<String, Object> snapshot = diagnostic.snapshot(200);
        ((Map<String, Object>) ((Map<?, ?>) snapshot.get("attempt")).get("error")).clear();
        List<Map<String, Object>> events = (List<Map<String, Object>>) snapshot.get("events");
        ((Map<String, Object>) events.get(1).get("error")).clear();
        events.clear();
        Map<?, ?> fresh = diagnostic.snapshot(200);
        assertEquals(2, ((List<?>) fresh.get("events")).size());
        assertEquals("CONNECTION_TIMEOUT", ((Map<?, ?>) ((Map<?, ?>) fresh.get("attempt")).get("error")).get("code"));
    }

    @Test
    public void failedBindingAndCleanupRemainExplainedWithoutNativeMessages() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(1, 1000, 100, true);
        diagnostic.binding(false, 1100, 200);
        diagnostic.fail("CONNECTION_FAILED", "verification", 3, false, 1110, 210);
        diagnostic.cleanup(true, 1120, 220);
        Map<?, ?> state = (Map<?, ?>) diagnostic.snapshot(220).get("attempt");
        assertEquals("failed", state.get("outcome"));
        assertEquals("released", state.get("routing"));
        assertEquals(true, state.get("cleanupSucceeded"));
        Map<?, ?> error = (Map<?, ?>) state.get("error");
        assertEquals("3", error.get("nativeCode"));
        assertFalse(error.containsKey("nativeMessage"));
        assertFalse(state.containsKey("ssid"));
        assertFalse(state.containsKey("ipAddress"));
    }

    @Test
    public void wallClockChangesDoNotAffectElapsedTimeOrHistoricalOutcome() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(1, 1000, 100, false);
        diagnostic.succeed(900, 200);
        diagnostic.cleanup(true, 800, 300);
        Map<?, ?> state = (Map<?, ?>) diagnostic.snapshot(400).get("attempt");
        assertEquals("connected", state.get("outcome"));
        assertEquals(300L, state.get("elapsedMs"));
        assertEquals("cleanup", state.get("stage"));
    }

    @Test
    public void terminalOutcomeCannotBeOverwrittenByLateCallbacks() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(2, 1000, 100, false);
        diagnostic.fail("CONNECTION_TIMEOUT", "verification", null, false, 1100, 200);
        diagnostic.succeed(1200, 300);
        diagnostic.fail("CONNECTION_FAILED", "cleanup", null, false, 1300, 400);

        Map<?, ?> snapshot = diagnostic.snapshot(400);
        Map<?, ?> state = (Map<?, ?>) snapshot.get("attempt");
        assertEquals("failed", state.get("outcome"));
        assertEquals("CONNECTION_TIMEOUT", ((Map<?, ?>) state.get("error")).get("code"));
        assertEquals(2, ((List<?>) snapshot.get("events")).size());
    }

    @Test
    public void associationConfirmationIsRecordedOnlyOnceAndBeforeSuccess() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(3, 1000, 100, false);
        diagnostic.associationConfirmed(1100, 200);
        diagnostic.associationConfirmed(1200, 300);
        diagnostic.succeed(1300, 400);

        List<?> events = (List<?>) diagnostic.snapshot(400).get("events");
        assertEquals(3, events.size());
        assertEquals("associationConfirmed", ((Map<?, ?>) events.get(1)).get("type"));
        assertEquals("connectionSucceeded", ((Map<?, ?>) events.get(2)).get("type"));
    }

    @Test
    public void verificationStartDoesNotClaimAnSsidObservation() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(5, 1000, 100, false);
        diagnostic.record(WifiConnectionDiagnostics.EventType.VERIFICATION_STARTED, "verification", 1100, 200);

        Map<String, Object> snapshot = diagnostic.snapshot(200);
        Map<?, ?> attempt = (Map<?, ?>) snapshot.get("attempt");
        List<?> events = (List<?>) snapshot.get("events");
        assertEquals("unavailable", attempt.get("ssidVerification"));
        assertFalse(attempt.containsKey("ssidObservedAt"));
        assertEquals("verificationStarted", ((Map<?, ?>) events.get(1)).get("type"));
    }

    @Test
    public void routingFailuresKeepTheirDistinctStageInTheSnapshot() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(4, 1000, 100, true);
        diagnostic.binding(false, 1100, 200);
        diagnostic.fail("CONNECTION_FAILED", "routing", null, false, 1200, 300);

        Map<?, ?> attempt = (Map<?, ?>) diagnostic.snapshot(300).get("attempt");
        Map<?, ?> error = (Map<?, ?>) attempt.get("error");
        assertEquals("failed", attempt.get("outcome"));
        assertEquals("routing", attempt.get("stage"));
        assertEquals("routing", error.get("stage"));
        assertEquals("failed", attempt.get("routing"));
    }

    @Test
    public void timeoutAndPermissionFailuresHaveDistinctTerminalEventTypes() {
        WifiConnectionDiagnostics timeout = new WifiConnectionDiagnostics(6, 1000, 100, false);
        timeout.fail("CONNECTION_TIMEOUT", "verification", null, false, 1100, 200);
        List<?> timeoutEvents = (List<?>) timeout.snapshot(200).get("events");
        assertEquals("connectionTimedOut", ((Map<?, ?>) timeoutEvents.get(1)).get("type"));

        WifiConnectionDiagnostics permission = new WifiConnectionDiagnostics(7, 1000, 100, false);
        permission.fail("PERMISSION_DENIED", "permission", null, false, 1100, 200);
        List<?> permissionEvents = (List<?>) permission.snapshot(200).get("events");
        assertEquals("permissionDenied", ((Map<?, ?>) permissionEvents.get(1)).get("type"));
    }

    @Test
    public void validationFailureCanBeSnapshottedBeforeNativeRequestStarts() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(8, 1000, 100, false);
        diagnostic.fail("INVALID_CONFIGURATION", "validation", null, false, 1010, 110);

        Map<String, Object> snapshot = diagnostic.snapshot(120);
        Map<?, ?> attempt = (Map<?, ?>) snapshot.get("attempt");
        List<?> events = (List<?>) snapshot.get("events");
        assertEquals(8L, attempt.get("id"));
        assertEquals("failed", attempt.get("outcome"));
        assertEquals("validation", attempt.get("stage"));
        assertEquals("connectionFailed", ((Map<?, ?>) events.get(1)).get("type"));
    }

    @Test
    public void cancelledFailureHasItsOwnTerminalEventType() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(9, 1000, 100, false);
        diagnostic.fail("CONNECTION_FAILED", "association", null, true, 1100, 200);
        List<?> events = (List<?>) diagnostic.snapshot(200).get("events");
        Map<?, ?> attempt = (Map<?, ?>) diagnostic.snapshot(200).get("attempt");
        assertEquals("cancelled", attempt.get("outcome"));
        assertEquals("connectionCancelled", ((Map<?, ?>) events.get(1)).get("type"));
    }

    @Test
    public void emptyAndUnavailableMetadataAreDifferent() {
        assertEquals("unknown", WifiConnectionDiagnostics.classifyAddresses(null));
        assertEquals("missing", WifiConnectionDiagnostics.classifyAddresses(new ArrayList<>()));
    }

    @Test
    public void unobservedTargetIsMarkedUnknownWithObservationTime() {
        WifiConnectionDiagnostics diagnostic = new WifiConnectionDiagnostics(10, 1000, 100, false);
        diagnostic.observeIp("unknown", 1100, 200);
        Map<?, ?> attempt = (Map<?, ?>) diagnostic.snapshot(200).get("attempt");
        assertEquals("unknown", attempt.get("ipReadiness"));
        assertEquals(1100L, attempt.get("ipObservedAt"));
    }

    @Test
    public void selfAssignedAndLinkLocalOnlyAddressesDoNotProveIpv4Readiness() throws Exception {
        assertEquals("linkLocalOnly", WifiConnectionDiagnostics.classifyAddresses(addresses("169.254.8.2", "fe80::1")));
        assertEquals("ipv6Only", WifiConnectionDiagnostics.classifyAddresses(addresses("169.254.8.2", "fd00::2")));
    }

    @Test
    public void eligibleIpv4TakesPrecedenceOverEarlyIpv6() throws Exception {
        assertEquals("ipv4Assigned", WifiConnectionDiagnostics.classifyAddresses(addresses("fe80::1", "fd00::2", "192.168.8.2")));
        assertEquals("ipv4Assigned", WifiConnectionDiagnostics.classifyAddresses(addresses("::ffff:192.168.8.2")));
    }

    @Test
    public void loopbackUnspecifiedMulticastAndBroadcastAreNotReady() throws Exception {
        assertEquals(
            "missing",
            WifiConnectionDiagnostics.classifyAddresses(
                addresses("0.0.0.0", "127.0.0.1", "::", "::1", "224.0.0.1", "ff02::1", "255.255.255.255")
            )
        );
    }
}
