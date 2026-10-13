# S3 Diagnostics Validation Guide

Use this guide to validate the plugin-side S3 diagnostics and readiness work on Android and iOS. It covers the changes currently in the working tree: a bounded `getConnectionDiagnostics()` snapshot, network/IP observations, association and verification events, and terminal outcome protection.

The latest S3 refinements may be uncommitted. Build and test the exact working tree you intend to validate, and record both the current Git commit and any local changes included in the app build. Testing the `b818e85` commit alone does not include later working-tree refinements.

## 1. Run the available plugin checks

From the plugin repository root:

```bash
bun run build
bun run verify:android
bun run verify:web
bun run lint
```

If Bun is installed but not on the shell's `PATH`, add its bin directory first:

```bash
export PATH="$HOME/.bun/bin:$PATH"
```

Android verification also needs Java 21 and an Android SDK configured. For example, adjust these paths if your local installations differ:

```bash
export JAVA_HOME="$HOME/.jdks/jbr-21.0.11"
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
```

On a Mac with Swift and Xcode, also run:

```bash
bun run test:ios:lifecycle
bun run verify:ios
```

The lifecycle command runs Foundation-only policy and diagnostics tests. `verify:ios` compiles the plugin against Apple and Capacitor frameworks. Neither substitutes for device testing.

## 2. Build an app with this working tree

For Silla, follow [Building Silla with the Local Wi-Fi Fork](../../silla-local-build-guide.md). Point Silla's dependency at this checkout, refresh its installed dependency, synchronize the native project, and build the app. Confirm the generated native dependency actually references this fork and the working-tree changes.

Use a **physical phone** for Wi-Fi association and routing checks. A simulator cannot validate those behaviors. A local Capacitor app that embeds this checkout can be used for diagnostics API checks, but Silla is needed for charger-service validation.

Record:

- Plugin Git commit and whether uncommitted changes were included.
- Silla app/build identifier and native plugin dependency location.
- Device model and OS version; charger model/firmware if available.
- Whether location permission/services are enabled and whether cellular is enabled.

The plugin version string remains `8.5.5`, so it does not identify this fork revision by itself.

## 3. Capture a successful attempt

Temporarily add diagnostics capture to Silla's connection flow, or call it from an app debug action:

```typescript
try {
  await CapacitorWifi.connect({
    ssid: chargerSsid,
    password: chargerPassword,
    autoRouteTraffic: true, // Android only
  });
} catch (error) {
  console.error('Wi-Fi connect failed', error);
}

const report = await CapacitorWifi.getConnectionDiagnostics();
console.log(JSON.stringify(report, null, 2));
```

Do not log credentials. The snapshot keeps only the latest accepted attempt, so retrieve it before starting another connection attempt.

For an S3 Android integration check, connect to the charger's **offline** access point with cellular enabled and `autoRouteTraffic: true`. In Silla, verify local health/login and sustained WebSocket traffic separately; the plugin snapshot cannot establish service readiness. On iOS, connect to the same offline access point and capture the report after connect resolves.

### Expected successful-attempt observations

- The report has an attempt ID, `outcome: "connected"`, and an event history in increasing `sequence` order.
- Events for the attempt have a consistent `attemptId`.
- `verificationStarted` identifies the beginning of SSID verification; it is not itself an SSID result.
- `associationConfirmed` appears once when the requested SSID is observed, before `connectionSucceeded`.
- `connectionTimedOut` and `permissionDenied` distinguish those terminal failures from generic `connectionFailed`; inspect the `stage` and stable `code` in attempt state for detail.
- Android records a `bindingResult` when process routing was requested. iOS reports routing as `notApplicable`.
- IP readiness is a classification (`unknown`, `missing`, `linkLocalOnly`, `ipv4Assigned`, or `ipv6Only`), not an address. It does not prove a working route or charger service.
- On Android, observations refer to the requested network. On iOS, refreshed IP information is attributed to the target only if the current SSID matches.

## 4. Check disconnect and retained history

After saving the connected report, disconnect through Silla and retrieve diagnostics again:

```typescript
await CapacitorWifi.disconnect();
const afterDisconnect = await CapacitorWifi.getConnectionDiagnostics();
console.log(JSON.stringify(afterDisconnect, null, 2));
```

Check for a cleanup event. The outcome describes the historical `connect()` result and can remain `connected` after disconnect. Android includes `cleanupSucceeded` when it can report resource-release status. iOS does not claim confirmed configuration removal because Apple's removal API has no completion result.

## 5. Exercise a failure and late completion

Use a test network, unavailable AP, or short timeout. Capture diagnostics in the error path:

```typescript
try {
  await CapacitorWifi.connect({
    ssid: testSsid,
    password: testPassword,
    timeoutMs: 5000,
    requestTimeoutMs: 5000, // iOS only
  });
} catch (error) {
  const report = await CapacitorWifi.getConnectionDiagnostics();
  console.log('connect error', error);
  console.log('diagnostics', JSON.stringify(report, null, 2));
}
```

Check that the report outcome is `failed` or `cancelled`, and that the stage and stable error code describe the failure. Timeouts should include a `connectionTimedOut` event; permission failures should include `permissionDenied`; cancellations should include `connectionCancelled`. Other failures use `connectionFailed`. If a native callback arrives after timeout or disconnect, confirm it does not replace the already-recorded terminal outcome or add a second success/failure completion event. The plugin cannot dismiss an iOS system consent prompt; report late OS behavior separately.

Avoid deliberately testing against production credentials or recording a real charger password. A wrong test password can be used only on a controlled test AP.

## 6. Check privacy and bounded history

Inspect the full returned report and any app logs before sharing them. Confirm they do not contain:

- SSIDs or BSSIDs.
- Actual IP addresses.
- Passwords, tokens, or other credentials.
- Raw localized native error messages.

The event history is in-memory and bounded to 64 events. When older events are evicted, `droppedEventCount` should reflect the discarded history. Reports are not persisted by the plugin.

## 7. Report results

For each platform, report:

1. Build identity (commit plus dirty working-tree changes), app build, device model, and OS version.
2. Whether connect succeeded or failed, and whether charger-local HTTP/WebSocket operations worked.
3. A **redacted** diagnostics snapshot for success and failure, if both were tested.
4. What happened on disconnect and whether normal cloud traffic recovered.
5. Any unexpected event order, duplicate/overwritten outcome, wrong IP classification, build/test failure, or privacy leak.

Do not label `ipv4Assigned` as proof of charger readiness; include the app's actual local service results separately.
