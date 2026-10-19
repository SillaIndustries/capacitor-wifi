import { registerPlugin } from '@capacitor/core';
const CapacitorWifi = registerPlugin('CapacitorWifi', {
    web: () => import('./web').then((m) => new m.CapacitorWifiWeb()),
});
export * from './definitions';
export { CapacitorWifi };
//# sourceMappingURL=index.js.map