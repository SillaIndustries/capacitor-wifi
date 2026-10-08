package ee.forgr.plugin.capacitor_wifi;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.TransportInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import androidx.activity.result.ActivityResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

@CapacitorPlugin(
    name = "CapacitorWifi",
    permissions = {
        @Permission(alias = "location", strings = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION })
    }
)
public class CapacitorWifiPlugin extends Plugin {

    private final String pluginVersion = "8.5.5";

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private BroadcastReceiver scanResultsReceiver;
    private ConnectivityManager.NetworkCallback networkCallback;
    private WifiProcessBinding<Network> processBinding;
    private static final String ACTION_WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR = "android.settings.WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR";
    private static final String EXTRA_WIFI_SECURITY = "wifi_security";
    private static final String EXTRA_WIFI_SSID = "wifi_ssid";
    private static final String EXTRA_WIFI_PRE_SHARED_KEY = "wifi_psk";
    private static final int WIFI_SECURITY_WPA_PSK = 2;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 30000;
    private static final long SSID_VERIFY_POLL_MS = 500;

    // Connection methods, callbacks, timers, and cleanup all run on this handler.
    private int connectGeneration;
    private final Handler connectHandler = new Handler(Looper.getMainLooper());
    private PluginCall activeConnectCall;
    private WifiConnectionState<Network> connectionState;
    private Runnable connectTimeoutRunnable;
    private Runnable ssidVerifyRunnable;
    private WifiManager.LocalOnlyConnectionFailureListener localOnlyFailureListener;
    private Boolean pendingAutoRouteTraffic;
    private String connectionStage = "request";
    private WifiConnectionState.Verification ssidVerification = WifiConnectionState.Verification.UNAVAILABLE;
    private Boolean bindingSucceeded;
    private String attemptedCallId;
    private long connectStartedAt;
    private boolean destroyed;

    @Override
    public void load() {
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        processBinding = new WifiProcessBinding<>(
            new WifiProcessBinding.Driver<Network>() {
                @Override
                public Network current() {
                    return connectivityManager.getBoundNetworkForProcess();
                }

                @Override
                public boolean bind(Network network) {
                    return connectivityManager.bindProcessToNetwork(network);
                }
            }
        );
    }

