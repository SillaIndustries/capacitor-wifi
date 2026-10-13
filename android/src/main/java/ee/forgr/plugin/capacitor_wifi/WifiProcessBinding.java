package ee.forgr.plugin.capacitor_wifi;

/** Tracks only bindings established by this plugin; callers serialize access. */
final class WifiProcessBinding<N> {

    interface Driver<N> {
        N current();
        boolean bind(N network);
    }

    private final Driver<N> driver;
    private N ownedNetwork;

    WifiProcessBinding(Driver<N> driver) {
        this.driver = driver;
    }

    boolean bind(N network) {
        if (!release() || !driver.bind(network)) {
            return false;
        }
        ownedNetwork = network;
        return true;
    }

    boolean release() {
        if (ownedNetwork == null) {
            return true;
        }
        if (ownedNetwork.equals(driver.current()) && !driver.bind(null)) {
            // Keep ownership so explicit disconnect or replacement can retry cleanup.
            return false;
        }
        ownedNetwork = null;
        return true;
    }
}
