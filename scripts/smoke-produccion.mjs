import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { setTimeout } from 'node:timers/promises';

// Sin dependencias ni secretos productivos. Cada ejecucion crea y retira SOLO
// su proyecto Compose y volumen nuevos; no usa el .env ni la BD del desarrollador.
const project = `moica-p11-${randomUUID().slice(0, 8)}`;
const composeArgs = ['compose', '--env-file', '.env.example', '-f', 'compose.smoke.yml', '-p', project];
const origin = `http://127.0.0.1:${process.env.MOICA_SMOKE_PORT || 18080}`;
const docker = (...args) => execFileSync('docker', [...composeArgs, ...args], { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
const check = (condition, label) => { if (!condition) throw new Error(label); };
const cookies = new Map();
async function request(path, { method = 'GET', body, csrf = true, headers = {} } = {}) {
  const response = await fetch(origin + path, {
    method, redirect: 'manual', signal: AbortSignal.timeout(10000),
    headers: {
      Cookie: [...cookies].map(([key, value]) => `${key}=${value}`).join('; '),
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(csrf && cookies.has('XSRF-TOKEN') ? { 'X-XSRF-TOKEN': decodeURIComponent(cookies.get('XSRF-TOKEN')) } : {}),
      ...headers,
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  for (const cookie of response.headers.getSetCookie()) {
    const [name, ...value] = cookie.split(';')[0].split('=');
    cookies.set(name, value.join('='));
  }
  return response;
}
async function waitForBackend() {
  for (let attempt = 0; attempt < 120; attempt++) {
    try {
      const response = await request('/actuator/health');
      if (response.status === 200 && (await response.json()).status === 'UP') return;
    } catch { /* El proceso puede estar arrancando o migrando. */ }
    await setTimeout(2000);
  }
  throw new Error('Backend no alcanzo UP en 240 segundos');
}
const sql = query => docker('exec', '-T', 'postgres', 'psql', '-U', 'moica_dev', '-d', 'moica_db', '-tAc', query).trim();

try {
  docker('up', '-d', ...(process.argv.includes('--no-build') ? ['--no-build'] : ['--build']));
  await waitForBackend();
  check(sql('SELECT count(*) FROM flyway_schema_history WHERE success') === '15', 'Deben aplicarse las 15 migraciones');
  check(sql("SELECT count(*) FROM flyway_schema_history WHERE version IN ('52', '90') AND success") === '2', 'Faltan V52 o V90');
  check(sql('SELECT count(*) FROM usuario') === '0', 'La base debe nacer sin usuarios');
  const history = sql('SELECT string_agg(version, \',\' ORDER BY installed_rank) FROM flyway_schema_history WHERE success');
  console.log(`PASS PostgreSQL limpio: 15 migraciones (${history})`);
  docker('exec', '-T', 'frontend', 'nginx', '-t');
  for (const path of ['/healthz', '/', '/explorar', '/iniciar-sesion', '/manifest.webmanifest', '/sw.js', '/icono-192.png', '/icono-512.png']) {
    const response = await request(path);
    check(response.status === 200, `No carga ${path}`);
    if (path.endsWith('.js')) check(response.headers.get('content-type')?.includes('javascript'), 'SW debe ser JavaScript');
  }
  const home = await (await request('/')).text();
  const assetPaths = [...home.matchAll(/(?:src|href)="(\/assets\/[^\"]+)"/g)].map(match => match[1]);
  check(assetPaths.length > 0, 'Faltan assets versionados');
  for (const path of assetPaths) {
    const response = await request(path);
    check(response.status === 200 && response.headers.get('cache-control')?.includes('immutable'), 'Asset no cacheable/versionado');
  }
  check(await (await request('/explorar')).text() === home, 'Falla fallback SPA');
  for (const path of ['/actuator/env', '/.env', '/assets/no-existe.js', '/no-existe.js']) {
    check((await request(path)).status === 404, `Ruta no debe publicarse: ${path}`);
  }
  const health = await request('/actuator/health', { headers: {
    'X-Forwarded-Proto': 'http', 'X-Forwarded-Host': 'attacker.invalid',
    Forwarded: 'proto=http;host=attacker.invalid',
  } });
  check(JSON.stringify(await health.json()) === '{"status":"UP"}', 'Health revela detalles');
  check(health.headers.has('strict-transport-security'), 'Spring no reconoce HTTPS o confia en headers externos');
  const search = await request('/api/servicios');
  check(search.status === 200 && search.headers.get('content-type')?.includes('json'), 'API no llega al backend');
  check(!search.headers.has('access-control-allow-origin'), 'No debe abrir CORS');
  check(search.headers.get('cache-control') === 'no-store', 'API no debe cachearse');
  console.log('PASS Nginx, SPA directa, PWA/assets, API mismo origen, health y headers saneados');

  const credentials = { correoElectronico: `smoke-${randomUUID()}@example.org`, clave: `Moica!${randomUUID()}` };
  const register = { ...credentials, nombreCompleto: 'Prueba Docker' };
  check((await request('/api/usuarios', { method: 'POST', body: register, csrf: false })).status === 403, 'Registro debe exigir CSRF');
  check((await request('/api/usuarios', { method: 'POST', body: register })).status === 201, 'Registro con CSRF falla');
  const login = await request('/api/auth/sesion', { method: 'POST', body: credentials });
  check(login.status === 201, 'Login falla');
  const sessionCookie = login.headers.getSetCookie().find(value => value.startsWith('moica_sesion='));
  check(sessionCookie && /; HttpOnly/i.test(sessionCookie) && /; Secure/i.test(sessionCookie) && /; SameSite=Lax/i.test(sessionCookie), 'Atributos de cookie incorrectos');
  const savedSession = cookies.get('moica_sesion');
  check((await request('/api/auth/sesion')).status === 200, 'Sesion no valida');
  check((await request('/api/auth/sesion', { method: 'DELETE', csrf: false })).status === 403, 'Logout debe exigir CSRF');

  docker('restart', 'backend');
  await waitForBackend();
  check(sql('SELECT count(*) FROM usuario') === '1', 'Reinicio pierde usuarios');
  check(sql('SELECT string_agg(version, \',\' ORDER BY installed_rank) FROM flyway_schema_history WHERE success') === history, 'Reinicio altera migraciones');
  check((await request('/api/auth/sesion')).status === 200, 'Sesion no persiste tras reinicio');
  check((await request('/api/auth/sesion', { method: 'DELETE' })).status === 204, 'Logout falla');
  cookies.set('moica_sesion', savedSession);
  check((await request('/api/auth/sesion')).status === 401, 'JWT revocado debe rechazarse');
  console.log('PASS registro/login, cookie HttpOnly/Secure/Lax, CSRF, persistencia tras reinicio y revocacion');
  console.log('NOTA: transporte local HTTP con proxy simulando terminacion HTTPS; TLS publico y R2 requieren Railway real.');
} catch (error) {
  // No imprimir respuestas/cookies, entorno ni stdout/stderr de subprocessos.
  console.error(`FAIL ${error instanceof Error ? error.message.split('\n')[0] : 'smoke'}`);
  process.exitCode = 1;
} finally {
  docker('down', '-v', '--remove-orphans');
}
