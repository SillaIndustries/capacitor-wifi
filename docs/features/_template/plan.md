# Implementation Plan

Before milestone authorization, consolidate approved requirements, design, and remaining work. After hardening, prepare the delivered specification for final validation; finalize acceptance records only after the owner accepts. Replace completed task narratives with concise accepted outcomes and links to milestone history.

## Planning Rules

- Prefer reviewable vertical slices over broad frontend, backend, or database phases.
- Each milestone has one coherent outcome and follows the [mandatory milestone completion sequence](../README.md#mandatory-milestone-completion-sequence).
- During owner refinement, follow the framework's [owner refinement verification rule](../README.md#owner-refinement-verification): rely on the owner's direct product review and avoid Playwright or E2E runs for routine fixes unless a major refactor is involved or the owner requests them.
- Only one milestone is active unless the owner explicitly approves parallel work.
- Tasks may change inside an approved milestone when the outcome and boundaries do not change.
- Scope, user behavior, architecture, data, compatibility, dependency, or requirement changes need owner approval.
- When this feature belongs to an initiative, a milestone also requires eligibility under the initiative's authoritative coordinated sequence.

## Initiative Coordination

Initiative: None, or [<Initiative name>](../../initiatives/<initiative-name>/README.md)

Authoritative sequence: Not applicable, or [Coordinated implementation sequence](../../initiatives/<initiative-name>/implementation-sequence.md)

Current coordinated stage: Not applicable

Next eligible stage: Not applicable

## Milestone Overview

| Milestone | Coordinated stage | Outcome | Requirements | Status      | Approval       |
| --------- | ----------------- | ------- | ------------ | ----------- | -------------- |
| M1        | None              |         |              | Not started | Not authorized |

Allowed milestone statuses: `Not started`, `Authorized`, `In progress`, `Owner refinement`, `Hardening review`, `Hardening implementation`, `Final validation`, `Accepted`, or `Blocked`.

Keep the milestone codes, names, and outcomes synchronized with the feature README checklist and, when applicable, the initiative README checklist. Check entries only after definitive owner acceptance, following the [checklist rules](../README.md#readme-milestone-checklists).

## M1: <User-visible outcome>

### Outcome

Describe what the owner can observe and review when this milestone is complete.

### Requirements Advanced

- FR-001

### Included

- To be defined.

### Excluded

- To be defined.

### Tasks

- [ ] Consolidate the approved specification before requesting milestone authorization.
- [ ] Add the smallest coherent implementation task.
- [ ] Add or update automated tests.
- [ ] Perform the milestone's manual review.
- [ ] Create or update `browser-review.md` if this milestone is meaningfully testable in the browser.
- [ ] Obtain owner approval of the implemented behavior.
- [ ] Review the whole milestone for maintainability, weak boundaries, unsafe failure paths, inadequate tests, performance risks, and documentation incongruences.
- [ ] Obtain owner approval for any proposed hardening work.
- [ ] Implement and verify the approved hardening work.
- [ ] Consolidate the delivered specification and evidence links after hardening.
- [ ] Obtain definitive owner acceptance after the final test.
- [ ] Record acceptance and synchronize documentation and decision records.

### Validation

Automated:

- <Command and expected result>

Manual:

- <Review scenario>
- Owner browser path: [Browser review guide](browser-review.md), or `Not applicable` with a short reason.

### Approval to Start

Status: Not approved

Coordinated-stage eligibility: Not applicable, or link the authoritative stage and its prerequisites.

Approved by:

Date:

Scope and conditions:

### Checkpoint Report

Status: Not reached

Completed work:

Validation evidence:

Browser review guide:

Deviations from the approved plan:

New decisions or risks:

Known limitations:

Implementation review decision: Pending

Implementation review date:

### Hardening Review

Status: Not reached

Code and architecture findings:

Documentation findings:

Approved hardening scope:

Verification after hardening:

### Definitive Acceptance

Owner decision: Pending

Decision date:

Documentation synchronized:

Historical milestone record:

Remaining evidence gaps:

Conditions for the next milestone:

## Plan Review Checklist

- [ ] Milestones are coherent and reviewable.
- [ ] Milestones stop at meaningful decision boundaries.
- [ ] Every approved requirement is covered by at least one milestone.
- [ ] Each milestone defines automated and manual validation.
- [ ] Dependencies and migration ordering are clear.
- [ ] Every milestone belonging to an initiative maps to its coordinated stage.
- [ ] The current coordinated stage and next eligible cross-feature work are visible in the feature README and plan.
- [ ] The owner has approved the plan gate in `README.md`.
- [ ] The owner has explicitly approved M1 before implementation begins.
- [ ] The milestone cannot become `Accepted` until its hardening review, final owner validation, and definitive acceptance are complete.
