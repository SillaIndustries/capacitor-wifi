# Verification

Keep current requirement coverage, reusable checks, dated evidence summaries, and known gaps here. Move completed run logs and refinement narratives to milestone evidence. Scope old results to the implementation tested; acceptance does not prove replacement code or later requirements.

## Verification Strategy

Explain how the feature will be evaluated at unit, integration, system, and manual levels. Include only levels relevant to this feature.

## Requirement Coverage

Every approved requirement must have evidence or an explicitly accepted gap.

| Requirement | Automated evidence | Manual evidence | Status       | Notes |
| ----------- | ------------------ | --------------- | ------------ | ----- |
| FR-001      |                    |                 | Not verified |       |

Allowed statuses: `Not verified`, `Passed`, `Failed`, or `Accepted gap`.

## Automated Checks

Record stable commands and the behavior they cover.

| Check      | Command | Coverage | Result and date |
| ---------- | ------- | -------- | --------------- |
| Unit tests |         |          |                 |

## Manual Scenarios

If the current milestone is meaningfully testable in the browser, provide the owner-facing steps in [Browser review guide](browser-review.md). Keep detailed requirement traceability and exhaustive edge cases here.

### V-001: <Scenario name>

Requirements: FR-001

Preconditions:

Steps:

1. <Action>

Expected result:

Evidence:

Result: Not run

## Cross-Cutting Review

Include relevant checks for:

- [ ] Keyboard and assistive technology behavior
- [ ] Responsive layouts and supported devices
- [ ] Loading, empty, error, permission, and recovery states
- [ ] Existing and migrated data
- [ ] Localization and long content
- [ ] Performance targets
- [ ] Security and privacy requirements
- [ ] Regression risks outside the feature

Mark irrelevant items as `N/A` with a short reason instead of deleting them.

## Known Gaps

List failures, unverified behavior, deferred tests, or environment limitations. An accepted gap requires explicit owner approval.

## Milestone Hardening Review

Complete this after the owner approves the implemented behavior and before definitive milestone acceptance.

- [ ] Review the milestone code as a whole for accidental complexity, duplication, unclear ownership, oversized modules, weak contracts, unsafe failure paths, resource leaks, performance risks, and misleading test coverage.
- [ ] Review directly affected feature and initiative documents for contradictions, stale status, missing decisions, and evidence gaps.
- [ ] Record the findings, including an explicit no-findings result when applicable.
- [ ] Obtain owner approval before implementing proposed hardening work.
- [ ] Rerun proportionate automated and manual verification after hardening.
- [ ] Consolidate the delivered specification, preserve evidence gaps, and check the default reading path and links.
- [ ] Ask the owner for a final test.
- [ ] Record definitive owner acceptance before marking the milestone accepted.

## Final Acceptance Checklist

- [ ] Every approved requirement has evidence or an accepted gap.
- [ ] All milestone checkpoints are accepted.
- [ ] Every milestone completed the mandatory hardening review and final owner validation.
- [ ] Automated checks pass at the agreed scope.
- [ ] Manual scenarios have been reviewed.
- [ ] Every browser-testable checkpoint had an accurate plain-language browser review guide.
- [ ] Known limitations and follow-up work are recorded.
- [ ] Current project memory has been updated where the feature established stable knowledge.
- [ ] The owner has approved final acceptance in `README.md`.
