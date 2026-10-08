# fix(android): verify the requested Wi-Fi SSID without redacted network information

## Summary

Fix a suspected Android connection regression introduced in `@capgo/capacitor-wifi` 8.5.0 and present in 8.5.4. The plugin can associate with the requested charger Wi-Fi but fail its new SSID verification, time out, and release the temporary connection.

This is a proposed upstream fix/handoff, not an implemented native patch. Source inspection confirms the problematic code path; a physical-device test is still required to establish that it caused the reported failure.

## Release mitigation in Silla

Pin `@capgo/capacitor-wifi` to **8.4.1 exactly**, update the dependency lockfile, and regenerate native plugin references. Do not use `^8.4.1`, which permits reinstalling 8.5.x. Keep the startup, request-diagnostics, and offline-banner improvements.

The app upgraded from 8.4.1 to 8.5.4 in commit `9264b0af` on September 21, 2026, before the diagnostics/banner commit `d65d29db`. The earlier upgrade was part of broader Capacitor/iOS work. The rollback therefore also needs an iOS local-connection smoke test before an iOS release; it is not a claim that all intervening iOS changes are unnecessary.

## Problem / observed behavior

Environment: Silla app 1.9.4 under test, Android API 31 (Android 12), Wi-Fi plugin 8.5.4, charger local API, `autoRouteTraffic: true`.

After the user accepts Android's native Wi-Fi connection prompt:

1. The app returns to the foreground; Capacitor Network reports a connected cellular default network.
2. A cloud status request fails with a network error after approximately 89 ms, not its configured HTTP timeout.
3. `CapacitorWifi.connect()` rejects with `CONNECTION_TIMEOUT`, message `Timed out waiting for Wi-Fi connection confirmation.`, and `data.connectionStage: verification`.
4. A subsequent `getSsid()` briefly identifies the charger network; `getIpAddress()` reports no address.
5. SSID becomes unknown, then changes to the previous Wi-Fi network.
6. No charger `/health` or `/login` request appears in the supplied excerpt.

Expected: the plugin verifies the requested Wi-Fi connection and keeps it available for local HTTP and WebSocket traffic, even when that network has no internet access.

The timeout handler always labels its stage `verification`; this field alone does **not** prove that `onAvailable()` ran. Likewise, the post-timeout missing IP does not prove DHCP never completed before cleanup.

## Source comparison

Compared published npm archives for 8.4.1, 8.5.0, and 8.5.4, specifically:

`android/src/main/java/ee/forgr/plugin/capacitor_wifi/CapacitorWifiPlugin.java`

| Behavior | 8.4.1 | 8.5.0 / 8.5.4 |
| --- | --- | --- |
| Modern Android `onAvailable()` | Attempts process binding when requested, then resolves the call | Attempts binding, then starts SSID verification |
| Additional native SSID verification | Absent | Every 500 ms |
| Plugin-managed connection deadline | No equivalent timer in this path | 30 seconds by default; configurable `timeoutMs` |
| Concurrent connection attempts | No equivalent guard | `CONNECTION_IN_PROGRESS` rejection |
| Error reporting | Generic rejection messages | Structured codes, platform/API level, stage |
| Verification timeout | Not applicable | Rejects and unregisters the network callback |

Between 8.5.0 and 8.5.4, this Java file differs only in the plugin version string and the option-presence check (`call.hasOption("timeoutMs")` became `call.getData().has("timeoutMs")`). The verification code is unchanged.

## Root-cause candidate

`getConnectedSsid(network)` calls:

```java
NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
TransportInfo transportInfo = capabilities.getTransportInfo();
if (transportInfo instanceof WifiInfo) {
    return normalizeSsid(((WifiInfo) transportInfo).getSSID());
}
```

Android documents that `getNetworkCapabilities()` removes location-sensitive information. SSID is sensitive Wi-Fi information. The supported callback approach on Android 12+ uses `NetworkCallback.FLAG_INCLUDE_LOCATION_INFO`, with the necessary location permission and location setting.

If `WifiInfo` exists but contains an unknown SSID, `normalizeSsid()` returns null immediately. The later `wifiManager.getConnectionInfo()` fallback is skipped. Verification can therefore fail indefinitely despite association.

