import XCTest
@testable import CapacitorWifiPlugin

final class WifiConnectionLifecycleTests: XCTestCase {
    private func makeAttempt() -> WifiConnectionLifecycle {
        WifiConnectionLifecycle(startedAt: 100, requestSeconds: 120, verificationSeconds: 30)
    }

    func testTimeoutLimitsRejectNonFiniteAndOutOfRangeValues() {
        for value in [0, -1, Double.infinity, Double.nan, 600001] {
            XCTAssertFalse(WifiConnectionLifecycle.isValidTimeoutMilliseconds(value))
        }
        XCTAssertTrue(WifiConnectionLifecycle.isValidTimeoutMilliseconds(0.5))
        XCTAssertTrue(WifiConnectionLifecycle.isValidTimeoutMilliseconds(600000))
    }

    func testSSIDCannotConfirmBeforeRequestCompletion() {
        let attempt = makeAttempt()
        XCTAssertFalse(attempt.observe(ssid: "charger", expected: "charger", at: 110))
        XCTAssertEqual(attempt.phase, .request)
    }

    func testDelayedConsentGetsFullVerificationBudget() {
        let attempt = makeAttempt()
        XCTAssertTrue(attempt.beginVerification(at: 160))
        XCTAssertEqual(attempt.deadline, 190)
        XCTAssertNil(attempt.expiredPhase(at: 189))
        XCTAssertEqual(attempt.expiredPhase(at: 190), .verification)
    }

    func testRequestExpiresIndependentlyOfNativeCompletion() {
        let attempt = makeAttempt()
        XCTAssertEqual(attempt.expiredPhase(at: 220), .request)
        XCTAssertFalse(attempt.beginVerification(at: 220))
        XCTAssertTrue(attempt.finish())
        XCTAssertFalse(attempt.beginVerification(at: 230))
    }

    func testVerificationDeadlineStillAppliesWithoutSSIDCompletion() {
        let attempt = makeAttempt()
        attempt.beginVerification(at: 110)
        XCTAssertEqual(attempt.expiredPhase(at: 140), .verification)
        XCTAssertFalse(attempt.observe(ssid: "charger", expected: "charger", at: 140))
    }

    func testLateSSIDAfterCancellationCannotSucceed() {
        let attempt = makeAttempt()
        attempt.beginVerification(at: 110)
        XCTAssertTrue(attempt.finish())
        XCTAssertFalse(attempt.observe(ssid: "charger", expected: "charger", at: 120))
        XCTAssertFalse(attempt.finish())
        XCTAssertNil(attempt.expiredPhase(at: 999))
    }

    func testCompletionSettlesOnceAndBlocksOldRequestCallbacks() {
        let attempt = makeAttempt()
        XCTAssertTrue(attempt.finish())
        XCTAssertFalse(attempt.finish())
        XCTAssertFalse(attempt.beginVerification(at: 110))
    }

    func testUnreadableSSIDIsDistinctFromKnownMismatch() {
        let attempt = makeAttempt()
        attempt.beginVerification(at: 110)
        XCTAssertFalse(attempt.observe(ssid: nil, expected: "charger", at: 111))
        XCTAssertEqual(attempt.verification, .unavailable)
        XCTAssertFalse(attempt.observe(ssid: "home", expected: "charger", at: 112))
        XCTAssertEqual(attempt.verification, .mismatch)
        XCTAssertTrue(attempt.observe(ssid: "charger", expected: "charger", at: 113))
        XCTAssertEqual(attempt.verification, .match)
    }

    func testVerificationCannotStartTwiceOrExtendItsDeadline() {
        let attempt = makeAttempt()
        XCTAssertTrue(attempt.beginVerification(at: 110))
        XCTAssertFalse(attempt.beginVerification(at: 130))
        XCTAssertEqual(attempt.deadline, 140)
    }

    func testOldCancelledAttemptCannotRemoveNewSameSSIDConfiguration() {
        let ownership = WifiConfigurationOwnership()
        _ = ownership.claim(ssid: "charger")
        let disconnect = ownership.claim(ssid: "charger")
        XCTAssertTrue(ownership.isCurrent(ssid: "charger", generation: disconnect))
        let replacement = ownership.claim(ssid: "charger")
        XCTAssertFalse(ownership.isCurrent(ssid: "charger", generation: disconnect))
        XCTAssertTrue(ownership.isCurrent(ssid: "charger", generation: replacement))
    }

    func testCleanupStillAllowedWhenNewRequestTargetsDifferentSSID() {
        let ownership = WifiConfigurationOwnership()
        let disconnect = ownership.claim(ssid: "old-charger")
        _ = ownership.claim(ssid: "new-charger")
        XCTAssertTrue(ownership.isCurrent(ssid: "old-charger", generation: disconnect))
    }

    func testConfigurationMutationsInvalidateDelayedDisconnectLookup() {
        let ownership = WifiConfigurationOwnership()
        let lookupSequence = ownership.sequence
        _ = ownership.claim(ssid: "charger")
        XCTAssertNotEqual(lookupSequence, ownership.sequence)
    }
}
