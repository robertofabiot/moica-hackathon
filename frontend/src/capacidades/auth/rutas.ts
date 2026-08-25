/**
 * Rutas y avisos de la capacidad de acceso.
 *
 * El motivo por el que se llega a la pantalla de inicio de sesión viaja en la dirección y no en un
 * estado global: así el aviso sobrevive a una recarga y se puede comprobar en una prueba sin
 * montar la aplicación entera.
 */

export const RUTA_REGISTRO = '/registro';
export const RUTA_INICIO_SESION = '/iniciar-sesion';
export const RUTA_SEGURIDAD = '/seguridad';
export const RUTA_VERIFICACION_SEGUNDO_FACTOR = '/verificar-segundo-factor';

export const PARAMETRO_MOTIVO = 'motivo';

/** La sesión venció o fue revocada mientras la persona usaba Moica. */
export const MOTIVO_SESION_VENCIDA = 'sesion-vencida';

/** La cuenta acaba de crearse y falta iniciar sesión. */
export const MOTIVO_CUENTA_CREADA = 'cuenta-creada';

/**
 * Las credenciales acaban de cambiar y todas las sesiones quedaron revocadas.
 *
 * Es lo mismo que ocurre al cambiar la contraseña y al desactivar el segundo factor: el backend
 * revoca todo con motivo `CAMBIO_CREDENCIALES` y hay que volver a entrar.
 */
export const MOTIVO_CREDENCIALES_CAMBIADAS = 'credenciales-cambiadas';

export function rutaDeInicioSesion(motivo?: string): string {
  return motivo === undefined
    ? RUTA_INICIO_SESION
    : `${RUTA_INICIO_SESION}?${PARAMETRO_MOTIVO}=${motivo}`;
}
