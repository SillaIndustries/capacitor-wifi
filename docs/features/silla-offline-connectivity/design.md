# Design

Status: M1/M2 implemented for batched owner review; later architecture remains proposed. Device behavior and full native iOS compilation are unverified.

## Design Summary and Current System

Retain the generic Capacitor plugin and introduce explicit attempt ownership and observation semantics. Current source entry points are `android/src/main/java/ee/forgr/plugin/capacitor_wifi/CapacitorWifiPlugin.java`, `ios/Sources/CapacitorWifiPlugin/CapacitorWifiPlugin.swift`, and `src/definitions.ts`. Audit evidence concerns published versions through 8.5.4; this checkout is 8.5.5.

## Proposed Architecture, Data, and State

- A generation/attempt identity owns its promise, deadline, verification work, network callback, failure listener, and requested binding/configuration effects.
- Terminal completion is single-use. Stale callbacks and cleanup cannot mutate another generation.
- Successful Android network ownership continues beyond promise completion until disconnect/replacement.
- Android 12+ callbacks request location-inclusive information, guarded for older APIs. Verification consumes callback-provided capabilities for the requested network. Fallback eligibility must account for simultaneous Wi-Fi connections.
- Observations distinguish unknown/redacted information, known mismatch, and match. Permissions are reported only when supported by actual authorization evidence.
- Process routing and network-request internet capability policy are reviewed separately; device tests decide the supported offline policy.
- iOS uses the native apply callback plus bounded plugin lifecycle and invalidation. A timeout cannot promise cancellation of the system's pending operation.
- Configuration retention is explicit; persistence ownership must not be inferred merely from the current SSID.
- Diagnostics use bounded storage with attempt identity and monotonic elapsed time. Absolute timestamps correlate with app logs.
- Generic readiness observations stop short of charger protocol checks; Silla owns health/login/WebSocket validation and user recovery.

## Requirement Traceability

### M1 implementation boundaries

Current 8.5.5 source was checked against the Android audit. Connection entry points, permission continuations, callback state changes, timers, completion, and cleanup now run on one main handler. A generation identifies each native attempt; callbacks also require a pending attempt before verification/binding. Completion retains the successful callback; failure, loss, disconnect, replacement, and destruction release owned resources.

`WifiConnectionState` holds requested-network identity, available/mismatch/match observation policy, and terminal/released state. `WifiProcessBinding` tracks only bindings this plugin establishes, avoids clearing a different component's binding, and retains ownership if cleanup fails so it can be retried. Cleanup failures block replacement rather than silently discarding callback ownership.

Android 12+ uses `FLAG_INCLUDE_LOCATION_INFO` and supplied `onCapabilitiesChanged` capabilities, never a redacted snapshot to read SSID. Android 10/11 may use the legacy SSID reader only when exactly one visible Wi-Fi network exists and equals the requested network. A known mismatch is not overridden by fallback. Below Android 10 the legacy SSID reader remains; routing selects a Wi-Fi network rather than a potentially cellular default.

Modern Android checks precise location permission and location services before the native request; the existing location permission callback now resumes the appropriate platform path and ignores cancelled calls. Unknown SSID observations remain bounded by the connection deadline and are labeled unavailable rather than falsely diagnosed as association failure.

Owner-approved contract: all modern requests remove the internet capability requirement; routing remains optional, and requested binding failure rejects with CONNECTION_FAILED. Android timeout still includes Wi-Fi consent time, but begins after location authorization. Success means SSID association plus requested binding, not charger-service readiness.

Initial diagnostics are native log events without SSIDs/passwords, and optional error fields `ssidVerification`, `bindingSucceeded`, and `elapsedMs`. No support snapshot/event API or persistent diagnostic buffer is introduced in M1.

### M2 implementation boundaries

Connection/disconnect state, native completions, and scheduled work are confined to the main queue. A `ConnectAttempt` owns its call, target, identity, lifecycle, watchdog, and polling work. Identity is rechecked after every native callback; terminal completion cancels work, clears the active slot, and releases the call reference exactly once.

`WifiConnectionLifecycle` is Foundation-only policy with a monotonic request deadline and a fresh verification deadline. Request default is 120 seconds; verification default is 30 seconds. An independent watchdog runs while apply or fetchCurrent is pending. Callback processing rechecks deadlines, preventing delayed queue delivery from producing success after expiry. Unknown SSID information remains distinct from a known mismatch. The app must be able to execute for watchdog delivery.

