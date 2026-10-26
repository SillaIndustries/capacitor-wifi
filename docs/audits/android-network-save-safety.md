# Android network-save safety

## What changed

`addNetwork()` no longer opens Android's saved-network dialog on Android
11, 12, or 12L (SDK 30–32). It rejects with Capacitor's `UNAVAILABLE` code
and recommends `connect()` for a temporary session or manual saving in
WiFi settings. Android 10 (SDK 29), which predates the Settings save API,
also rejects with `UNAVAILABLE` rather than trying to launch it.

Android 13+ keeps the Settings save flow and waits for its result. Android
9 and earlier retain the legacy persistent-save API. No temporary session
is silently substituted for a persistent save, and the plugin never opens
manual WiFi settings as an automatic fallback.

The native result callback rejects malformed or unreadable result extras
instead of throwing out of Capacitor's callback and leaving the JavaScript
promise pending. Canceled, missing, failed, and unrecognized results remain
failures; successful saves and already-existing configurations resolve.

`isNetworkSaved()` now rejects with `UNAVAILABLE` on Android 10+ rather than
querying app-owned suggestions and returning a misleading false result for
user-saved configurations. Android 9 and earlier retain their configured
network lookup. iOS behavior is unchanged. Signatures remain unchanged;
callers must handle these documented rejections.

## Why a version guard is necessary

The connected Android 12 phone's crash records identified an uncaught
`IllegalArgumentException: No Wifi icon found for level: -1` in
`com.android.settings.wifi.addappnetworks.AddAppNetworksFragment`.
Android then force-finished both the Settings activity and the calling
Silla/Giotto activity. A try/catch in this plugin cannot intercept an
exception in that other process.

The AOSP Android 11, 12, and 12L implementations forward an unreachable
signal level directly to the icon renderer. Android 13 adds a check for
`WifiEntry.WIFI_LEVEL_UNREACHABLE` before rendering. Blocking SDK 30–32 is
a conservative safeguard even on OEM builds that might have backported
the fix; there is no reliable public API to inspect their private renderer.
It prevents the identified save-dialog route from this plugin on those
versions. It does not guarantee that arbitrary OEM Settings implementations,
other dialogs, or unrelated application code can never crash.

- [AOSP Android 12 save dialog](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/android12-release/src/com/android/settings/wifi/addappnetworks/AddAppNetworksFragment.java)
- [AOSP Android 13 save dialog](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/android13-release/src/com/android/settings/wifi/addappnetworks/AddAppNetworksFragment.java)

Android documents that `ACTION_WIFI_ADD_NETWORKS` saves user-owned network
configurations through an API separate from network suggestions. Modern
ordinary apps cannot reliably read the user's saved-network list. A cache
of successful saves would also become stale when a user forgets a network.

- [Android saved-network API](https://developer.android.com/develop/connectivity/wifi/wifi-save-network-passpoint-config)
- [WifiManager configured-network access](https://developer.android.com/reference/android/net/wifi/WifiManager#getConfiguredNetworks())

## Silla follow-up for its separate branch

No Silla files were modified for this change. The inspected Silla connection
procedure already calls `connect()` directly with `autoRouteTraffic: true`,
without `addNetwork()` or `isNetworkSaved()`. Keep that approach. It retains
the native connection approval flow and process routing needed for local
HTTP and WebSocket traffic, without persistent saving.

To consume this patch, update Silla's plugin dependency/lockfile to the
revision containing these changes, reinstall dependencies, sync the native
project, and rebuild. The existing locked Git revision does not include
uncommitted changes in an adjacent checkout.

Any other application branch that still checks saved status before joining
a charger must remove that gate and use `connect()` directly. Do not catch
`UNAVAILABLE` as `isSaved: false` and call `addNetwork()` automatically.
An explicit persistent-save feature must surface its unavailable result on
Android 10–12 without retrying the blocked system UI.

## Follow-up: the connection approval dialog

`connect()` also invokes system-owned UI on Android 10+: the plugin builds
an exact-SSID `WifiNetworkSpecifier` and calls
`ConnectivityManager.requestNetwork()`. This is independent of the
saved-network activity blocked by the patch above.

Source inspection found a similar unchecked icon-rendering path in the
AOSP Android 10–12 `NetworkRequestDialogFragment`: a WiFi entry's signal
level is passed directly to `Utils.getWifiIconResource(level)`. The Android
13 implementation checks for the unreachable level before rendering it.
Thus removing the persistent-save prompt does not establish that all older
connection approval dialogs are safe.

There is an important qualification: Android 10 uses the list dialog, while
AOSP Android 11+ has a separate single-SSID dialog that displays no signal
icon. The plugin already requests an exact SSID; which UI an OEM actually
uses must be checked on that phone. The existence of vulnerable list-dialog
source is not proof that an exact-SSID request on Android 11/12 took that
path. Older connection activities also show/dismiss fragments in response
to asynchronous system callbacks, so a reported dialog crash could be a
different lifecycle failure rather than the signal-level exception.

- [AOSP Android 10 connection list](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/android10-release/src/com/android/settings/wifi/NetworkRequestDialogFragment.java)
- [AOSP Android 12 connection list](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/android12-release/src/com/android/settings/wifi/NetworkRequestDialogFragment.java)
- [AOSP Android 13 guarded connection list](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/android13-release/src/com/android/settings/wifi/NetworkRequestDialogFragment.java)
- [AOSP Android 12 single-SSID dialog](https://android.googlesource.com/platform/packages/apps/Settings/+/refs/heads/android12-release/src/com/android/settings/wifi/NetworkRequestSingleSsidDialogFragment.java)

No device was connected during this follow-up. The earlier retained native
traces identify `AddAppNetworksFragment`, not `NetworkRequestDialogFragment`.
A native crash trace from the affected connect attempt is needed to
attribute that incident and select a targeted mitigation. No connection
API was disabled based only on this source comparison. An exception handler
in the plugin cannot catch a crash inside the separate Settings process;
completely preventing a faulty system dialog from executing requires
avoiding it, with corresponding connection-flow changes.

## Validation

Completed on Linux:

- `bun run verify:android` with Java 21: debug/release builds and 47 tests
  per variant passed, including 13 save-flow regression tests per variant.
- `bun run verify:web`: TypeScript, documentation generation, and bundles passed.
- `bun run fmt`, `bun run lint`, and `bun run check:wiring` completed.
  ESLint reports six existing unused-argument warnings in `src/web.ts`;
  SwiftLint skips execution on Linux.
- `bun run verify` was attempted but stops at its first iOS command because
  `xcodebuild` is unavailable. Android and web were verified separately.
- No ADB device was connected for a physical-device retest.

Robolectric tests exercise public plugin methods and native result callbacks:
blocked Android versions never launch a save intent; modern saved status
rejects rather than claiming false; supported saves wait for their result;
successful, canceled, missing, failed, malformed, and unreadable results
settle correctly; lost calls are harmless; and legacy save/lookup tracks
configuration removal. These run in an Android 13 sandbox with SDK branch
selection overridden for version-policy cases. They are not physical-device
or cross-version system-image tests.

Before shipping, rebuild a consuming app and test the affected Android 12
phone (safe rejection, temporary connection, local API and WebSocket), plus
Android 13+ persistent saving and cancellation. Verify iOS with Xcode.
