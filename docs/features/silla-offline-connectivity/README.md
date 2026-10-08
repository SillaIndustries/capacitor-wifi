# Silla Offline Charger Connectivity

## Summary

Make Silla's connection to an EV charger's offline Wi-Fi reliable, explainable, and independently releasable. Cover the native plugin, its Silla integration contract, and the regression controls needed to maintain the internal fork.

## Status

Lifecycle: Discovery

Current milestone: None

Last updated: 2026-10-08

All requirements, technical designs, and delivery slices are proposals, not implemented behavior or approved milestones.

## Owner

Product owner: Silla team; individual acceptance owner to be confirmed.

## Milestone Checklist

No milestones defined yet.

## Initiative and Sequencing

Initiative: None. Cross-cutting work is contained in this dossier.

Current coordinated stage: Not applicable

Next cross-feature dependency: Silla application repository and physical Android/iOS devices for integration validation.

Proposed ordering: Android verification/cleanup → iOS timing/cancellation → diagnostics and readiness → permission recovery → optional iOS connection experiment. Release validation begins with the first slice. See [Plan](plan.md).

## Documentation Reading Rules

Follow the [documentation consolidation process](../README.md#documentation-consolidation). Keep proposed scope distinct from delivered behavior. Historical evidence does not authorize implementation, and audit claims must be checked against current code before changes.

Every plugin API contract change must follow the [public API documentation rule](../README.md#public-api-documentation): update `src/definitions.ts` and regenerate the root README API documentation in the same change, including behavioral changes with unchanged signatures.

## Documents

During refinement, start with [Discovery](discovery.md). Before implementation read:

1. [Requirements](requirements.md)
2. [Design](design.md)
3. [Decisions](decisions.md)
4. [Plan](plan.md)
5. [Verification](verification.md)

Browser review is not applicable to native association/routing. Physical-device review scenarios are in verification; add a browser guide only if a later slice introduces meaningfully browser-reviewable behavior.

## Approval Gates

| Gate | Status | Approved by | Date | Scope and conditions |
| --- | --- | --- | --- | --- |
| Discovery | Awaiting review | | | Backlog captured from audits and discussion |
| Requirements | Awaiting review | | | Proposed acceptance criteria; open product choices remain |
| Design | Not ready | | | Current-code review and timeout/retention choices needed |
| Plan | Not ready | | | Proposed slices only; no milestone authorization |
| Final acceptance | Not ready | | | No implementation or device validation |

## Milestone Completion Rule

Follow the [mandatory milestone completion sequence](../README.md#mandatory-milestone-completion-sequence). Owner implementation review begins hardening; definitive acceptance follows hardening and final owner validation.

## Current Blockers

- Confirm timeout, routing-failure, and configuration-retention semantics before the affected implementation slice.
- Device access and a Silla integration build are required to establish runtime correctness.

## Pause or Cancellation Record

Not paused or cancelled.

## History and Evidence

- [Android regression investigation](../../audits/capacitor-wifi-android-verification-fix-handoff.md)
- [iOS offline-AP investigation](../../audits/capacitor-wifi-ios-offline-ap-investigation.md)
- Owner direction: develop this fork for Silla following repeated regressions; recorded in [Decisions](decisions.md).
