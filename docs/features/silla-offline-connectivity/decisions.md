# Decisions

## Decision Index

| ID | Title | Status | Date | Superseded by |
| --- | --- | --- | --- | --- |
| D-001 | Silla-controlled fork | Accepted | 2026-10-08 | |
| D-002 | Keep charger-service readiness in Silla | Proposed | 2026-10-08 | |
| D-003 | Preserve persistent iOS default during temporary-connection research | Proposed | 2026-10-08 | |
| D-004 | Review timeout, routing failure, and retention contracts before changes | Proposed | 2026-10-08 | |

## D-001: Silla-controlled fork

Decision maker: Silla owner in the conversation.

Repeated regressions have required pins and corrective work. The owner chose to continue independently in this fork for now. Internal release control takes precedence over upstream merge timing; contributing upstream is optional. This direction does not approve individual API changes or implementation milestones. Revisit if upstream validation and responsiveness demonstrably meet Silla's needs.

## D-002: Keep charger-service readiness in Silla

The generic plugin should supply accurate association/routing observations; Silla should probe its health/login/WebSocket endpoints. This preserves reusable boundaries but requires changes and validation in another repository. Alternative: embed charger-specific readiness in the plugin, at the cost of coupling. Review before diagnostics/readiness implementation.

## D-003: Preserve persistent iOS default during temporary-connection research

Both investigated releases use persistent configuration. Temporary joins can disconnect during backgrounding, sleep, or termination. Propose an optional setting only after comparison on hardware; do not claim it fixes offline AP selection. Revisit when experiments establish an actual benefit and monitoring requirements are agreed.

## D-004: Review timeout, routing failure, and retention contracts before changes

These choices change user-visible behavior: whether consent consumes the budget, whether requested binding failure rejects connect, and whether timeout removes an app-owned configuration. Document alternatives and obtain owner approval for the affected slice rather than silently selecting new defaults. Technical mechanisms inside approved contracts can be chosen during design review.
