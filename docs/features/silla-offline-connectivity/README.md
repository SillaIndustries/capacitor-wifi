# Silla Offline Charger Connectivity

## Summary

Make Silla's connection to an EV charger's offline Wi-Fi reliable, explainable, and independently releasable. Cover the native plugin, its Silla integration contract, and the regression controls needed to maintain the internal fork.

## Status

Lifecycle: Implementation; plugin-side S3 increment ready for integration testing

Current work: M1 Android and M2 iOS accepted; plugin-side S3 diagnostics/readiness implementation and hardening are complete in this checkout; device validation and Silla-app integration are next

Last updated: 2026-10-13

The owner reports having tested M1 and M2 thoroughly and accepts both milestones on 2026-10-12. Per-scenario device/build records were not included in this update and remain to be captured if available. On 2026-10-12 the owner directed S3 plugin-side diagnostics/readiness implementation (D-009). That implementation and source-level hardening are complete in the current checkout; Android build/tests and Web/API build pass. S3 physical-device validation, iOS test/native compilation, Silla-app integration, and final owner acceptance remain outstanding.

## Owner

Product owner: Silla team; individual acceptance owner to be confirmed.

## Milestone Checklist

- [x] M1 Android verified offline connection: verify the requested network, require requested routing, and release owned resources safely. Owner accepted 2026-10-12.
- [x] M2 iOS bounded/cancellable connection: separate consent/verification budgets, settle once, and retain configuration until disconnect. Owner accepted 2026-10-12.

S3 implementation is authorized for the plugin-side increment described in the [plan](plan.md). Its full Silla-app integration and acceptance remain incomplete; later delivery slices remain proposals.

## Initiative and Sequencing

Initiative: None. Cross-cutting work is contained in this dossier.

Current coordinated stage: Not applicable

Next cross-feature dependency: Silla application repository and physical Android/iOS devices for integration validation.

Proposed ordering: Android verification/cleanup → iOS timing/cancellation → diagnostics and readiness → permission recovery → optional iOS connection experiment. M1/M2 are owner-accepted; the owner has authorized continued plugin-side S3 diagnostics/readiness work. See [Plan](plan.md).

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

For hands-on S3 diagnostics checks, use the [S3 Diagnostics Validation Guide](s3-diagnostics-validation.md).

Browser review is not applicable to native association/routing. Physical-device review scenarios are in verification; add a browser guide only if a later slice introduces meaningfully browser-reviewable behavior.

## Approval Gates

| Gate | Status | Approved by | Date | Scope and conditions |
| --- | --- | --- | --- | --- |
| Discovery | Approved | Silla owner | 2026-10-08 | Direction to begin the first Android slice; remaining backlog not accepted |
| Requirements | Approved | Silla owner | 2026-10-08 | M1/M2 scope; explicit routing/offline choices and separate iOS budgets with retention until disconnect |
| Design | Awaiting review | | | M1/M2 accepted; S3 diagnostics implementation and remaining technical review documented |
| Plan | Awaiting review | | | M1/M2 accepted; S3 plugin increment authorized and lifecycle-event checkpoint accepted 2026-10-12; S3 completion pending |
| M1/M2 final acceptance | Accepted | Silla feature owner | 2026-10-12 | Owner reports thorough testing and accepts both milestones; per-scenario device/build evidence not recorded here |
| S3 final acceptance | Not ready | | | S3 implementation, verification, owner review, and integration remain outstanding |

## Milestone Completion Rule

Follow the [mandatory milestone completion sequence](../README.md#mandatory-milestone-completion-sequence). Owner implementation review begins hardening; definitive acceptance follows hardening and final owner validation.

Owner exception: phone reviews may be batched across milestones and development may continue while they are outstanding. M1/M2 were subsequently accepted by the owner on 2026-10-12. S3 remains separate and unaccepted.

## Current Blockers

- Detailed M1/M2 device/build records were not supplied with the 2026-10-12 acceptance; capture them retrospectively if available. This does not change the owner's acceptance.
- Full native iOS compilation/SwiftLint requires macOS/Xcode; Linux policy tests and syntax parsing do not replace it.
- S3 plugin-side diagnostics work is implemented in the current worktree; API docs, Android build/tests, Web build, and ESLint pass. iOS policy tests/native build need Swift/macOS; device-level snapshot, cleanup, routing, and charger-service checks remain. Silla-side service readiness and error-preservation work requires the separate app repository.
- Android `timeoutMs` retains its pre-existing positive-integer range; the accidental 600000 ms Android ceiling was removed. The 600000 ms limit remains documented and enforced for iOS.

## Pause or Cancellation Record

Not paused or cancelled.

## History and Evidence

- [Android regression investigation](../../audits/capacitor-wifi-android-verification-fix-handoff.md)
- [iOS offline-AP investigation](../../audits/capacitor-wifi-ios-offline-ap-investigation.md)
- Owner direction: develop this fork for Silla following repeated regressions; recorded in [Decisions](decisions.md).
