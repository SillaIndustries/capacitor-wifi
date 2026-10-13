import Foundation

/// Pure lifecycle policy. The native adapter serializes access on the main queue.
final class WifiConnectionLifecycle {
    static func isValidTimeoutMilliseconds(_ value: Double) -> Bool {
        value.isFinite && value > 0 && value <= 600000
    }

    enum Phase: String {
        case request
        case verification
        case finished
    }

    enum Verification: String {
        case unavailable
        case mismatch
        case match
    }

    let startedAt: TimeInterval
    private let verificationSeconds: TimeInterval
    private(set) var phase: Phase = .request
    private(set) var deadline: TimeInterval
    private(set) var verification: Verification = .unavailable

    init(startedAt: TimeInterval, requestSeconds: TimeInterval, verificationSeconds: TimeInterval) {
        self.startedAt = startedAt
        self.verificationSeconds = verificationSeconds
        self.deadline = startedAt + requestSeconds
    }

    func expiredPhase(at now: TimeInterval) -> Phase? {
        phase != .finished && now >= deadline ? phase : nil
    }

    @discardableResult
    func beginVerification(at now: TimeInterval) -> Bool {
        guard phase == .request, expiredPhase(at: now) == nil else { return false }
        phase = .verification
        deadline = now + verificationSeconds
        return true
    }

    func observe(ssid: String?, expected: String, at now: TimeInterval) -> Bool {
        guard phase == .verification, expiredPhase(at: now) == nil else { return false }
        if let ssid = ssid {
            verification = ssid == expected ? .match : .mismatch
        } else {
            verification = .unavailable
        }
        return verification == .match
    }

    @discardableResult
    func finish() -> Bool {
        guard phase != .finished else { return false }
        phase = .finished
        return true
    }
}

/// Version each configuration mutation so late cancellation cleanup cannot undo a newer request.
final class WifiConfigurationOwnership {
    private(set) var sequence = 0
    private var owners: [String: Int] = [:]

    func claim(ssid: String) -> Int {
        sequence += 1
        owners[ssid] = sequence
        return sequence
    }

    func isCurrent(ssid: String, generation: Int) -> Bool {
        owners[ssid] == generation
    }
}
