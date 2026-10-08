# Discovery

## Problem Statement

Repeated plugin regressions interrupt Silla's central workflow: joining an EV charger's offline access point and communicating with its local API. Pins, native investigation, and corrective PRs have made upstream releases an unreliable production dependency for the team.

## Desired Outcome

A validated internal baseline that preserves charger connectivity, releases its resources predictably, explains failures, and can adopt upstream changes selectively.

## Users and Context

Silla users connect with home Wi-Fi initially selected and cellular potentially enabled. Charger Wi-Fi has no internet. Local HTTP login/health checks and sustained WebSocket traffic must work independently of cloud availability. Developers and support staff need diagnostics suitable for device reports.

## Current Workflow

The app requests a connection with Android process routing, then polls SSID and IPv4. Investigated mitigation pins 8.4.1. Native 8.5.x verification can reject before app-side polling begins; Android timeout cleanup may release the requested network.

## Current Product Behavior

The audit documents source inspection of published 8.4.1, 8.5.0, and 8.5.4 packages. This checkout reports 8.5.5. Audit findings are evidence for investigation, not a fresh validation of every current code path.

- Android's newer verification reads potentially redacted network capabilities and can skip its legacy fallback; timeout cleanup does not explicitly undo process binding.
- iOS added SSID verification but starts its deadline before system consent, has no independent pending-apply watchdog, and does not invalidate verification on disconnect.
- iOS `joinOnce = false` predates the suspected regressions. Neither audit establishes why the affected iPhone refuses to switch APs.
- Silla diagnostics can lose error metadata; syntactic IPv4 checks can accept link-local addresses; cleanup can target the current instead of requested SSID.

## Owner Vision

Confirmed direction: work on Silla's fork independently for now and capture the full improvement backlog before implementing the most urgent work. Specific API behavior and milestone acceptance are not yet approved.

## Evidence

| Evidence | Type | What it establishes |
| --- | --- | --- |
| [Android audit](../../audits/capacitor-wifi-android-verification-fix-handoff.md) | Source-confirmed path; observed app symptoms | Strong verification regression candidate, not device-proven causality |
| [iOS audit](../../audits/capacitor-wifi-ios-offline-ap-investigation.md) | Source investigation | Contract/lifecycle weaknesses; association failure remains unexplained |
| [Upstream PR #19](https://github.com/Cap-go/capacitor-wifi/pull/19) | Published change description | Verification intended to improve reliability; device/lifecycle matrix explicitly untested |
| Owner discussion, 2026-10-08 | Confirmed | Internal fork chosen after repeated regressions |

## Early Scope Boundary

Included: Android verification/routing, iOS lifecycle, readiness semantics, diagnostics, permissions, optional temporary configuration research, Silla integration, and release controls.

Excluded: claiming a reproduced iOS offline-switch fix, forcing removal of user-owned iOS networks, charger firmware changes, or embedding charger-specific HTTP endpoints in the generic plugin.

## Assumptions

| ID | Assumption | Impact if wrong | How to resolve | Status |
| --- | --- | --- | --- | --- |
| A-001 | Process routing remains necessary for Silla Android traffic | Routing contract could differ | Test HTTP and WebSocket behavior on supported devices | Open |
| A-002 | A matching SSID is observable with the supported permission setup | Strict verification could still exclude valid connections | Capture callback capabilities and authorization states | Open |
| A-003 | Persistent iOS configuration is required for some monitoring flows | Temporary configuration could be preferable | Owner review plus background/sleep experiments | Open |

## Open Questions

| ID | Question | Owner | Blocking | Resolution |
| --- | --- | --- | --- | --- |
| Q-001 | Which Android/iOS versions and devices are release-required? | Silla | Yes, release acceptance | Pending |
| Q-002 | Who provides final acceptance and physical-device runs? | Silla | Yes, acceptance | Pending |
| Q-003 | Does the affected iPhone never associate, leave the AP later, or stay associated with failed traffic? | Silla/device investigator | Yes, causal iOS fix | Capture a timestamped trace |

## Discovery Review Checklist

- [x] Problem, context, owner direction, and evidence gaps recorded.
- [ ] Current checkout behavior checked before implementation.
- [ ] Blocking questions resolved or explicitly deferred for the relevant slice.
- [ ] Owner approved the discovery gate.
