// Generates terrain in each dimension and has BlueMap render it, then reports what exists.
// Usage: node prep.js <dir> <jar> <port>, relative to the work folder.
const { Server, sleep, ROOT } = require('./lib.js'); const fs = require('fs'), path = require('path');
const [dir, jar, port] = process.argv.slice(2);
function count(root) { let n = 0; const walk = (d) => { for (const e of fs.readdirSync(d, { withFileTypes: true })) { if (e.isDirectory()) walk(path.join(d, e.name)); else if (e.name.endsWith('.png')) n++; } }; if (fs.existsSync(root)) walk(root); return n; }
(async () => {
  const s = new Server(dir, jar, +port); await s.start();
  console.log('started', (s.log.match(/Commands: .*/) || [''])[0]);
  await sleep(4000);
  // On a new server BlueMap first downloads the client's resources, which takes a while. Reloading in the
  // middle of that interrupts it, so only reload after it has really failed.
  for (let i = 0; i < 24; i++) {
    const m = s.mark(); s.cmd('bmt maps'); await sleep(1500);
    if (!/FAILED maps/.test(s.since(m))) break;
    const failed = /Failed to load!/.test(s.since(m)) || (i > 0 && /Failed to load!/.test(s.log.slice(-4000)));
    console.log('BlueMap not loaded yet, check', i + 1, failed ? '(failed, reloading)' : '(still loading)');
    if (failed) { s.cmd('bluemap reload'); await sleep(15000); } else await sleep(5000);
  }
  await s.run('bmt maps', /BMT api .*/, 15000);
  console.log(s.log.split('\n').filter((l) => /BMT (map|root|api)/.test(l)).map((l) => l.slice(0, 200)).join('\n'));
  const dims = ['minecraft:overworld', 'minecraft:the_nether', 'minecraft:the_end'];
  for (const d of dims) { const m = s.mark(); s.cmd(`execute in ${d} run forceload add -128 -128 112 112`); await sleep(9000); console.log(d, s.since(m).split('\n').filter(Boolean).slice(-1)[0]?.slice(0, 120)); }
  s.cmd('save-all flush'); await sleep(8000);
  // BlueMap 5.29 needs the map named when the command comes from the console.
  for (const map of ['world', 'world_the_nether', 'world_the_end']) s.cmd(`bluemap force-update ${map}`);
  const root = s.path('bluemap/web/maps'); let last = -1, stable = 0;
  for (let i = 0; i < 90; i++) {
    await sleep(5000);
    const maps = fs.existsSync(root) ? fs.readdirSync(root) : [];
    const totals = maps.map((m) => `${m}:${count(path.join(root, m, 'tiles'))}`);
    const total = maps.reduce((a, m) => a + count(path.join(root, m, 'tiles')), 0);
    if (i % 4 === 0) console.log(new Date().toISOString().slice(11, 19), totals.join(' '));
    stable = total === last && total > 0 ? stable + 1 : 0; last = total;
    const m = s.mark(); s.cmd('bluemap'); await sleep(800);
    const out = s.since(m);
    if (stable >= 3 && !/Running|running|\d+\/\d+|ETA|% /i.test(out.split('\n').filter((l) => /render|task|update/i.test(l)).join(' '))) { console.log(out.slice(0, 500)); break; }
  }
  for (const d of dims) s.cmd(`execute in ${d} run forceload remove all`);
  // "BMT FAILED maps" is the expected reply while BlueMap is still loading, not an error.
  console.log('errors:', s.errors().filter((l) => !/BMT FAILED maps/.test(l)).slice(0, 12).join('\n').slice(0, 1500) || 'none');
  await s.stop(); console.log('stopped');
  process.exit(0);
})().catch((e) => { console.error('PREP FAILED', e); process.exit(1); });
