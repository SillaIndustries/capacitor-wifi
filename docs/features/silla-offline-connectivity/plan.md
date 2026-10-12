# Implementation Plan

Status: M1 Android and M2 iOS were accepted by the owner on 2026-10-12. The owner authorized continued plugin-side S3 diagnostics/readiness implementation on 2026-10-12; commit `b818e85` is its initial baseline. S3 completion and later delivery slices remain pending.

## Initiative Coordination

Initiative: None. This dossier owns ordering. One implementation milestone at a time unless the owner explicitly authorizes parallel work.

Owner exception, 2026-10-08: implementation was allowed to advance before per-milestone device review. M1's pending review/acceptance did not block M2 development. The owner subsequently accepted both M1 and M2 on 2026-10-12. See D-006 and D-008.

## Proposed Delivery Order

| Slice | Outcome and included scope | Requirements | Prerequisites |
| --- | --- | --- | --- |
| S1 — Android verified offline connection | Requested-network verification, safe fallback, retained callback, owned binding cleanup, observable routing result; initial acceptance fixture | FR-001–FR-008; relevant FR-015–FR-016, FR-025; QR-001–QR-004 | Current-code review, binding contract, Android device access |
| S2 — iOS bounded/cancellable connection | Defined timeout budget, pending-apply watchdog, late completion guards, disconnect invalidation, explicit configuration retention | FR-009–FR-011; relevant FR-015–FR-016; QR-001–QR-004 | Timeout/retention decisions, iPhone and signed build |
| S3 — Explainable readiness and support | Connection milestone contract, network/IP observations, full diagnostics/snapshot, Silla service readiness, preserved errors and requested identity | FR-012–FR-017, FR-022–FR-024; QR-003–QR-004 | Stable native attempt model, Silla repo/build, privacy/API choices |
| S4 — Permission-aware recovery | Permission/location/observability distinctions, iOS accuracy context, entitlement guidance, Local Network recovery | FR-018–FR-020; QR-001, QR-003–QR-004 | Platform permission experiments and Silla integration |
| S5 — Temporary iOS configuration research | Persistent vs temporary comparison; optional API only if justified | FR-021; QR-001, QR-004 | Reproduce/classify iOS failure; owner monitoring requirements |
| S6 — Sustained release controls | Complete device acceptance matrix, immutable versions, baseline/notes, upstream adoption review, maintained lifecycle automation | FR-025; QR-001–QR-004 | Starts with S1; full coverage accumulates across slices |

S6 is ongoing release work, not permission to defer release checks until the end. S1/S2 include enough diagnostics to validate their behavior; S3 expands the support interface.

QR-005 applies to every slice that changes the plugin API contract. Include updates to `src/definitions.ts` types/JSDoc and regeneration/review of the root README API docs in that slice, even when signatures remain unchanged. Documentation is part of delivery and acceptance, not deferred release work.

## First Slice Preparation

- [x] Recheck Android code in the current 8.5.5 checkout against the audit.
- [ ] Confirm required device/API matrix and owner of acceptance.
- [x] Resolve requested-routing failure semantics and any capability-policy change.
- [x] Refine S1 requirements/design and define a formal M1 with synchronized README checklist.
- [x] Define initial device scenarios and JUnit state/ownership tests; agree final device matrix with owner.
- [x] Obtain explicit implementation authorization (owner: begin feature, plus explicit routing/offline choices).

## Milestone Overview

| Milestone | Coordinated stage | Outcome | Requirements | Status | Approval |
| --- | --- | --- | --- | --- | --- |
| M1 Android verified offline connection | None | Requested-network verification, required routing when requested, and owned cleanup | FR-001–FR-008; initial FR-015–FR-016; applicable QR-001–QR-005 | Accepted | Start authorized 2026-10-08; accepted by owner 2026-10-12 after thorough testing |
| M2 iOS bounded/cancellable connection | None | Separate request/verification budgets, single settlement, and retention until explicit disconnect | FR-009–FR-011; initial FR-015–FR-016; applicable QR-001–QR-005 | Accepted | Start/behavior authorized 2026-10-08; accepted by owner 2026-10-12 after thorough testing |

On 2026-10-12 the owner directed continued S3 implementation (D-009). The authorized increment is plugin-side diagnostic lifecycle accuracy and readiness observations, with matching types/docs/tests. Silla-app service/error integration remains dependent on its separate repository and requires its own integration evidence; final S3 acceptance remains pending.

