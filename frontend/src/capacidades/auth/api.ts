import type { CuerpoDeError, ErrorDeCampo, SesionActual, Usuario } from './tipos';

/**
 * Llamadas a la API de autenticación.
 *
 * Todas usan rutas relativas: en producción el frontend y la API comparten origen y en desarrollo
 * el proxy de Vite reenvía `/api` a Spring Boot. Por eso también basta con `same-origin` para las
 * credenciales: la cookie de sesión viaja sola y ningún script puede leerla.
 */

const RUTA_SESION = '/api/auth/sesion';
const RUTA_USUARIOS = '/api/usuarios';

const COOKIE_CSRF = 'XSRF-TOKEN';
const CABECERA_CSRF = 'X-XSRF-TOKEN';

const MENSAJE_SIN_RESPUESTA =
  'No pudimos comunicarnos con Moica. Revisa tu conexión e inténtalo otra vez.';

/** Error de la API con la forma uniforme que devuelve el backend. */
export class ErrorDeApi extends Error {
  readonly estado: number;
  readonly codigo: string;
  readonly errores: ErrorDeCampo[];

  constructor(estado: number, codigo: string, mensaje: string, errores: ErrorDeCampo[] = []) {
    super(mensaje);
    this.name = 'ErrorDeApi';
    this.estado = estado;
    this.codigo = codigo;
    this.errores = errores;
  }
}

export interface DatosDeRegistro {
  nombreCompleto: string;
  correoElectronico: string;
  clave: string;
}

export interface DatosDeInicioSesion {
  correoElectronico: string;
  clave: string;
}

/** Consulta la sesión en curso. Devuelve `null` cuando no hay ninguna vigente. */
export async function obtenerSesionActual(): Promise<SesionActual | null> {
  const respuesta = await enviar('GET', RUTA_SESION);

  if (respuesta.status === 401) {
    return null;
  }
  return (await comoJson(respuesta)) as SesionActual;
}

/** Crea una cuenta. No inicia sesión: autenticarse es un paso aparte. */
export async function registrarUsuario(datos: DatosDeRegistro): Promise<Usuario> {
  const respuesta = await enviar('POST', RUTA_USUARIOS, datos);
  return (await comoJson(respuesta)) as Usuario;
}

/** Inicia sesión. La cookie con el token la pone el backend en la respuesta. */
export async function iniciarSesion(datos: DatosDeInicioSesion): Promise<SesionActual> {
  const respuesta = await enviar('POST', RUTA_SESION, datos);
  return (await comoJson(respuesta)) as SesionActual;
}

async function enviar(metodo: string, ruta: string, cuerpo?: unknown): Promise<Response> {
  const cabeceras: Record<string, string> = {};

  if (cuerpo !== undefined) {
    cabeceras['Content-Type'] = 'application/json';
  }
  if (metodo !== 'GET') {
    const token = await asegurarTokenCsrf();
    if (token !== null) {
      cabeceras[CABECERA_CSRF] = token;
    }
  }

  try {
    return await fetch(ruta, {
      method: metodo,
      credentials: 'same-origin',
      headers: cabeceras,
      body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
    });
  } catch {
    // `fetch` solo falla así cuando la petición no llegó a completarse.
    throw new ErrorDeApi(0, 'SIN_RESPUESTA', MENSAJE_SIN_RESPUESTA);
  }
}

/**
 * Devuelve el token CSRF, pidiéndoselo al backend si el navegador todavía no lo tiene.
 *
 * El backend emite la cookie `XSRF-TOKEN` en cualquier respuesta, así que consultar la sesión
 * actual basta para conseguirla. Sin este paso, una operación mutable hecha nada más abrir la
 * aplicación se quedaría sin token.
 */
async function asegurarTokenCsrf(): Promise<string | null> {
  const guardado = leerCookie(COOKIE_CSRF);
  if (guardado !== null) {
    return guardado;
  }

  try {
    await fetch(RUTA_SESION, { credentials: 'same-origin' });
  } catch {
    throw new ErrorDeApi(0, 'SIN_RESPUESTA', MENSAJE_SIN_RESPUESTA);
  }
  return leerCookie(COOKIE_CSRF);
}

function leerCookie(nombre: string): string | null {
  const prefijo = `${nombre}=`;
  const encontrada = document.cookie
    .split(';')
    .map((cookie) => cookie.trim())
    .find((cookie) => cookie.startsWith(prefijo));

  return encontrada === undefined ? null : decodeURIComponent(encontrada.slice(prefijo.length));
}

async function comoJson(respuesta: Response): Promise<unknown> {
  if (!respuesta.ok) {
    throw await comoErrorDeApi(respuesta);
  }
  return respuesta.json();
}

async function comoErrorDeApi(respuesta: Response): Promise<ErrorDeApi> {
  const cuerpo = await leerCuerpoDeError(respuesta);

  return new ErrorDeApi(
    respuesta.status,
    cuerpo?.codigo ?? 'ERROR_DESCONOCIDO',
    cuerpo?.mensaje ?? 'No pudimos completar la operación. Inténtalo de nuevo.',
    cuerpo?.errores ?? []
  );
}

async function leerCuerpoDeError(respuesta: Response): Promise<CuerpoDeError | null> {
  try {
    return (await respuesta.json()) as CuerpoDeError;
  } catch {
    // Un error sin cuerpo JSON sigue siendo un error: se usa el mensaje genérico.
    return null;
  }
}
