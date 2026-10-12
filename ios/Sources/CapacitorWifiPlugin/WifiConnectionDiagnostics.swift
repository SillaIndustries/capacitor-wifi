import Foundation
#if canImport(Darwin)
import Darwin
#else
import Glibc
#endif

/// Identifier-free, bounded attempt history. The native adapter confines access to the main queue.
final class WifiConnectionDiagnostics {
    static let eventLimit = 64

    enum EventType: String {
        case attemptStarted
        case permissionRequested
        case requestStarted
        case requestCompleted
        case networkAvailable
        case associationConfirmed
        case verificationStarted
        case capabilitiesChanged
        case ssidVerification
        case bindingResult
        case ipReadiness
        case connectionSucceeded
        case connectionFailed
        case connectionTimedOut
        case permissionDenied
        case connectionCancelled
        case networkLost
        case cleanup
    }

    enum IpReadiness: String {
        case unknown
        case missing
        case linkLocalOnly
        case ipv4Assigned
        case ipv6Only
    }

    let id: Int
    private let startedAt: Double
    private let startedElapsed: Double
    private var updatedAt: Double
    private(set) var revision = 0
    private var sequence = 0
    private var outcome = "pending"
    private var stage = "validation"
    private var ssidVerification: WifiConnectionLifecycle.Verification = .unavailable
    private var ipReadiness: IpReadiness = .unknown
    private var ipObservedAt: Double?
    private var ssidObservedAt: Double?
    private var error: [String: Any]?
    private var associationConfirmed = false
    private var events: [[String: Any]] = []

    init(id: Int, timestamp: Double, elapsedClock: Double) {
        self.id = id
        startedAt = timestamp
        updatedAt = timestamp
        startedElapsed = elapsedClock
        record(.attemptStarted, stage: "validation", timestamp: timestamp, elapsedClock: elapsedClock)
    }

    func record(_ type: EventType, stage: String, timestamp: Double, elapsedClock: Double) {
        self.stage = stage
        updatedAt = timestamp
        revision += 1
        sequence += 1
        var event = state(elapsedClock: elapsedClock)
        event.removeValue(forKey: "id")
        event.removeValue(forKey: "startedAt")
        event.removeValue(forKey: "updatedAt")
        event["attemptId"] = id
        event["sequence"] = sequence
        event["timestamp"] = timestamp
        event["type"] = type.rawValue
        events.append(event)
        if events.count > Self.eventLimit {
            events.removeFirst()
        }
    }

    func observeSsid(_ value: WifiConnectionLifecycle.Verification, timestamp: Double, elapsedClock: Double) {
        let changed = ssidObservedAt == nil || ssidVerification != value
        ssidVerification = value
        ssidObservedAt = timestamp
        updatedAt = timestamp
        revision += 1
        if changed {
            record(.ssidVerification, stage: stage, timestamp: timestamp, elapsedClock: elapsedClock)
        }
    }

    func observeIp(_ value: IpReadiness, timestamp: Double, elapsedClock: Double) {
        let changed = ipObservedAt == nil || ipReadiness != value
        ipReadiness = value
        ipObservedAt = timestamp
        updatedAt = timestamp
        revision += 1
        if changed {
            record(.ipReadiness, stage: stage, timestamp: timestamp, elapsedClock: elapsedClock)
        }
    }

    func succeed(timestamp: Double, elapsedClock: Double) {
        guard outcome == "pending" else { return }
        outcome = "connected"
        record(.connectionSucceeded, stage: "connected", timestamp: timestamp, elapsedClock: elapsedClock)
    }

    func associationConfirmed(timestamp: Double, elapsedClock: Double) {
        guard !associationConfirmed, outcome == "pending" else { return }
        associationConfirmed = true
        record(.associationConfirmed, stage: "association", timestamp: timestamp, elapsedClock: elapsedClock)
    }

