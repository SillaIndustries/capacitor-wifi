# Verification

## Verification Strategy

Combine meaningful native lifecycle automation, compilation/static checks, and physical-device end-to-end validation through Silla. Compilation and JavaScript tests cannot prove association, routing, or configuration persistence. Browser review is not applicable to these native outcomes.

## Requirement Coverage

| Requirements | Automated evidence planned | Manual scenarios | Status |
| --- | --- | --- | --- |
| FR-001–FR-004 | Observation/identity guards and API compatibility | V-001, V-002, V-003 | Not verified |
| FR-005–FR-008 | Ownership, stale callbacks, routing-result tests | V-001, V-004, V-005 | Not verified |
| FR-009–FR-011 | Deadline/single-settlement/cancellation tests | V-006, V-007 | Not verified |
| FR-012–FR-014 | Readiness classification and app error propagation | V-001, V-008 | Not verified |
| FR-015–FR-017 | Event stages, correlation, snapshot bounds | V-009 | Not verified |
| FR-018–FR-020 | Authorization mapping where testable | V-003, V-010 | Not verified |
| FR-021 | Option/default compatibility if adopted | V-011 | Not verified |
| FR-022–FR-024 | App metadata and target-identity tests | V-008, V-009, V-010 | Not verified |
| FR-025, QR-001–QR-004 | Build/package/release checks; lifecycle and privacy tests | V-001–V-012 | Not verified |
| QR-005 | `bun run docgen` or `bun run build`; review generated root README diff | Compare API documentation with each changed contract and its device evidence | Not verified |

## Automated Checks

| Check | Command | Coverage | Result and date |
| --- | --- | --- | --- |
| Full repository verification | `bun run verify` | iOS, Android, Web platform checks | Documentation preparation, 2026-10-08: blocked; `bun` is not installed in this environment |
| Individual platforms | `bun run verify:ios`, `bun run verify:android`, `bun run verify:web` | Isolate platform/environment blockers | Not run for implementation |
| Lint | `bun run lint` | Existing repository style checks | Not run for implementation |
| API documentation/build | `bun run build` | TypeScript, docgen, bundles | Not run for implementation |

Select native lifecycle test tooling during slice design; do not invent a passing test command before it exists. Verify both SPM and CocoaPods when changing iOS package integration. Use Java 21 for Android.

## Manual Scenarios

All results below: **Not run**. Record device/OS, signed app and native plugin version, commit/artifact, charger model/firmware, starting networks, permission state, timestamped events, expected result, and actual result. Exclude credentials.

| ID | Scenario and steps | Expected result |
| --- | --- | --- |
| V-001 | Start on home Wi-Fi with cellular enabled; connect offline charger; call health/login and sustain WebSocket; disconnect | Requested AP verified, local traffic works without internet, successful request persists, cloud recovers after cleanup |
| V-002 | Supply wrong/default SSID evidence; repeat with simultaneous Wi-Fi networks where supported | Another network is never accepted as the requested one; redacted primary data exercises only eligible fallback |
| V-003 | Repeat Android 12+ with location permission denied and location services disabled, then restore them; test guarded path below API 31 | Understandable bounded failures/diagnostics; authorized redaction path works; older APIs remain compatible |
| V-004 | Cancel during request/verification; retry; issue concurrent calls; deliver old callbacks after a newer attempt | Single settlement, no overlap leakage, old cleanup cannot affect new resources |
| V-005 | Wrong password, unavailable charger, binding failure, and timeout; inspect cleanup and attempt cloud requests | Native-specific causes only when available, accurate stage, correct callback/binding released, cloud access recovers |
| V-006 | On iPhone delay native consent beyond the former 30-second deadline; test pending apply and late completion | Approved timeout semantics hold; plugin outcome bounded; late completion cannot settle twice |
| V-007 | Disconnect during iOS apply/verification; retry and inspect app-owned configurations | Attempt invalidated, no stale success, retention policy honored without unrelated configuration removal |
| V-008 | Observe absent IP, link-local IPv4, valid IPv4 but unavailable charger service, and healthy charger | Association/IP/service distinctions reach callers; local service probe gates readiness; cloud failure does not suppress local operations |
| V-009 | Export Silla Error-instance/plain-error diagnostics and plugin snapshot | Codes/native metadata retained, last meaningful failure preserved, attempt correlation/stages accurate, bounded buffers, no credentials |
| V-010 | Inspect signed entitlements; test precise/reduced/denied location and Local Network denial independently; switch back home before cleanup | Permission guidance follows evidence; app-configured eligibility respected; cleanup targets requested charger identity |
| V-011 | Compare iOS persistent and temporary configuration with home AP available/unavailable and cellular on/off; background, sleep, terminate; compare manual Settings selection | Evidence classifies AP switching versus traffic failure and documents temporary lifecycle; no unsupported claim of a fix |
| V-012 | Run agreed matrix on immutable internal artifact; inspect package integrations, version notes, baseline and rollback | Reproducible release evidence and supported integrations; untested cases visibly recorded |

## Cross-Cutting Review

- Keyboard/assistive technology and responsive layout: N/A for native plugin; review if Silla UI scope changes.
- Review error, permission, cancellation, and recovery states.
- Review retained configurations and migration/default compatibility.
- Native messages are diagnostic-only; Silla recovery text must be localizable.
- Check polling/timer termination and bounded diagnostic memory.
- Review credentials/identifier privacy and regressions to normal cloud routing.

## Known Gaps

Initial documentation preparation on 2026-10-08 checked all eight new documents' local file links and confirmed 25 functional plus four quality requirement IDs. QR-005 was subsequently added to require API documentation updates for every contract change. The initial check is documentation validation only, not native implementation evidence.

No physical-device causal reproduction or patch validation exists in the audit evidence. Supported device matrix and test owners remain unconfirmed. Silla app changes require its repository. iOS signed capabilities and Local Network authorization cannot be established on this Linux checkout. No gap is accepted by default.

## Milestone Hardening and Final Acceptance

- [ ] Review whole-slice ownership, complexity, races, leaks, performance, compatibility, and test adequacy.
- [ ] Review affected docs and record findings, including an explicit no-findings result.
- [ ] Obtain approval for hardening; implement and rerun proportionate checks.
- [ ] Consolidate delivered docs and evidence; ask for final owner test.
- [ ] For every API contract change, update `src/definitions.ts` types/JSDoc, regenerate and review the root README API documentation, and update relevant handwritten guidance/examples in the same change.
- [ ] Ensure every requirement has evidence or an explicitly accepted gap.
- [ ] Record definitive acceptance before marking any milestone accepted.
