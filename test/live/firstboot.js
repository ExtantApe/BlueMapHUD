// Starts a new server once so Paper and BlueMap write their default files, then stops it.
const { Server } = require('./lib.js');
const [dir, jar, port] = process.argv.slice(2);
(async () => { const s = new Server(dir, jar, +port); await s.start(); await s.stop(); console.log('first boot done'); process.exit(0); })()
  .catch((e) => { console.error('FIRST BOOT FAILED', e); process.exit(1); });
