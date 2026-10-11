import XCTest
@testable import CapacitorWifiPlugin

final class WifiConnectionDiagnosticsTests: XCTestCase {
    private func makeDiagnostics() -> WifiConnectionDiagnostics {
        WifiConnectionDiagnostics(id: 7, timestamp: 1000, elapsedClock: 100)
    }

    func testHistoryIsBoundedAndTruncationPreservesSequenceOrder() {
        let diagnostic = makeDiagnostics()
        for index in 0..<100 {
            diagnostic.record(.ssidVerification, stage: "verification", timestamp: Double(1001 + index),
                              elapsedClock: Double(101 + index))
        }
        let snapshot = diagnostic.snapshot(elapsedClock: 300)
        let events = snapshot["events"] as! [[String: Any]]
        XCTAssertEqual(events.count, 64)
        XCTAssertEqual(snapshot["droppedEventCount"] as? Int, 37)
        XCTAssertEqual(events.first?["sequence"] as? Int, 38)
        XCTAssertEqual(events.last?["sequence"] as? Int, 101)
    }

    func testReturnedSnapshotsDoNotMutateStoredHistoryAndExcludeIdentifiers() {
        let diagnostic = makeDiagnostics()
        diagnostic.fail(code: "CONNECTION_TIMEOUT", stage: "verification", nativeCode: 42,
                        nativeDomain: "other", cancelled: false, timestamp: 1100, elapsedClock: 200)
        var snapshot = diagnostic.snapshot(elapsedClock: 200)
        snapshot["events"] = []
        snapshot["attempt"] = [:]
        let fresh = diagnostic.snapshot(elapsedClock: 200)
        XCTAssertEqual((fresh["events"] as? [[String: Any]])?.count, 2)
        let attempt = fresh["attempt"] as! [String: Any]
        XCTAssertNil(attempt["ssid"])
        XCTAssertNil(attempt["ipAddress"])
        let error = attempt["error"] as! [String: Any]
        XCTAssertEqual(error["nativeCode"] as? String, "42")
        XCTAssertNil(error["nativeMessage"])
    }

    func testNewObservationInvalidatesOlderLookupRevisionEvenWhenClassificationUnchanged() {
        let diagnostic = makeDiagnostics()
        diagnostic.observeIp(.ipv4Assigned, timestamp: 1100, elapsedClock: 200)
        let earlierRevision = diagnostic.revision
        diagnostic.observeIp(.ipv4Assigned, timestamp: 1200, elapsedClock: 300)
        XCTAssertNotEqual(earlierRevision, diagnostic.revision)
    }

    func testCleanupKeepsHistoricalOutcomeWithoutInventingConfirmedRemoval() {
        let diagnostic = makeDiagnostics()
        diagnostic.succeed(timestamp: 900, elapsedClock: 200)
        diagnostic.cleanup(timestamp: 800, elapsedClock: 300)
        let attempt = diagnostic.snapshot(elapsedClock: 400)["attempt"] as! [String: Any]
        XCTAssertEqual(attempt["outcome"] as? String, "connected")
        XCTAssertEqual(attempt["elapsedMs"] as? Double, 300)
        XCTAssertEqual(attempt["routing"] as? String, "notApplicable")
        XCTAssertNil(attempt["cleanupSucceeded"])
    }

    func testEmptyAndUnavailableMetadataAreDifferent() {
        XCTAssertEqual(WifiConnectionDiagnostics.classifyAddresses(nil), .unknown)
        XCTAssertEqual(WifiConnectionDiagnostics.classifyAddresses([]), .missing)
    }

    func testSelfAssignedAndLinkLocalOnlyAddressesDoNotProveIpv4Readiness() {
        XCTAssertEqual(WifiConnectionDiagnostics.classifyAddresses(["169.254.8.2", "fe80::1%en0"]), .linkLocalOnly)
        XCTAssertEqual(WifiConnectionDiagnostics.classifyAddresses(["169.254.8.2", "fd00::2"]), .ipv6Only)
    }

    func testEligibleIpv4TakesPrecedenceOverEarlyIpv6() {
        XCTAssertEqual(WifiConnectionDiagnostics.classifyAddresses(["fe80::1", "fd00::2", "192.168.8.2"]), .ipv4Assigned)
        XCTAssertEqual(WifiConnectionDiagnostics.classifyAddresses(["::ffff:192.168.8.2"]), .ipv4Assigned)
    }

    func testInvalidLoopbackUnspecifiedMulticastAndBroadcastAreNotReady() {
        XCTAssertEqual(WifiConnectionDiagnostics.classifyAddresses(["invalid", "0.0.0.0", "127.0.0.1", "::", "::1", "224.0.0.1", "ff02::1", "255.255.255.255"]), .missing)
    }
}