## M1: Android verified offline connection

### Outcome and Scope

Silla can request an offline charger network without an internet eligibility requirement, verify the requested network, and require its requested app routing. Failure and disconnect release owned resources. Includes API docs and meaningful state/ownership tests. Excludes iOS implementation, Silla application edits, broad diagnostics API, and publishing an internal release.

### Tasks

- [x] Check current source and resolve product consequences before implementation.
- [x] Implement location-inclusive callbacks, identity-safe older-API fallback, and distinct SSID observation states.
- [x] Serialize lifecycle changes; ignore stale/cancelled callbacks and retain successful requests.
- [x] Require successful requested process binding and implement owned cleanup/retry behavior.
- [x] Add native request/availability/verification/binding/failure/cleanup diagnostics without credentials.
- [x] Add JUnit identity/completion and process-binding ownership tests.
- [x] Update `src/definitions.ts` and regenerate root README API documentation.
- [x] Build Android debug/release and run unit tests; build Web and run repository lint.
- [x] Run physical-device scenarios and obtain owner implementation review. Owner reports thorough M1 testing and accepts the milestone on 2026-10-12; detailed scenario records were not included here.
- [x] Complete M1 owner review and hardening gate. Owner acceptance recorded 2026-10-12; no separate hardening findings were supplied.
- [x] Consolidate delivered specification/evidence for owner acceptance. Existing automated evidence is recorded in verification; device evidence detail was not supplied.
- [x] Obtain final owner validation and definitive acceptance; synchronize milestone checklist. Accepted by owner 2026-10-12.

### Validation

Automated: `bun run verify:android`, `bun run verify:web`, `bun run lint`; full `bun run verify` attempted but blocked by unavailable Xcode on Linux. Unit tests use existing JUnit dependency; no new runtime dependency. Package scripts now invoke Bun consistently with repository guidance.

Manual: V-001–V-005 and relevant V-009 checks in verification. The owner reports thorough M1 testing and accepted the milestone on 2026-10-12. The tested device/API matrix and per-scenario records were not supplied here. Browser path: Not applicable.

### Approval to Start

Status: Approved by Silla owner on 2026-10-08 in conversation: begin feature, reject requested-binding failure, allow offline Wi-Fi. Scope: M1 only. Coordinated stage: Not applicable. No definitive acceptance implied.

### Checkpoint Report

Status: Owner accepted M1 on 2026-10-12 after reporting thorough testing. Existing automated verification evidence is recorded above and in verification; device/build/scenario details were not included in this update.

Implementation review and definitive owner acceptance: Accepted 2026-10-12. Owner reports thorough M1 device testing; detailed device/build/scenario records were not included in this conversation. Browser review guide: Not applicable. No separate hardening findings were supplied. M2 was authorized and developed under the earlier batched-validation exception.

## M2: iOS bounded/cancellable connection

### Outcome and Scope

Users have time to answer Apple's prompt without consuming SSID confirmation time. Missing apply/SSID callbacks have independent plugin watchdogs; disconnect cancels plugin verification and targets the requested app-owned configuration. Persistent configuration survives timeout/error. Includes narrow addNetwork ownership coordination, diagnostics, policy tests, API docs, and combined review coverage. Excludes temporary joinOnce behavior, Silla app changes, reproducing the unexplained iOS association failure, and claiming a native operation can be cancelled.

### Tasks

- [x] Confirm separate waiting periods and retention until disconnect with owner.
- [x] Replace lock/polling lifecycle with serialized attempt identity and phase-specific watchdogs.
- [x] Bound pending apply and pending SSID reads, reject stale results, and settle once.
- [x] Track explicit/pending/latest target and guard late removal against newer same-SSID writes.
- [x] Preserve configuration on timeout/error and persistent default.
- [x] Add Foundation policy/ownership XCTest cases and portable test runner without new runtime dependencies.
- [x] Update `src/definitions.ts` and regenerate root README API documentation.
- [x] Complete available automated checks and record precise native-build limitations.
- [x] Run batched Android/iPhone review and obtain owner implementation review. Owner reports thorough testing of M1 and M2 and accepts both on 2026-10-12; per-scenario records were not included here.
- [x] Complete M2 owner review and hardening gate. Owner acceptance recorded; no separate hardening findings were supplied.
- [x] Consolidate evidence, obtain final owner validation, and record definitive acceptance. Owner accepted M2 on 2026-10-12.

