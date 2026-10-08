# Implementation Plan

Status: Discovery backlog. No formal milestones defined or authorized. The proposed slices below capture prioritization without inventing approved discovery milestones.

## Initiative Coordination

Initiative: None. This dossier owns ordering. One implementation milestone at a time unless the owner explicitly authorizes parallel work.

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

- [ ] Recheck Android code in the current 8.5.5 checkout against the audit.
- [ ] Confirm required device/API matrix and owner of acceptance.
- [ ] Resolve requested-routing failure semantics and any capability-policy change.
- [ ] Refine S1 requirements/design and define a formal M1 with synchronized README checklist.
- [ ] Agree on device scenarios and meaningful lifecycle test mechanisms.
- [ ] Obtain scope/design/plan review and explicit implementation authorization.

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

Approval to start: Not approved. Checkpoint: Not reached. Hardening: Not reached. Definitive acceptance: Pending. No slice is delivered.

## Plan Review Checklist

- [x] Every backlog requirement maps to proposed delivery work.
- [x] App dependencies and experimental scope are explicit.
- [ ] Next formal milestone has agreed scope, validation, and authorization.
- [ ] Owner approved the plan gate.
