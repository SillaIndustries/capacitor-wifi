import Foundation
import Capacitor
import NetworkExtension
import CoreLocation

@objc(CapacitorWifiPlugin)
public class CapacitorWifiPlugin: CAPPlugin, CAPBridgedPlugin, CLLocationManagerDelegate {
    private let pluginVersion: String = "8.5.5"
    public let identifier = "CapacitorWifiPlugin"
    public let jsName = "CapacitorWifi"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "addNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "connect", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "disconnect", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getAvailableNetworks", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getIpAddress", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getRssi", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getSsid", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getWifiInfo", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isEnabled", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startScan", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "checkPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "isNetworkSaved", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "shareNetwork", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getPluginVersion", returnType: CAPPluginReturnPromise)
    ]

    private var hotspotManager: NEHotspotConfigurationManager?
    private var locationManager: CLLocationManager?
    private var permissionCalls: [CAPPluginCall] = []
    // Connection state, native completions, watchdogs, and cleanup use the main queue.
    private var activeConnectAttempt: ConnectAttempt?
    private var pendingApplications: [Int: PendingApplication] = [:]
    private let configurationOwnership = WifiConfigurationOwnership()
    private var lastRequestedSSID: String?
    private let defaultConnectTimeoutMs: Double = 30000
    private let defaultRequestTimeoutMs: Double = 120000
    private let ssidVerifyPollSeconds: TimeInterval = 0.5

    private final class ConnectAttempt {
        var call: CAPPluginCall?
        let ssid: String
        let generation: Int
        let lifecycle: WifiConnectionLifecycle
        var watchdog: DispatchWorkItem?
        var poll: DispatchWorkItem?
        var removeWhenAppliedGeneration: Int?

        init(call: CAPPluginCall, ssid: String, generation: Int, lifecycle: WifiConnectionLifecycle) {
            self.call = call
            self.ssid = ssid
            self.generation = generation
            self.lifecycle = lifecycle
        }
    }

    private final class PendingApplication {
        weak var attempt: ConnectAttempt?

        init(_ attempt: ConnectAttempt) {
            self.attempt = attempt
        }
    }

    override public func load() {
        hotspotManager = NEHotspotConfigurationManager.shared
        locationManager = CLLocationManager()
        locationManager?.delegate = self
    }

    @objc func addNetwork(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.addNetworkOnMain(call)
        }
    }

    private func addNetworkOnMain(_ call: CAPPluginCall) {
        guard let ssid = call.getString("ssid") else {
            call.reject("SSID is required")
            return
        }

        let password = call.getString("password")

        let configuration: NEHotspotConfiguration
        if let password = password, !password.isEmpty {
            configuration = NEHotspotConfiguration(ssid: ssid, passphrase: password, isWEP: false)
        } else {
            configuration = NEHotspotConfiguration(ssid: ssid)
        }

        configuration.joinOnce = false

        // Protect a new addNetwork request from cleanup of an older cancelled connect.
        _ = configurationOwnership.claim(ssid: ssid)

        hotspotManager?.apply(configuration) { error in
            if let error = error {
                call.reject("Failed to add network: \(error.localizedDescription)", nil, error)
            } else {
                call.resolve()
            }
        }
    }

    @objc func connect(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.connectOnMain(call)
        }
    }

    private func connectOnMain(_ call: CAPPluginCall) {
        if activeConnectAttempt != nil {
            rejectConnect(
                call,
                code: "CONNECTION_IN_PROGRESS",
                message: "Another Wi-Fi connection attempt is already in progress.",
                stage: "request",
                nativeCode: nil,
                nativeMessage: nil,
                error: nil
            )
            return
        }
        guard let ssid = call.getString("ssid"), !ssid.isEmpty else {
            rejectConnect(
                call,
                code: "INVALID_CONFIGURATION",
                message: "SSID is required.",
                stage: "validation",
                nativeCode: nil,
                nativeMessage: nil,
                error: nil
            )
            return
        }

        guard let timeoutMs = resolveTimeoutMs(call, option: "timeoutMs", defaultValue: defaultConnectTimeoutMs),
              let requestTimeoutMs = resolveTimeoutMs(call, option: "requestTimeoutMs", defaultValue: defaultRequestTimeoutMs) else {
            rejectConnect(
                call,
                code: "INVALID_CONFIGURATION",
                message: "timeoutMs and requestTimeoutMs must be finite positive numbers no greater than 600000.",
                stage: "validation",
                nativeCode: nil,
                nativeMessage: nil,
                error: nil
            )
            return
        }

        let password = call.getString("password")
        if let password = password, !password.isEmpty, !isValidWpaPassphrase(password) {
            rejectConnect(
                call,
                code: "INVALID_CONFIGURATION",
                message: "Invalid WPA passphrase format.",
                stage: "validation",
                nativeCode: nil,
                nativeMessage: nil,
                error: nil
            )
            return
        }

        let configuration: NEHotspotConfiguration
        if let password = password, !password.isEmpty {
            configuration = NEHotspotConfiguration(ssid: ssid, passphrase: password, isWEP: false)
        } else {
            configuration = NEHotspotConfiguration(ssid: ssid)
        }

        configuration.joinOnce = false
        guard let manager = hotspotManager else {
            rejectConnect(
                call,
                code: "UNKNOWN",
                message: "Hotspot configuration manager is unavailable.",
                stage: "request",
                nativeCode: nil,
                nativeMessage: nil,
                error: nil
            )
            return
        }

        let generation = configurationOwnership.claim(ssid: ssid)
        let lifecycle = WifiConnectionLifecycle(
            startedAt: ProcessInfo.processInfo.systemUptime,
            requestSeconds: requestTimeoutMs / 1000,
            verificationSeconds: timeoutMs / 1000
        )
        let attempt = ConnectAttempt(call: call, ssid: ssid, generation: generation, lifecycle: lifecycle)
        activeConnectAttempt = attempt
        lastRequestedSSID = ssid
        pendingApplications = pendingApplications.filter { $0.value.attempt != nil }
        pendingApplications[generation] = PendingApplication(attempt)
        logConnect(attempt, event: "request")
        armWatchdog(attempt)

        manager.apply(configuration) { [weak self] error in
            DispatchQueue.main.async {
                guard let self = self else { return }
                self.pendingApplications.removeValue(forKey: generation)
                guard self.activeConnectAttempt === attempt else {
                    self.removeCancelledConfigurationIfNeeded(attempt)
                    return
                }
                guard !self.expireIfNeeded(attempt) else { return }
                if let error = error as NSError?,
                   !(error.domain == NEHotspotConfigurationErrorDomain &&
                     error.code == NEHotspotConfigurationError.alreadyAssociated.rawValue) {
                    let mapped = self.mapHotspotConfigurationError(error)
                    self.completeConnect(
                        attempt, code: mapped.code, message: mapped.message, stage: mapped.stage,
                        error: error
                    )
                    return
                }
                guard attempt.lifecycle.beginVerification(at: ProcessInfo.processInfo.systemUptime) else { return }
                self.logConnect(attempt, event: "verification-start")
                self.armWatchdog(attempt)
                self.verifyConnectedSsid(attempt)
            }
        }
    }

    @objc func disconnect(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            self.disconnectOnMain(call)
        }
    }

    private func disconnectOnMain(_ call: CAPPluginCall) {
        let target = call.getString("ssid") ?? activeConnectAttempt?.ssid ?? lastRequestedSSID
        if let attempt = activeConnectAttempt {
            completeConnect(
                attempt, code: "CONNECTION_FAILED",
                message: "Connection cancelled because disconnect() was called.", stage: attempt.lifecycle.phase.rawValue
            )
        }
        if let target = target {
            removeConfigurationForDisconnect(target)
            call.resolve()
            return
        }

        // Preserve the current-network fallback after app restart, but bound the lookup and
        // never let its late result remove a configuration requested after this disconnect.
        let sequence = configurationOwnership.sequence
        let lookup = WifiConnectionLifecycle(
            startedAt: ProcessInfo.processInfo.systemUptime,
            requestSeconds: defaultRequestTimeoutMs / 1000, verificationSeconds: 0
        )
        let watchdog = DispatchWorkItem {
            if lookup.finish() {
                call.reject("Timed out reading the current Wi-Fi network for disconnect.", "CONNECTION_TIMEOUT")
            }
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + defaultRequestTimeoutMs / 1000, execute: watchdog)
        NEHotspotNetwork.fetchCurrent { [weak self] network in
            DispatchQueue.main.async {
                if lookup.expiredPhase(at: ProcessInfo.processInfo.systemUptime) != nil {
                    if lookup.finish() {
                        watchdog.cancel()
                        call.reject("Timed out reading the current Wi-Fi network for disconnect.", "CONNECTION_TIMEOUT")
                    }
                    return
                }
                guard lookup.finish() else { return }
                watchdog.cancel()
                if let self = self, self.configurationOwnership.sequence == sequence, let ssid = network?.ssid {
                    self.removeConfigurationForDisconnect(ssid)
                }
                call.resolve()
            }
        }
    }

    @objc func getAvailableNetworks(_ call: CAPPluginCall) {
        call.reject("Not supported on iOS")
    }

    @objc func getIpAddress(_ call: CAPPluginCall) {
        var ifaddr: UnsafeMutablePointer<ifaddrs>?

        guard getifaddrs(&ifaddr) == 0, let firstAddr = ifaddr else {
            call.reject("Failed to get IP address")
            return
        }

        defer { freeifaddrs(ifaddr) }

        var ipv4Address: String?
        var routableIPv6Address: String?
        var linkLocalIPv6Address: String?

        for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next }) {
            let interface = ptr.pointee
            let addrFamily = interface.ifa_addr.pointee.sa_family
            let name = String(cString: interface.ifa_name)

            guard name == "en0",
                  addrFamily == UInt8(AF_INET) || addrFamily == UInt8(AF_INET6) else { continue }

            var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            getnameinfo(interface.ifa_addr, socklen_t(interface.ifa_addr.pointee.sa_len),
                        &hostname, socklen_t(hostname.count),
                        nil, socklen_t(0), NI_NUMERICHOST)
            let resolved = String(cString: hostname)

            if addrFamily == UInt8(AF_INET) {
                ipv4Address = resolved
                break
            } else if resolved.hasPrefix("fe80") {
                if linkLocalIPv6Address == nil { linkLocalIPv6Address = resolved }
            } else {
                if routableIPv6Address == nil { routableIPv6Address = resolved }
            }
        }

        if let address = ipv4Address ?? routableIPv6Address ?? linkLocalIPv6Address {
            call.resolve(["ipAddress": address])
        } else {
            call.reject("No IP address found")
        }
    }

    @objc func getRssi(_ call: CAPPluginCall) {
        call.reject("Not supported on iOS")
    }

    @objc func getSsid(_ call: CAPPluginCall) {
        Task {
            if let ssid = await fetchCurrentNetwork()?.ssid {
                call.resolve(["ssid": ssid])
            } else {
                call.reject("Failed to get SSID")
            }
        }
    }

    @objc func getWifiInfo(_ call: CAPPluginCall) {
        Task {
            guard let network = await fetchCurrentNetwork() else {
                call.reject("Failed to get SSID")
                return
            }

            var result: [String: Any] = [
                "ssid": network.ssid,
                "bssid": network.bssid
            ]

            // Get IP Address
            if let ipAddress = self.getIPAddress() {
                result["ip"] = ipAddress
            } else {
                call.reject("Failed to get IP address")
                return
            }

            // Note: frequency, linkSpeed, and signalStrength are not available on iOS
            // through public APIs, so we only return ssid, bssid, and ip

            call.resolve(result)
        }
    }

    @objc func isEnabled(_ call: CAPPluginCall) {
        call.reject("Not supported on iOS")
    }

    @objc func startScan(_ call: CAPPluginCall) {
        call.reject("Not supported on iOS")
    }

    @objc override public func checkPermissions(_ call: CAPPluginCall) {
        let status = getLocationPermissionStatus()
        call.resolve(["location": status])
    }

    @objc override public func requestPermissions(_ call: CAPPluginCall) {
        DispatchQueue.main.async {
            let status = self.getLocationPermissionStatus()
            if status != "prompt" {
                call.resolve(["location": status])
                return
            }

            self.permissionCalls.append(call)
            if self.permissionCalls.count > 1 {
                return
            }

            self.locationManager?.requestWhenInUseAuthorization()
        }
    }

    @objc func isNetworkSaved(_ call: CAPPluginCall) {
        guard let ssid = call.getString("ssid") else {
            call.reject("SSID is required")
            return
        }

        guard let manager = hotspotManager else {
            call.reject("Hotspot configuration manager is unavailable")
            return
        }

        manager.getConfiguredSSIDs { ssids in
            call.resolve(["isSaved": ssids.contains(ssid)])
        }
    }

    @objc func shareNetwork(_ call: CAPPluginCall) {
        if #available(iOS 26.2, *) {
            #if canImport(AccessorySetupKit) && canImport(WiFiInfrastructure)
            let bluetoothIdentifier = call.getString("bluetoothIdentifier")
            let accessoryIdentifier = call.getString("accessoryIdentifier")
            let requestAuthorization = call.getBool("requestAuthorization") ?? false
            let askToShare = call.getBool("askToShare") ?? true

            Task {
                do {
                    let response = try await WifiNetworkSharingHelper.shareNetwork(
                        WifiNetworkSharingHelper.ShareRequest(
                            bluetoothIdentifier: bluetoothIdentifier,
                            accessoryIdentifier: accessoryIdentifier,
                            requestAuthorization: requestAuthorization,
                            askToShare: askToShare
                        )
                    )

                    var result = JSObject()
                    result["started"] = response.started
                    if let authorizationState = response.authorizationState {
                        result["authorizationState"] = authorizationState
                    }
                    if let askToShareState = response.askToShareState {
                        result["askToShareState"] = askToShareState
                    }
                    call.resolve(result)
                } catch {
                    call.reject(error.localizedDescription, nil, error)
                }
            }
            return
            #endif
        }

        call.reject("Wi-Fi Infrastructure sharing requires iOS 26.2 or later.")
    }

    @objc func getPluginVersion(_ call: CAPPluginCall) {
        call.resolve(["version": self.pluginVersion])
    }


    private func resolveTimeoutMs(_ call: CAPPluginCall, option: String, defaultValue: Double) -> Double? {
        guard let raw = call.getValue(option), !(raw is NSNull) else {
            return defaultValue
        }
        if let value = call.getDouble(option) {
            return WifiConnectionLifecycle.isValidTimeoutMilliseconds(value) ? value : nil
        }
        if let value = call.getInt(option) {
            return WifiConnectionLifecycle.isValidTimeoutMilliseconds(Double(value)) ? Double(value) : nil
        }
        return nil
    }

    private func armWatchdog(_ attempt: ConnectAttempt) {
        attempt.watchdog?.cancel()
        let watchdog = DispatchWorkItem { [weak self, weak attempt] in
            guard let self = self, let attempt = attempt, self.activeConnectAttempt === attempt else { return }
            if !self.expireIfNeeded(attempt) {
                self.armWatchdog(attempt)
            }
        }
        attempt.watchdog = watchdog
        let delay = max(0, attempt.lifecycle.deadline - ProcessInfo.processInfo.systemUptime)
        DispatchQueue.main.asyncAfter(deadline: .now() + delay, execute: watchdog)
    }

    private func expireIfNeeded(_ attempt: ConnectAttempt) -> Bool {
        guard let phase = attempt.lifecycle.expiredPhase(at: ProcessInfo.processInfo.systemUptime) else { return false }
        completeConnect(
            attempt, code: "CONNECTION_TIMEOUT",
            message: phase == .request
                ? "Timed out waiting for the iOS Wi-Fi configuration request."
                : "Timed out waiting for Wi-Fi connection confirmation.",
            stage: phase.rawValue
        )
        return true
    }

    private func verifyConnectedSsid(_ attempt: ConnectAttempt) {
        guard activeConnectAttempt === attempt, !expireIfNeeded(attempt) else { return }
        // The independent verification watchdog also bounds a missing fetchCurrent callback.
        NEHotspotNetwork.fetchCurrent { [weak self] network in
            DispatchQueue.main.async {
                guard let self = self, self.activeConnectAttempt === attempt, !self.expireIfNeeded(attempt) else { return }
                let previous = attempt.lifecycle.verification
                if attempt.lifecycle.observe(
                    ssid: network?.ssid, expected: attempt.ssid, at: ProcessInfo.processInfo.systemUptime
                ) {
                    self.logConnect(attempt, event: "verification:match")
                    self.completeConnect(attempt)
                    return
                }
                if previous != attempt.lifecycle.verification {
                    self.logConnect(attempt, event: "verification:\(attempt.lifecycle.verification.rawValue)")
                }
                let poll = DispatchWorkItem { [weak self, weak attempt] in
                    guard let self = self, let attempt = attempt else { return }
                    self.verifyConnectedSsid(attempt)
                }
                attempt.poll = poll
                DispatchQueue.main.asyncAfter(deadline: .now() + self.ssidVerifyPollSeconds, execute: poll)
            }
        }
    }

    private func completeConnect(
        _ attempt: ConnectAttempt, code: String? = nil, message: String = "",
        stage: String = "verification", error: NSError? = nil
    ) {
        guard activeConnectAttempt === attempt, attempt.lifecycle.finish() else { return }
        activeConnectAttempt = nil
        attempt.watchdog?.cancel()
        attempt.poll?.cancel()
        attempt.watchdog = nil
        attempt.poll = nil
        guard let call = attempt.call else { return }
        attempt.call = nil
        logConnect(attempt, event: code.map { "failure:\($0)" } ?? "success")
        if let code = code {
            rejectConnect(
                call, code: code, message: message, stage: stage,
                nativeCode: error?.code, nativeMessage: error?.localizedDescription,
                error: error, attempt: attempt
            )
        } else {
            call.resolve()
        }
        // Timeout/error intentionally preserves persistent configuration. Only disconnect
        // requests removal; the OS apply operation itself cannot be cancelled here.
    }

    private func removeConfigurationForDisconnect(_ ssid: String) {
        let cleanupGeneration = configurationOwnership.claim(ssid: ssid)
        for pending in pendingApplications.values {
            if let attempt = pending.attempt, attempt.ssid == ssid {
                attempt.removeWhenAppliedGeneration = cleanupGeneration
            }
        }
        hotspotManager?.removeConfiguration(forSSID: ssid)
        if lastRequestedSSID == ssid {
            lastRequestedSSID = nil
        }
    }

    private func removeCancelledConfigurationIfNeeded(_ attempt: ConnectAttempt) {
        guard let generation = attempt.removeWhenAppliedGeneration,
              configurationOwnership.isCurrent(ssid: attempt.ssid, generation: generation) else { return }
        hotspotManager?.removeConfiguration(forSSID: attempt.ssid)
        logConnect(attempt, event: "cancelled-configuration-cleanup")
    }

    private func logConnect(_ attempt: ConnectAttempt, event: String) {
        let elapsedMs = Int((ProcessInfo.processInfo.systemUptime - attempt.lifecycle.startedAt) * 1000)
        NSLog("CapacitorWifi connect attempt=%ld elapsedMs=%ld event=%@", attempt.generation, elapsedMs, event)
    }

    private func isValidWpaPassphrase(_ password: String) -> Bool {
        let length = password.count
        if length >= 8 && length <= 63 {
            return true
        }
        if length == 64 {
            let hex = CharacterSet(charactersIn: "0123456789abcdefABCDEF")
            return password.unicodeScalars.allSatisfy { hex.contains($0) }
        }
        return false
    }

    private func mapHotspotConfigurationError(_ error: NSError) -> (code: String, message: String, stage: String) {
        guard error.domain == NEHotspotConfigurationErrorDomain,
              let hotspotError = NEHotspotConfigurationError(rawValue: error.code) else {
            return ("UNKNOWN", "An unexpected Wi-Fi connection error occurred.", "request")
        }

        switch hotspotError {
        case .invalidSSID, .invalidSSIDPrefix, .invalidWPAPassphrase, .invalidWEPPassphrase,
             .invalidEAPSettings, .invalidHS20Settings, .invalidHS20DomainName:
            return ("INVALID_CONFIGURATION", "Invalid Wi-Fi connection configuration.", "validation")
        case .userDenied:
            return ("USER_DENIED", "The user denied the Wi-Fi connection request.", "request")
        case .pending:
            return ("CONNECTION_IN_PROGRESS", "Another Wi-Fi connection attempt is already in progress.", "request")
        case .internal, .systemConfiguration, .joinOnceNotSupported, .applicationIsNotInForeground:
            return ("CONNECTION_FAILED", "Failed to connect to network.", "request")
        case .invalid, .unknown:
            return ("UNKNOWN", "An unexpected Wi-Fi connection error occurred.", "request")
        case .alreadyAssociated:
            return ("CONNECTION_FAILED", "Failed to connect to network.", "verification")
        @unknown default:
            return ("CONNECTION_FAILED", "Failed to connect to network.", "request")
        }
    }

    private func rejectConnect(
        _ call: CAPPluginCall,
        code: String,
        message: String,
        stage: String,
        nativeCode: Int?,
        nativeMessage: String?,
        error: Error?,
        attempt: ConnectAttempt? = nil
    ) {
        var data = JSObject()
        data["platform"] = "ios"
        data["connectionStage"] = stage
        if let attempt = attempt {
            data["elapsedMs"] = (ProcessInfo.processInfo.systemUptime - attempt.lifecycle.startedAt) * 1000
            if stage == "verification" {
                data["ssidVerification"] = attempt.lifecycle.verification.rawValue
            }
        }
        if let nativeCode = nativeCode {
            data["nativeCode"] = nativeCode
        }
        if let nativeMessage = nativeMessage {
            data["nativeMessage"] = nativeMessage
        }
        call.reject(message, code, error, data)
    }

    // MARK: - Helper Methods

    /// Fetches the current Wi-Fi network asynchronously.
    /// NEHotspotNetwork.fetchCurrent uses a completion handler (async), so we bridge it
    /// with withCheckedContinuation to avoid returning nil before the callback fires.
    private func fetchCurrentNetwork() async -> NEHotspotNetwork? {
        await withCheckedContinuation { continuation in
            NEHotspotNetwork.fetchCurrent { network in
                continuation.resume(returning: network)
            }
        }
    }

    private func getIPAddress() -> String? {
        var ifaddr: UnsafeMutablePointer<ifaddrs>?

        guard getifaddrs(&ifaddr) == 0, let firstAddr = ifaddr else {
            return nil
        }

        defer { freeifaddrs(ifaddr) }

        var ipv4Address: String?
        var routableIPv6Address: String?
        var linkLocalIPv6Address: String?

        for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next }) {
            let interface = ptr.pointee
            let addrFamily = interface.ifa_addr.pointee.sa_family
            let name = String(cString: interface.ifa_name)

            guard name == "en0",
                  addrFamily == UInt8(AF_INET) || addrFamily == UInt8(AF_INET6) else { continue }

            var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
            getnameinfo(interface.ifa_addr, socklen_t(interface.ifa_addr.pointee.sa_len),
                        &hostname, socklen_t(hostname.count),
                        nil, socklen_t(0), NI_NUMERICHOST)
            let resolved = String(cString: hostname)

            if addrFamily == UInt8(AF_INET) {
                ipv4Address = resolved
                break
            } else if resolved.hasPrefix("fe80") {
                if linkLocalIPv6Address == nil { linkLocalIPv6Address = resolved }
            } else {
                if routableIPv6Address == nil { routableIPv6Address = resolved }
            }
        }

        return ipv4Address ?? routableIPv6Address ?? linkLocalIPv6Address
    }

    private func getLocationPermissionStatus() -> String {
        let manager = CLLocationManager()
        switch manager.authorizationStatus {
        case .authorizedAlways, .authorizedWhenInUse:
            return "granted"
        case .denied, .restricted:
            return "denied"
        case .notDetermined:
            return "prompt"
        @unknown default:
            return "prompt"
        }
    }

    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        resolvePermissionCallIfNeeded()
    }

    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        resolvePermissionCallIfNeeded()
    }

    private func resolvePermissionCallIfNeeded() {
        if !Thread.isMainThread {
            DispatchQueue.main.async {
                self.resolvePermissionCallIfNeeded()
            }
            return
        }

        if permissionCalls.isEmpty {
            return
        }

        let status = getLocationPermissionStatus()
        if status == "prompt" {
            return
        }

        let pendingCalls = permissionCalls
        permissionCalls.removeAll()
        for pendingCall in pendingCalls {
            pendingCall.resolve(["location": status])
        }
    }
}
