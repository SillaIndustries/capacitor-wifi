package ee.forgr.plugin.capacitor_wifi;

import static org.junit.Assert.*;

import org.junit.Test;

public class WifiConnectionStateTest {

    @Test
    public void verifiesCallbackInformationForRequestedNetwork() {
        WifiConnectionState<String> state = new WifiConnectionState<>("charger");
        state.onAvailable("requested-network");
        state.onCapabilities("requested-network", "\"charger\"");
        assertEquals(WifiConnectionState.Verification.MATCH, state.verify(null, null));
    }

    @Test
    public void redactedCallbackCanUseIdentityProvenFallback() {
        WifiConnectionState<String> state = new WifiConnectionState<>("charger");
        state.onAvailable("requested-network");
        state.onCapabilities("requested-network", "<unknown ssid>");
        assertEquals(WifiConnectionState.Verification.MATCH, state.verify("\"charger\"", "requested-network"));
    }

    @Test
    public void matchingDefaultSsidCannotVerifyAnotherNetwork() {
        WifiConnectionState<String> state = new WifiConnectionState<>("charger");
        state.onAvailable("requested-network");
        assertEquals(WifiConnectionState.Verification.UNAVAILABLE, state.verify("charger", "default-network"));
        assertEquals(WifiConnectionState.Verification.UNAVAILABLE, state.verify("charger", null));
    }

    @Test
    public void anotherNetworksCapabilitiesCannotConfirmOrOverwriteRequestedNetwork() {
        WifiConnectionState<String> state = new WifiConnectionState<>("charger");
        state.onAvailable("requested-network");
        assertFalse(state.onCapabilities("default-network", "charger"));
        assertFalse(state.onAvailable("requested-network"));
        assertFalse(state.onAvailable("default-network"));
        assertEquals("requested-network", state.getNetwork());
        assertEquals(WifiConnectionState.Verification.UNAVAILABLE, state.verify(null, null));
    }

    @Test
    public void knownMismatchCannotBeOverriddenByLegacyReader() {
        WifiConnectionState<String> state = new WifiConnectionState<>("charger");
        state.onAvailable("requested-network");
        state.onCapabilities("requested-network", "home");
        assertEquals(WifiConnectionState.Verification.MISMATCH, state.verify("charger", "requested-network"));
    }

    @Test
    public void redactionAndMismatchRemainDistinctAndCanRecover() {
        WifiConnectionState<String> state = new WifiConnectionState<>("charger");
        state.onAvailable("requested-network");
        state.onCapabilities("requested-network", "\"<unknown ssid>\"");
        assertEquals(WifiConnectionState.Verification.UNAVAILABLE, state.verify(null, null));
        state.onCapabilities("requested-network", "home");
        assertEquals(WifiConnectionState.Verification.MISMATCH, state.verify(null, null));
        state.onCapabilities("requested-network", "charger");
        assertEquals(WifiConnectionState.Verification.MATCH, state.verify(null, null));
    }

    @Test
    public void successCompletesOnceButKeepsNetworkUntilRelease() {
        WifiConnectionState<String> state = new WifiConnectionState<>("charger");
        state.onAvailable("requested-network");
        state.onCapabilities("requested-network", "charger");
        assertTrue(state.finish());
        assertFalse(state.finish());
        assertFalse(state.isPending());
        assertFalse(state.isReleased());
        assertEquals("requested-network", state.getNetwork());
        assertTrue(state.release());
        assertFalse(state.release());
    }

    @Test
    public void cancellationBlocksLateAvailabilityVerificationAndCompletion() {
        WifiConnectionState<String> state = new WifiConnectionState<>("charger");
        assertTrue(state.release());
        assertFalse(state.onAvailable("requested-network"));
        assertFalse(state.onCapabilities("requested-network", "charger"));
        assertFalse(state.finish());
        assertEquals(WifiConnectionState.Verification.UNAVAILABLE, state.verify("charger", "requested-network"));
    }

    @Test
    public void lateOldEventsDoNotCompleteReplacementAttempt() {
        WifiConnectionState<String> old = new WifiConnectionState<>("charger");
        old.onAvailable("old-network");
        old.release();
        WifiConnectionState<String> replacement = new WifiConnectionState<>("charger");
        replacement.onAvailable("new-network");
        assertFalse(old.onCapabilities("old-network", "charger"));
        assertFalse(old.finish());
        assertTrue(replacement.isPending());
        assertEquals(WifiConnectionState.Verification.UNAVAILABLE, replacement.verify(null, null));
    }
}
