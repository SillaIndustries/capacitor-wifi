# iOS: connecting to Prism Wi-Fi without internet

## Conclusion

Source investigation found no missing core iOS Wi-Fi capability in Silla and no plugin condition requiring internet access. It does identify plugin connection-contract weaknesses and app-side diagnostic/readiness limitations. None establishes the cause of the reported refusal to switch from home Wi-Fi to an offline Prism AP without a physical-device trace.

The Android redacted-SSID regression is not shared native code with iOS. Do not attribute this report to that regression or assume the Android rollback fixes iOS.

Forcing a disconnection from an arbitrary user-saved home network is not available through `NEHotspotConfigurationManager`. Applying the Prism configuration is the supported request to join; removing a configuration can only remove one this app previously added.

## Scope / evidence

Inspected Silla's checked-in source/configuration, upstream tagged Swift source, and the **published npm archives** for 8.4.1, 8.5.0, and 8.5.4. Investigated on Linux; no signed iOS application, provisioning profile, or physical iPhone was inspected. Installed/released binaries may differ from repository configuration; capture `getPluginVersion()` on the affected build.

`package.json:80` pins 8.4.1. `ios/App/CapApp-SPM/Package.swift:34` references that version's pnpm package directory.

## Silla configuration

| Requirement | Repository evidence | Assessment |
| --- | --- | --- |
| Hotspot Configuration entitlement | `ios/App/App/App.entitlements:16` | Present |
| Access Wi-Fi Information entitlement | `ios/App/App/App.entitlements:20` | Present |
| Entitlements used by build | `ios/App/App.xcodeproj/project.pbxproj:305,333` | Debug and Release use `App/App.entitlements` |
| Location usage description | `ios/App/App/Info.plist:85` | Present |
| Local Network usage description | `ios/App/App/Info.plist:81` | Present |
| Plain HTTP charger endpoints | `ios/App/App/Info.plist:40-72` | Exceptions exist for 192.168.8.1, 192.168.10.1, and 192.168.12.1 |

These declarations do not prove permissions were granted or the capabilities survived signing. Local Network permission controls charger traffic, not Wi-Fi association. A denial can look like a connection failure even when Settings shows Prism connected. Apple documents IP-literal ATS exception support from iOS 17; investigate ATS separately on older supported iOS versions if HTTP reports an ATS error. ATS does not explain a failure to change SSID.

`stores/local.ts:1236-1240` supplies SSID/password and `autoRouteTraffic: true`. That option is **Android-only**; iOS ignores it. There is no missing iOS equivalent flag in the plugin API.

## Plugin findings

Native file: `ios/Sources/CapacitorWifiPlugin/CapacitorWifiPlugin.swift`.

### 8.4.1: configuration success is treated as connection success

`connect()` constructs a WPA/open `NEHotspotConfiguration`, sets `joinOnce = false`, calls `apply()`, and resolves immediately when the completion handler returns no error.

Apple explicitly states that successful `apply()` does **not** mean the device joined the network, and association does not mean TCP/IP is ready. This is a confirmed connection-contract weakness in 8.4.1, but Silla mitigates it by polling SSID and IPv4 for 20 seconds after the native call (`stores/local.ts:1254-1283`). It is not by itself proof of this reported failure.

8.4.1 also rejects `alreadyAssociated` like any other error. Silla retains that error but still polls, so an already-associated Prism connection with readable SSID and IPv4 can succeed.

### 8.5.0 / 8.5.4: verification has different weaknesses

These versions accept `alreadyAssociated`, then poll `NEHotspotNetwork.fetchCurrent()` every 500 ms for the requested SSID, with a default 30-second deadline. They still use `joinOnce = false`; there is no internet-validation check.

- A nil `fetchCurrent()` result is treated like a not-yet-connected network until timeout. Missing entitlement or inaccessible network information can therefore masquerade as an association failure.
- The deadline starts **before** `apply()`, so time spent answering the system consent dialog consumes the verification budget. Accepting after the deadline produces a timeout before the first SSID read, even if the connection succeeds.
- There is no independent watchdog while `apply()` is pending; the documented timeout does not bound the entire operation.
- Native verification confirms SSID, not IPv4/service readiness.
- On verification timeout iOS does not remove the hotspot configuration. Unlike Android's timeout cleanup, it does not explicitly release the Wi-Fi request/network configuration.
- `disconnect()` does not cancel/invalidate an in-progress verification attempt.

The published 8.5.0 and 8.5.4 Swift files differ only in the version string and the timeout-option presence check (`hasOption` versus `getValue`/`NSNull`). These connection behaviors are shared.

Apple requires Access Wi-Fi Information and at least one eligibility condition for `fetchCurrent()`: precise location authorization, the app having configured the current network, an active qualifying VPN configuration, or active DNS settings configuration. Consequently, location denial alone does not necessarily prevent reading an app-configured Prism network. The plugin's location permission check reports authorization status only; it does not distinguish precise/reduced accuracy or check Local Network permission.

### `joinOnce` is a possible experiment, not a demonstrated fix

Both releases hardcode `joinOnce = false` for persistent configuration. The JS API exposes no choice. Apple supports temporary foreground connections with `joinOnce = true`, which may better fit some accessory setup flows, but its documentation does not promise to override internet preference or force an offline AP to remain selected.

