package ee.forgr.plugin.capacitor_wifi;

import static org.junit.Assert.*;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import androidx.activity.result.ActivityResult;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.PluginCall;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33, manifest = Config.NONE)
public class WifiNetworkSaveTest {

    private static class TestPlugin extends CapacitorWifiPlugin {

        Intent launchedIntent;
        String callback;

        @Override
        public PermissionState getPermissionState(String alias) {
            return PermissionState.GRANTED;
        }

        @Override
        public void startActivityForResult(PluginCall call, Intent intent, String callbackName) {
            launchedIntent = intent;
            callback = callbackName;
        }
    }

    private static class Call extends PluginCall {

        int resolutions;
        int rejections;
        String errorCode;
        String errorMessage;
        JSObject resolvedData;

        Call() {
            super(null, "CapacitorWifi", "test-call", "test", new JSObject().put("ssid", "charger").put("password", "test-password"));
        }

        @Override
        public void resolve() {
            resolutions++;
        }

        @Override
        public void resolve(JSObject data) {
            resolutions++;
            resolvedData = data;
        }

        @Override
        public void reject(String message, String code, Exception exception, JSObject data) {
            rejections++;
            errorCode = code;
            errorMessage = message;
        }
    }

    @After
    public void restoreSdk() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 33);
    }

    @Test
    public void vulnerableSaveDialogIsNeverLaunchedOnAndroid11Or12() {
        // Exercise the public plugin entry point, not just a version predicate.
        // The Settings crash cannot be caught in the calling app's process.
        for (int sdk : new int[] { 30, 31, 32 }) {
            ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", sdk);
            TestPlugin plugin = new TestPlugin();
            Call call = new Call();
            plugin.addNetwork(call);
            assertNull("must not launch Settings on SDK " + sdk, plugin.launchedIntent);
            assertEquals("UNAVAILABLE", call.errorCode);
            assertEquals(1, call.rejections);
            assertEquals(0, call.resolutions);
            assertTrue(call.errorMessage.contains("connect()"));
        }
    }

    @Test
    public void android10DoesNotLaunchAnUnsupportedSaveIntent() {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 29);
        TestPlugin plugin = new TestPlugin();
        Call call = new Call();
        plugin.addNetwork(call);
        assertNull(plugin.launchedIntent);
        assertEquals("UNAVAILABLE", call.errorCode);
        assertEquals(1, call.rejections);
    }

    @Test
    public void supportedSaveWaitsForItsResult() {
        TestPlugin plugin = new TestPlugin();
        Call call = new Call();
        plugin.addNetwork(call);
        assertEquals(Settings.ACTION_WIFI_ADD_NETWORKS, plugin.launchedIntent.getAction());
        assertTrue(plugin.launchedIntent.hasExtra(Settings.EXTRA_WIFI_NETWORK_LIST));
        assertEquals("handleAddNetworkModernResult", plugin.callback);
        assertEquals(0, call.resolutions);
        assertEquals(0, call.rejections);
    }

    @Test
    public void modernSavedStatusIsUnknownInsteadOfFalse() {
        for (int sdk : new int[] { 29, 30, 31, 32, 33, 34, 35, 36 }) {
            ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", sdk);
            TestPlugin plugin = new TestPlugin();
            Call call = new Call();
            // No WifiManager or bridge is installed: a suggestion query or
            // attempted UI launch would fail rather than produce this result.
            plugin.isNetworkSaved(call);
            assertEquals("UNAVAILABLE", call.errorCode);
            assertEquals(1, call.rejections);
            assertEquals(0, call.resolutions);
            assertNull(plugin.launchedIntent);
        }
    }

    @Test
    public void missingSsidStillFailsValidation() {
        Call call = new Call();
        call.getData().remove("ssid");
        TestPlugin plugin = new TestPlugin();
        plugin.addNetwork(call);
        assertEquals("SSID is required", call.errorMessage);
        assertNull(plugin.launchedIntent);
    }

    @Test
    public void legacySaveAndLookupStillUseConfiguredNetworks() throws Exception {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 28);
        TestPlugin plugin = new TestPlugin();
        WifiManager manager = (WifiManager) RuntimeEnvironment.getApplication().getSystemService(Context.WIFI_SERVICE);
        ReflectionHelpers.setField(plugin, "wifiManager", manager);

        Call before = new Call();
        plugin.isNetworkSaved(before);
        assertEquals(1, before.resolutions);
        assertFalse(before.resolvedData.getBoolean("isSaved"));

        Call save = new Call();
        plugin.addNetwork(save);
        assertEquals(1, save.resolutions);
        assertEquals(0, save.rejections);
        assertNull(plugin.launchedIntent);

        Call after = new Call();
        plugin.isNetworkSaved(after);
        assertEquals(1, after.resolutions);
        assertTrue(after.resolvedData.getBoolean("isSaved"));

        for (WifiConfiguration network : manager.getConfiguredNetworks()) {
            manager.removeNetwork(network.networkId);
        }
        Call forgotten = new Call();
        plugin.isNetworkSaved(forgotten);
        assertFalse(forgotten.resolvedData.getBoolean("isSaved"));
    }

    @Test
    public void successfulAndAlreadyExistingSavesResolve() throws Exception {
        for (int code : new int[] { Settings.ADD_WIFI_RESULT_SUCCESS, Settings.ADD_WIFI_RESULT_ALREADY_EXISTS }) {
            Call call = new Call();
            handleResult(call, Activity.RESULT_OK, codes(code));
            assertEquals(1, call.resolutions);
            assertEquals(0, call.rejections);
        }
    }

    @Test
    public void failedOrUnexpectedSaveCodesReject() throws Exception {
        for (int code : new int[] { Settings.ADD_WIFI_RESULT_ADD_OR_UPDATE_FAILED, 100 }) {
            Call call = new Call();
            handleResult(call, Activity.RESULT_OK, codes(code));
            assertEquals(0, call.resolutions);
            assertEquals(1, call.rejections);
        }
    }

    @Test
    public void canceledSaveRejectsEvenIfPayloadClaimsSuccess() throws Exception {
        Call call = new Call();
        handleResult(call, Activity.RESULT_CANCELED, codes(Settings.ADD_WIFI_RESULT_SUCCESS));
        assertEquals(0, call.resolutions);
        assertEquals(1, call.rejections);
    }

    @Test
    public void missingAndEmptySaveResultsReject() throws Exception {
        Intent emptyCodes = new Intent().putIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST, new ArrayList<>());
        for (Intent data : new Intent[] { null, new Intent(), emptyCodes }) {
            Call call = new Call();
            handleResult(call, Activity.RESULT_OK, data);
            assertEquals(0, call.resolutions);
            assertEquals(1, call.rejections);
        }
    }

    @Test
    public void malformedResultCannotEscapeCallbackOrLeavePromisePending() throws Exception {
        Intent data = new Intent().putStringArrayListExtra(
            Settings.EXTRA_WIFI_NETWORK_RESULT_LIST,
            new ArrayList<>(Arrays.asList("invalid"))
        );
        Call call = new Call();
        handleResult(call, Activity.RESULT_OK, data);
        assertEquals(0, call.resolutions);
        assertEquals(1, call.rejections);
    }

    @Test
    public void unreadableExtrasCannotEscapeCallback() throws Exception {
        Intent data = new Intent() {
            @Override
            public boolean hasExtra(String name) {
                throw new IllegalStateException("Unreadable result extras");
            }
        };
        Call call = new Call();
        handleResult(call, Activity.RESULT_OK, data);
        assertEquals(0, call.resolutions);
        assertEquals(1, call.rejections);
    }

    @Test
    public void callbackAfterCallWasLostDoesNotCrash() throws Exception {
        handleResult(null, Activity.RESULT_OK, codes(Settings.ADD_WIFI_RESULT_SUCCESS));
    }

    private Intent codes(int code) {
        return new Intent().putIntegerArrayListExtra(Settings.EXTRA_WIFI_NETWORK_RESULT_LIST, new ArrayList<>(Arrays.asList(code)));
    }

    private void handleResult(PluginCall call, int resultCode, Intent data) throws Exception {
        Method method = CapacitorWifiPlugin.class.getDeclaredMethod("handleAddNetworkModernResult", PluginCall.class, ActivityResult.class);
        method.setAccessible(true);
        method.invoke(new TestPlugin(), call, new ActivityResult(resultCode, data));
    }
}