The standalone plugin `getSsid()` uses `wifiManager.getConnectionInfo()` directly. This explains why it can return the charger SSID while the verification helper fails.

On timeout, `completeConnectFailure()` unregisters the network callback, potentially releasing the temporary network. It does not explicitly clear `boundNetwork` or undo process binding. That is a related cleanup defect to address, with ownership/generation guards so an old failure cannot unbind a newer connection.

## Proposed implementation

- On API 31+, request location-inclusive network callbacks and inspect the supplied `NetworkCapabilities` in `onCapabilitiesChanged()` for the **requested network**. Guard API-specific constructors for older Android releases.
- Keep network identity and attempt generation checks. Do not accept an arbitrary current/default Wi-Fi network as proof that the requested charger is connected.
- Distinguish redacted/missing SSID information from a known SSID mismatch. Define an explicit permission/location-unavailable failure instead of silently polling unusable data until timeout.
- If using a legacy SSID fallback, do not return early solely because the network-specific SSID is redacted; also account for concurrent Wi-Fi networks before accepting the fallback.
- On failure/cancellation, release resources owned by that attempt and clear its process binding. Retain the callback after a successful temporary connection until explicit disconnect.
- Preserve structured errors, concurrency protection, and a bounded timeout.
- Add native diagnostic events for network availability, verification result/redaction, binding result, and cleanup. Avoid logging Wi-Fi passwords or other credentials.

Do not fix this by merely increasing the timeout, removing all verification, or disabling `autoRouteTraffic` in Silla. The app relies on process routing for reliable charger access on Android.

## App-side considerations

- `stores/local.ts` calls `WiFi.connect()` and then polls SSID/IPv4 for up to 20 seconds. In 8.5.x that polling begins **after** native verification times out and releases the network request; it cannot reliably rescue that attempt.
- The new offline monitor only reads network status; it does not switch or disconnect Wi-Fi.
- A connected cellular default network does not guarantee that this app's requests can use it while the process is bound to charger Wi-Fi. The cloud error is consistent with that routing transition, but the logs do not prove binding occurred.
- Do not suppress local HTTP/Bluetooth failures based on internet availability.

## Validation / acceptance criteria

- [ ] Reproduce on the affected Android 12 phone with 8.5.4 and capture native callback/SSID/binding events.
- [ ] Compare the same charger/phone with 8.4.1, changing only the plugin version.
- [ ] Test the patched version on Android 12+ with cellular enabled and a charger AP without internet.
- [ ] Confirm `connect()` resolves, local `/health` and `/login` succeed, and WebSocket traffic remains available.
- [ ] Verify location permission denied and location services disabled produce understandable, bounded failures.
- [ ] Verify a different/default SSID is not accepted; include simultaneous Wi-Fi networks where supported.
- [ ] Test wrong password, unavailable charger, cancellation, repeated connect, and concurrent attempts.
- [ ] Confirm failure/disconnect releases the correct binding and normal cloud traffic recovers.
- [ ] Test Android below API 31 for compatibility with guarded callback construction.
- [ ] Smoke-test iOS connection and IPv4 readiness after the release rollback.

No physical-device reproduction or native patch validation was performed during this source investigation. JavaScript tests alone cannot validate Android network association or routing.

## References

- [Published 8.4.1 package](https://www.npmjs.com/package/@capgo/capacitor-wifi/v/8.4.1)
- [Published 8.5.0 package](https://www.npmjs.com/package/@capgo/capacitor-wifi/v/8.5.0)
- [Published 8.5.4 package](https://www.npmjs.com/package/@capgo/capacitor-wifi/v/8.5.4)
- [Upstream repository](https://github.com/Cap-go/capacitor-wifi)
- [Android: getNetworkCapabilities and redaction](https://developer.android.com/reference/android/net/ConnectivityManager#getNetworkCapabilities(android.net.Network))
- [Android: NetworkCallback.FLAG_INCLUDE_LOCATION_INFO](https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback#FLAG_INCLUDE_LOCATION_INFO)
