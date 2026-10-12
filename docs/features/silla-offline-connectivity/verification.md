# Verification

## Verification Strategy

Combine meaningful native lifecycle automation, compilation/static checks, and physical-device end-to-end validation through Silla. Compilation and JavaScript tests cannot prove association, routing, or configuration persistence. Browser review is not applicable to these native outcomes.

## Requirement Coverage

| Requirements | Automated evidence planned | Manual scenarios | Status |
| --- | --- | --- | --- |
| FR-001–FR-004 | Observation/identity guards and API compatibility | V-001, V-002, V-003 | Owner accepted M1 on 2026-10-12; details not recorded here |
| FR-005–FR-008 | Ownership, stale callbacks, routing-result tests | V-001, V-004, V-005 | Owner accepted M1 on 2026-10-12; details not recorded here |
| FR-009–FR-011 | Deadline/single-settlement/cancellation tests | V-006, V-007 | Owner accepted M2 on 2026-10-12; details not recorded here |
| FR-012–FR-014 | Readiness classification and app error propagation | V-001, V-008 | Partially implemented; IP classification build-checked, Silla service and device readiness checks outstanding |
| FR-015–FR-017 | Event stages, correlation, snapshot bounds | V-009 | Android diagnostics/build, API/docs, Web build, lint pass 2026-10-12; iOS policy tests, native/device coverage, and full S3 acceptance remain outstanding |
| FR-018–FR-020 | Authorization mapping where testable | V-003, V-010 | Not verified |
| FR-021 | Option/default compatibility if adopted | V-011 | Not verified |
| FR-022–FR-024 | App metadata and target-identity tests | V-008, V-009, V-010 | Not verified |
| FR-025, QR-001–QR-004 | Build/package/release checks; lifecycle and privacy tests | V-001–V-012 | Not verified |
| QR-005 | `bun run docgen` or `bun run build`; review generated root README diff | Compare API documentation with each changed contract and its device evidence | M1/M2 API docs accepted; S3 API docs regenerated and build passed 2026-10-12 |

M1 automated evidence: Android debug/release compilation and 14 new JUnit tests passed on 2026-10-08. These test network identity, redaction/fallback policy, terminal state, and binding ownership/retry. They do not simulate Android callback delivery, permission prompts, timer scheduling, or actual traffic. The owner reports thorough M1 testing and accepted the milestone on 2026-10-12; specific device runs are not detailed in this dossier.

M2 automated evidence, 2026-10-08: 12 Foundation-only XCTest cases passed using Swift 6.0.3 on Linux, covering separate deadlines, late callbacks after terminal completion, missing-observation timeout policy, finite timeout limits, and per-SSID cleanup ownership. Swift frontend syntax parsing of the changed adapter/policy/tests passed. These checks do not type-check Capacitor/NetworkExtension integration or execute Dispatch watchdogs against Apple APIs. The owner reports thorough M2 testing and accepted the milestone on 2026-10-12; specific device/build runs are not detailed in this dossier.

S3 implementation present in commit `b818e85` (2026-10-11), with owner-approved lifecycle-event refinements (D-010) and further plugin implementation on 2026-10-12: Android and Foundation-only iOS diagnostics classes/tests, native snapshot methods, IP readiness classification, `getConnectionDiagnostics()` API types, and unsupported Web method. Refinements add distinct association-confirmed and verification-started events, align Android routing failures with the public `connectionStage` type, distinguish timeout and permission-denied terminal events, protect outcomes from late duplicate completion, correct legacy Android request/availability event timing, preserve unknown IP state before an attributable sample, and defensively copy iOS snapshot events/errors. Android tests/build, Web build, lint, and API documentation regeneration passed on 2026-10-12. New Android diagnostics tests passed; new iOS policy tests were added but not run because Swift is unavailable. Native iOS build and remaining S3 device/app flows remain outstanding.

Source review confirms native adapters reserve the diagnostics attempt before validation/permission checks. iOS returns cached diagnostics when no network target is tracked; target refresh uses a watchdog and attempt/revision guards. The iOS diagnostics model returns defensive copies of event history and nested error metadata, with mutation-isolation XCTest coverage. The latest XCTest additions could not be run because Swift is unavailable, and adapter call-site behavior still requires device evidence.

Source review confirms native adapters reserve the latest diagnostics attempt before validation/permission checks, and the iOS snapshot returns cached state without requiring a tracked network target. Target refresh has a five-second watchdog and revision/current-attempt guards that suppress stale mutation. The iOS diagnostics model now returns defensive copies of event history and nested error dictionaries; tests exercise snapshot mutation isolation but remain unexecuted in this Linux environment. Adapter call-site behavior still requires device evidence.

