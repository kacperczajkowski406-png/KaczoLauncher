'use strict';
// Strona czyta wydania z GitHuba: najnowszy instalator do pobrania + lista zmian.
(function () {
  const $ = (id) => document.getElementById(id);

  function detectRepo() {
    const q = new URLSearchParams(location.search).get('repo');
    if (q && /^[\w.-]+\/[\w.-]+$/.test(q)) return q;
    if (window.KACZO_REPO) return window.KACZO_REPO;
    const m = /^([\w-]+)\.github\.io$/i.exec(location.hostname);
    if (m) {
      const first = location.pathname.split('/').filter(Boolean)[0];
      if (first) return m[1] + '/' + first;
    }
    return '';
  }

  const repo = detectRepo();
  const fmtDate = (s) => new Date(s).toLocaleDateString('pl-PL', { day: 'numeric', month: 'long', year: 'numeric' });
  const fmtMb = (b) => (b / 1048576).toFixed(0) + ' MB';

  function noRepo() {
    $('dl-version').textContent = 'wkrótce';
    $('dl-btn').removeAttribute('href');
    $('dl-btn').setAttribute('aria-disabled', 'true');
    $('dl-meta').textContent = 'Wydanie pojawi się tutaj po pierwszym buildzie.';
    $('dl-all').hidden = true;
    $('foot-repo').hidden = true;
    $('log').innerHTML = '<li class="log-item log-empty">Brak wydań do pokazania.</li>';
  }

  function clean(body) {
    return String(body || '')
      .split(/\r?\n/)
      .map((l) => l.replace(/^[-*]\s+/, '').replace(/\s+by @[\w-]+ in https?:\S+/i, '').replace(/\*\*Full Changelog\*\*.*/i, '').replace(/^#+\s*.*/, '').trim())
      .filter((l) => l && !/^what'?s changed$/i.test(l));
  }

  function render(releases) {
    const list = releases.filter((r) => !r.draft && !r.prerelease);
    const latest = list[0];
    if (!latest) return noRepo();
    const exe = (latest.assets || []).find((a) => /setup.*\.exe$/i.test(a.name));
    const ver = latest.tag_name.replace(/^v/, '');
    $('dl-version').textContent = 'wersja ' + ver;
    if (exe) {
      $('dl-btn').href = exe.browser_download_url;
      $('dl-meta').textContent = 'Windows 10 / 11, 64-bit · ' + fmtMb(exe.size) + ' · wydano ' + fmtDate(latest.published_at);
    } else {
      $('dl-btn').href = latest.html_url;
      $('dl-meta').textContent = 'Wydano ' + fmtDate(latest.published_at);
    }
    $('dl-all').href = 'https://github.com/' + repo + '/releases';
    $('foot-repo').href = 'https://github.com/' + repo;

    const items = list.slice(0, 8).map((r, i) => {
      const lines = clean(r.body);
      const li = document.createElement('li');
      li.className = 'log-item';
      const head = document.createElement('div');
      head.className = 'log-head';
      const b = document.createElement('strong');
      b.textContent = 'Wersja ' + r.tag_name.replace(/^v/, '') + (i === 0 ? ' (najnowsza)' : '');
      const t = document.createElement('time');
      t.dateTime = r.published_at;
      t.textContent = fmtDate(r.published_at);
      head.append(b, t);
      const ul = document.createElement('ul');
      ul.className = 'log-body';
      (lines.length ? lines.slice(0, 8) : ['Poprawki i ulepszenia.']).forEach((l) => {
        const x = document.createElement('li');
        x.textContent = l;
        ul.append(x);
      });
      li.append(head, ul);
      return li;
    });
    $('log').replaceChildren(...items);
  }

  if (!repo) { noRepo(); return; }
  $('dl-all').href = 'https://github.com/' + repo + '/releases';
  $('foot-repo').href = 'https://github.com/' + repo;

  const cached = (() => { try { return JSON.parse(sessionStorage.getItem('kaczo-rel') || 'null'); } catch { return null; } })();
  if (cached && Date.now() - cached.t < 5 * 60 * 1000) render(cached.d);

  fetch('https://api.github.com/repos/' + repo + '/releases?per_page=10', { headers: { Accept: 'application/vnd.github+json' } })
    .then((r) => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
    .then((d) => { try { sessionStorage.setItem('kaczo-rel', JSON.stringify({ t: Date.now(), d })); } catch { /* brak sessionStorage */ } render(d); })
    .catch(() => {
      if (cached) return;
      $('dl-version').textContent = 'GitHub';
      $('dl-btn').href = 'https://github.com/' + repo + '/releases/latest';
      $('dl-meta').textContent = 'Nie udało się pobrać danych o wydaniu. Przycisk otworzy stronę wydań.';
      $('log').innerHTML = '<li class="log-item log-empty">Nie udało się wczytać listy zmian. Zobacz <a href="https://github.com/' + repo + '/releases">wydania na GitHubie</a>.</li>';
    });
})();
