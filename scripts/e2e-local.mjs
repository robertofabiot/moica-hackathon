import { execFile, spawn } from "node:child_process";
import { promisify } from "node:util";
import { fileURLToPath } from "node:url";
import { randomUUID } from "node:crypto";

const execFileAsync = promisify(execFile);
const root = fileURLToPath(new URL("../", import.meta.url));
const composeFile = `${root}compose.smoke.yml`;
const envFile = `${root}.env.example`;
const port = process.env.MOICA_E2E_PORT ?? "18081";
const project = `moica-e2e-${randomUUID()}`;
const composeArgs = [
  "compose",
  "--env-file",
  envFile,
  "-f",
  composeFile,
  "-p",
  project,
];
const composeEnv = Object.fromEntries(
  Object.entries(process.env).filter(([name]) => !name.startsWith("MOICA_")),
);
composeEnv.MOICA_SMOKE_PORT = port;
composeEnv.MOICA_SMOKE_COOKIE_SEGURA = "false";
composeEnv.MOICA_SMOKE_PERFIL = "local-e2e";

const docker = (...args) =>
  execFileAsync("docker", [...composeArgs, ...args], {
    cwd: root,
    env: composeEnv,
    maxBuffer: 10 * 1024 * 1024,
  });

async function esperarAplicacion() {
  const origen = `http://127.0.0.1:${port}`;
  const limite = Date.now() + 10 * 60 * 1000;
  let ultimoError = "sin respuesta";

  while (Date.now() < limite) {
    try {
      const respuesta = await fetch(`${origen}/healthz`);
      if (respuesta.ok) {
        const catalogo = await fetch(`${origen}/api/catalogos/categorias`);
        if (catalogo.ok) return;
        ultimoError = `catalogo ${catalogo.status}`;
      } else {
        ultimoError = `health ${respuesta.status}`;
      }
    } catch (error) {
      ultimoError = error instanceof Error ? error.message : String(error);
    }
    await new Promise((resolver) => setTimeout(resolver, 2_000));
  }

  throw new Error(`El entorno E2E no estuvo listo: ${ultimoError}`);
}

let apagando = false;
async function apagar() {
  if (apagando) return;
  apagando = true;
  try {
    await docker("down", "--volumes", "--remove-orphans");
  } catch (error) {
    console.error(error instanceof Error ? error.message : error);
  }
}

try {
  await docker("up", "--build", "--detach");
  await esperarAplicacion();

  process.on("SIGINT", async () => {
    await apagar();
    process.exit(130);
  });
  process.on("SIGTERM", async () => {
    await apagar();
    process.exit(143);
  });

  const child = spawn(process.execPath, [
    fileURLToPath(new URL('../frontend/node_modules/@playwright/test/cli.js', import.meta.url)),
    'test', ...process.argv.slice(2),
  ], {
    cwd: fileURLToPath(new URL('../frontend/', import.meta.url)),
    env: { ...process.env, MOICA_E2E_PROJECT: project, E2E_BASE_URL: `http://127.0.0.1:${port}` },
    stdio: 'inherit',
  });
  process.exitCode = await new Promise((resolve, reject) => {
    child.on('error', reject);
    child.on('exit', (code) => resolve(code ?? 1));
  });
} catch (error) {
  await apagar();
  console.error(error instanceof Error ? error.stack : error);
  process.exitCode = 1;
} finally {
  await apagar();
}