Do not change this globally based on this report alone: `true` disconnects/removes the configuration after more than 15 seconds in the background, device sleep, or app termination. That matters to charger monitoring. If testing proves a benefit, propose an optional iOS `joinOnce` argument with the current persistent default preserved.

## App-side findings

- Silla polls the actual requested SSID and IPv4 rather than accepting internet reachability as charger connectivity. The IPv4 preference in the plugin and the app's IPv4 gate already address the known early IPv6-link-local readiness problem.
- The IPv4 regex in `utils/local-wifi.ts:112-114` accepts self-assigned `169.254.x.x` addresses. It is not proof of a usable route to the charger. SSID + syntactic IPv4 should be followed by actual local service readiness; a matching SSID with link-local IPv4 is a DHCP diagnostic, not internet refusal.
- `stores/local.ts:1310-1316` returns a generic failure regardless of the last `ssidNotFound`/`ipNotAssigned` cause. This hides the distinction from callers.
- `utils/local-wifi.ts:34-67` drops `code` and `data` for errors that are `instanceof Error`, and drops `data` for plain structured errors. The newer plugin's `connectionStage`/`nativeCode` can therefore be lost from exported diagnostics. Preserve selected native error metadata when improving diagnostics.
- `stores/wifi.ts:442-466` checks location permission after any transport failure. This can suggest location access even when the actual failure is Local Network denial, DHCP, HTTP, or login. Treat it as a diagnostic limitation rather than proof of missing permissions.
- `stores/local.ts:1329-1333` disconnects by the *current* SSID. If iOS already switched back to home Wi-Fi, that does not clean up the previous Prism configuration. Tracking the app-owned target SSID is more reliable. It still cannot disconnect a user-owned home network.

## Recommended next steps

1. Reproduce on the affected iPhone with offline Prism, home Wi-Fi initially connected, and cellular enabled. Record iOS version, app/native plugin version, timestamped `apply()` result (NSError domain/code), foreground transitions, requested/current SSID, `en0` IPv4, and charger `/health`, `/login`, and WebSocket outcomes. Avoid recording passwords/tokens.
2. Observe Settings during/after failure: did association never happen, did Prism join and later disappear, or did it stay connected while local traffic failed? Capture the existing `connect:preflight`, `connect:request-error`, `connect:poll`, and request diagnostic events.
3. Repeat with home AP unavailable, then cellular disabled, then manual Prism selection in Settings. If manual selection stays connected and local requests succeed but `apply()` does not switch, focus on hotspot configuration/system association. If Settings stays on Prism but requests fail, focus on DHCP, route readiness, Local Network authorization, and HTTP errors.
4. Verify Local Network access in Settings and the entitlements in the signed archive. Test precise location on/off independently; permission descriptions alone are not authorization.
5. Compare 8.4.1 and 8.5.4 on the same device/AP. Include delayed consent beyond 30 seconds to isolate the newer deadline issue. Keep the current Android mitigation while investigating iOS.
6. If association is the remaining failure, test a small native `NEHotspotConfigurationManager` reproduction with `joinOnce = false` and `true`. Only propose an offline-AP fix after comparing those results.

Suitable upstream improvements are accurate association/readiness semantics, interpretable unavailable-SSID diagnostics, bounded request/verification timing, cancellation ownership, and optional temporary iOS configuration. They should be presented separately from a claim that the user's offline-network report has been reproduced.

## References

- [Plugin guide/repository](https://github.com/Cap-go/capacitor-wifi)
- [8.4.1 iOS source](https://github.com/Cap-go/capacitor-wifi/blob/8.4.1/ios/Sources/CapacitorWifiPlugin/CapacitorWifiPlugin.swift)
- [8.5.4 iOS source](https://github.com/Cap-go/capacitor-wifi/blob/8.5.4/ios/Sources/CapacitorWifiPlugin/CapacitorWifiPlugin.swift)
- [Published npm 8.4.1 archive](https://registry.npmjs.org/@capgo/capacitor-wifi/-/capacitor-wifi-8.4.1.tgz)
- [Published npm 8.5.0 archive](https://registry.npmjs.org/@capgo/capacitor-wifi/-/capacitor-wifi-8.5.0.tgz)
- [Published npm 8.5.4 archive](https://registry.npmjs.org/@capgo/capacitor-wifi/-/capacitor-wifi-8.5.4.tgz)
- [Apple: apply configuration and connectivity readiness](https://developer.apple.com/documentation/networkextension/nehotspotconfigurationmanager/apply(_:completionhandler:))
- [Apple: fetchCurrent eligibility](https://developer.apple.com/documentation/networkextension/nehotspotnetwork/fetchcurrent(completionhandler:))
- [Apple: joinOnce lifecycle](https://developer.apple.com/documentation/networkextension/nehotspotconfiguration/joinonce)
- [Apple: removeConfiguration ownership](https://developer.apple.com/documentation/networkextension/nehotspotconfigurationmanager/removeconfiguration(forssid:))
- [Apple: ATS exception domains and IP addresses](https://developer.apple.com/documentation/bundleresources/information-property-list/nsapptransportsecurity/nsexceptiondomains)
