// Tapety launchera (te same co w menu gry): statyczne i „live”. Wybór zapamiętany lokalnie.
(function () {
  const LIST = [
    { id: 'silk', name: 'Black Silk', cat: 'Dark', live: true, layer: null },
    { id: 'aurora', name: 'Northern Lights', cat: 'Nature', live: true, layer: 'scroll', speed: 70 },
    { id: 'nebula', name: 'Nebula', cat: 'Space', live: true, layer: 'scroll', speed: 120 },
    { id: 'synthwave', name: 'Synthwave', cat: 'Colors', live: true, layer: 'pulse' },
    { id: 'sakura', name: 'Sakura Dusk', cat: 'Nature', live: true, layer: 'scroll', speed: 45 },
    { id: 'galaxy', name: 'Galaxy', cat: 'Space', live: true, layer: 'pulse' },
    { id: 'ocean', name: 'Ocean Sunset', cat: 'Nature', live: false },
    { id: 'mountains', name: 'Dusk Peaks', cat: 'Nature', live: false },
    { id: 'citynight', name: 'City Night', cat: 'Dark', live: false },
    { id: 'moonlake', name: 'Moon Lake', cat: 'Nature', live: false },
    { id: 'forest', name: 'Misty Forest', cat: 'Nature', live: false },
    { id: 'ember', name: 'Ember', cat: 'Colors', live: false },
    { id: 'ice', name: 'Glacier', cat: 'Colors', live: false },
    { id: 'graphite', name: 'Graphite', cat: 'Dark', live: false },
    { id: 'duck', name: 'Kaczo Gold', cat: 'Colors', live: false },
    { id: 'violet', name: 'Violet', cat: 'Colors', live: false },
  ];
  const get = (id) => LIST.find((w) => w.id === id) || LIST[0];
  const cur = () => { try { return get(localStorage.getItem('kaczo.wp')); } catch (e) { return LIST[0]; } };
  function paint(w, base, layer, animated) {
    base.style.backgroundImage = `url(tex/wp/${w.id}.jpg)`;
    base.className = animated && w.live ? 'drift' : '';
    if (w.layer && animated) {
      layer.style.backgroundImage = `url(tex/wp/${w.id}_l.png)`;
      layer.className = w.layer;
      layer.style.setProperty('--dur', w.layer === 'scroll' ? Math.round(1920 / (w.speed || 60) * 6) + 's' : '5s');
    } else { layer.style.backgroundImage = 'none'; layer.className = ''; }
  }
  function apply() {
    const root = document.getElementById('wp');
    if (!root) return;
    paint(cur(), document.getElementById('wp-base'), document.getElementById('wp-layer'), true);
  }
  window.KWall = {
    list: LIST,
    current: () => cur().id,
    set(id) { try { localStorage.setItem('kaczo.wp', id); } catch (e) { /* bez zapisu */ } apply(); },
    paint,
  };
  if (document.readyState !== 'loading') apply(); else document.addEventListener('DOMContentLoaded', apply);
})();
