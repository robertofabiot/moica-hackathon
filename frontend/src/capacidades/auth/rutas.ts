/**
 * Rutas y avisos de la capacidad de acceso.
 *
 * El motivo por el que se llega a la pantalla de inicio de sesión viaja en la dirección y no en un
 * estado global: así el aviso sobrevive a una recarga y se puede comprobar en una prueba sin
 * montar la aplicación entera.
 */

export const RUTA_REGISTRO = '/registro';
export const RUTA_INICIO_SESION = '/iniciar-sesion';

export const PARAMETRO_MOTIVO = 'motivo';

/** La sesión venció o fue revocada mientras la persona usaba Moica. */
export const MOTIVO_SESION_VENCIDA = 'sesion-vencida';

/** La cuenta acaba de crearse y falta iniciar sesión. */
export const MOTIVO_CUENTA_CREADA = 'cuenta-creada';

export function rutaDeInicioSesion(motivo?: string): string {
  return motivo === undefined
    ? RUTA_INICIO_SESION
    : `${RUTA_INICIO_SESION}?${PARAMETRO_MOTIVO}=${motivo}`;
}
