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
        var events = snapshot["events"] as! [[String: Any]]
        var failureEvent = events[1]
        var eventError = failureEvent["error"] as! [String: Any]
        eventError["code"] = "MUTATED"
        failureEvent["error"] = eventError
        events[1] = failureEvent
        events.removeAll()
        snapshot["events"] = events
        var attemptState = snapshot["attempt"] as! [String: Any]
        var attemptError = attemptState["error"] as! [String: Any]
        attemptError["code"] = "MUTATED"
        attemptState["error"] = attemptError
        snapshot["attempt"] = attemptState

        let fresh = diagnostic.snapshot(elapsedClock: 200)
        XCTAssertEqual((fresh["events"] as? [[String: Any]])?.count, 2)
        let attempt = fresh["attempt"] as! [String: Any]
        XCTAssertNil(attempt["ssid"])
        XCTAssertNil(attempt["ipAddress"])
        let error = attempt["error"] as! [String: Any]
        XCTAssertEqual(error["nativeCode"] as? String, "42")
        XCTAssertNil(error["nativeMessage"])
        XCTAssertEqual(((fresh["events"] as? [[String: Any]])?[1]["error"] as? [String: Any])?["code"] as? String,
                       "CONNECTION_TIMEOUT")
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

    func testTerminalOutcomeCannotBeOverwrittenByLateCallbacks() {
        let diagnostic = makeDiagnostics()
        diagnostic.fail(code: "CONNECTION_TIMEOUT", stage: "verification", nativeCode: nil,
                        nativeDomain: nil, cancelled: false, timestamp: 1100, elapsedClock: 200)
        diagnostic.succeed(timestamp: 1200, elapsedClock: 300)
        diagnostic.fail(code: "CONNECTION_FAILED", stage: "cleanup", nativeCode: nil,
                        nativeDomain: nil, cancelled: false, timestamp: 1300, elapsedClock: 400)

        let snapshot = diagnostic.snapshot(elapsedClock: 400)
        let attempt = snapshot["attempt"] as! [String: Any]
        XCTAssertEqual(attempt["outcome"] as? String, "failed")
        XCTAssertEqual((attempt["error"] as? [String: Any])?["code"] as? String, "CONNECTION_TIMEOUT")
        XCTAssertEqual((snapshot["events"] as? [[String: Any]])?.count, 2)
    }

    func testAssociationConfirmationIsRecordedOnceAndBeforeSuccess() {
        let diagnostic = makeDiagnostics()
        diagnostic.associationConfirmed(timestamp: 1100, elapsedClock: 200)
        diagnostic.associationConfirmed(timestamp: 1200, elapsedClock: 300)
        diagnostic.succeed(timestamp: 1300, elapsedClock: 400)

        let events = diagnostic.snapshot(elapsedClock: 400)["events"] as! [[String: Any]]
        XCTAssertEqual(events.count, 3)
        XCTAssertEqual(events[1]["type"] as? String, "associationConfirmed")
        XCTAssertEqual(events[2]["type"] as? String, "connectionSucceeded")
    }

    func testVerificationStartIsDistinctFromAnSsidObservation() {
        let diagnostic = makeDiagnostics()
        diagnostic.record(.verificationStarted, stage: "verification", timestamp: 1100, elapsedClock: 200)

        let snapshot = diagnostic.snapshot(elapsedClock: 200)
        let attempt = snapshot["attempt"] as! [String: Any]
        let events = snapshot["events"] as! [[String: Any]]
        XCTAssertEqual(attempt["ssidVerification"] as? String, "unavailable")
        XCTAssertEqual(events.last?["type"] as? String, "verificationStarted")
        XCTAssertNil(attempt["ssidObservedAt"])
    }

    func testTimeoutAndPermissionFailuresHaveDistinctTerminalEventTypes() {
        let timeout = makeDiagnostics()
        timeout.fail(code: "CONNECTION_TIMEOUT", stage: "verification", nativeCode: nil,
                     nativeDomain: nil, cancelled: false, timestamp: 1100, elapsedClock: 200)
        let timeoutEvents = timeout.snapshot(elapsedClock: 200)["events"] as! [[String: Any]]
        XCTAssertEqual(timeoutEvents.last?["type"] as? String, "connectionTimedOut")

        let permission = WifiConnectionDiagnostics(id: 8, timestamp: 1000, elapsedClock: 100)
        permission.fail(code: "PERMISSION_DENIED", stage: "permission", nativeCode: nil,
                        nativeDomain: nil, cancelled: false, timestamp: 1100, elapsedClock: 200)
        let permissionEvents = permission.snapshot(elapsedClock: 200)["events"] as! [[String: Any]]
        XCTAssertEqual(permissionEvents.last?["type"] as? String, "permissionDenied")
    }

    func testValidationFailureCanBeSnapshottedBeforeNativeRequestStarts() {
        let diagnostic = makeDiagnostics()
        diagnostic.fail(code: "INVALID_CONFIGURATION", stage: "validation", nativeCode: nil,
                        nativeDomain: nil, cancelled: false, timestamp: 1010, elapsedClock: 110)

        let snapshot = diagnostic.snapshot(elapsedClock: 120)
        let attempt = snapshot["attempt"] as! [String: Any]
        let events = snapshot["events"] as! [[String: Any]]
        XCTAssertEqual(attempt["id"] as? Int, 7)
        XCTAssertEqual(attempt["outcome"] as? String, "failed")
        XCTAssertEqual(attempt["stage"] as? String, "validation")
        XCTAssertEqual(events.last?["type"] as? String, "connectionFailed")
    }

    func testCancelledFailureHasItsOwnTerminalEventType() {
        let diagnostic = makeDiagnostics()
        diagnostic.fail(code: "CONNECTION_FAILED", stage: "association", nativeCode: nil,
                        nativeDomain: nil, cancelled: true, timestamp: 1100, elapsedClock: 200)
        let snapshot = diagnostic.snapshot(elapsedClock: 200)
        let attempt = snapshot["attempt"] as! [String: Any]
        let events = snapshot["events"] as! [[String: Any]]
        XCTAssertEqual(attempt["outcome"] as? String, "cancelled")
        XCTAssertEqual(events.last?["type"] as? String, "connectionCancelled")
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
