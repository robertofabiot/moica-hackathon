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

/** Evita que una petición se quede colgada (por ejemplo, al perder la red) y deje la interfaz en carga. */
export const TIEMPO_DE_ESPERA_MS = 10_000;

let tiempoDeEsperaMs = TIEMPO_DE_ESPERA_MS;

/** Las pruebas acortan la espera para no depender de diez segundos reales. */
export function definirTiempoDeEsperaMs(milisegundos: number): void {
  tiempoDeEsperaMs = milisegundos;
}

export const MENSAJE_SIN_RESPUESTA =
  'No pudimos comunicarnos con Moica. Revisa tu conexión e inténtalo otra vez.';

const MENSAJE_TIEMPO_AGOTADO =
  'Tardamos demasiado en obtener respuesta. Revisa tu conexión e inténtalo otra vez.';

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

/** Cierra la sesión: el backend la revoca y caduca la cookie. */
export async function cerrarSesion(): Promise<void> {
  const respuesta = await enviar('DELETE', RUTA_SESION);

  if (!respuesta.ok) {
    throw await comoErrorDeApi(respuesta);
  }
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

  return fetchConTiempoDeEspera(ruta, {
    method: metodo,
    credentials: 'same-origin',
    headers: cabeceras,
    body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
  });
}

/**
 * Envuelve `fetch` con el tiempo de espera del cliente.
 *
 * Sin esto, un `DELETE` hecho sin red (o a través de un proxy que no corta) puede
 * quedarse pendiente para siempre y dejar el botón en «Cerrando sesión…».
 *
 * El `Promise.race` es necesario porque, en Offline de DevTools, `abort()` a
 * veces no hace que `fetch` rechace: si solo esperáramos su promesa, la
 * interfaz seguiría en carga aunque el temporizador ya hubiera vencido.
 */
async function fetchConTiempoDeEspera(ruta: string, opciones: RequestInit): Promise<Response> {
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    throw new ErrorDeApi(0, 'SIN_RESPUESTA', MENSAJE_SIN_RESPUESTA);
  }

  const controlador = new AbortController();
  const temporizador = setTimeout(() => controlador.abort(), tiempoDeEsperaMs);

  const peticion = fetch(ruta, { ...opciones, signal: controlador.signal });
  const limite = new Promise<never>((_, reject) => {
    const vencer = () => reject(new ErrorDeApi(0, 'TIEMPO_AGOTADO', MENSAJE_TIEMPO_AGOTADO));
    if (controlador.signal.aborted) {
      vencer();
      return;
    }
    controlador.signal.addEventListener('abort', vencer, { once: true });
  });

  try {
    return await Promise.race([peticion, limite]);
  } catch (error) {
    if (error instanceof ErrorDeApi) {
      throw error;
    }
    if (controlador.signal.aborted) {
      throw new ErrorDeApi(0, 'TIEMPO_AGOTADO', MENSAJE_TIEMPO_AGOTADO);
    }
    // `fetch` solo falla así cuando la petición no llegó a completarse.
    throw new ErrorDeApi(0, 'SIN_RESPUESTA', MENSAJE_SIN_RESPUESTA);
  } finally {
    clearTimeout(temporizador);
    // Si `fetch` sigue colgado tras el aborto, su rechazo tardío no debe quedar sin capturar.
    void peticion.catch(() => undefined);
    void limite.catch(() => undefined);
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

  await fetchConTiempoDeEspera(RUTA_SESION, { credentials: 'same-origin' });
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
