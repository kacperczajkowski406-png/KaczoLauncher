const net = require('net');
const os = require('os');
const path = require('path');
const crypto = require('crypto');

// Discord Rich Presence przez lokalny IPC (bez zewnętrznych bibliotek).
// Wymaga ID aplikacji z https://discord.com/developers/applications – w niej, w zakładce
// „Rich Presence → Art Assets”, trzeba wgrać obrazek o nazwie „logo” (build/icon.png).
// Nazwa aplikacji w Discordzie ("Kaczo Client") to nazwa tej aplikacji.

let sock = null;
let ready = false;
let pending = null;

function frame(op, data) {
  const body = Buffer.from(JSON.stringify(data));
  const head = Buffer.alloc(8);
  head.writeInt32LE(op, 0);
  head.writeInt32LE(body.length, 4);
  return Buffer.concat([head, body]);
}

function pipePath(i) {
  if (process.platform === 'win32') return `\\\\?\\pipe\\discord-ipc-${i}`;
  const base = process.env.XDG_RUNTIME_DIR || process.env.TMPDIR || os.tmpdir();
  return path.join(base, `discord-ipc-${i}`);
}

function tryConnect(i, clientId, done) {
  if (i > 9) return done(null);
  const s = net.createConnection(pipePath(i));
  s.once('connect', () => done(s));
  s.once('error', () => { s.destroy(); tryConnect(i + 1, clientId, done); });
}

function send(activity) {
  if (!sock || !ready) return;
  sock.write(frame(1, {
    cmd: 'SET_ACTIVITY',
    args: { pid: process.pid, activity: activity || null },
    nonce: crypto.randomUUID(),
  }));
}

/** Ustawia status w Discordzie. Ciche, gdy Discord nie działa lub brak ID aplikacji. */
function start(clientId, activity) {
  stop();
  if (!/^\d{15,25}$/.test(String(clientId || ''))) return false;
  pending = activity;
  tryConnect(0, clientId, (s) => {
    if (!s) return;
    sock = s;
    let buf = Buffer.alloc(0);
    s.on('data', (chunk) => {
      buf = Buffer.concat([buf, chunk]);
      while (buf.length >= 8) {
        const len = buf.readInt32LE(4);
        if (buf.length < 8 + len) break;
        let msg = {};
        try { msg = JSON.parse(buf.subarray(8, 8 + len).toString()); } catch { /* pomiń */ }
        buf = buf.subarray(8 + len);
        if (msg.evt === 'READY' && !ready) { ready = true; send(pending); }
      }
    });
    s.on('error', () => {});
    s.on('close', () => { ready = false; sock = null; });
    s.write(frame(0, { v: 1, client_id: String(clientId) }));
  });
  return true;
}

function stop() {
  try { if (sock) { if (ready) send(null); sock.end(); sock.destroy(); } } catch { /* ignoruj */ }
  sock = null; ready = false; pending = null;
}

module.exports = { start, stop, frame };
