# Design

Status: Proposed architecture; verify current implementation before authorization.

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

Preserve `connect(): Promise<void>`, current options/defaults, and stable error codes initially. Specify the resolution boundary and timeout scope before implementation. Any new diagnostics, observation fields, authorization details, or `joinOnce` option are additive proposals to document in `src/definitions.ts`; generate public API docs with docgen.

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

Before the relevant slice: approve timeout semantics, routing-failure behavior, configuration retention, and identifier collection. Check current code, API guards, concurrency, privacy, and compatibility; record material decisions. Design gate remains Not ready.