    func fail(code: String, stage: String, nativeCode: Int?, nativeDomain: String?, cancelled: Bool,
              timestamp: Double, elapsedClock: Double) {
        guard outcome == "pending" else { return }
        outcome = cancelled ? "cancelled" : "failed"
        var details: [String: Any] = ["code": code, "stage": stage]
        if let nativeCode = nativeCode { details["nativeCode"] = String(nativeCode) }
        if let nativeDomain = nativeDomain { details["nativeDomain"] = nativeDomain }
        error = details
        let event: EventType
        if cancelled {
            event = .connectionCancelled
        } else if code == "CONNECTION_TIMEOUT" {
            event = .connectionTimedOut
        } else if code == "PERMISSION_DENIED" {
            event = .permissionDenied
        } else {
            event = .connectionFailed
        }
        record(event, stage: stage, timestamp: timestamp, elapsedClock: elapsedClock)
    }

    func cleanup(timestamp: Double, elapsedClock: Double) {
        // Apple's removal API has no completion result: do not claim confirmed cleanup.
        observeIp(.unknown, timestamp: timestamp, elapsedClock: elapsedClock)
        record(.cleanup, stage: "cleanup", timestamp: timestamp, elapsedClock: elapsedClock)
    }

    func snapshot(elapsedClock: Double) -> [String: Any] {
        let copiedEvents = events.map { event -> [String: Any] in
            var copy = event
            if let error = event["error"] as? [String: Any] {
                copy["error"] = error
            }
            return copy
        }
        return ["attempt": state(elapsedClock: elapsedClock), "events": copiedEvents,
         "droppedEventCount": max(0, sequence - events.count)]
    }

    private func state(elapsedClock: Double) -> [String: Any] {
        var result: [String: Any] = [
            "id": id, "startedAt": startedAt, "updatedAt": updatedAt,
            "elapsedMs": max(0, elapsedClock - startedElapsed),
            "outcome": outcome, "stage": stage, "routing": "notApplicable",
            "ssidVerification": ssidVerification.rawValue, "ipReadiness": ipReadiness.rawValue
        ]
        if let ipObservedAt = ipObservedAt { result["ipObservedAt"] = ipObservedAt }
        if let ssidObservedAt = ssidObservedAt { result["ssidObservedAt"] = ssidObservedAt }
        if let error = error { result["error"] = error }
        return result
    }

    static func classifyAddresses(_ addresses: [String]?) -> IpReadiness {
        guard let addresses = addresses else { return .unknown }
        var linkLocal = false
        var usableIpv6 = false
        for address in addresses {
            let numeric = String(address.split(separator: "%", maxSplits: 1).first ?? "")
            var ipv4 = in_addr()
            if numeric.withCString({ inet_pton(AF_INET, $0, &ipv4) }) == 1 {
                let bytes = withUnsafeBytes(of: ipv4) { Array($0) }
                let kind = classifyIpv4(bytes)
                if kind == .ipv4Assigned { return kind }
                linkLocal = linkLocal || kind == .linkLocalOnly
                continue
            }
            var ipv6 = in6_addr()
            guard numeric.withCString({ inet_pton(AF_INET6, $0, &ipv6) }) == 1 else { continue }
            let bytes = withUnsafeBytes(of: ipv6) { Array($0) }
            if bytes.prefix(10).allSatisfy({ $0 == 0 }) && bytes[10] == 255 && bytes[11] == 255 {
                let kind = classifyIpv4(Array(bytes.suffix(4)))
                if kind == .ipv4Assigned { return kind }
                linkLocal = linkLocal || kind == .linkLocalOnly
            } else if bytes.allSatisfy({ $0 == 0 }) ||
                        (bytes.prefix(15).allSatisfy({ $0 == 0 }) && bytes[15] == 1) || bytes[0] == 255 {
                continue
            } else if bytes[0] == 254 && bytes[1] & 192 == 128 {
                linkLocal = true
            } else {
                usableIpv6 = true
            }
        }
        return usableIpv6 ? .ipv6Only : linkLocal ? .linkLocalOnly : .missing
    }

    private static func classifyIpv4(_ bytes: [UInt8]) -> IpReadiness {
        guard bytes[0] != 0, bytes[0] != 127, bytes[0] < 224 else { return .missing }
        return bytes[0] == 169 && bytes[1] == 254 ? .linkLocalOnly : .ipv4Assigned
    }
}
