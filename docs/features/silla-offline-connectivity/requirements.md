# Requirements

Status: M1/M2 scope authorized for implementation. Device acceptance criteria remain unverified and reviews are batched by owner exception; requirements outside these milestones remain proposed. No milestone is accepted.

## Scope and User Workflows

Connect to offline charger Wi-Fi, establish local communication, recover from failure, disconnect, and restore cloud access. Plugin-native requirements and Silla-app requirements are assigned explicitly below. Charger endpoint probing belongs in Silla. Arbitrary user-owned iOS network removal is excluded.

## Functional Requirements

Every row states an observable requirement and its acceptance criterion. M1 covers FR-001–FR-008; M2 covers FR-009–FR-011. Both include initial FR-015–FR-016 diagnostics and applicable QR-001–QR-005. Broader diagnostic exports, complete release controls, additional iOS changes, and Silla changes remain future scope.

| ID | Priority | Requirement | Acceptance criterion | Owner |
| --- | --- | --- | --- | --- |
| FR-001 | Must | Verify the requested Android network with usable network-specific information | On Android 12+, a correctly authorized offline charger attempt resolves on the requested network without a redaction-induced timeout; older APIs retain guarded compatibility | Plugin |
| FR-002 | Must | Distinguish unavailable/redacted SSID from known mismatch | Controlled unavailable-information and wrong-network scenarios produce distinguishable diagnostics and bounded outcomes | Plugin |
| FR-003 | Must | Safely handle redacted primary information and fallback | Unknown primary SSID does not automatically skip eligible fallback; another/default Wi-Fi network cannot prove the requested network | Plugin |
| FR-004 | Must | Retain successful temporary Android connectivity | Local HTTP and WebSocket traffic continue after resolution until disconnect/replacement | Plugin |
| FR-005 | Must | Release only attempt-owned resources | Failure, cancellation, and replacement release the owning callback/binding; stale callbacks cannot unbind or settle a newer attempt | Plugin |
| FR-006 | Must | Require requested routing | When autoRouteTraffic is true, binding failure rejects connect with CONNECTION_FAILED and records bindingSucceeded: false; failed requests are cleaned up | Plugin |
| FR-007 | Must | Restore routing after disconnect/failure | Normal cloud traffic recovers after an owned binding is released | Plugin + Silla |
| FR-008 | Should | Keep offline-network eligibility independent of routing policy | Android 10+ requests do not require internet access for either routing option; cellular-enabled offline AP access is validated on hardware | Plugin |
| FR-009 | Must | Give iOS separate bounded waiting periods | requestTimeoutMs defaults to 120000 for apply/consent; timeoutMs defaults to a fresh 30000 verification budget after success/alreadyAssociated. Each iOS value is finite, positive, and at most 600000. Independent watchdogs settle missing native callbacks, and late OS completion cannot settle again | Plugin |
| FR-010 | Must | Make iOS cancellation and completion deterministic | Disconnect during apply/verification invalidates the attempt; each call settles once and subsequent connect remains usable | Plugin |
| FR-011 | Must | Retain iOS configuration until explicit disconnect | Timeout/error preserves persistent configuration, including pre-existing app-owned configuration. Disconnect removes the selected app-owned target; late cancellation cleanup cannot remove a newer same-SSID request. User/other-app configurations are outside removal ownership | Plugin |
| FR-012 | Must | Separate connection milestones | Docs and results distinguish request, association, routing where applicable, IP readiness, and service readiness; connect resolution has an explicit boundary | Plugin + Silla |
| FR-013 | Should | Supply useful network/IP readiness information | Silla can distinguish absent IP and link-local IPv4 from usable configuration; lack of internet is not a readiness failure | Plugin + Silla |
| FR-014 | Must | Verify charger services in the app | Silla checks actual local service readiness after native success and reports DHCP/routing/service failures separately | Silla |
| FR-015 | Must | Provide attempt-level structured diagnostics | Events identify attempt, timestamp, elapsed time, native request, availability/capabilities, SSID result, binding, IP observations where available, timeout, and cleanup | Plugin |
| FR-016 | Must | Preserve stable errors and accurate failure stages | Errors preserve existing codes/native metadata; a request-stage deadline is not always labeled verification | Plugin |
| FR-017 | Should | Export a support diagnostic snapshot | A bounded snapshot explains the latest attempt without requiring ad hoc native logging | Plugin + Silla |
| FR-018 | Must | Explain authorization and observability limitations | Permission denial, disabled location services, unavailable network information, and known mismatch are distinct where OS evidence permits | Plugin |
| FR-019 | Should | Improve iOS authorization context | Available location accuracy information and entitlement guidance are documented; location denial alone is not asserted to prevent app-configured SSID reads | Plugin |
| FR-020 | Must | Keep local-traffic permission failures distinct | Silla does not present Local Network/HTTP failures as proven association or location failures | Silla |
| FR-021 | Could | Offer optional iOS temporary configuration after experimentation | Compare persistent/temporary behavior; if adopted expose an optional setting with persistent default and documented background/sleep/termination consequences | Plugin |
| FR-022 | Must | Preserve Silla error metadata and meaningful last failure | Exported diagnostics retain code/selected data; callers receive the last meaningful association/IP/service failure | Silla |
| FR-023 | Must | Clean up using the requested charger identity | Switching back to home Wi-Fi does not make Silla target home SSID instead of its app-owned charger configuration | Silla |
| FR-024 | Must | Keep local operation independent of cloud reachability | Cloud failures/offline banners do not suppress local HTTP/Bluetooth operations | Silla |
| FR-025 | Must | Control internal releases and upstream adoption | Releases reference immutable artifacts/commits, record validated baseline and notes, and review connection-related upstream changes before adoption | Fork maintenance |

