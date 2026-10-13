# Building Silla with the Local Wi-Fi Fork

This guide explains how to compile and run Silla with this fork before testing the Android and iOS connection improvements together.

## Understand the Two Build Steps

1. **Build the plugin:** generates its JavaScript bundle and API documentation.
2. **Build Silla's native app:** compiles the plugin's Swift or Java code into the application.

You do not need to produce a separate plugin binary before building Silla. Its native build compiles the plugin source.

The initial Android native build and plugin JavaScript build passed during development. iOS lifecycle policy tests and syntax checks also passed on Linux, but these do not replace compiling the full iOS plugin with Xcode.

## Prerequisites

- A Mac with Xcode and its command-line tools available for the iOS build.
- Bun installed for this plugin's dependency installation and build commands.
- The Silla app repository and this fork available locally.
- Silla's existing dependency-install workflow available.
- A physical iPhone, with appropriate signing and provisioning for Silla.

The examples below assume adjacent directories:

```text
parent-directory/
├── capacitor-wifi/   # This fork
└── silla/            # The Silla application
```

Adjust paths to match your checkout. Run each command block from the directory named in its instructions.

## 1. Get the Actual Changes onto the Mac

Make sure the Mac's fork checkout includes the implementation changes you intend to test. Cloning or pulling a repository does not include uncommitted work from another machine.

Record the commit and any additional local changes included in the test build. Use the same plugin revision for the eventual Android and iOS testing batch.

## 2. Build the Plugin

From the `capacitor-wifi` directory:

```bash
bun install --frozen-lockfile
bun run build
```

The build generates the JavaScript bundles and regenerates the root README API documentation from `src/definitions.ts`.

On the Mac, also run the standalone iOS verification:

```bash
bun run verify:ios
```

This attempts an Xcode build of the plugin. It does not prove the Silla application has the correct dependency installed or that charger connectivity works.

Optional lifecycle policy tests, with Swift available:

```bash
bun run test:ios:lifecycle
```

These test timing and configuration-ownership rules in isolation, rather than actual Wi-Fi association.

## 3. Point Silla at the Local Fork

In Silla's `package.json`, replace the registry version of the plugin with a local dependency. With the example directory layout:

```json
{
  "dependencies": {
    "@capgo/capacitor-wifi": "file:../capacitor-wifi"
  }
}
```

This is a fragment, not a replacement for Silla's entire package file. Keep its other dependencies.

The package name remains `@capgo/capacitor-wifi`; only the dependency source changes.

From the Silla directory, run its existing dependency-install workflow so the lockfile and installed package reflect the local reference. Confirm the installed package contains this fork's changed native source. After subsequent plugin edits, rebuild the plugin and refresh Silla's installed dependency as needed; local dependency copying/linking behavior depends on the package manager.

## 4. Build Silla's Web Assets and Synchronize iOS

From the Silla directory, run Silla's normal application build command to produce the web assets expected by its Capacitor configuration. The plugin's `bun run build` does not build the Silla application.

Then run:

```bash
bunx cap sync ios
bunx cap open ios
```

Synchronization updates native dependencies and copies the application's built web assets. Opening iOS launches the native project in Xcode.

The existing Silla audit indicates that the app uses **Swift Package Manager**. Inspect its generated native package references and confirm they point to the installed fork, rather than the old pinned package directory. Let Capacitor synchronization regenerate references before making manual changes.

## 5. Compile and Run in Xcode

In Xcode:

1. Select Silla's app scheme.
2. Select the connected physical iPhone as the destination.
3. Confirm the signing team and provisioning are valid.
4. Build with **Command-B**.
5. Once compilation succeeds, run with **Command-R**.

If compilation fails, capture the first relevant compiler error and its file/line. Resolve build failures before beginning the charger-testing batch.

Use a **physical iPhone** for Wi-Fi association testing. A simulator cannot validate the charger's Wi-Fi connection behavior.

## Confirm Which Implementation Was Built

The fork currently retains the upstream version string **8.5.5**. Calling `getPluginVersion()` alone therefore cannot distinguish this implementation from upstream 8.5.5.

Before the shared testing batch:

- Verify the installed dependency and native package reference point to this fork.
- Record the plugin revision/local changes and the Silla build identifier.
- Give the test build a clear revision identifier in its build notes or diagnostics.
- Confirm the native connection logs include the attempt-level events introduced by the fork when exercising a connection.

## Android Build

The same local dependency setup applies to Android. Rebuild the plugin, refresh Silla's installed dependency, and build Silla's web assets before synchronization.

From the Silla directory:

```bash
bunx cap sync android
bunx cap open android
```

Build and run Silla through its normal Android Studio workflow using **Java 21** and a physical test phone.

Standalone Android plugin verification, from the fork directory with Java 21 and the Android SDK configured:

```bash
bun run verify:android
```

## Combined Device Review

Once both native application builds succeed, follow the [combined M1/M2 device review](features/silla-offline-connectivity/verification.md#combined-m1m2-device-review).

Device tests are intentionally batched by owner agreement. Compilation and unit-test success do not constitute milestone acceptance; record the Android/iPhone outcomes and any remaining evidence gaps in the feature documentation.
