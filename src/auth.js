const crypto = require('crypto');
const { seal, unseal } = require('./store');
const { T } = require('./lang');
const owner = require('./owner');

const NICK_RE = /^[A-Za-z0-9_]{3,16}$/;

/** UUID offline liczony tak jak w oryginalnym kliencie: MD5("OfflinePlayer:" + nick), wersja 3. */
function offlineUuid(name) {
  const h = crypto.createHash('md5').update('OfflinePlayer:' + name, 'utf8').digest();
  h[6] = (h[6] & 0x0f) | 0x30;
  h[8] = (h[8] & 0x3f) | 0x80;
  const hex = h.toString('hex');
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

/** Sprawdza, czy nick można użyć na koncie offline (zarezerwowany nick właściciela jest zawsze zablokowany). */
function assertOfflineNameAllowed(name) {
  if (!NICK_RE.test(name)) throw new Error(T('err.nick'));
  if (owner.isReservedName(name)) throw new Error(T('err.reserved'));
  if (owner.isBlockedByOwner(name)) throw new Error(T('err.blocked'));
}

function createOffline(name) {
  const nick = String(name || '').trim();
  assertOfflineNameAllowed(nick);
  return {
    id: 'offline-' + offlineUuid(nick),
    type: 'offline',
    name: nick,
    uuid: offlineUuid(nick),
  };
}

function friendlyError(e) {
  const msg = String((e && e.message) || e);
  if (/closed|cancel/i.test(msg)) return T('auth.closed');
  if (/entitlement|profile|owns|game/i.test(msg)) return T('auth.noGame');
  return T('auth.failed', { msg });
}

/** Logowanie premium w oknie Microsoft (biblioteka msmc). */
async function loginMicrosoft() {
  try {
    const { Auth } = require('msmc');
    const manager = new Auth('select_account');
    const xbox = await manager.launch('electron');
    const mc = await xbox.getMinecraft();
    const auth = mc.mclc();
    return {
      id: 'ms-' + auth.uuid,
      type: 'microsoft',
      name: auth.name,
      uuid: auth.uuid,
      refresh: seal(xbox.save()),
    };
  } catch (e) {
    throw new Error(friendlyError(e));
  }
}

/**
 * Zwraca obiekt `authorization` dla minecraft-launcher-core.
 * Dla kont Microsoft odświeża sesję i zapisuje nowy token odświeżania w `account`.
 */
async function authorize(account) {
  if (account.type === 'offline') {
    // konto offline z zarezerwowanym nickiem nigdy nie wystartuje (nawet po ręcznej edycji pliku danych)
    assertOfflineNameAllowed(account.name);
    return {
      access_token: account.uuid,
      client_token: account.uuid,
      uuid: account.uuid,
      name: account.name,
      user_properties: '{}',
      meta: { type: 'mojang', demo: false },
    };
  }
  try {
    const { Auth } = require('msmc');
    const manager = new Auth('select_account');
    const xbox = await manager.refresh(unseal(account.refresh));
    const mc = await xbox.getMinecraft();
    const result = mc.mclc();
    account.refresh = seal(xbox.save());
    account.name = result.name;
    account.uuid = result.uuid;
    return result;
  } catch (e) {
    throw new Error(T('auth.expired', { msg: friendlyError(e) }));
  }
}

module.exports = { createOffline, loginMicrosoft, authorize, offlineUuid, assertOfflineNameAllowed, NICK_RE };