## Automated Checks

| Check | Command | Coverage | Result and date |
| --- | --- | --- | --- |
| Full repository verification | `bun run verify` | iOS, Android, Web platform checks | 2026-10-08 M1: blocked at iOS; `xcodebuild` unavailable on Linux |
| Android | `bun run verify:android` | Debug/release build, JUnit tests, Android lint report | 2026-10-08: passed build/tests using Java 21 and SDK 36; lint report retains unrelated existing findings |
| Web | `bun run verify:web` | TypeScript, generated docs, Rollup | 2026-10-08: passed |
| Lint | `bun run lint` | ESLint, Prettier, SwiftLint wrapper | 2026-10-08: passed with six existing Web unused-parameter warnings; SwiftLint skipped by wrapper on Linux |
| API documentation/build | `bun run docgen`, `bun run build` | TypeScript, docgen, bundles | 2026-10-08: root README regenerated; Web verification runs build |
| iOS lifecycle policy | `bun run test:ios:lifecycle` | Actual Foundation policy/ownership source and XCTest cases in an isolated temporary package | 2026-10-08 M2: 12 tests passed; temporary package removed after execution |
| Swift syntax | `swiftc -frontend -parse ios/Sources/CapacitorWifiPlugin/CapacitorWifiPlugin.swift ios/Sources/CapacitorWifiPlugin/WifiConnectionLifecycle.swift ios/Tests/CapacitorWifiPluginTests/WifiConnectionLifecycleTests.swift` | Swift syntax only, not Apple API type checking | 2026-10-08 M2: passed |
| Android diagnostics tests | `bun run verify:android` | Android debug/release compilation and JUnit tests, including `WifiConnectionDiagnosticsTest` | 2026-10-12: passed after lifecycle, routing, timeout/permission, legacy timing, defensive model, and IP classification refinements with Java 21 and SDK; lint completed with baseline findings only |
| iOS diagnostics/lifecycle policy | `bun run test:ios:lifecycle` | Foundation-only diagnostics and lifecycle policy XCTest cases in temporary package | 2026-10-12: blocked; `swift` is unavailable on PATH in this Linux environment; newly added iOS tests, including snapshot mutation isolation, are not executed here |
| S3 API documentation/build | `bun run build` | Generated README API method/types, TypeScript, and Rollup bundles | 2026-10-12: passed; README regenerated with diagnostics event types and routing failure-stage type |
| Repository lint | `bun run lint` | ESLint, Prettier, SwiftLint wrapper | 2026-10-12: passed after current increment; six existing Web unused-parameter warnings; SwiftLint skipped on Linux |
| Web verification | `bun run verify:web` | TypeScript, generated docs, Rollup | 2026-10-12: passed after current increment |

Temporary Bun, Java 21, and Android SDK installations under `/tmp/opencode` enabled validation without changing system configuration. Android test reports contain nine `WifiConnectionStateTest` and five `WifiProcessBindingTest` cases per debug/release variant, plus the existing template example test. Android lint's existing MissingPermission findings concern scanning and saved-network queries; other existing warnings concern older APIs/dependencies. The new callback API-level lint finding encountered during development was corrected with a guarded callback class annotation.

M1 uses the existing JUnit dependency for pure Java state and ownership tests, executed by Android verification. Additional native integration test tooling remains future work. Verify both SPM and CocoaPods when changing iOS package integration. Use Java 21 for Android.

M2 uses a temporary Swift installation and compatible runtime libraries under `/tmp/opencode`. Its test runner copies the actual policy/test files into a disposable package so Apple's framework imports are excluded. The production SPM manifest and CocoaPods spec remain unchanged; both already include the new source file. Repeated full verification remains blocked at unavailable Xcode. M2 build/docgen and repository lint passed with the existing Web warnings; SwiftLint remains skipped on Linux.

## Manual Scenarios

The owner accepted M1 and M2 on 2026-10-12 after reporting thorough testing. Per-scenario results and device/build identifiers were not supplied here, so this table remains an evidence-capture checklist rather than a reconstructed pass/fail log. Record device/OS, signed app and native plugin version, commit/artifact, charger model/firmware, starting networks, permission state, timestamped events, expected result, and actual result if those records become available. Exclude credentials.

