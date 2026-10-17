const fs = require('fs');
const path = require('path');
const { execFile } = require('child_process');
const { promisify } = require('util');
const { P } = require('./paths');
const { downloadFile } = require('./http');
const { T } = require('./lang');

const execFileP = promisify(execFile);

const OS = { win32: 'windows', darwin: 'mac', linux: 'linux' }[process.platform];
const ARCH = { x64: 'x64', arm64: 'aarch64' }[process.arch];
const EXE = process.platform === 'win32' ? 'javaw.exe' : 'java';
const MARKER = '.kaczo-complete'; // znacznik: rozpakowanie zakończyło się w całości

function findJavaBin(dir, depth = 0) {
  if (depth > 6 || !fs.existsSync(dir)) return null;
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    if (entry.isFile() && entry.name === EXE && path.basename(dir) === 'bin') return path.join(dir, entry.name);
  }
  for (const entry of entries) {
    if (entry.isDirectory()) {
      const found = findJavaBin(path.join(dir, entry.name), depth + 1);
      if (found) return found;
    }
  }
  return null;
}

/**
 * Rozpakowuje archiwum. Na Windowsie używamy PowerShella (.NET) – wbudowany `tar` bywa
 * zawodny przy większych plikach .zip; na macOS i Linuksie wystarcza `tar`.
 */
async function extractArchive(archive, dir) {
  if (process.platform === 'win32') {
    const q = (s) => s.replace(/'/g, "''");
    const script =
      `$ProgressPreference='SilentlyContinue'; $ErrorActionPreference='Stop'; ` +
      `Expand-Archive -LiteralPath '${q(archive)}' -DestinationPath '${q(dir)}' -Force`;
    await execFileP('powershell.exe', ['-NoProfile', '-NonInteractive', '-ExecutionPolicy', 'Bypass', '-Command', script], {
      maxBuffer: 64 * 1024 * 1024,
      windowsHide: true,
    });
  } else {
    await execFileP('tar', ['-xf', archive, '-C', dir], { maxBuffer: 64 * 1024 * 1024 });
  }
}

/**
 * Zwraca ścieżkę do Javy dla danej wersji głównej.
 * Własna ścieżka z ustawień ma pierwszeństwo, w przeciwnym razie pobieramy JRE z Adoptium.
 * Pobieranie i rozpakowanie są powtarzane raz, jeśli pierwsza próba się nie powiedzie.
 */
async function ensureJava(major, settings, onProgress) {
  if (settings.javaPath && fs.existsSync(settings.javaPath)) return settings.javaPath;
  if (!OS || !ARCH) throw new Error(T('java.platform', { p: `${process.platform}/${process.arch}` }));

  const dir = path.join(P.runtime, `java-${major}`);

  // Ufamy tylko w pełni rozpakowanej Javie – po przerwanej instalacji folder jest czyszczony.
  if (fs.existsSync(path.join(dir, MARKER))) {
    const existing = findJavaBin(dir);
    if (existing) return existing;
  }

  const ext = process.platform === 'win32' ? 'zip' : 'tar.gz';
  const url = `https://api.adoptium.net/v3/binary/latest/${major}/ga/${OS}/${ARCH}/jre/hotspot/normal/eclipse`;
  let lastError;

  for (let attempt = 1; attempt <= 2; attempt++) {
    fs.rmSync(dir, { recursive: true, force: true });
    fs.mkdirSync(dir, { recursive: true });
    const archive = path.join(dir, `jre.${ext}`);
    try {
      await downloadFile(url, archive, {
        onProgress: (done, total) => onProgress && onProgress(T('java.download', { major }), done, total),
      });
      onProgress && onProgress(T('java.extract', { major }), 0, 0);
      await extractArchive(archive, dir);
      fs.rmSync(archive, { force: true });

      const bin = findJavaBin(dir);
      if (!bin) throw new Error(T('java.nobin'));
      fs.writeFileSync(path.join(dir, MARKER), new Date().toISOString());
      return bin;
    } catch (e) {
      lastError = e;
    }
  }

  fs.rmSync(dir, { recursive: true, force: true });
  throw new Error(T('java.fail', { major, msg: lastError && lastError.message ? lastError.message : lastError }));
}

module.exports = { ensureJava, extractArchive, findJavaBin, MARKER };
