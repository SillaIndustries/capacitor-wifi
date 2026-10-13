# Design

Status: M1/M2 accepted by the owner on 2026-10-12; plugin-side S3 diagnostics/readiness implementation and source-level hardening are complete in this checkout. S3 device behavior, native iOS compilation, and Silla-app integration remain unverified.

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

M1 initially added native log events without SSIDs/passwords, and optional error fields `ssidVerification`, `bindingSucceeded`, and `elapsedMs`. A later partial S3 implementation adds the support snapshot API described below; that API is not part of M1's original scope.

### M2 implementation boundaries

Connection/disconnect state, native completions, and scheduled work are confined to the main queue. A `ConnectAttempt` owns its call, target, identity, lifecycle, watchdog, and polling work. Identity is rechecked after every native callback; terminal completion cancels work, clears the active slot, and releases the call reference exactly once.

`WifiConnectionLifecycle` is Foundation-only policy with a monotonic request deadline and a fresh verification deadline. Request default is 120 seconds; verification default is 30 seconds. An independent watchdog runs while apply or fetchCurrent is pending. Callback processing rechecks deadlines, preventing delayed queue delivery from producing success after expiry. Unknown SSID information remains distinct from a known mismatch. The app must be able to execute for watchdog delivery.

`WifiConfigurationOwnership` versions mutations per SSID. Disconnect cancels the plugin attempt, then removes its explicit target or the pending/latest connect target. Pending OS apply contexts are weakly indexed so a disconnect after timeout can mark their late completion for cleanup without retaining calls or creating ownership cycles. Cleanup is suppressed when a newer connect/addNetwork request owns that same target. addNetwork mutations are serialized/registered for this protection; its existing completion contract is otherwise unchanged.

If no connection target is tracked, disconnect preserves the existing current-SSID fallback with a 120-second lookup watchdog and a mutation-sequence check. A delayed fallback cannot remove a newly requested configuration. An explicit different SSID cancels pending verification but removes only the explicit target.

Persistent configuration (`joinOnce = false`) is retained on error/timeout; no rollback snapshot or automatic failure removal is needed. OS apply cannot be cancelled, so association may occur later or retry may report native pending. Native events contain attempt/time/stage/observation only, no credentials or SSIDs. iOS adds elapsedMs and verification-stage ssidVerification to errors.

The existing SPM source target and CocoaPods source glob include the new policy file without integration changes. XCTest policy tests also run in an isolated temporary Swift package on Linux via `bun run test:ios:lifecycle`; this does not compile the Capacitor/NetworkExtension adapter.

### S3 diagnostics/readiness implementation progress

Commit `b818e85` adds `WifiConnectionDiagnostics` on Android and iOS and exposes `getConnectionDiagnostics()` through the plugin API. The latest attempt is held in memory with at most 64 events; snapshots include attempt ID, lifecycle stage/outcome, timestamps/elapsed time, SSID verification state, routing state where applicable, IP readiness, and selected structured error metadata. Earlier events are counted when evicted. Reports intentionally omit network identifiers, actual addresses, credentials, and raw native messages. Web reports the native-only method as unimplemented.

Android samples link addresses from the requested `Network`'s `LinkProperties`; iOS refreshes the current SSID and interface addresses under a bounded lookup and attributes IP state only when the SSID matches the tracked target. These are observations, not proof of end-to-end routing or charger-service readiness. Silla must still probe its local services.

The implementation includes Android and Foundation-only iOS tests for event bounds, snapshot-copy behavior, privacy, IP classification, terminal outcomes, event ordering, routing-stage reporting, and timeout/permission event mapping. On 2026-10-13, Android debug/release builds and JUnit tests, Web build/docgen, ESLint, and root README API regeneration passed. The portable iOS policy test runner could not run because Swift is unavailable in this environment. iOS native adapter type-check/build, S3 device validation, cleanup behavior, and Silla integration remain outstanding. Review found an unintended 600,000 ms Android timeout ceiling relative to the prior positive-integer contract; that ceiling was removed and covered by an Android policy test. iOS retains its documented 600,000 ms limit.

| Requirements | Design element |
| --- | --- |
| FR-001–FR-004 | Requested-network callbacks, observation states, identity-safe fallback, retained successful request |
| FR-005–FR-008 | Owned cleanup, checked binding, independent offline capability policy |
| FR-009–FR-011 | iOS bounded lifecycle, late-callback invalidation, configuration policy |
| FR-012–FR-014 | Partial observations only; complete milestone contract and app-owned service readiness remain |
| FR-015–FR-017 | Plugin snapshot/events/IP observations, timeout/permission distinction, and bounded history implemented; Android checks pass; iOS execution/build and device/app evidence outstanding |
| FR-018–FR-020 | Platform authorization context and app permission recovery |
| FR-021 | Optional temporary-configuration experiment |
| FR-022–FR-024 | External Silla error/readiness/cleanup integration |
| FR-025, QR-001–QR-004 | Controlled releases, regression matrix, lifecycle tests, privacy and compatibility |

## Interfaces and Contracts

Preserve `connect(): Promise<void>`, stable error codes, and existing persistent/route defaults. M1 changes requested binding failure to rejection and removes the modern internet requirement. M2 adds iOS-only requestTimeoutMs, makes iOS timeoutMs a post-apply verification budget, and validates iOS budgets at a maximum of ten minutes. The owner explicitly selected separate waiting periods and retention until disconnect. Error fields are additive. The diagnostics API, timeout/permission event codes, Android routing stage, and generated README docs are implemented in the current worktree. Android retains positive integer `timeoutMs` values as before; iOS validates finite positive budgets up to 600,000 ms. Broader authorization APIs and `joinOnce` remain proposals.

QR-005 applies to every API contract change, including behavior-only fixes. Update `src/definitions.ts` types/JSDoc and regenerate the root README API sections with `bun run docgen` or `bun run build` in the same change. Review relevant handwritten guidance/examples too. Generated sections must never be edited manually.

## User Experience and Failure Recovery

Silla translates structured stages into association, permission, DHCP, routing, or service recovery. Cloud reachability is independent. Never claim a wrong password or denied permission from timeout alone. Retry begins a new attempt and cannot inherit leaked routing or stale completion.

## Security, Privacy, Accessibility, and Internationalization

Never capture credentials. The partial diagnostics API omits SSIDs, BSSIDs, actual IP addresses, credentials, and raw native messages and bounds retained history to 64 events. Review these privacy and retention properties against the implementation during S3 hardening. Keep codes machine-readable and native messages diagnostic-only. Plugin changes have no direct UI; Silla owns accessible, localized recovery content.

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

M1/M2 are owner-accepted. Plugin-side S3 diagnostics have been reviewed and implemented; physical-device testing, native iOS build evidence, and Silla-app integration/review remain outstanding. Broader identifier collection requires a later decision.
