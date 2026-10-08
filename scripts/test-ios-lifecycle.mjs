import { copyFileSync, mkdirSync, mkdtempSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// Test the actual Foundation-only policy on Linux/macOS without importing iOS frameworks.
// Native integration still requires Xcode and a signed/device build.
const root = fileURLToPath(new URL('..', import.meta.url));
const temporaryDirectory = mkdtempSync(join(tmpdir(), 'capacitor-wifi-ios-lifecycle-'));

try {
  for (const [source, destination] of [
    [
      'ios/Sources/CapacitorWifiPlugin/WifiConnectionLifecycle.swift',
      'Sources/CapacitorWifiPlugin/WifiConnectionLifecycle.swift',
    ],
    [
      'ios/Tests/CapacitorWifiPluginTests/WifiConnectionLifecycleTests.swift',
      'Tests/CapacitorWifiPluginTests/WifiConnectionLifecycleTests.swift',
    ],
  ]) {
    const target = join(temporaryDirectory, destination);
    mkdirSync(dirname(target), { recursive: true });
    copyFileSync(join(root, source), target);
  }
  writeFileSync(
    join(temporaryDirectory, 'Package.swift'),
    `// swift-tools-version: 5.9
import PackageDescription
let package = Package(
    name: "WifiLifecyclePolicyTests",
    targets: [
        .target(name: "CapacitorWifiPlugin"),
        .testTarget(name: "CapacitorWifiPluginTests", dependencies: ["CapacitorWifiPlugin"])
    ]
)
`,
  );
  const result = spawnSync('swift', ['test', '--package-path', temporaryDirectory], {
    stdio: 'inherit',
    timeout: 600000,
  });
  if (result.error) {
    throw result.error;
  }
  process.exitCode = result.status ?? 1;
} finally {
  rmSync(temporaryDirectory, { recursive: true, force: true });
}
