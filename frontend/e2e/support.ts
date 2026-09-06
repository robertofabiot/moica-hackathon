import { randomUUID } from 'node:crypto';
import { TOTP } from 'otpauth';
import { execFile } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';
import { request, type APIRequestContext, type Page } from '@playwright/test';

const execFileAsync = promisify(execFile);
const root = fileURLToPath(new URL('../..', import.meta.url));
const composeFile = `${root}/compose.smoke.yml`;
const envFile = `${root}/.env.example`;
const composeProject = process.env.MOICA_E2E_PROJECT ?? 'moica-e2e';
const composeArgs = ['compose', '--env-file', envFile, '-f', composeFile, '-p', composeProject];
const baseURL = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:18081';

export const E2E_PASSWORD = `Moica1!${randomUUID()}`;

export type CuentaE2E = {
  nombre: string;
  correo: string;
  clave: string;
  idUsuario: number;
};

export type ServicioE2E = CuentaE2E & {
  idServicio: number;
  nombreServicio: string;
};

type CatalogoDepartamento = {
  municipios: Array<{ idMunicipio: number }>;
};

type CatalogoCategoria = {
  subcategorias: Array<{ idSubcategoriaServicio: number }>;
};

type RespuestaDeSesion = {
  usuario: { idUsuario: number };
};

const db = leerVariablesDeEjemplo();

