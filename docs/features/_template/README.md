# <Feature Name>

## Summary

Describe the feature in one or two sentences without prescribing a solution.

## Status

Lifecycle: Discovery

Current milestone: None

Last updated: YYYY-MM-DD

## Owner

Product owner: <name>

## Milestone Checklist

List every milestone from [the plan](plan.md) in plan order, including accepted, active, and future milestones. Use `- [ ] Milestone Code and Name: a few words that define it.` Check an entry only after definitive owner acceptance, then update the initiative overview too when applicable. Keep scope, dates, and evidence in the plan.

- [ ] M1 <Milestone name>: <Short outcome>.

If no milestones have been defined, replace the example with `No milestones defined yet.` Do not invent milestones during discovery.

## Initiative and Sequencing

Initiative: None, or [<Initiative name>](../../initiatives/<initiative-name>/README.md)

When this feature belongs to an initiative, read its mandatory shared documents before this dossier. List direct links here instead of linking only the initiative overview:

1. [Coordinated implementation sequence](../../initiatives/<initiative-name>/implementation-sequence.md)
2. [Dependency register](../../initiatives/<initiative-name>/dependencies.md)
3. Add any other documents marked mandatory by the initiative overview.

Current coordinated stage: Not applicable

Next cross-feature dependency: None

The initiative owns cross-feature ordering. Local milestone adjacency does not authorize work when the coordinated stage or a shared gate is not eligible.

## Documentation Reading Rules

Follow the [documentation consolidation process](../README.md#documentation-consolidation) before milestone implementation, after hardening, and after an approved major change of direction. Keep approved future scope distinct from delivered behavior. Consolidation preserves approved meaning, evidence gaps, and acceptance status.

Follow the [public API documentation rule](../README.md#public-api-documentation) for every plugin API contract change: update `src/definitions.ts` types/JSDoc and regenerate the root README API documentation in the same change, including behavior-only changes. Never manually edit generated API sections.

Historical records are outside the default reading path and do not authorize implementation. Consult relevant records when revisiting a design choice, investigating a regression, or checking past approvals. Current constraints and their essential rationale are stated in the active specification.

## Documents

During discovery and requirements refinement, start with [Discovery](discovery.md). For implementation and maintenance, read these in order:

1. [Requirements](requirements.md)
2. [Design](design.md)
3. [Decisions](decisions.md)
4. [Plan](plan.md)
5. [Verification](verification.md)
6. [Browser review guide](browser-review.md), required only when the current milestone can be meaningfully reviewed in the browser

## Approval Gates

Allowed states: `Not ready`, `Awaiting review`, `Changes requested`, or `Approved`.

| Gate             | Status    | Approved by | Date | Scope and conditions |
| ---------------- | --------- | ----------- | ---- | -------------------- |
| Discovery        | Not ready |             |      |                      |
| Requirements     | Not ready |             |      |                      |
| Design           | Not ready |             |      |                      |
| Plan             | Not ready |             |      |                      |
| Final acceptance | Not ready |             |      |                      |

Milestone approvals and checkpoint results are recorded in `plan.md`.

## Milestone Completion Rule

Every milestone follows the [mandatory milestone completion sequence](../README.md#mandatory-milestone-completion-sequence). Owner approval of the implemented behavior begins the whole-slice hardening review. It does not accept the milestone or authorize the next one. Definitive acceptance is recorded only after any approved hardening work and the owner's final validation.

## Current Blockers

- None.

## Pause or Cancellation Record

Complete this only if the feature is paused or cancelled.

Reason:

Consequences:

Conditions for resuming:

## History and Evidence

When records exist, link the history index or completed milestone records here. Keep settled discovery, owner interviews, completed review guides, and superseded designs outside the implementation reading path. Do not create empty history files.
