/**
 * Forma de lo que devuelve la API de autenticación.
 *
 * El frontend declara sus propios tipos en lugar de reutilizar los del backend: si un día la
 * respuesta cambia, el compilador debe avisar aquí.
 */

/** Estado operativo de una cuenta. Coincide con el dominio `EstadoCuenta` del diccionario. */
export type EstadoCuenta =
  'ACTIVA' | 'RESTRINGIDA_TEMPORAL' | 'SUSPENDIDA_TEMPORAL' | 'SUSPENDIDA_PERMANENTE';

/** Datos públicos de una cuenta. Nunca incluye la contraseña ni su hash. */
export interface Usuario {
  idUsuario: number;
  nombreCompleto: string;
  correoElectronico: string;
  estadoCuenta: EstadoCuenta;
  fechaRegistro: string;
}

/** Vigencia de la sesión en curso. */
export interface VigenciaDeSesion {
  fechaInicio: string;
  fechaExpiracion: string;
  segundoFactorVerificado: boolean;
}

/** Respuesta de la API cuando hay una sesión activa. */
export interface SesionActual {
  usuario: Usuario;
  sesion: VigenciaDeSesion;
}

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
