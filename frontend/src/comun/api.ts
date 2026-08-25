/**
 * Infraestructura de red compartida por todas las capacidades.
 *
 * Vivía en la capacidad de acceso mientras solo ella la usaba; con `admin` y ahora `prestador` y
 * `portafolio` como consumidoras, se muda aquí tal como anticipaba su propia nota. Todas las
 * llamadas usan rutas relativas: en producción el frontend y la API comparten origen y en
 * desarrollo el proxy de Vite reenvía `/api` a Spring Boot. Por eso también basta con
 * `same-origin` para las credenciales: la cookie de sesión viaja sola y ningún script puede
 * leerla.
 */

const RUTA_SESION = '/api/auth/sesion';

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

/** Detalle de un campo que el backend no admitió. */
export interface ErrorDeCampo {
  campo: string;
  mensaje: string;
}

/** Cuerpo uniforme con el que la API responde a cualquier error. */
export interface CuerpoDeError {
  instante: string;
  estado: number;
  codigo: string;
  mensaje: string;
  ruta: string;
  errores?: ErrorDeCampo[];
}

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

/** Lee un recurso JSON. */
export async function obtenerJson<T>(ruta: string): Promise<T> {
  const respuesta = await enviar('GET', ruta);
  return (await comoJson(respuesta)) as T;
}

/** Envía un cuerpo JSON y devuelve la respuesta JSON. */
export async function enviarJson<T>(metodo: string, ruta: string, cuerpo?: unknown): Promise<T> {
  const respuesta = await enviar(metodo, ruta, cuerpo);
  return (await comoJson(respuesta)) as T;
}

/** Envía una operación cuya respuesta correcta no trae cuerpo, como un `DELETE` con 204. */
export async function enviarSinRespuesta(
  metodo: string,
  ruta: string,
  cuerpo?: unknown
): Promise<void> {
  const respuesta = await enviar(metodo, ruta, cuerpo);

  if (!respuesta.ok) {
    throw await comoErrorDeApi(respuesta);
  }
}

/**
 * Envía un formulario multipart, como la subida de una imagen.
 *
 * El `Content-Type` no se escribe a mano: el navegador lo pone él mismo con la frontera del
 * formulario. El token CSRF sí viaja, igual que en cualquier otra operación mutable.
 */
export async function enviarArchivo<T>(
  metodo: string,
  ruta: string,
  formulario: FormData
): Promise<T> {
  const cabeceras: Record<string, string> = {};
  const token = await asegurarTokenCsrf();
  if (token !== null) {
    cabeceras[CABECERA_CSRF] = token;
  }

  const respuesta = await fetchConTiempoDeEspera(ruta, {
    method: metodo,
    credentials: 'same-origin',
    headers: cabeceras,
    body: formulario,
  });

  return (await comoJson(respuesta)) as T;
}

/** La petición cruda, para los pocos casos que necesitan decidir según el estado HTTP. */
export async function enviar(metodo: string, ruta: string, cuerpo?: unknown): Promise<Response> {
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

/** Traduce la respuesta a JSON, o al {@link ErrorDeApi} uniforme si no fue bien. */
export async function comoJson(respuesta: Response): Promise<unknown> {
  if (!respuesta.ok) {
    throw await comoErrorDeApi(respuesta);
  }
  return respuesta.json();
}

export async function comoErrorDeApi(respuesta: Response): Promise<ErrorDeApi> {
  const cuerpo = await leerCuerpoDeError(respuesta);

  return new ErrorDeApi(
    respuesta.status,
    cuerpo?.codigo ?? 'ERROR_DESCONOCIDO',
    cuerpo?.mensaje ?? 'No pudimos completar la operación. Inténtalo de nuevo.',
    cuerpo?.errores ?? []
  );
}

/**
 * Envuelve `fetch` con el tiempo de espera del cliente.
 *
 * Sin esto, un `DELETE` hecho sin red (o a través de un proxy que no corta) puede
 * quedarse pendiente para siempre y dejar el botón en «Cerrando sesión…».
 *
 * El temporizador rechaza la carrera directamente: en Offline de DevTools,
 * `fetch` a `localhost` puede quedar colgado sin rechazar ni disparar un
 * `abort` fiable; depender solo del evento `abort` deja la mutación pendiente.
 */
async function fetchConTiempoDeEspera(ruta: string, opciones: RequestInit): Promise<Response> {
  if (typeof navigator !== 'undefined' && navigator.onLine === false) {
    throw new ErrorDeApi(0, 'SIN_RESPUESTA', MENSAJE_SIN_RESPUESTA);
  }

  const controlador = new AbortController();
  const opcionesConSenal: RequestInit = { ...opciones, signal: controlador.signal };

  let temporizador: ReturnType<typeof setTimeout> | undefined;

  const limiteDeTiempo = new Promise<never>((_, reject) => {
    temporizador = setTimeout(() => {
      controlador.abort();
      reject(new ErrorDeApi(0, 'TIEMPO_AGOTADO', MENSAJE_TIEMPO_AGOTADO));
    }, tiempoDeEsperaMs);
  });

  const peticion = fetch(ruta, opcionesConSenal);

  try {
    return await Promise.race([peticion, limiteDeTiempo]);
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
    if (temporizador !== undefined) {
      clearTimeout(temporizador);
    }
    // Si `fetch` sigue colgado tras el aborto, su rechazo tardío no debe quedar sin capturar.
    void peticion.catch(() => undefined);
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

async function leerCuerpoDeError(respuesta: Response): Promise<CuerpoDeError | null> {
  let temporizador: ReturnType<typeof setTimeout> | undefined;

  const limiteDeTiempo = new Promise<never>((_, reject) => {
    temporizador = setTimeout(
      () => reject(new ErrorDeApi(0, 'TIEMPO_AGOTADO', MENSAJE_TIEMPO_AGOTADO)),
      tiempoDeEsperaMs
    );
  });

  try {
    return (await Promise.race([respuesta.json(), limiteDeTiempo])) as CuerpoDeError;
  } catch (error) {
    if (error instanceof ErrorDeApi) {
      throw error;
    }
    // Un error sin cuerpo JSON sigue siendo un error: se usa el mensaje genérico.
    return null;
  } finally {
    if (temporizador !== undefined) {
      clearTimeout(temporizador);
    }
  }
}
