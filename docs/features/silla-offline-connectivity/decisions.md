# Decisions

## Decision Index

| ID | Title | Status | Date | Superseded by |
| --- | --- | --- | --- | --- |
| D-001 | Silla-controlled fork | Accepted | 2026-10-08 | |
| D-002 | Keep charger-service readiness in Silla | Proposed | 2026-10-08 | |
| D-003 | Preserve persistent iOS default during temporary-connection research | Proposed | 2026-10-08 | |
| D-004 | Review timeout, routing failure, and retention contracts before changes | Proposed | 2026-10-08 | |
| D-005 | Android requested routing is required; Wi-Fi internet access is not | Accepted | 2026-10-08 | |
| D-006 | Batch device validation across milestones | Accepted | 2026-10-08 | |
| D-007 | Separate iOS budgets and retain configuration until disconnect | Accepted | 2026-10-08 | |
| D-008 | Accept M1 and M2 after owner testing | Accepted | 2026-10-12 | |
| D-009 | Continue plugin-side S3 diagnostics/readiness implementation | Accepted | 2026-10-12 | |
| D-010 | Accept S3 lifecycle-event refinement checkpoint | Accepted | 2026-10-12 | |

## D-001: Silla-controlled fork

Decision maker: Silla owner in the conversation.

Repeated regressions have required pins and corrective work. The owner chose to continue independently in this fork for now. Internal release control takes precedence over upstream merge timing; contributing upstream is optional. This direction does not approve individual API changes or implementation milestones. Revisit if upstream validation and responsiveness demonstrably meet Silla's needs.

## D-002: Keep charger-service readiness in Silla

The generic plugin should supply accurate association/routing observations; Silla should probe its health/login/WebSocket endpoints. This preserves reusable boundaries but requires changes and validation in another repository. Alternative: embed charger-specific readiness in the plugin, at the cost of coupling. Review before diagnostics/readiness implementation.

## D-003: Preserve persistent iOS default during temporary-connection research

Both investigated releases use persistent configuration. Temporary joins can disconnect during backgrounding, sleep, or termination. Propose an optional setting only after comparison on hardware; do not claim it fixes offline AP selection. Revisit when experiments establish an actual benefit and monitoring requirements are agreed.

## D-004: Review timeout, routing failure, and retention contracts before changes

These choices change user-visible behavior: whether consent consumes the budget, whether requested binding failure rejects connect, and whether timeout removes an app-owned configuration. Document alternatives and obtain owner approval for the affected slice rather than silently selecting new defaults. Technical mechanisms inside approved contracts can be chosen during design review.

Android routing/offline choices were resolved in D-005. Android's existing request budget remains unchanged; iOS choices were resolved in D-007.

## D-005: Android requested routing is required; Wi-Fi internet access is not

Status: Accepted. Decision maker: Silla owner, explicit answers during M1 preparation.

When autoRouteTraffic is true, binding failure rejects connect with CONNECTION_FAILED instead of silently resolving. Modern Wi-Fi requests remove the internet capability requirement for either routing option, allowing offline charger APs. The owner approved both consequences after a plain-language explanation.

Alternative: preserve best-effort binding and existing capability policy. Rejected because it lets a successful-looking connection leave Silla unable to reach the charger and couples local routing to internet eligibility. Device tests remain necessary; acceptance of this design choice is not acceptance of runtime correctness.

## D-006: Batch device validation across milestones

The owner explicitly authorized continuing development while M1 phone testing was outstanding because repeated device setup/review is costly. Develop subsequent milestones with their own automated checks and API docs, then provide a combined device review. At the time, pending phone evidence was not an accepted gap; the owner later accepted M1 and M2 as recorded in D-008. This exception permitted M2 preparation/implementation after its product choices were resolved; it did not silently approve iOS timeout or retention semantics.

## D-007: Separate iOS budgets and retain configuration until disconnect

The owner explicitly selected up to two minutes for Apple's request/consent, followed by a separate default 30 seconds to confirm the SSID. Expose requestTimeoutMs as an iOS-only option and retain timeoutMs for verification; both are finite positive values capped at ten minutes under repository policy. The owner also selected retaining persistent configuration on timeout/error and removing the selected configuration only on explicit disconnect.

Compared with one overall timeout, separate budgets protect verification time from delayed consent. Compared with failure rollback, retention avoids erasing a previous working configuration. Consequences: total default wait can reach 150 seconds; iOS can still join after plugin timeout; disconnect cannot dismiss Apple's prompt, so late cleanup needs ownership guards. Full native and device validation are deferred evidence, not accepted gaps.

## D-008: Accept M1 and M2 after owner testing

Decision maker: Silla feature owner.

On 2026-10-12, the owner stated that M1 and M2 had been tested thoroughly and explicitly accepted both milestones. This records owner acceptance; individual device/build/scenario results were not supplied in that update and are not reconstructed here. S3 remains a separate, partial implementation and is not covered by this acceptance.

## D-009: Continue plugin-side S3 diagnostics/readiness implementation

Decision maker: Silla feature owner.

On 2026-10-12, after accepting M1 and M2, the owner directed continued S3 implementation. The authorized increment is plugin-side diagnostic lifecycle accuracy and network/IP readiness observations, with matching public types, generated API docs, and focused tests. Silla-side health/login/WebSocket readiness and app recovery remain external integration work. This direction authorizes development of the S3 plugin increment; it does not accept S3 or waive its verification, review, and device evidence.

## D-010: Accept S3 lifecycle-event refinement checkpoint

Decision maker: Silla feature owner.

On 2026-10-12, the owner tested and approved the S3 lifecycle-event refinement: distinct association-confirmed and verification-started observations, and terminal diagnostic outcomes protected from late duplicate completion. This accepts that implementation checkpoint only. It does not accept all S3 requirements; IP/readiness validation, remaining diagnostics review, native iOS checks, Silla integration, and S3 final acceptance remain open.
