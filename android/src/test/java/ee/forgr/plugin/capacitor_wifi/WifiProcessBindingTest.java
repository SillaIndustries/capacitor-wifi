package ee.forgr.plugin.capacitor_wifi;

import static org.junit.Assert.*;

import org.junit.Test;

public class WifiProcessBindingTest {

    private static class Driver implements WifiProcessBinding.Driver<String> {

        String current;
        boolean succeed = true;
        int calls;

        public String current() {
            return current;
        }

        public boolean bind(String network) {
            calls++;
            if (succeed) {
                current = network;
            }
            return succeed;
        }
    }

    @Test
    public void failureCleanupUnbindsOwnedNetworkOnce() {
        Driver driver = new Driver();
        WifiProcessBinding<String> binding = new WifiProcessBinding<>(driver);
        assertTrue(binding.bind("charger"));
        assertTrue(binding.release());
        assertNull(driver.current);
        assertTrue(binding.release());
        assertEquals(2, driver.calls);
    }

    @Test
    public void externalReplacementIsNotUnboundByCleanup() {
        Driver driver = new Driver();
        WifiProcessBinding<String> binding = new WifiProcessBinding<>(driver);
        binding.bind("charger");
        driver.current = "another-component";
        assertTrue(binding.release());
        assertEquals("another-component", driver.current);
        assertEquals(1, driver.calls);
    }

    @Test
    public void unsuccessfulBindingDoesNotClaimOwnership() {
        Driver driver = new Driver();
        driver.current = "external";
        driver.succeed = false;
        WifiProcessBinding<String> binding = new WifiProcessBinding<>(driver);
        assertFalse(binding.bind("charger"));
        assertTrue(binding.release());
        assertEquals("external", driver.current);
        assertEquals(1, driver.calls);
    }

    @Test
    public void failedCleanupRetainsOwnershipForRetryAndBlocksReplacement() {
        Driver driver = new Driver();
        WifiProcessBinding<String> binding = new WifiProcessBinding<>(driver);
        binding.bind("charger");
        driver.succeed = false;
        assertFalse(binding.release());
        assertFalse(binding.bind("new-charger"));
        assertEquals("charger", driver.current);
        driver.succeed = true;
        assertTrue(binding.release());
        assertNull(driver.current);
    }

    @Test
    public void oldOwnerCannotReleaseNewOwnersBinding() {
        Driver driver = new Driver();
        WifiProcessBinding<String> old = new WifiProcessBinding<>(driver);
        WifiProcessBinding<String> replacement = new WifiProcessBinding<>(driver);
        old.bind("old-network");
        replacement.bind("new-network");
        assertTrue(old.release());
        assertEquals("new-network", driver.current);
        assertTrue(replacement.release());
        assertNull(driver.current);
    }
}
