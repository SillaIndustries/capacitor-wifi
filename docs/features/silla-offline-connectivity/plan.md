# Implementation Plan

Status: M1 Android and M2 iOS implementations await combined device review. M2 product choices were explicitly resolved; later delivery slices remain proposals.

## Initiative Coordination

Initiative: None. This dossier owns ordering. One implementation milestone at a time unless the owner explicitly authorizes parallel work.

Owner exception, 2026-10-08: implementation may advance before per-milestone device review. M1's pending review/acceptance does not block M2 development. Automated verification and documentation remain per milestone; combine physical-device review and preserve each milestone's evidence and acceptance status. See D-006.

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
| M1 Android verified offline connection | None | Requested-network verification, required routing when requested, and owned cleanup | FR-001–FR-008; initial FR-015–FR-016; applicable QR-001–QR-005 | Owner refinement | Start authorized 2026-10-08; acceptance pending |
| M2 iOS bounded/cancellable connection | None | Separate request/verification budgets, single settlement, and retention until explicit disconnect | FR-009–FR-011; initial FR-015–FR-016; applicable QR-001–QR-005 | Owner refinement | Start/behavior authorized 2026-10-08; batched acceptance pending |

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
- [ ] Run physical-device scenarios and obtain owner implementation review.
- [ ] Perform mandatory whole-slice hardening review after owner review; obtain approval for any hardening work.
- [ ] Implement/verify approved hardening and consolidate delivered specification/evidence.
- [ ] Obtain final owner validation and definitive acceptance; synchronize milestone checklist.

### Validation

Automated: `bun run verify:android`, `bun run verify:web`, `bun run lint`; full `bun run verify` attempted but blocked by unavailable Xcode on Linux. Unit tests use existing JUnit dependency; no new runtime dependency. Package scripts now invoke Bun consistently with repository guidance.

Manual: V-001–V-005 and relevant V-009 checks in verification. Primary acceptance device is the affected Android 12 phone; Android 10/11 fallback and Android 12+ callback variants also require device coverage. Final supported matrix/acceptance owner must be confirmed. Browser path: Not applicable.

### Approval to Start

Status: Approved by Silla owner on 2026-10-08 in conversation: begin feature, reject requested-binding failure, allow offline Wi-Fi. Scope: M1 only. Coordinated stage: Not applicable. No definitive acceptance implied.

### Checkpoint Report

Status: Implementation available for owner review. Native debug/release builds and state/ownership tests pass; Web build/docgen and repository lint pass. Physical association, traffic routing, and cleanup recovery remain untested. iOS verification is unavailable on Linux. Android lint has existing unrelated findings; details in verification.

Implementation review decision: Pending, deferred to the combined device review. Browser review guide: Not applicable. Hardening review: Not reached. Definitive owner acceptance: Pending. Owner exception authorizes preparation of M2 while these remain outstanding.

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
- [ ] Run batched Android/iPhone review and obtain owner implementation review.
- [ ] Perform whole-slice hardening, obtain approval for changes, and verify approved work.
- [ ] Consolidate evidence, obtain final owner validation, and record definitive acceptance.

### Validation and Approval to Start

Policy automation: `bun run test:ios:lifecycle`. Syntax: Swift frontend parse of changed sources. Public API/build: `bun run build`, `bun run lint`, and full `bun run verify` attempted. Native iOS adapter compilation/SwiftLint requires macOS/Xcode and remains a separate evidence requirement. Existing SPM and CocoaPods integrations remain intact.

Manual: combined V-001–V-007 plus iOS retention/late cleanup and relevant V-009/V-010. Browser path: Not applicable.

Start approved by Silla owner on 2026-10-08: continue under batched device-validation exception; explicit selections of separate waiting periods and keep-until-disconnect policy. Scope: M2. Acceptance: Pending.

### Checkpoint, Hardening, and Acceptance

Implementation available for combined review. Automated evidence is recorded in verification; native iOS build and phone testing remain outstanding. Implementation review: Pending. Hardening: Not reached. Definitive acceptance: Pending.

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

M1/M2 implementation is authorized and available for batched review. No milestone has definitive acceptance; later slices remain proposals.

## Plan Review Checklist

- [x] Every backlog requirement maps to proposed delivery work.
- [x] App dependencies and experimental scope are explicit.
- [ ] Next formal milestone has agreed scope, validation, and authorization.
- [ ] Owner approved the plan gate.
