import { WebPlugin } from '@capacitor/core';
import type { AddNetworkOptions, CapacitorWifiPlugin, ConnectOptions, DisconnectOptions, GetAvailableNetworksResult, GetIpAddressResult, GetRssiResult, GetSsidResult, IsEnabledResult, IsNetworkSavedOptions, IsNetworkSavedResult, PermissionStatus, RequestPermissionsOptions, ShareNetworkOptions, ShareNetworkResult, WifiInfo, WifiConnectionDiagnosticsSnapshot } from './definitions';
export declare class CapacitorWifiWeb extends WebPlugin implements CapacitorWifiPlugin {
    addNetwork(_options: AddNetworkOptions): Promise<void>;
    connect(_options: ConnectOptions): Promise<void>;
    disconnect(_options?: DisconnectOptions): Promise<void>;
    getConnectionDiagnostics(): Promise<WifiConnectionDiagnosticsSnapshot>;
    getAvailableNetworks(): Promise<GetAvailableNetworksResult>;
    getIpAddress(): Promise<GetIpAddressResult>;
    getRssi(): Promise<GetRssiResult>;
    getSsid(): Promise<GetSsidResult>;
    getWifiInfo(): Promise<WifiInfo>;
    isEnabled(): Promise<IsEnabledResult>;
    startScan(): Promise<void>;
    checkPermissions(): Promise<PermissionStatus>;
    requestPermissions(_options?: RequestPermissionsOptions): Promise<PermissionStatus>;
    isNetworkSaved(_options: IsNetworkSavedOptions): Promise<IsNetworkSavedResult>;
    shareNetwork(_options?: ShareNetworkOptions): Promise<ShareNetworkResult>;
    getPluginVersion(): Promise<{
        version: string;
    }>;
}