export function identificadorUnico(prefijo: string): string {
  return `${prefijo}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
}

export async function crearCuentaApi(prefijo: string, nombre = 'Persona E2E') {
  const api = await request.newContext({ baseURL });
  const correo = `${identificadorUnico(prefijo)}@example.org`;
  const cuenta = await registrar(api, nombre, correo);
  await api.dispose();
  return cuenta;
}

export async function crearPrestadorApi(prefijo: string, verificado = true): Promise<ServicioE2E> {
  const api = await request.newContext({ baseURL });
  const nombre = `Prestador ${prefijo}`;
  const cuenta = await registrar(api, nombre, `${identificadorUnico(prefijo)}@example.org`);
  await iniciarSesion(api, cuenta);

  const departamentos = (await consultar(
    api,
    '/api/catalogos/departamentos'
  )) as CatalogoDepartamento[];
  const categorias = (await consultar(api, '/api/catalogos/categorias')) as CatalogoCategoria[];
  const idMunicipio = departamentos[0]?.municipios[0]?.idMunicipio;
  const idSubcategoria = categorias[0]?.subcategorias[0]?.idSubcategoriaServicio;
  if (idMunicipio === undefined || idSubcategoria === undefined) {
    await api.dispose();
    throw new Error('El catálogo E2E no tiene municipio o subcategoría');
  }

  await mutar(api, 'POST', '/api/prestador/perfil', {
    nombrePublico: nombre,
    descripcion: 'Servicios técnicos preparados para la demostración E2E.',
    tipoPrestador: 'INDEPENDIENTE',
    idMunicipioPrincipal: idMunicipio,
    descripcionCobertura: 'Managua y alrededores.',
  });
  await mutar(api, 'POST', '/api/prestador/contactos', { contenido: '8888-0000' });

  const nombreServicio = `Reparación E2E ${prefijo}`;
  const servicio = (await mutar(api, 'POST', '/api/prestador/servicios', {
    nombre: nombreServicio,
    descripcion: 'Servicio de prueba visible para los recorridos críticos.',
    idSubcategoriaServicio: idSubcategoria,
    precioReferencia: 450,
  })) as { idServicioPublicado: number };

  if (verificado)
    await ejecutarSql(
      `UPDATE perfil_prestador SET nivel_verificacion = 'VERIFICADO_BASICO' WHERE id_prestador = ${cuenta.idUsuario};\n` +
        `UPDATE servicio_publicado SET estado = 'ACTIVO' WHERE id_servicio_publicado = ${servicio.idServicioPublicado};`
    );
  await api.dispose();

  return { ...cuenta, idServicio: servicio.idServicioPublicado, nombreServicio };
}

export async function crearAdministradorApi(prefijo: string): Promise<CuentaE2E> {
  const api = await request.newContext({ baseURL });
  const cuenta = await registrar(
    api,
    `Administración ${prefijo}`,
    `${identificadorUnico(`admin-${prefijo}`)}@example.org`
  );
  await ejecutarSql(
    `INSERT INTO administrador (id_administrador) VALUES (${cuenta.idUsuario}) ON CONFLICT DO NOTHING;`
  );
  await api.dispose();
  return cuenta;
}

export async function activarSegundoFactorApi(cuenta: CuentaE2E): Promise<string> {
  const api = await request.newContext({ baseURL });
  await iniciarSesion(api, cuenta);
  const activacion = (await mutar(api, 'POST', '/api/auth/segundo-factor', {})) as {
    claveManual: string;
  };
  await mutar(api, 'POST', '/api/auth/segundo-factor/activacion', {
    codigo: codigoTotp(activacion.claveManual),
  });
  await api.dispose();
  return activacion.claveManual;
}

export async function crearCasoModeracionApi(prefijo: string) {
  const servicio = await crearPrestadorApi(`caso-${prefijo}`);
  const apiCliente = await request.newContext({ baseURL });
  const cliente = await registrar(
    apiCliente,
    `Cliente ${prefijo}`,
    `${identificadorUnico(`cliente-${prefijo}`)}@example.org`
  );
  await iniciarSesion(apiCliente, cliente);
  const departamentos = (await consultar(
    apiCliente,
    '/api/catalogos/departamentos'
  )) as CatalogoDepartamento[];
  const idMunicipio = departamentos[0]?.municipios[0]?.idMunicipio;
  if (idMunicipio === undefined) {
    await apiCliente.dispose();
    throw new Error('El catálogo E2E no tiene municipio');
  }

  const solicitud = (await mutar(apiCliente, 'POST', '/api/solicitudes', {
    idServicioPublicado: servicio.idServicio,
    descripcionNecesidad: 'Necesito revisar una instalación del hogar.',
    idMunicipio,
    indicacionUbicacion: 'Barrio E2E, casa con portón azul.',
    fechaPreferida: null,
  })) as { idSolicitudServicio: number };
  await apiCliente.dispose();

  const apiPrestador = await request.newContext({ baseURL });
  await iniciarSesion(apiPrestador, servicio);
  await mutar(
    apiPrestador,
    'POST',
    `/api/solicitudes/${solicitud.idSolicitudServicio}/aceptacion`,
    {}
  );
  await apiPrestador.dispose();

  const apiClienteActivo = await request.newContext({ baseURL });
  await iniciarSesion(apiClienteActivo, cliente);
  await mutar(
    apiClienteActivo,
    'POST',
    `/api/solicitudes/${solicitud.idSolicitudServicio}/mensajes`,
    { contenido: 'Mensaje de evidencia para el expediente.' }
  );
  await mutar(
    apiClienteActivo,
    'POST',
    `/api/solicitudes/${solicitud.idSolicitudServicio}/caso-moderacion`,
    { motivo: 'Trato irrespetuoso', descripcion: 'El caso E2E requiere revisión administrativa.' }
  );
  await apiClienteActivo.dispose();

  await ejecutarSql(
    `INSERT INTO medida_administrativa (codigo, nombre, descripcion, nivel_severidad, estado_cuenta_resultante, requiere_fecha_fin) ` +
      `VALUES ('E2E_SUSPENSION', 'Suspensión E2E', 'Medida de prueba del recorrido administrativo.', 1, 'SUSPENDIDA_PERMANENTE', FALSE) ` +
      `ON CONFLICT (codigo) DO NOTHING;`
  );

  return { servicio, cliente, idSolicitud: solicitud.idSolicitudServicio };
}

export async function crearSolicitudPendienteApi(servicio: ServicioE2E) {
  const api = await request.newContext({ baseURL });
  const cliente = await registrar(
    api,
    'Cliente responsive',
    `${identificadorUnico('responsive')}@example.org`
  );
  await iniciarSesion(api, cliente);
  const departamentos = (await consultar(
    api,
    '/api/catalogos/departamentos'
  )) as CatalogoDepartamento[];
  const idMunicipio = departamentos[0]?.municipios[0]?.idMunicipio;
  if (idMunicipio === undefined) {
    await api.dispose();
    throw new Error('El catálogo E2E no tiene municipio');
  }
  const solicitud = (await mutar(api, 'POST', '/api/solicitudes', {
    idServicioPublicado: servicio.idServicio,
    descripcionNecesidad: 'Solicitud para comprobar el detalle responsive.',
    idMunicipio,
    indicacionUbicacion: 'Zona responsive.',
    fechaPreferida: null,
  })) as { idSolicitudServicio: number };
  await api.dispose();
  return { cliente, idSolicitud: solicitud.idSolicitudServicio };
}

export async function seedVerificationRequest(idPrestador: number): Promise<number> {
  const salida = await consultarSql(
    `INSERT INTO solicitud_verificacion_prestador (id_prestador, nivel_solicitado) ` +
      `VALUES (${idPrestador}, 'BASICA') RETURNING id_solicitud_verificacion;`
  );
  const id = Number(salida.trim().split(/\r?\n/)[0]);
  if (!Number.isInteger(id)) throw new Error('No se pudo crear la solicitud de verificación E2E');
  await ejecutarSql(
    `INSERT INTO documento_verificacion_prestador ` +
      `(id_solicitud_verificacion, tipo_documento, clave_almacenamiento, nombre_original, tipo_mime, tamano_bytes) ` +
      `VALUES (${id}, 'IDENTIDAD', 'e2e/documento.png', 'documento-e2e.png', 'image/png', 64);`
  );
  return id;
}

export async function iniciarSesionEnPagina(
  page: Page,
  cuenta: CuentaE2E,
  secretoTotp?: string
): Promise<void> {
  await page.goto('/iniciar-sesion');
  await page.getByLabel('Correo electrónico').fill(cuenta.correo);
  await page.getByLabel('Contraseña', { exact: true }).fill(cuenta.clave);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();
  if (secretoTotp !== undefined) {
    await page.waitForURL('**/verificar-segundo-factor');
    await page.getByLabel('Código de verificación').fill(codigoTotp(secretoTotp));
    await page.getByRole('button', { name: 'Verificar y entrar' }).click();
    await page.waitForURL('**/');
  } else {
    await page.waitForURL('**/');
  }
}

export function codigoTotp(secreto: string, instante = Date.now()): string {
  return new TOTP({ secret: secreto, algorithm: 'SHA1', digits: 6, period: 30 }).generate({
    timestamp: instante,
  });
}

async function registrar(
  api: APIRequestContext,
  nombre: string,
  correo: string
): Promise<CuentaE2E> {
  const clave = E2E_PASSWORD;
  const respuesta = await mutar(api, 'POST', '/api/usuarios', {
    nombreCompleto: nombre,
    correoElectronico: correo,
    clave,
  });
  const datos = respuesta as { idUsuario: number };
  return { nombre, correo, clave, idUsuario: datos.idUsuario };
}

async function iniciarSesion(
  api: APIRequestContext,
  cuenta: CuentaE2E
): Promise<RespuestaDeSesion> {
  return (await mutar(api, 'POST', '/api/auth/sesion', {
    correoElectronico: cuenta.correo,
    clave: cuenta.clave,
  })) as RespuestaDeSesion;
}

async function consultar(api: APIRequestContext, ruta: string): Promise<unknown> {
  const respuesta = await api.get(ruta);
  if (!respuesta.ok()) throw new Error(`GET ${ruta} respondió ${respuesta.status()}`);
  return respuesta.json();
}

export async function mutar(
  api: APIRequestContext,
  metodo: 'POST' | 'PUT' | 'DELETE',
  ruta: string,
  datos: unknown
): Promise<unknown> {
  const token = await tokenCsrf(api);
  const respuesta = await api.fetch(ruta, {
    method: metodo,
    headers: { 'content-type': 'application/json', 'x-xsrf-token': token },
    data: datos,
  });
  if (!respuesta.ok()) {
    throw new Error(`${metodo} ${ruta} respondió ${respuesta.status()}: ${await respuesta.text()}`);
  }
  return respuesta.status() === 204 ? null : respuesta.json();
}

async function tokenCsrf(api: APIRequestContext): Promise<string> {
  const sesion = await api.get('/api/auth/sesion');
  const catalogo = await api.get('/api/catalogos/categorias');
  const estado = await api.storageState();
  const cookie = estado.cookies.find((c) => c.name === 'XSRF-TOKEN');

  if (cookie === undefined) {
    throw new Error(
      `El entorno E2E no entregó la cookie CSRF (sesión ${sesion.status()}, ` +
        `Set-Cookie sesión=${sesion.headers()['set-cookie'] !== undefined}, ` +
        `Set-Cookie catálogo=${catalogo.headers()['set-cookie'] !== undefined})`
    );
  }
  return decodeURIComponent(cookie.value);
}

export async function ejecutarSql(sql: string): Promise<void> {
  await execFileAsync(
    'docker',
    [
      ...composeArgs,
      'exec',
      '-T',
      'postgres',
      'psql',
      '-U',
      db.usuario,
      '-d',
      db.nombre,
      '-v',
      'ON_ERROR_STOP=1',
      '-c',
      sql,
    ],
    { cwd: root }
  );
}

async function consultarSql(sql: string): Promise<string> {
  const salida = await execFileAsync(
    'docker',
    [
      ...composeArgs,
      'exec',
      '-T',
      'postgres',
      'psql',
      '-U',
      db.usuario,
      '-d',
      db.nombre,
      '-v',
      'ON_ERROR_STOP=1',
      '-t',
      '-A',
      '-c',
      sql,
    ],
    { cwd: root }
  );
  return salida.stdout;
}

function leerVariablesDeEjemplo(): { usuario: string; nombre: string } {
  const contenido = readFileSync(envFile, 'utf8');
  const valor = (nombre: string): string => {
    const linea = contenido.split(/\r?\n/).find((actual) => actual.startsWith(`${nombre}=`));
    if (linea === undefined) throw new Error(`Falta ${nombre} en .env.example`);
    return linea.slice(nombre.length + 1).trim();
  };
  return { usuario: valor('MOICA_DB_USUARIO'), nombre: valor('MOICA_DB_NOMBRE') };
}