### Validation and Approval to Start

Policy automation: `bun run test:ios:lifecycle`. Syntax: Swift frontend parse of changed sources. Public API/build: `bun run build`, `bun run lint`, and full `bun run verify` attempted. Native iOS adapter compilation/SwiftLint requires macOS/Xcode and remains a separate evidence requirement. Existing SPM and CocoaPods integrations remain intact.

Manual: combined V-001–V-007 plus iOS retention/late cleanup and relevant V-009/V-010. Browser path: Not applicable.

Start approved by Silla owner on 2026-10-08: continue under batched device-validation exception; explicit selections of separate waiting periods and keep-until-disconnect policy. Scope: M2. Owner reports thorough testing and accepted M2 on 2026-10-12; detailed device/build/scenario records were not included here.

### Checkpoint, Hardening, and Acceptance

Owner accepted M2 on 2026-10-12 after reporting thorough testing. Automated evidence is recorded in verification; device/build/scenario details were not included in this update. Definitive owner acceptance: Accepted.

## S3: Explainable readiness and support — implementation progress

### Current branch checkpoint

Commit `b818e85` (`backup`, 2026-10-11) began the plugin-owned diagnostics/readiness portion of S3 on both native platforms. D-009 records owner authorization to continue this plugin-side increment on 2026-10-12; that authorization does not imply full S3 completion or acceptance. Silla-app deliverables remain separate.

- [x] Add public `getConnectionDiagnostics()` contract and diagnostic data types in `src/definitions.ts`; add Android/iOS native entry points and an unsupported Web implementation.
- [x] Add a latest-attempt, in-memory diagnostic report with a 64-event bound, sequence/drop accounting, attempt correlation, lifecycle/error details, and privacy exclusions for SSID, BSSID, IP addresses, credentials, and raw native messages.
- [x] Add address classification for unknown, missing, link-local-only, IPv4-assigned, and IPv6-only observations; Android reads target-network `LinkProperties`, while iOS attributes refreshed `en0` observations only when current SSID matches the tracked target.
- [x] Add Android and Foundation-only iOS unit tests for bounds, snapshot isolation, metadata privacy, and address classification; extend the portable iOS test runner.
- [x] Regenerate and review root `README.md` API documentation from `src/definitions.ts` (QR-005); `bun run build` regenerated the API docs successfully on 2026-10-12.
- [x] Run Android diagnostics tests and available build checks against the current plugin increment: Android debug/release build, JUnit tests, Web build/docgen, lint, and diff checks pass on 2026-10-12.
- [ ] Run iOS lifecycle/diagnostics tests; `bun run test:ios:lifecycle` is blocked because Swift is unavailable in this Linux environment (includes the latest iOS defensive-snapshot tests).
- [x] Record distinct association confirmation and verification-start events instead of reporting verification start as an SSID observation.
- [x] Make success/failure outcomes terminal in the diagnostics model and add tests that late duplicate outcomes cannot overwrite them; Android tests passed, iOS tests added but unexecuted because Swift is unavailable.
- [x] Owner tested and approved this S3 lifecycle-event refinement checkpoint on 2026-10-12 (D-010); this is not final S3 acceptance.
- [x] Align the public `connectionStage` type with Android's `routing` failure stage and regenerate the root API docs; `bun run build` and Android debug/release JUnit checks pass.
- [x] Test the routing-stage diagnostic contract in Android unit tests; add an iOS test ensuring verification-start does not create an SSID observation.
- [x] Add explicit timeout and permission-denied terminal diagnostic event types, preserving stage and stable error code in the attempt state.
- [x] Add Android/iOS policy tests for timeout and permission event mapping; Android tests pass, iOS test execution remains blocked by missing Swift.
- [x] Correct legacy Android diagnostics so a successful `reconnect()` call is not reported as request completion/network availability; availability is recorded only after the requested SSID is observed.
- [x] Review early-attempt attribution: Android and iOS reserve a diagnostic attempt before validation/permission checks; validation failures can retain their own terminal report, while concurrent rejected calls do not replace the active report.
- [x] Review iOS snapshot no-target behavior: returns the available attempt report without starting a network lookup; tracked-target refresh is bounded and only applies if the attempt/revision remains current.
- [x] Verify source-level cleanup/event paths for Android modern, Android legacy, and iOS; terminal outcome remains historical after cleanup and late iOS removal does not override a newer target.
- [ ] Obtain Android/iOS device evidence for cleanup events, IP observations, routing and cross-platform event parity.
- [x] Review diagnostic model terminal outcome and bounded-history consistency; make iOS snapshots copy event history and nested error fields like Android, with mutation-isolation test coverage.
- [ ] Review observation freshness/target changes, cleanup API guarantees, and Android timeout-cap compatibility on devices; address findings.
- [ ] Complete native iOS compilation/SwiftLint on macOS and signed-device verification; test snapshots for pending, success, failure, cancellation, disconnect, stale observations, and target changes.
- [ ] Agree S3 scope/acceptance with owner; integrate Silla-side service probes, error metadata/last-meaningful-failure handling, requested-identity cleanup, and cloud-independent local operations in the Silla app repository.
- [ ] Complete V-008/V-009 and relevant V-010 device/app checks; perform owner review, hardening, final validation, and only then update acceptance status.