| ID | Scenario and steps | Expected result |
| --- | --- | --- |
| V-001 | Start on home Wi-Fi with cellular enabled; connect offline charger; call health/login and sustain WebSocket; disconnect | Requested AP verified, local traffic works without internet, successful request persists, cloud recovers after cleanup |
| V-002 | Supply wrong/default SSID evidence; repeat with simultaneous Wi-Fi networks where supported | Another network is never accepted as the requested one; redacted primary data exercises only eligible fallback |
| V-003 | Repeat Android 12+ with location permission denied and location services disabled, then restore them; test guarded path below API 31 | Understandable bounded failures/diagnostics; authorized redaction path works; older APIs remain compatible |
| V-004 | Cancel during request/verification; retry; issue concurrent calls; deliver old callbacks after a newer attempt | Single settlement, no overlap leakage, old cleanup cannot affect new resources |
| V-005 | Wrong password, unavailable charger, binding failure, and timeout; inspect cleanup and attempt cloud requests | Native-specific causes only when available, accurate stage, correct callback/binding released, cloud access recovers |
| V-006 | On iPhone accept consent after 60 seconds with defaults; use a short requestTimeoutMs to expire while apply is pending; separately expire timeoutMs during verification | Consent under 120 seconds gets a fresh 30-second verification budget; request/verification failures have accurate stages, retain configuration, and late completion cannot settle twice |
| V-007 | Disconnect during iOS apply/verification or after timeout; retry the same SSID and then a different SSID; issue a newer addNetwork for the same target before old completion | Pending attempt rejects once, no stale success, late cleanup removes only the disconnected target and cannot remove newer same-SSID configuration |
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

## M1 Owner Device Review

Use a Silla build that actually embeds this checkout's plugin; record the commit and native plugin version. The unchanged version string alone cannot distinguish this patch from upstream 8.5.5.

1. Start connected to home Wi-Fi, leave cellular enabled, and switch to the charger's offline Wi-Fi from Silla.
2. Confirm Silla can reach the charger, log in, and keep its live connection working.
3. Disconnect through Silla and confirm cloud access recovers.
4. Repeat a connect/disconnect cycle, cancel a connection, and try an unavailable charger or wrong password. Confirm a later correct attempt still works.
5. Repeat with location permission denied and location services disabled; confirm the failure explains the relevant requirement.
6. Export app diagnostics and capture native `CapacitorWifi` log events for correlation. Check SSID observation and binding results without recording credentials.

Review/acceptance date: 2026-10-12. Owner reports thorough M1 testing and accepts the milestone. Reviewer/device/build and per-scenario results were not provided in this dossier.

## Combined M1/M2 Device Review

Owner authorization on 2026-10-08 permitted collecting device evidence in one batch. On 2026-10-12 the owner reported thorough testing and accepted both M1 and M2. The same immutable revision/device/build identifiers and step-by-step results were not provided in this dossier; retain the following scenarios as a template for recording evidence if available:

1. Start on home Wi-Fi with cellular enabled, connect to the offline charger, and confirm local health/login/live communication.
2. On a connection that displays Apple's consent prompt, wait about one minute before accepting. With default settings, the prompt delay must not consume the later 30-second SSID confirmation budget. Record if no prompt is displayed.
3. Use a short requestTimeoutMs in a test build and leave the prompt unanswered until timeout. Confirm the error stage is request and any later OS completion does not change the rejected result. Verify retained configuration and inspect Settings.
4. Exercise verification timeout separately where reproducible; confirm verification diagnostics and retained configuration. Record an untestable scenario as an evidence gap rather than a pass.
5. Disconnect while connecting, then retry the same target. Confirm old results cannot report success or remove the newer request. Repeat with another target and inspect app-owned saved configurations.
6. After a timed-out attempt, disconnect without specifying an SSID; confirm it targets the tracked charger, even if home Wi-Fi is current. With an explicit target, verify only that app-owned configuration is selected for removal.
7. Capture foreground/background transitions, actual native plugin/build identity, stages, elapsed times, SSID observations, and local service outcomes without credentials.

The owner has accepted M1 and M2. Native compilation, SwiftLint, and device/build details are not independently documented here; the acceptance record is not a substitute for preserving those details when available. S3 remains unaccepted and requires its own evidence and review.

## Known Gaps

Commit `b818e85` adds the initial plugin diagnostics and address classification but does not implement Silla-side health/login/WebSocket probes or recovery. The snapshot outcome is historical, and IP classification does not demonstrate local route or charger-service readiness. Android diagnostics tests, API/docs, Web build, and lint passed on 2026-10-12, including terminal outcomes, association ordering, routing stage, timeout/permission classification, legacy timing, unknown-versus-missing IP, and snapshot model isolation. Foundation-only iOS tests could not run because Swift is unavailable. Native iOS compilation/SwiftLint, device validation, and Silla integration remain outstanding; adapter call-site/device coverage, cleanup reporting, Android timeout-cap compatibility, and full cross-platform event parity remain open.

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