## Quality Requirements

| ID | Priority | Requirement | Acceptance criterion |
| --- | --- | --- | --- |
| QR-001 | Must | Device-backed regression acceptance | Release matrix includes offline AP/cellular/home Wi-Fi, delayed consent, bad credentials/unavailable AP, permissions, cancellation/retry/concurrency, backgrounding, local HTTP/WebSocket, and cloud recovery |
| QR-002 | Must | Meaningful lifecycle automation | Tests exercise races, single completion, timer termination, and owned cleanup without treating mocks as association evidence |
| QR-003 | Must | Diagnostic privacy and bounded storage | No passwords/tokens in events or snapshots; identifier policy is explicit and buffers have documented bounds |
| QR-004 | Must | Integration compatibility | Existing public APIs remain compatible unless explicitly approved otherwise; both CocoaPods and SPM remain valid and plugin major follows Capacitor |
| QR-005 | Must | Public API documentation matches every contract change | Each API contract change updates types/JSDoc in `src/definitions.ts` and regenerates the root README API docs in the same change; review confirms signatures, behavioral semantics, defaults, errors, and platform differences match implementation |

## Constraints

- C-001 — Repository guidance: use Bun, Java 21, and checks/timeouts at or below 10 minutes; do not manually edit generated API docs or changelog.
- C-002 — iOS platform ownership: hotspot configuration removal cannot forcibly disconnect an arbitrary user-saved home network.
- C-003 — Evidence: neither audit supplies a physical-device reproduction proving the suspected native cause.
- C-004 — Scope: Silla application changes are external deliverables, not changes implicitly available in this repository.

## Error and Edge-Case Behavior

All attempts require bounded plugin outcomes, single settlement, explicit ownership, and intelligible recovery. Unknown/redacted information is not a known mismatch. No detailed native cause is invented from generic errors. An OS operation may outlive a plugin timeout; its late callback must be handled explicitly.

## Dependencies and Open Questions

Physical devices, supported-version matrix, Silla integration repository, signed iOS entitlements, and owner acceptance are required. M1 routing/offline choices and M2 separate budgets/retention were explicitly approved on 2026-10-08. Android retains its 30-second request/consent budget after location authorization. iOS uses the separate budgets in FR-009; Apple can finish applying after plugin timeout/cancellation, and a retry can receive native pending. Plugin cancellation does not dismiss the OS prompt. Background suspension can delay watchdog delivery. Temporary iOS configuration remains experimental.

## Requirements Review Checklist

- [x] Complete backlog assigned stable IDs, priorities, acceptance criteria, and ownership.
- [ ] Product trade-offs and blocking questions resolved for the next slice.
- [ ] Owner approved requirements in README.
