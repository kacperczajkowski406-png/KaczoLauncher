const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { Readable } = require('stream');
const { pipeline } = require('stream/promises');
const { T } = require('./lang');

const UA = 'KaczoClient/1.0';

async function request(url, opts = {}) {
  let res;
  try {
    res = await fetch(url, {
      ...opts,
      headers: { 'User-Agent': UA, ...(opts.headers || {}) },
      signal: AbortSignal.timeout(opts.timeout || 30000),
    });
  } catch (e) {
    // brak internetu, DNS, timeout – komunikat zrozumiały dla gracza, a flaga `network` dla reszty kodu
    const err = new Error(T('http.offline', { host: new URL(url).host }));
    err.network = true;
    err.cause = e;
    throw err;
  }
  if (!res.ok) throw new Error(T('http.status', { code: res.status, host: new URL(url).host }));
  return res;
}

async function getJson(url) {
  return (await request(url)).json();
}

/**
 * Pobiera plik do dest (przez plik .part), raportuje postęp, sprawdza rozmiar
 * (gdy serwer go podaje) i opcjonalnie sumę sha1. Niepełny plik nigdy nie zostaje pod docelową nazwą.
 */
async function downloadFile(url, dest, { onProgress, sha1 } = {}) {
  fs.mkdirSync(path.dirname(dest), { recursive: true });
  const res = await request(url, { timeout: 15 * 60 * 1000 });
  const total = Number(res.headers.get('content-length')) || 0;
  const part = dest + '.part';
  const hash = crypto.createHash('sha1');
  let done = 0;

  try {
    await pipeline(
      Readable.fromWeb(res.body),
      async function* (source) {
        for await (const chunk of source) {
          done += chunk.length;
          hash.update(chunk);
          if (onProgress) onProgress(done, total);
          yield chunk;
        }
      },
      fs.createWriteStream(part)
    );
  } catch (e) {
    fs.rmSync(part, { force: true });
    throw new Error(T('http.interrupted', { msg: e && e.message ? e.message : e }));
  }

  if (total && done !== total) {
    fs.rmSync(part, { force: true });
    throw new Error(T('http.incomplete', { done, total }));
  }
  if (sha1 && hash.digest('hex') !== sha1.toLowerCase()) {
    fs.rmSync(part, { force: true });
    throw new Error(T('http.sha'));
  }
  fs.renameSync(part, dest);
  return dest;
}

module.exports = { getJson, downloadFile, request, UA };
