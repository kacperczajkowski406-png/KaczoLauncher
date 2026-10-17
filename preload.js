const { contextBridge, ipcRenderer } = require('electron');

// Interfejs dostaje tylko wąskie, jawnie wymienione API – bez dostępu do Node.
const INVOKE = new Set([
  'app:init',
  'versions:list', 'versions:fabric',
  'account:microsoft', 'account:offline', 'account:select', 'account:remove',
  'instance:create', 'instance:update', 'instance:remove', 'instance:select', 'instance:openFolder',
  'mods:list', 'mods:toggle', 'mods:remove', 'mods:addLocal', 'mods:search', 'mods:install',
  'settings:save', 'settings:pickJava', 'app:openData',
  'owner:diagnostics', 'owner:copyDiagnostics', 'owner:openLogs', 'owner:devtools',
  'owner:resetOptions', 'owner:clearJava', 'owner:discordId', 'owner:blockNick', 'owner:unblockNick',
  'game:play', 'game:stop',
  'update:check', 'update:install', 'update:state',
]);
const EVENTS = new Set(['game:state', 'game:progress', 'game:log', 'update:state']);

contextBridge.exposeInMainWorld('api', {
  invoke: async (channel, ...args) => {
    if (!INVOKE.has(channel)) throw new Error('Niedozwolony kanał: ' + channel);
    const res = await ipcRenderer.invoke(channel, ...args);
    if (!res.ok) throw new Error(res.error);
    return res.data;
  },
  on: (channel, callback) => {
    if (!EVENTS.has(channel)) throw new Error('Niedozwolony kanał: ' + channel);
    ipcRenderer.on(channel, (_event, payload) => callback(payload));
  },
});
