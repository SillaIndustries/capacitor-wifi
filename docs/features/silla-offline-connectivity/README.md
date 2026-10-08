# Silla Offline Charger Connectivity

## Summary

Make Silla's connection to an EV charger's offline Wi-Fi reliable, explainable, and independently releasable. Cover the native plugin, its Silla integration contract, and the regression controls needed to maintain the internal fork.

## Status

Lifecycle: Implementation

Current milestone: M2 iOS bounded/cancellable connection — Owner refinement; M1 review deferred to the same batch

Last updated: 2026-10-08

M1 and M2 implementations await combined owner/device review under the [batched-validation exception](../README.md#silla-batched-device-validation-exception). Later slices remain proposals. No milestone has definitive acceptance or physical-device validation.

## Owner

Product owner: Silla team; individual acceptance owner to be confirmed.

## Milestone Checklist

- [ ] M1 Android verified offline connection: verify the requested network, require requested routing, and release owned resources safely.
- [ ] M2 iOS bounded/cancellable connection: separate consent/verification budgets, settle once, and retain configuration until disconnect.

Later delivery slices remain discovery proposals, not formal milestones.

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
| Discovery | Approved | Silla owner | 2026-10-08 | Direction to begin the first Android slice; remaining backlog not accepted |
| Requirements | Approved | Silla owner | 2026-10-08 | M1/M2 scope; explicit routing/offline choices and separate iOS budgets with retention until disconnect |
| Design | Awaiting review | | | M1/M2 technical mechanisms documented for combined review |
| Plan | Awaiting review | | | M1/M2 development authorized; phone reviews batched, broader plan/release matrix open |
| Final acceptance | Not ready | | | Owner review, hardening, and device evidence outstanding |

## Milestone Completion Rule

Follow the [mandatory milestone completion sequence](../README.md#mandatory-milestone-completion-sequence). Owner implementation review begins hardening; definitive acceptance follows hardening and final owner validation.

Owner exception: phone reviews may be batched across milestones and development may continue while they are outstanding. This does not accept either implementation or waive combined review/hardening/final validation.

## Current Blockers

- Combined Android/iPhone review requires a Silla integration build and an agreed device matrix; this is deferred evidence, not a development blocker under the exception.
- Full native iOS compilation/SwiftLint requires macOS/Xcode; Linux policy tests and syntax parsing do not replace it.

## Pause or Cancellation Record

Not paused or cancelled.

## History and Evidence

- [Android regression investigation](../../audits/capacitor-wifi-android-verification-fix-handoff.md)
- [iOS offline-AP investigation](../../audits/capacitor-wifi-ios-offline-ap-investigation.md)
- Owner direction: develop this fork for Silla following repeated regressions; recorded in [Decisions](decisions.md).
