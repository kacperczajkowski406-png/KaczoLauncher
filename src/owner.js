const store = require('./store');

// Właściciel launchera. Rozpoznajemy go po UUID konta Microsoft (nick można zmienić, UUID nie).
const OWNER = {
  name: 'I_Kaczor_I',
  uuid: '2377550c438e4e55b510ecf2ffd6ffe5',
};

const norm = (u) => String(u || '').replace(/-/g, '').toLowerCase();

/**
 * „Szkielet” nicku: ujednolica znaki łudząco podobne (I/l/1, O/0) i pomija podkreślniki,
 * dzięki czemu nie da się podszyć pod właściciela nickiem w stylu „IKaczorI” albo „I__Kaczor_l”.
 */
function skeleton(name) {
  return String(name || '')
    .toLowerCase()
    .replace(/[l1|!]/g, 'i')
    .replace(/0/g, 'o')
    .replace(/[^a-z0-9]/g, '');
}

const OWNER_SKELETON = skeleton(OWNER.name);

function isOwnerAccount(account) {
  return !!account && account.type === 'microsoft' && norm(account.uuid) === OWNER.uuid;
}

/** Nick zarezerwowany dla właściciela (także w wersjach „na oko” identycznych). */
function isReservedName(name) {
  return skeleton(name) === OWNER_SKELETON;
}

function isBlockedByOwner(name) {
  const sk = skeleton(name);
  return store.load().settings.blockedNicks.some((n) => skeleton(n) === sk);
}

module.exports = { OWNER, isOwnerAccount, isReservedName, isBlockedByOwner, skeleton };