`WifiConfigurationOwnership` versions mutations per SSID. Disconnect cancels the plugin attempt, then removes its explicit target or the pending/latest connect target. Pending OS apply contexts are weakly indexed so a disconnect after timeout can mark their late completion for cleanup without retaining calls or creating ownership cycles. Cleanup is suppressed when a newer connect/addNetwork request owns that same target. addNetwork mutations are serialized/registered for this protection; its existing completion contract is otherwise unchanged.

If no connection target is tracked, disconnect preserves the existing current-SSID fallback with a 120-second lookup watchdog and a mutation-sequence check. A delayed fallback cannot remove a newly requested configuration. An explicit different SSID cancels pending verification but removes only the explicit target.

Persistent configuration (`joinOnce = false`) is retained on error/timeout; no rollback snapshot or automatic failure removal is needed. OS apply cannot be cancelled, so association may occur later or retry may report native pending. Native events contain attempt/time/stage/observation only, no credentials or SSIDs. iOS adds elapsedMs and verification-stage ssidVerification to errors.

The existing SPM source target and CocoaPods source glob include the new policy file without integration changes. XCTest policy tests also run in an isolated temporary Swift package on Linux via `bun run test:ios:lifecycle`; this does not compile the Capacitor/NetworkExtension adapter.

| Requirements | Design element |
| --- | --- |
| FR-001–FR-004 | Requested-network callbacks, observation states, identity-safe fallback, retained successful request |
| FR-005–FR-008 | Owned cleanup, checked binding, independent offline capability policy |
| FR-009–FR-011 | iOS bounded lifecycle, late-callback invalidation, configuration policy |
| FR-012–FR-014 | Explicit milestones and app-owned service readiness |
| FR-015–FR-017 | Structured events, stage-specific errors, bounded snapshot |
| FR-018–FR-020 | Platform authorization context and app permission recovery |
| FR-021 | Optional temporary-configuration experiment |
| FR-022–FR-024 | External Silla error/readiness/cleanup integration |
| FR-025, QR-001–QR-004 | Controlled releases, regression matrix, lifecycle tests, privacy and compatibility |

## Interfaces and Contracts

Preserve `connect(): Promise<void>`, stable error codes, and existing persistent/route defaults. M1 changes requested binding failure to rejection and removes the modern internet requirement. M2 adds iOS-only requestTimeoutMs, makes iOS timeoutMs a post-apply verification budget, and validates iOS budgets at a maximum of ten minutes. The owner explicitly selected separate waiting periods and retention until disconnect. Error fields are additive. Broader observation APIs, authorization APIs, and `joinOnce` remain proposals.

QR-005 applies to every API contract change, including behavior-only fixes. Update `src/definitions.ts` types/JSDoc and regenerate the root README API sections with `bun run docgen` or `bun run build` in the same change. Review relevant handwritten guidance/examples too. Generated sections must never be edited manually.

## User Experience and Failure Recovery

Silla translates structured stages into association, permission, DHCP, routing, or service recovery. Cloud reachability is independent. Never claim a wrong password or denied permission from timeout alone. Retry begins a new attempt and cannot inherit leaked routing or stale completion.

## Security, Privacy, Accessibility, and Internationalization

Never capture credentials. Decide whether SSIDs are omitted, masked, or explicitly collected for support; bound diagnostic retention. Keep codes machine-readable and native messages diagnostic-only. Plugin changes have no direct UI; Silla owns accessible, localized recovery content.

## Alternatives Considered

| Alternative | Benefit | Cost/risk | Proposed disposition |
| --- | --- | --- | --- |
| Increase timeout | Small change | Cannot fix unreadable verification information | Reject as root-cause fix |
| Remove all verification | Avoids SSID redaction gate | Weakens requested-network contract | Prefer correct observable verification |
| Disable process routing | Avoids binding lifecycle | Can break local charger access | Do not use as Silla mitigation |
| Enable joinOnce globally | Temporary lifecycle | Background/sleep disconnection | Optional experiment only |

## Dependencies, Risks, and Rollout

No new runtime dependency proposed. Use existing platform/Capacitor APIs; native test tooling selection remains open. Risks include OEM differences, concurrent Wi-Fi ambiguity, OS requests outliving timeout, and configurations surviving failure. Mitigate with attempt ownership, explicit contracts, and device evidence.

Ship immutable internal versions against a validated baseline with rollback references. Preserve both iOS package integrations and Capacitor major alignment. Silla application rollout must identify the actual native plugin version.

## Open Questions and Design Review

M1/M2 product choices are approved; technical design awaits combined owner review. Device matrix and native iOS build evidence are outstanding. Broader identifier collection requires a later decision.
