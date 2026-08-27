import { comoJson, enviar, enviarJson, enviarSinRespuesta, obtenerJson } from '../../comun/api';
import type { ActivacionDeSegundoFactor, SegundoFactor, SesionActual, Usuario } from './tipos';

/**
 * Llamadas a la API de autenticación.
 *
 * La infraestructura de red —tiempos de espera, CSRF, errores uniformes— vive en `src/comun/api`,
 * compartida por todas las capacidades; aquí quedan solo las operaciones de acceso. Se reexporta
 * lo que el resto de la capacidad y sus pruebas ya usaban desde este módulo.
 */

export {
  definirTiempoDeEsperaMs,
  ErrorDeApi,
  MENSAJE_SIN_RESPUESTA,
  obtenerJson,
  TIEMPO_DE_ESPERA_MS,
} from '../../comun/api';

const RUTA_SESION = '/api/auth/sesion';
const RUTA_VERIFICACION = '/api/auth/sesion/segundo-factor';
const RUTA_SEGUNDO_FACTOR = '/api/auth/segundo-factor';
const RUTA_CLAVE = '/api/auth/clave';
const RUTA_USUARIOS = '/api/usuarios';

export interface DatosDeRegistro {
  nombreCompleto: string;
  correoElectronico: string;
  clave: string;
}

export interface DatosDeInicioSesion {
  correoElectronico: string;
  clave: string;
}

export interface DatosDeCambioDeClave {
  claveActual: string;
  claveNueva: string;
}

export interface DatosDeDesactivacion {
  claveActual: string;
  codigo: string;
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
export function registrarUsuario(datos: DatosDeRegistro): Promise<Usuario> {
  return enviarJson('POST', RUTA_USUARIOS, datos);
}

/** Inicia sesión. La cookie con el token la pone el backend en la respuesta. */
export function iniciarSesion(datos: DatosDeInicioSesion): Promise<SesionActual> {
  return enviarJson('POST', RUTA_SESION, datos);
}

/** Cierra la sesión: el backend la revoca y caduca la cookie. */
export function cerrarSesion(): Promise<void> {
  return enviarSinRespuesta('DELETE', RUTA_SESION);
}

/**
 * Completa la sesión en curso presentando el código del segundo factor.
 *
 * Solo afecta a esta sesión: las que estén abiertas en otros dispositivos siguen pendientes.
 */
export function verificarSegundoFactorDeLaSesion(codigo: string): Promise<SesionActual> {
  return enviarJson('POST', RUTA_VERIFICACION, { codigo });
}

/** Estado del segundo factor de la cuenta. Nunca devuelve el secreto. */
export function obtenerSegundoFactor(): Promise<SegundoFactor> {
  return obtenerJson(RUTA_SEGUNDO_FACTOR);
}

/**
 * Empieza la activación del segundo factor.
 *
 * Es la única respuesta que trae el secreto. Quien la reciba debe mostrarlo y olvidarlo: repetir
 * esta llamada genera otro secreto y el anterior deja de valer.
 */
export function iniciarActivacionDeSegundoFactor(): Promise<ActivacionDeSegundoFactor> {
  return enviarJson('POST', RUTA_SEGUNDO_FACTOR, {});
}

/** Confirma la activación con el primer código válido. */
export function confirmarActivacionDeSegundoFactor(codigo: string): Promise<SegundoFactor> {
  return enviarJson('POST', `${RUTA_SEGUNDO_FACTOR}/activacion`, { codigo });
}

/** Desactiva el segundo factor. El backend revoca todas las sesiones de la cuenta. */
export function desactivarSegundoFactor(datos: DatosDeDesactivacion): Promise<void> {
  return enviarSinRespuesta('POST', `${RUTA_SEGUNDO_FACTOR}/desactivacion`, datos);
}

/** Cambia la contraseña. El backend revoca todas las sesiones, incluida esta. */
export function cambiarClave(datos: DatosDeCambioDeClave): Promise<void> {
  return enviarSinRespuesta('PUT', RUTA_CLAVE, datos);
}
