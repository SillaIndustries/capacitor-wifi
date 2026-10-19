'use strict';

var core = require('@capacitor/core');

/**
 * Stable error codes returned by {@link CapacitorWifiPlugin.connect} on failure.
 * Specific codes are returned only when the native platform explicitly provides that reason.
 * Otherwise the plugin returns {@link WifiConnectionErrorCode.CONNECTION_FAILED}.
 *
 * @since 8.5.0
 */
exports.WifiConnectionErrorCode = void 0;
(function (WifiConnectionErrorCode) {
    /**
     * The SSID, password format, security configuration, timeout, or native request parameters are invalid.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["INVALID_CONFIGURATION"] = "INVALID_CONFIGURATION";
    /**
     * A required permission or authorization is missing or was denied.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["PERMISSION_DENIED"] = "PERMISSION_DENIED";
    /**
     * Wi-Fi is disabled and the connection cannot be started.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["WIFI_DISABLED"] = "WIFI_DISABLED";
    /**
     * The native API explicitly reports that the user rejected the connection request.
     * Available on iOS; on Android only when the OS reports user rejection (API 36+).
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["USER_DENIED"] = "USER_DENIED";
    /**
     * The native API explicitly reports an authentication failure.
     * Not reliably available on Android API 29–33.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["AUTHENTICATION_FAILED"] = "AUTHENTICATION_FAILED";
    /**
     * The native API explicitly reports that the requested access point was not found.
     * Not reliably available on Android API 29–33.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["NETWORK_NOT_FOUND"] = "NETWORK_NOT_FOUND";
    /**
     * The plugin timeout expired before the requested network was confirmed as connected.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["CONNECTION_TIMEOUT"] = "CONNECTION_TIMEOUT";
    /**
     * The connection failed, but the platform did not provide a reliable detailed reason.
     * Also returned on Android when requested process binding fails or disconnect()
     * cancels a pending connection.
     * On iOS, disconnect() also cancels a pending connection with this code.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["CONNECTION_FAILED"] = "CONNECTION_FAILED";
    /**
     * Another Wi-Fi connection attempt is already active.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["CONNECTION_IN_PROGRESS"] = "CONNECTION_IN_PROGRESS";
    /**
     * An unexpected plugin or native error occurred.
     *
     * @since 8.5.0
     */
    WifiConnectionErrorCode["UNKNOWN"] = "UNKNOWN";
})(exports.WifiConnectionErrorCode || (exports.WifiConnectionErrorCode = {}));
/**
 * Network security types
 *
 * @since 7.0.0
 */
exports.NetworkSecurityType = void 0;
(function (NetworkSecurityType) {
    /**
     * Open network with no security
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["OPEN"] = 0] = "OPEN";
    /**
     * WEP security
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["WEP"] = 1] = "WEP";
    /**
     * WPA/WPA2 Personal (PSK)
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["WPA2_PSK"] = 2] = "WPA2_PSK";
    /**
     * WPA/WPA2/WPA3 Enterprise (EAP)
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["EAP"] = 3] = "EAP";
    /**
     * WPA3 Personal (SAE)
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["SAE"] = 4] = "SAE";
    /**
     * WPA3 Enterprise
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["WPA3_ENTERPRISE"] = 5] = "WPA3_ENTERPRISE";
    /**
     * WPA3 Enterprise 192-bit mode
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["WPA3_ENTERPRISE_192_BIT"] = 6] = "WPA3_ENTERPRISE_192_BIT";
    /**
     * Passpoint network
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["PASSPOINT"] = 7] = "PASSPOINT";
    /**
     * Enhanced Open (OWE)
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["OWE"] = 8] = "OWE";
    /**
     * WAPI PSK
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["WAPI_PSK"] = 9] = "WAPI_PSK";
    /**
     * WAPI Certificate
     *
     * @since 7.0.0
     */
    NetworkSecurityType[NetworkSecurityType["WAPI_CERT"] = 10] = "WAPI_CERT";
})(exports.NetworkSecurityType || (exports.NetworkSecurityType = {}));

const CapacitorWifi = core.registerPlugin('CapacitorWifi', {
    web: () => Promise.resolve().then(function () { return web; }).then((m) => new m.CapacitorWifiWeb()),
});

class CapacitorWifiWeb extends core.WebPlugin {
    async addNetwork(_options) {
        throw this.unimplemented('Not implemented on web.');
    }
    async connect(_options) {
        throw this.unimplemented('Not implemented on web.');
    }
    async disconnect(_options) {
        throw this.unimplemented('Not implemented on web.');
    }
    async getConnectionDiagnostics() {
        throw this.unimplemented('Not implemented on web.');
    }
    async getAvailableNetworks() {
        throw this.unimplemented('Not implemented on web.');
    }
    async getIpAddress() {
        throw this.unimplemented('Not implemented on web.');
    }
    async getRssi() {
        throw this.unimplemented('Not implemented on web.');
    }
    async getSsid() {
        throw this.unimplemented('Not implemented on web.');
    }
    async getWifiInfo() {
        throw this.unimplemented('Not implemented on web.');
    }
    async isEnabled() {
        throw this.unimplemented('Not implemented on web.');
    }
    async startScan() {
        throw this.unimplemented('Not implemented on web.');
    }
    async checkPermissions() {
        throw this.unimplemented('Not implemented on web.');
    }
    async requestPermissions(_options) {
        throw this.unimplemented('Not implemented on web.');
    }
    async isNetworkSaved(_options) {
        throw this.unimplemented('Not implemented on web.');
    }
    async shareNetwork(_options) {
        throw this.unimplemented('Not implemented on web.');
    }
    async getPluginVersion() {
        return { version: '1.0.0' };
    }
}

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    CapacitorWifiWeb: CapacitorWifiWeb
});

exports.CapacitorWifi = CapacitorWifi;
//# sourceMappingURL=plugin.cjs.js.map
