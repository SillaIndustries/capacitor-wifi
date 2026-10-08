# Silla Feature Factory

Feature dossiers describe intended outcomes, evidence, design, delivery scope, and verification for Silla's internal Wi-Fi plugin. The owner has chosen to develop this fork independently for now; upstream contribution is optional.

## Fork origin and ownership

This repository is Silla's fork of [Capgo's `@capgo/capacitor-wifi`](https://github.com/Cap-go/capacitor-wifi). Silla maintains its own production baseline, fixes, and release decisions to support offline EV-charger connectivity. Upstream changes are adopted selectively after review and validation. Feature dossiers describe this fork's intended behavior; they do not imply that Capgo's upstream plugin implements or supports those changes.

## Public API documentation

Every plugin API contract change must include an update to the API documentation in the repository's root [README.md](../../README.md) in the same change. This includes behavioral changes without signature changes: success/readiness semantics, timeout scope, errors, cancellation, routing, permissions, persistence, defaults, and platform differences.

`src/definitions.ts` is the source of truth for generated API documentation. Update its types and JSDoc, then run `bun run docgen` or `bun run build` to regenerate the README API sections. Never edit `<docgen-index>` or `<docgen-api>` sections manually. Update relevant handwritten README guidance/examples when necessary. Review the generated API documentation against the implemented contract before milestone acceptance.

## Features

| Feature | Status | First proposed delivery slice |
| --- | --- | --- |
| [Silla offline charger connectivity](silla-offline-connectivity/README.md) | Discovery | Android requested-network verification and owned cleanup |

The supplied [_template](_template/README.md) is the dossier structure. This index supplies the shared process sections referenced by that template; feature-specific proposals remain subject to owner review.

## Documentation consolidation

Before implementation, after hardening, and after an approved major direction change, reconcile requirements, design, decisions, plan, and verification. Preserve stable IDs, unresolved evidence gaps, and approval scope. Clearly separate proposed behavior from delivered behavior. Move superseded narratives to a history directory only when actual records exist; keep essential rationale in the active specification.

## Decision escalation boundary

Level 1 decisions affect product behavior, public compatibility, configuration persistence, timeout meaning, dependencies, or release policy. Present their consequences to the owner for review.

Level 2 decisions are technical mechanisms within approved boundaries. Select and document them for design review rather than requesting a checkpoint for each implementation detail.

## README milestone checklists

Discovery dossiers have no formal milestones until scope is refined. Once defined, list every plan milestone in identical order in the feature README. Check entries only after definitive owner acceptance, never after implementation alone.

## Mandatory milestone completion sequence

1. Consolidate the specification and obtain explicit milestone authorization.
2. Implement and perform proportionate automated and manual verification.
3. Obtain owner review of implemented behavior and address refinement.
4. Review the whole slice for maintainability, resource ownership, failure paths, test adequacy, compatibility, and documentation consistency.
5. Obtain owner approval for proposed hardening, then implement and verify it.
6. Consolidate delivered specifications and evidence; verify that every API contract change is reflected in the root README API documentation, regenerated from `src/definitions.ts`; request final owner validation.
7. Record definitive acceptance before accepting the milestone or authorizing the next.

Only one milestone is active unless the owner authorizes parallel work. No approval is inferred from documentation preparation.

## Owner refinement verification

Use direct owner review for routine refinement. Avoid browser E2E runs unless a major refactor or owner request warrants them. Native Wi-Fi association and routing require physical-device evidence; browser review and compilation cannot substitute for it.
