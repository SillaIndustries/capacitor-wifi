import { WebPlugin } from '@capacitor/core';
export class CapacitorWifiWeb extends WebPlugin {
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
//# sourceMappingURL=web.js.map