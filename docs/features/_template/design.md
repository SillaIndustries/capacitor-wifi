# Design

Keep applicable architecture, contracts, essential rationale, and constraints here. Distinguish delivered design from approved future work. Move superseded architectures, detailed alternatives, and correction narratives to history after the relevant consolidation checkpoint.

## Design Summary

Explain the proposed solution and why it fits the approved requirements.

Apply the decision escalation boundary from `docs/features/README.md`. Ask the owner about product consequences and material trade-offs. Select Level 2 technical mechanisms, document them for gate review, and do not turn each one into a separate checkpoint.

## Requirement Traceability

| Requirement | Design element | Notes |
| --- | --- | --- |
| FR-001 | | |

## User Experience

Describe screens, states, interactions, keyboard behavior, responsive behavior, accessibility behavior, and content changes. Use diagrams or wireframes only when they clarify the design.

## Current System

Describe relevant existing behavior and code locations. Verify audit claims against the current code.

## Proposed Architecture

Describe component boundaries, responsibilities, data flow, and integration points.

## Data and State

Describe authoritative state, derived state, persistence, schemas, migrations, defaults, cleanup, and backward compatibility.

## Interfaces and Contracts

Describe APIs, events, shared types, validation, error contracts, and public module boundaries that will change.

## Security and Privacy

Describe trust boundaries, authorization, sensitive data, abuse cases, and privacy effects where relevant.

## Accessibility and Internationalization

Describe relevant semantics, keyboard access, focus behavior, announcements, contrast, motion, layout direction, and translatable content.

## Failure and Recovery

Describe failure modes, partial completion, retry behavior, conflict handling, fallback behavior, and observability.

## Alternatives Considered

| Alternative | Benefits | Costs and risks | Reason accepted or rejected |
| --- | --- | --- | --- |
| | | | |

## Dependencies

### Existing

- None identified.

### Proposed

For every new dependency, include its purpose, maintenance and security implications, alternatives, and approval status.

- None proposed.

## Risks

| ID | Risk | Likelihood | Impact | Mitigation or experiment |
| --- | --- | --- | --- | --- |
| R-001 | | | | |

## Rollout and Compatibility

Describe migrations, feature flags, staged rollout, fallback, rollback, and support for existing data or users.

## Open Questions

No blocking design questions should remain when this document is submitted for approval.

## Design Review Checklist

- [ ] Every approved requirement is addressed.
- [ ] Current code and data behavior have been verified.
- [ ] Material alternatives and trade-offs are visible.
- [ ] Owner checkpoints were reserved for Level 1 choices, while Level 2 choices are documented for gate review.
- [ ] Data, failure, accessibility, compatibility, and security concerns are addressed where relevant.
- [ ] New dependencies are justified and approved.
- [ ] Major decisions are recorded in `decisions.md`.
- [ ] The owner has approved the design gate in `README.md`.