    @PluginMethod
    public void addNetwork(PluginCall call) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addNetworkModern(call);
        } else {
            addNetworkLegacy(call);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void addNetworkModern(PluginCall call) {
        String ssid = call.getString("ssid");
        if (ssid == null || ssid.isEmpty()) {
            call.reject("SSID is required");
            return;
        }

        String password = call.getString("password");
        Boolean isHiddenSsid = call.getBoolean("isHiddenSsid", false);
        Integer securityType = call.getInt("securityType", 2); // Default to WPA2_PSK

        try {
            // Open system settings to add network
            Intent intent = new Intent(Settings.ACTION_WIFI_ADD_NETWORKS);
            WifiNetworkSuggestion.Builder suggestionBuilder = new WifiNetworkSuggestion.Builder().setSsid(ssid);

            if (isHiddenSsid != null && isHiddenSsid) {
                suggestionBuilder.setIsHiddenSsid(true);
            }

            if (password != null && !password.isEmpty()) {
                // Determine security type based on parameter
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    switch (securityType) {
                        case 1: // WEP
                            suggestionBuilder.setWpa2Passphrase(password); // WEP not supported, fallback to WPA2
                            break;
                        case 2: // WPA2_PSK
                            suggestionBuilder.setWpa2Passphrase(password);
                            break;
                        case 3: // EAP
                            // Enterprise networks require more configuration
                            suggestionBuilder.setWpa2Passphrase(password);
                            break;
                        case 4: // SAE (WPA3)
                            suggestionBuilder.setWpa3Passphrase(password);
                            break;
                        default:
                            suggestionBuilder.setWpa2Passphrase(password);
                            break;
                    }
                } else {
                    suggestionBuilder.setWpa2Passphrase(password);
                }
            }

            ArrayList<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
            suggestionsList.add(suggestionBuilder.build());

            intent.putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, suggestionsList);
            startActivityForResult(call, intent, "handleAddNetworkModernResult");
        } catch (Exception e) {
            call.reject("Failed to add network: " + e.getMessage(), e);
        }
    }

    @ActivityCallback
    private void handleAddNetworkModernResult(@Nullable PluginCall call, ActivityResult result) {
        if (call == null) {
            return;
        }

        if (result.getResultCode() != RESULT_OK) {
            call.reject("Adding network was canceled");
            return;
        }

        Intent data = result.getData();
        if (data == null || !data.hasExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST)) {
            call.reject("Failed to add network");
            return;
        }

        ArrayList<Integer> codes = data.getIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST);
        Integer firstCode = codes != null && !codes.isEmpty() ? codes.get(0) : null;

        if (firstCode == null) {
            call.reject("Failed to add network");
            return;
        }

        switch (firstCode) {
            case Settings.ADD_WIFI_RESULT_SUCCESS:
            case Settings.ADD_WIFI_RESULT_ALREADY_EXISTS:
                call.resolve();
                return;
            case Settings.ADD_WIFI_RESULT_ADD_OR_UPDATE_FAILED:
                call.reject("Failed to add network");
                return;
            default:
                call.reject("Failed to add network");
        }
    }

    private void addNetworkLegacy(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "addNetworkCallback");
            return;
        }

        String ssid = call.getString("ssid");
        if (ssid == null || ssid.isEmpty()) {
            call.reject("SSID is required");
            return;
        }

        String password = call.getString("password");
        Boolean isHiddenSsid = call.getBoolean("isHiddenSsid", false);
        Integer securityType = call.getInt("securityType", 2);

        try {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + ssid + "\"";

            if (isHiddenSsid != null && isHiddenSsid) {
                wifiConfig.hiddenSSID = true;
            }

            if (password != null && !password.isEmpty()) {
                if (securityType == 1) {
                    // WEP
                    wifiConfig.wepKeys[0] = "\"" + password + "\"";
                    wifiConfig.wepTxKeyIndex = 0;
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                } else {
                    // WPA/WPA2
                    wifiConfig.preSharedKey = "\"" + password + "\"";
                }
            } else {
                // Open network
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }

            int netId = wifiManager.addNetwork(wifiConfig);
            if (netId == -1) {
                call.reject("Failed to add network");
                return;
            }

            boolean enableResult = wifiManager.enableNetwork(netId, true);
            if (!enableResult) {
                call.reject("Failed to enable network");
                return;
            }

            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to add network: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void connect(PluginCall call) {
        connectHandler.post(() -> startConnect(call));
    }

    private void startConnect(PluginCall call) {
        if (destroyed) {
            rejectConnect(call, "CONNECTION_FAILED", "Plugin has been destroyed.", "request", null, null);
            return;
        }
        if (activeConnectCall != null) {
            rejectConnect(
                call,
                "CONNECTION_IN_PROGRESS",
                "Another Wi-Fi connection attempt is already in progress.",
                "request",
                null,
                null
            );
            return;
        }
        // Reserve the slot early so permission prompts cannot overlap with another connect().
        activeConnectCall = call;

        continueConnect(call);
    }

    private void continueConnect(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                connectModern(call);
            } else {
                connectLegacy(call);
            }
        } catch (SecurityException e) {
            rejectConnectAndClear(call, "PERMISSION_DENIED", "Missing permission to connect to Wi-Fi.", "permission");
        } catch (Exception e) {
            rejectConnectAndClear(call, "UNKNOWN", "Unable to start the Wi-Fi connection request.", "request");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void connectModern(PluginCall call) {
        String ssid = call.getString("ssid");
        if (ssid == null || ssid.isEmpty()) {
            rejectConnectAndClear(call, "INVALID_CONFIGURATION", "SSID is required.", "validation");
            return;
        }

        Integer timeoutMs = resolveTimeoutMs(call);
        if (timeoutMs == null) {
            rejectConnectAndClear(call, "INVALID_CONFIGURATION", "timeoutMs must be a positive number.", "validation");
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            rejectConnectAndClear(call, "WIFI_DISABLED", "Wi-Fi is disabled.", "request");
            return;
        }

        String password = call.getString("password");
        Boolean isHiddenSsid = call.getBoolean("isHiddenSsid", false);
        Boolean autoRouteTraffic = call.getBoolean("autoRouteTraffic", false);

        if (password != null && !password.isEmpty() && !isValidWpaPassphrase(password)) {
            rejectConnectAndClear(call, "INVALID_CONFIGURATION", "Invalid WPA passphrase format.", "validation");
            return;
        }

        if (getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionForAlias("location", call, "connectCallback");
            return;
        }
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null || !locationManager.isLocationEnabled()) {
            rejectConnectAndClear(call, "PERMISSION_DENIED", "Location services must be enabled to verify the Wi-Fi SSID.", "permission");
            return;
        }

        final int generation = beginConnectAttempt(call, ssid, autoRouteTraffic, timeoutMs);
        if (generation < 0) {
            return;
        }

        try {
            WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder().setSsid(ssid);

            if (isHiddenSsid != null && isHiddenSsid) {
                specifierBuilder.setIsHiddenSsid(true);
            }

            if (password != null && !password.isEmpty()) {
                specifierBuilder.setWpa2Passphrase(password);
            }

            NetworkSpecifier specifier = specifierBuilder.build();

            NetworkRequest.Builder requestBuilder = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier);

            // Joining an offline AP and binding app traffic are independent policies.
            requestBuilder.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

            NetworkRequest request = requestBuilder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                registerLocalOnlyFailureListener(generation, specifier);
            }

            networkCallback = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? new WifiConnectCallback(generation, ssid, ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO)
                : new WifiConnectCallback(generation, ssid);

            connectivityManager.requestNetwork(request, networkCallback);
        } catch (SecurityException e) {
            completeConnectFailure(
                generation,
                "PERMISSION_DENIED",
                "Missing permission to connect to Wi-Fi.",
                "permission",
                null,
                e.getMessage(),
                true
            );
        } catch (IllegalArgumentException e) {
            completeConnectFailure(
                generation,
                "INVALID_CONFIGURATION",
                "Invalid Wi-Fi connection configuration.",
                "validation",
                null,
                e.getMessage(),
                true
            );
        } catch (Exception e) {
            completeConnectFailure(
                generation,
                "UNKNOWN",
                "Unexpected error while connecting to Wi-Fi.",
                "request",
                null,
                e.getMessage(),
                true
            );
        }
    }

    private void connectLegacy(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "connectCallback");
            return;
        }

        String ssid = call.getString("ssid");
        if (ssid == null || ssid.isEmpty()) {
            rejectConnectAndClear(call, "INVALID_CONFIGURATION", "SSID is required.", "validation");
            return;
        }

        Integer timeoutMs = resolveTimeoutMs(call);
        if (timeoutMs == null) {
            rejectConnectAndClear(call, "INVALID_CONFIGURATION", "timeoutMs must be a positive number.", "validation");
            return;
        }

        if (!wifiManager.isWifiEnabled()) {
            rejectConnectAndClear(call, "WIFI_DISABLED", "Wi-Fi is disabled.", "request");
            return;
        }

        String password = call.getString("password");
        Boolean isHiddenSsid = call.getBoolean("isHiddenSsid", false);
        Boolean autoRouteTraffic = call.getBoolean("autoRouteTraffic", false);

        if (password != null && !password.isEmpty() && !isValidWpaPassphrase(password)) {
            rejectConnectAndClear(call, "INVALID_CONFIGURATION", "Invalid WPA passphrase format.", "validation");
            return;
        }

        final int generation = beginConnectAttempt(call, ssid, autoRouteTraffic, timeoutMs);
        if (generation < 0) {
            return;
        }

        try {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + ssid + "\"";

            if (isHiddenSsid != null && isHiddenSsid) {
                wifiConfig.hiddenSSID = true;
            }

            if (password != null && !password.isEmpty()) {
                wifiConfig.preSharedKey = "\"" + password + "\"";
            } else {
                wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }

            int netId = wifiManager.addNetwork(wifiConfig);
            if (netId == -1) {
                completeConnectFailure(
                    generation,
                    "CONNECTION_FAILED",
                    "Failed to add network configuration.",
                    "request",
                    null,
                    null,
                    true
                );
                return;
            }

            boolean disconnectResult = wifiManager.disconnect();
            if (!disconnectResult) {
                completeConnectFailure(
                    generation,
                    "CONNECTION_FAILED",
                    "Failed to disconnect from current network.",
                    "request",
                    null,
                    null,
                    true
                );
                return;
            }

            boolean enableResult = wifiManager.enableNetwork(netId, true);
            if (!enableResult) {
                completeConnectFailure(generation, "CONNECTION_FAILED", "Failed to enable network.", "request", null, null, true);
                return;
            }

            boolean reconnectResult = wifiManager.reconnect();
            if (!reconnectResult) {
                completeConnectFailure(generation, "CONNECTION_FAILED", "Failed to reconnect.", "request", null, null, true);
                return;
            }

            // Do not resolve after reconnect(); wait until the connected SSID matches.
            startSsidVerification(generation, null, ssid, false);
        } catch (SecurityException e) {
            completeConnectFailure(
                generation,
                "PERMISSION_DENIED",
                "Missing permission to connect to Wi-Fi.",
                "permission",
                null,
                e.getMessage(),
                true
            );
        } catch (IllegalArgumentException e) {
            completeConnectFailure(
                generation,
                "INVALID_CONFIGURATION",
                "Invalid Wi-Fi connection configuration.",
                "validation",
                null,
                e.getMessage(),
                true
            );
        } catch (Exception e) {
            completeConnectFailure(
                generation,
                "UNKNOWN",
                "Unexpected error while connecting to Wi-Fi.",
                "request",
                null,
                e.getMessage(),
                true
            );
        }
    }

    @PluginMethod
    public void disconnect(PluginCall call) {
        connectHandler.post(() -> disconnectOnHandler(call));
    }

    private void disconnectOnHandler(PluginCall call) {
        try {
            int generation = connectGeneration;
            completeConnectFailure(
                generation,
                "CONNECTION_FAILED",
                "Connection canceled because disconnect() was called.",
                "request",
                null,
                null,
                false
            );

            boolean released = releaseConnection();
            // Invalidate queued native/permission callbacks even after success.
            connectGeneration++;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                wifiManager.disconnect();
            }
            if (!released) {
                call.reject("Failed to release the plugin's Wi-Fi connection resources.");
                return;
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to disconnect: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void getAvailableNetworks(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "getAvailableNetworksCallback");
            return;
        }

        try {
            List<ScanResult> scanResults = wifiManager.getScanResults();
            JSArray networks = new JSArray();

            for (ScanResult result : scanResults) {
                JSObject network = new JSObject();
                network.put("ssid", result.SSID);
                network.put("rssi", result.level);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    JSArray securityTypes = new JSArray();
                    int[] types = result.getSecurityTypes();
                    for (int type : types) {
                        securityTypes.put(type);
                    }
                    network.put("securityTypes", securityTypes);
                }

                networks.put(network);
            }

            JSObject ret = new JSObject();
            ret.put("networks", networks);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to get available networks: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void getIpAddress(PluginCall call) {
        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                call.reject("Failed to get WiFi info");
                return;
            }

            String ipAddress = resolveWifiIpAddress(wifiInfo);

            if (ipAddress != null && !ipAddress.isEmpty()) {
                JSObject ret = new JSObject();
                ret.put("ipAddress", ipAddress);
                call.resolve(ret);
            } else {
                call.reject("No IP address found");
            }
        } catch (Exception e) {
            call.reject("Failed to get IP address: " + e.getMessage(), e);
        }
    }

    private String getWifiIpAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (intf.getName().contains("wlan")) {
                    List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                    for (InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PluginMethod
    public void getRssi(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "getRssiCallback");
            return;
        }

        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                int rssi = wifiInfo.getRssi();
                JSObject ret = new JSObject();
                ret.put("rssi", rssi);
                call.resolve(ret);
            } else {
                call.reject("Failed to get WiFi info");
            }
        } catch (Exception e) {
            call.reject("Failed to get RSSI: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void getSsid(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "getSsidCallback");
            return;
        }

        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                String ssid = wifiInfo.getSSID();
                if (ssid != null) {
                    // Remove quotes if present
                    ssid = ssid.replace("\"", "");
                    JSObject ret = new JSObject();
                    ret.put("ssid", ssid);
                    call.resolve(ret);
                } else {
                    call.reject("No SSID found");
                }
            } else {
                call.reject("Failed to get WiFi info");
            }
        } catch (Exception e) {
            call.reject("Failed to get SSID: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void getWifiInfo(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "getWifiInfoCallback");
            return;
        }

        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo == null) {
                call.reject("Failed to get WiFi info");
                return;
            }

            JSObject ret = new JSObject();

            // Get SSID
            String ssid = wifiInfo.getSSID();
            if (ssid != null) {
                ssid = ssid.replace("\"", "");
                ret.put("ssid", ssid);
            } else {
                call.reject("No SSID found");
                return;
            }

            // Get BSSID (MAC address of access point)
            String bssid = wifiInfo.getBSSID();
            if (bssid != null) {
                ret.put("bssid", bssid);
            }

            // Get IP Address
            String ipAddress = resolveWifiIpAddress(wifiInfo);
            if (ipAddress != null && !ipAddress.isEmpty()) {
                ret.put("ip", ipAddress);
            } else {
                call.reject("No IP address found");
                return;
            }

            // Get Frequency (API 21+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int frequency = wifiInfo.getFrequency();
                ret.put("frequency", frequency);
            }

            // Get Link Speed
            int linkSpeed = wifiInfo.getLinkSpeed();
            ret.put("linkSpeed", linkSpeed);

            // Get Signal Strength (0-100)
            int rssi = wifiInfo.getRssi();
            int signalStrength = calculateSignalStrength(rssi);
            ret.put("signalStrength", signalStrength);

            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to get WiFi info: " + e.getMessage(), e);
        }
    }

    private String resolveWifiIpAddress(@NonNull WifiInfo wifiInfo) {
        String ipAddress = formatIpv4Address(wifiInfo.getIpAddress());
        if (ipAddress != null) {
            return ipAddress;
        }
        return getWifiIpAddress();
    }

    private String formatIpv4Address(int ipAddress) {
        if (ipAddress == 0) {
            return null;
        }
        return (ipAddress & 0xff) + "." + ((ipAddress >> 8) & 0xff) + "." + ((ipAddress >> 16) & 0xff) + "." + ((ipAddress >> 24) & 0xff);
    }

    /**
     * Calculate signal strength percentage (0-100) from RSSI
     * RSSI typically ranges from -100 (weak) to -50 (strong)
     */
    private int calculateSignalStrength(int rssi) {
        if (rssi <= -100) {
            return 0;
        } else if (rssi >= -50) {
            return 100;
        } else {
            return 2 * (rssi + 100);
        }
    }

    @PluginMethod
    public void isEnabled(PluginCall call) {
        try {
            boolean enabled = wifiManager.isWifiEnabled();
            JSObject ret = new JSObject();
            ret.put("enabled", enabled);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to check WiFi status: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void startScan(PluginCall call) {
        if (getPermissionState("location") != PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "startScanCallback");
            return;
        }

        try {
            // Register broadcast receiver for scan results
            if (scanResultsReceiver == null) {
                scanResultsReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                        if (success) {
                            notifyListeners("networksScanned", new JSObject());
                        }
                    }
                };

                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                getContext().registerReceiver(scanResultsReceiver, intentFilter);
            }

            boolean scanStarted = wifiManager.startScan();
            if (scanStarted) {
                call.resolve();
            } else {
                call.reject("Failed to start scan");
            }
        } catch (Exception e) {
            call.reject("Failed to start scan: " + e.getMessage(), e);
        }
    }

    @PluginMethod
    public void isNetworkSaved(PluginCall call) {
        String ssid = call.getString("ssid");
        if (ssid == null || ssid.isEmpty()) {
            call.reject("SSID is required");
            return;
        }

        try {
            boolean isSaved;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                isSaved = isNetworkSavedModern(ssid);
            } else {
                // API 29 (Q): getNetworkSuggestions() isn't available yet, fall back to the
                // legacy WifiConfiguration-based lookup which works on API 28 and API 29.
                isSaved = isNetworkSavedLegacy(ssid);
            }
            JSObject ret = new JSObject();
            ret.put("isSaved", isSaved);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Failed to check network saved status: " + e.getMessage(), e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private boolean isNetworkSavedModern(String ssid) {
        List<WifiNetworkSuggestion> suggestions = wifiManager.getNetworkSuggestions();
        for (WifiNetworkSuggestion suggestion : suggestions) {
            // getSsid() returns the raw (unquoted) SSID; strip quotes defensively in case the
            // platform ever returns a quoted value.
            String suggestionSsid = suggestion.getSsid();
            if (suggestionSsid != null) {
                suggestionSsid = suggestionSsid.replace("\"", "");
            }
            if (ssid.equals(suggestionSsid)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private boolean isNetworkSavedLegacy(String ssid) {
        List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
        if (configs == null) {
            // getConfiguredNetworks() can return null when the caller lacks ACCESS_WIFI_STATE
            // permission or Wi-Fi is disabled; treat this as an indeterminate state.
            throw new IllegalStateException("Unable to retrieve configured networks; check permissions and Wi-Fi state");
        }
        String quotedSsid = "\"" + ssid + "\"";
        for (WifiConfiguration config : configs) {
            if (quotedSsid.equals(config.SSID)) {
                return true;
            }
        }
        return false;
    }

    @PluginMethod
    public void shareNetwork(PluginCall call) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            call.reject("Wi-Fi credential sharing requires Android 10 or later");
            return;
        }

        if (!wifiManager.isEasyConnectSupported()) {
            call.reject("Wi-Fi Easy Connect is not supported on this device");
            return;
        }

        String dppUri = call.getString("dppUri");
        if (dppUri != null && !dppUri.isEmpty()) {
            shareNetworkViaDppUri(call, dppUri);
            return;
        }

        shareNetworkViaQrGenerator(call);
    }

    private void shareNetworkViaDppUri(PluginCall call, String dppUri) {
        try {
            Intent intent = new Intent(Settings.ACTION_PROCESS_WIFI_EASY_CONNECT_URI);
            intent.setData(Uri.parse(dppUri));
            startActivityForResult(call, intent, "handleShareNetworkResult");
        } catch (Exception e) {
            call.reject("Failed to start Wi-Fi Easy Connect flow: " + e.getMessage(), e);
        }
    }

    private void shareNetworkViaQrGenerator(PluginCall call) {
        try {
            Intent intent = new Intent(ACTION_WIFI_DPP_CONFIGURATOR_QR_CODE_GENERATOR);

            String ssid = call.getString("ssid");
            String password = call.getString("password");

            if (ssid != null && !ssid.isEmpty()) {
                intent.putExtra(EXTRA_WIFI_SSID, ssid);
                if (password != null && !password.isEmpty()) {
                    intent.putExtra(EXTRA_WIFI_SECURITY, WIFI_SECURITY_WPA_PSK);
                    intent.putExtra(EXTRA_WIFI_PRE_SHARED_KEY, password);
                }
            }

            startActivityForResult(call, intent, "handleShareNetworkResult");
        } catch (Exception e) {
            call.reject("Failed to start Wi-Fi sharing UI: " + e.getMessage(), e);
        }
    }

    @ActivityCallback
    private void handleShareNetworkResult(@Nullable PluginCall call, ActivityResult result) {
        if (call == null) {
            return;
        }

        if (result.getResultCode() != RESULT_OK) {
            call.reject("Wi-Fi sharing was canceled");
            return;
        }

        JSObject ret = new JSObject();
        ret.put("started", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void getPluginVersion(final PluginCall call) {
        try {
            final JSObject ret = new JSObject();
            ret.put("version", this.pluginVersion);
            call.resolve(ret);
        } catch (final Exception e) {
            call.reject("Could not get plugin version", e);
        }
    }

    @PermissionCallback
    private void addNetworkCallback(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            addNetworkLegacy(call);
        } else {
            call.reject("Location permission is required");
        }
    }

    @PermissionCallback
    private void connectCallback(PluginCall call) {
        connectHandler.post(() -> {
            if (destroyed || activeConnectCall != call) {
                return;
            }
            if (getContext().checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                rejectConnectAndClear(
                    call,
                    "PERMISSION_DENIED",
                    "Precise location permission is required to verify the Wi-Fi SSID.",
                    "permission"
                );
            } else {
                continueConnect(call);
            }
        });
    }

    @PermissionCallback
    private void getAvailableNetworksCallback(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            getAvailableNetworks(call);
        } else {
            call.reject("Location permission is required");
        }
    }

    @PermissionCallback
    private void getRssiCallback(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            getRssi(call);
        } else {
            call.reject("Location permission is required");
        }
    }

    @PermissionCallback
    private void getSsidCallback(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            getSsid(call);
        } else {
            call.reject("Location permission is required");
        }
    }

    @PermissionCallback
    private void getWifiInfoCallback(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            getWifiInfo(call);
        } else {
            call.reject("Location permission is required");
        }
    }

    @PermissionCallback
    private void startScanCallback(PluginCall call) {
        if (getPermissionState("location") == PermissionState.GRANTED) {
            startScan(call);
        } else {
            call.reject("Location permission is required");
        }
    }

    @Override
    protected void handleOnDestroy() {
        if (scanResultsReceiver != null) {
            try {
                getContext().unregisterReceiver(scanResultsReceiver);
            } catch (Exception e) {
                // Receiver not registered
            }
            scanResultsReceiver = null;
        }

        connectHandler.post(() -> {
            destroyed = true;
            completeConnectFailure(connectGeneration, "CONNECTION_FAILED", "Plugin has been destroyed.", "request", null, null, true);
            releaseConnection();
            connectGeneration++;
        });
    }

    private void clearConnectReservation() {
        activeConnectCall = null;
    }

    private void rejectConnectAndClear(PluginCall call, String code, String message, String stage) {
        clearConnectReservation();
        rejectConnect(call, code, message, stage, null, null);
    }

    private Integer resolveTimeoutMs(PluginCall call) {
        if (!call.getData().has("timeoutMs")) {
            return DEFAULT_CONNECT_TIMEOUT_MS;
        }
        Integer timeoutMs = call.getInt("timeoutMs");
        if (timeoutMs == null || timeoutMs <= 0) {
            return null;
        }
        return timeoutMs;
    }

    private boolean isValidWpaPassphrase(String password) {
        int length = password.length();
        if (length >= 8 && length <= 63) {
            return true;
        }
        if (length == 64) {
            for (int i = 0; i < length; i++) {
                char c = password.charAt(i);
                boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
                if (!hex) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private int beginConnectAttempt(PluginCall call, String ssid, Boolean autoRouteTraffic, int timeoutMs) {
        if (!releaseConnection()) {
            rejectConnectAndClear(call, "CONNECTION_FAILED", "Failed to release the previous Wi-Fi connection resources.", "request");
            return -1;
        }
        int generation = ++connectGeneration;
        activeConnectCall = call;
        connectionState = new WifiConnectionState<>(ssid);
        pendingAutoRouteTraffic = autoRouteTraffic;
        connectionStage = "association";
        ssidVerification = WifiConnectionState.Verification.UNAVAILABLE;
        bindingSucceeded = null;
        connectStartedAt = SystemClock.elapsedRealtime();
        attemptedCallId = call.getCallbackId();
        logConnect("request");

        connectTimeoutRunnable = () ->
            completeConnectFailure(
                generation,
                "CONNECTION_TIMEOUT",
                "Timed out waiting for Wi-Fi connection confirmation.",
                connectionStage,
                null,
                null,
                true
            );
        connectHandler.postDelayed(connectTimeoutRunnable, timeoutMs);
        return generation;
    }

    private boolean isCurrentConnectAttempt(int generation) {
        return !destroyed && generation == connectGeneration;
    }

    private boolean isPendingConnectAttempt(int generation) {
        return isCurrentConnectAttempt(generation) && activeConnectCall != null && connectionState != null && connectionState.isPending();
    }

    private void startSsidVerification(int generation, @Nullable Network network, String expectedSsid, boolean keepCallbackOnSuccess) {
        clearSsidVerification();
        ssidVerifyRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPendingConnectAttempt(generation)) {
                    return;
                }

                if (ssidMatches(expectedSsid, network)) {
                    completeConnectSuccess(generation, network, keepCallbackOnSuccess);
                    return;
                }

                connectHandler.postDelayed(this, SSID_VERIFY_POLL_MS);
            }
        };
        connectHandler.post(ssidVerifyRunnable);
    }

    private boolean ssidMatches(String expectedSsid, @Nullable Network network) {
        WifiConnectionState.Verification previous = ssidVerification;
        if (network == null) {
            // Pre-29 does not use a specifier request or concurrent local-only Wi-Fi.
            String current = getLegacySsid();
            ssidVerification = current == null
                ? WifiConnectionState.Verification.UNAVAILABLE
                : expectedSsid.equals(current)
                    ? WifiConnectionState.Verification.MATCH
                    : WifiConnectionState.Verification.MISMATCH;
        } else {
            Network fallbackNetwork = null;
            String fallbackSsid = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                // API 29/30 do not support the location-inclusive callback flag.
                // Accept the legacy reader only if exactly one visible Wi-Fi network exists
                // and it is the one returned by this request. Never use a default SSID alone.
                fallbackNetwork = getOnlyWifiNetwork();
                if (network.equals(fallbackNetwork)) {
                    fallbackSsid = getLegacySsid();
                }
            }
            ssidVerification = connectionState.verify(fallbackSsid, fallbackNetwork);
        }
        if (previous != ssidVerification) {
            logConnect("verification:" + ssidVerification.name().toLowerCase(java.util.Locale.ROOT));
        }
        return ssidVerification == WifiConnectionState.Verification.MATCH;
    }

    @Nullable
    private String getLegacySsid() {
        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                return normalizeSsid(wifiInfo.getSSID());
            }
        } catch (Exception e) {
            android.util.Log.e("CapacitorWifi", "Failed to read connected SSID: " + e.getMessage());
        }
        return null;
    }

    @Nullable
    private String normalizeSsid(@Nullable String ssid) {
        return WifiConnectionState.normalizeSsid(ssid);
    }

    @Nullable
    private Network getOnlyWifiNetwork() {
        Network only = null;
        try {
            for (Network candidate : connectivityManager.getAllNetworks()) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(candidate);
                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    if (only != null) {
                        return null;
                    }
                    only = candidate;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return only;
    }

    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private void registerLocalOnlyFailureListener(int generation, NetworkSpecifier specifier) {
        removeLocalOnlyFailureListener();
        if (localOnlyFailureListener != null) {
            throw new IllegalStateException("Previous Wi-Fi failure listener could not be released.");
        }
        if (!(specifier instanceof WifiNetworkSpecifier)) {
            return;
        }
        localOnlyFailureListener = (failedSpecifier, failureReason) -> {
            if (!isPendingConnectAttempt(generation) || !specifier.equals(failedSpecifier)) {
                return;
            }
            String code = mapLocalOnlyFailureReason(failureReason);
            String stage = mapLocalOnlyFailureStage(failureReason);
            completeConnectFailure(generation, code, "Failed to connect to network.", stage, failureReason, null, true);
        };
        Executor executor = (command) -> connectHandler.post(command);
        wifiManager.addLocalOnlyConnectionFailureListener(executor, localOnlyFailureListener);
    }

    private String mapLocalOnlyFailureReason(int failureReason) {
        if (failureReason == WifiManager.STATUS_LOCAL_ONLY_CONNECTION_FAILURE_AUTHENTICATION) {
            return "AUTHENTICATION_FAILED";
        }
        if (failureReason == WifiManager.STATUS_LOCAL_ONLY_CONNECTION_FAILURE_NOT_FOUND) {
            return "NETWORK_NOT_FOUND";
        }
        if (failureReason == WifiManager.STATUS_LOCAL_ONLY_CONNECTION_FAILURE_USER_REJECT) {
            return "USER_DENIED";
        }
        // ASSOCIATION, IP_PROVISIONING, NO_RESPONSE, UNKNOWN -> CONNECTION_FAILED
        return "CONNECTION_FAILED";
    }

    private String mapLocalOnlyFailureStage(int failureReason) {
        if (failureReason == WifiManager.STATUS_LOCAL_ONLY_CONNECTION_FAILURE_AUTHENTICATION) {
            return "authentication";
        }
        if (failureReason == WifiManager.STATUS_LOCAL_ONLY_CONNECTION_FAILURE_IP_PROVISIONING) {
            return "ipProvisioning";
        }
        if (failureReason == WifiManager.STATUS_LOCAL_ONLY_CONNECTION_FAILURE_ASSOCIATION) {
            return "association";
        }
        return "request";
    }

    private void completeConnectSuccess(int generation, @Nullable Network network, boolean keepCallbackOnSuccess) {
        if (!isPendingConnectAttempt(generation)) {
            return;
        }

        // Legacy path: bind after SSID confirmation when requested.
        if (!keepCallbackOnSuccess && Boolean.TRUE.equals(pendingAutoRouteTraffic)) {
            bindingSucceeded = false;
            Network wifiNetwork = getOnlyWifiNetwork();
            if (wifiNetwork == null || !bindConnectNetwork(generation, wifiNetwork)) {
                if (isPendingConnectAttempt(generation)) {
                    completeConnectFailure(
                        generation,
                        "CONNECTION_FAILED",
                        "Cannot identify a Wi-Fi network for app routing.",
                        "verification",
                        null,
                        null,
                        true
                    );
                }
                return;
            }
        }

        if (!connectionState.finish()) {
            return;
        }
        PluginCall call = activeConnectCall;
        activeConnectCall = null;
        clearConnectTimeout();
        clearSsidVerification();
        removeLocalOnlyFailureListener();
        logConnect("success");
        call.resolve();
    }

    private void completeConnectFailure(
        int generation,
        String code,
        String message,
        String stage,
        @Nullable Integer nativeCode,
        @Nullable String nativeMessage,
        boolean cleanupNetworkCallback
    ) {
        if (generation != connectGeneration || activeConnectCall == null) {
            return;
        }

        if (connectionState != null && connectionState.isPending()) {
            connectionState.finish();
        }
        PluginCall call = activeConnectCall;
        activeConnectCall = null;

        clearConnectTimeout();
        clearSsidVerification();
        removeLocalOnlyFailureListener();

        logConnect("failure:" + code);
        rejectConnect(call, code, message, stage, nativeCode, nativeMessage);
        if (cleanupNetworkCallback) {
            releaseConnection();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private final class WifiConnectCallback extends ConnectivityManager.NetworkCallback {

        private final int generation;
        private final String ssid;

        WifiConnectCallback(int generation, String ssid) {
            this.generation = generation;
            this.ssid = ssid;
        }

        @RequiresApi(api = Build.VERSION_CODES.S)
        WifiConnectCallback(int generation, String ssid, int flags) {
            super(flags);
            this.generation = generation;
            this.ssid = ssid;
        }

        @Override
        public void onAvailable(@NonNull Network network) {
            connectHandler.post(() -> {
                if (!isPendingConnectAttempt(generation) || !connectionState.onAvailable(network)) {
                    return;
                }
                connectionStage = "verification";
                logConnect("available");
                if (Boolean.TRUE.equals(pendingAutoRouteTraffic) && !bindConnectNetwork(generation, network)) {
                    return;
                }
                startSsidVerification(generation, network, ssid, true);
            });
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities capabilities) {
            connectHandler.post(() -> {
                if (!isPendingConnectAttempt(generation)) {
                    return;
                }
                TransportInfo transport = capabilities.getTransportInfo();
                String currentSsid = transport instanceof WifiInfo ? ((WifiInfo) transport).getSSID() : null;
                if (connectionState.onCapabilities(network, currentSsid)) {
                    // Use the supplied capabilities: getNetworkCapabilities() redacts SSIDs.
                    if (ssidMatches(ssid, network)) {
                        completeConnectSuccess(generation, network, true);
                    }
                }
            });
        }

        @Override
        public void onUnavailable() {
            connectHandler.post(() -> {
                if (isPendingConnectAttempt(generation)) {
                    // A detailed failure listener may run first; otherwise settle generically.
                    completeConnectFailure(
                        generation,
                        "CONNECTION_FAILED",
                        "Failed to connect to network.",
                        "association",
                        null,
                        null,
                        true
                    );
                }
            });
        }

        @Override
        public void onLost(@NonNull Network network) {
            connectHandler.post(() -> {
                if (!isCurrentConnectAttempt(generation) || connectionState == null || !network.equals(connectionState.getNetwork())) {
                    return;
                }
                if (isPendingConnectAttempt(generation)) {
                    completeConnectFailure(
                        generation,
                        "CONNECTION_FAILED",
                        "Requested Wi-Fi network was lost.",
                        connectionStage,
                        null,
                        null,
                        true
                    );
                } else {
                    logConnect("lost");
                    releaseConnection();
                }
            });
        }
    }

    private boolean bindConnectNetwork(int generation, Network network) {
        if (!isPendingConnectAttempt(generation)) {
            return false;
        }
        try {
            bindingSucceeded = processBinding.bind(network);
            logConnect("binding:" + bindingSucceeded);
            if (bindingSucceeded) {
                return true;
            }
        } catch (Exception e) {
            bindingSucceeded = false;
            logConnect("binding:false");
        }
        completeConnectFailure(
            generation,
            "CONNECTION_FAILED",
            "Failed to route app traffic through the requested Wi-Fi network.",
            "verification",
            null,
            null,
            true
        );
        return false;
    }

    private boolean releaseConnection() {
        clearConnectTimeout();
        clearSsidVerification();
        removeLocalOnlyFailureListener();
        if (connectionState != null) {
            connectionState.release();
        }
        boolean released = localOnlyFailureListener == null;
        if (processBinding != null) {
            try {
                released = processBinding.release() && released;
            } catch (Exception e) {
                released = false;
            }
        }
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
                networkCallback = null;
            } catch (IllegalArgumentException ignored) {
                // Callback already unregistered by Android.
                networkCallback = null;
            } catch (Exception e) {
                released = false;
                logConnect("callback-cleanup-failed");
            }
        }
        if (connectionState != null) {
            logConnect("cleanup:" + released);
        }
        return released;
    }

    private void logConnect(String event) {
        android.util.Log.d(
            "CapacitorWifi",
            "connect attempt=" + connectGeneration + " elapsedMs=" + (SystemClock.elapsedRealtime() - connectStartedAt) + " event=" + event
        );
    }

    private void rejectConnect(
        PluginCall call,
        String code,
        String message,
        String stage,
        @Nullable Integer nativeCode,
        @Nullable String nativeMessage
    ) {
        JSObject data = new JSObject();
        data.put("platform", "android");
        data.put("androidApiLevel", Build.VERSION.SDK_INT);
        data.put("connectionStage", stage);
        if (connectionState != null && call.getCallbackId().equals(attemptedCallId)) {
            data.put("ssidVerification", ssidVerification.name().toLowerCase(java.util.Locale.ROOT));
            data.put("elapsedMs", SystemClock.elapsedRealtime() - connectStartedAt);
            if (bindingSucceeded != null) {
                data.put("bindingSucceeded", bindingSucceeded);
            }
        }
        if (nativeCode != null) {
            data.put("nativeCode", nativeCode);
        }
        if (nativeMessage != null) {
            data.put("nativeMessage", nativeMessage);
        }
        call.reject(message, code, data);
    }

    private void clearConnectTimeout() {
        if (connectTimeoutRunnable != null) {
            connectHandler.removeCallbacks(connectTimeoutRunnable);
            connectTimeoutRunnable = null;
        }
    }

    private void clearSsidVerification() {
        if (ssidVerifyRunnable != null) {
            connectHandler.removeCallbacks(ssidVerifyRunnable);
            ssidVerifyRunnable = null;
        }
    }

    private void removeLocalOnlyFailureListener() {
        if (localOnlyFailureListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                wifiManager.removeLocalOnlyConnectionFailureListener(localOnlyFailureListener);
                localOnlyFailureListener = null;
            } catch (IllegalArgumentException ignored) {
                // already removed
                localOnlyFailureListener = null;
            } catch (Exception e) {
                logConnect("failure-listener-cleanup-failed");
            }
        }
    }
}