### Explicit boundary and remaining S3 work

The plugin snapshot reports historical `connect()` outcome and observed network/IP signals; it does not guarantee route health, internet access, or charger-service readiness. The current changes do not implement a complete request/association/routing/IP/service milestone model, nor a plugin-side support-export workflow in Silla. Silla remains responsible for health/login/WebSocket probes and localized recovery. S3 also requires verifying preservation of error metadata and meaningful last failures in the app, cleanup by requested charger identity, and keeping local operations independent from cloud availability (FR-012–FR-017, FR-022–FR-024). Those app changes require the separate Silla repository.

S3 plugin diagnostics increment authorized by owner direction on 2026-10-12. The lifecycle-event checkpoint is owner-approved under D-010; timeout/permission distinction, public contract alignment, early-attempt attribution review, bounded iOS refresh guards, defensive snapshot copies, legacy event timing, and unknown-versus-missing IP classification pass available Android/Web checks. iOS policy tests/native compilation, end-to-end device review, Silla integration, and final S3 acceptance remain pending. The batched device-validation exception permits combined review; it does not mark those requirements verified.

## Validation Per Slice

Automated: use Bun build/lint and repository verification, native compilation/tests where available, plus meaningful lifecycle tests. Keep command timeouts at or below 10 minutes and record platform/environment blockers. Browser review: not applicable to native networking.

Manual: run the relevant scenarios in [Verification](verification.md) with immutable plugin/app identifiers. S1 requires Android offline AP, local HTTP/WebSocket, redaction/permissions, wrong-network rejection, cancellation/retry, cleanup, and cloud recovery. S2 requires delayed consent, pending apply, cancellation, repeat connect, and configuration retention on iPhone. S3/S4 require actual Silla diagnostics and recovery flows. S5 requires background/sleep/termination comparison. S6 requires the agreed release matrix.

## Milestone Authorization and Completion Records

When a slice becomes a milestone, add its outcome, requirements, included/excluded scope, tasks, validation, approval-to-start record, checkpoint report, hardening review, and definitive acceptance record using the supplied template.

For every milestone:

1. Consolidate the approved specification before authorization.
2. Implement the smallest coherent slice and proportionate tests.
3. Obtain owner implementation review and complete refinement.
4. Review the whole slice for resource ownership, weak boundaries, misleading tests, performance, and documentation consistency.
5. Obtain approval for hardening, implement and verify it.
6. Consolidate delivered specifications and evidence; verify API contract changes are documented in `src/definitions.ts` and the regenerated root README API sections, with relevant handwritten guidance/examples updated.
7. Obtain final owner validation and definitive acceptance; synchronize README and decisions.

M1 and M2 are accepted by the owner as of 2026-10-12. The authorized plugin-side S3 diagnostics increment is implemented in the current worktree; Android/Web builds, Android JUnit suite, API docs, formatting, and lint pass on 2026-10-12. Remaining S3 gates are iOS tests/native build, device validation, Silla app integration, whole-slice review/hardening, and final acceptance. No later slice has definitive acceptance.

## Plan Review Checklist

- [x] Every backlog requirement maps to proposed delivery work.
- [x] App dependencies and experimental scope are explicit.
- [ ] Next formal milestone has agreed scope, validation, and authorization.
- [ ] Owner approved the plan gate.
