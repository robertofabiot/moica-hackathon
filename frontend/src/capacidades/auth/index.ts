/**
 * Punto de entrada de la capacidad de acceso.
 *
 * El resto de la aplicación usa solo lo que se publica aquí; nadie importa archivos internos de la
 * capacidad.
 */

export { ErrorDeApi } from './api';
export { useSesionActual } from './hooks/useSesionActual';
export { useInicioSesion, useRegistro } from './hooks/useAcceso';
export { default as InicioSesion } from './paginas/InicioSesion';
export { default as Registro } from './paginas/Registro';
export {
  MOTIVO_CUENTA_CREADA,
  MOTIVO_SESION_VENCIDA,
  PARAMETRO_MOTIVO,
  RUTA_INICIO_SESION,
  RUTA_REGISTRO,
  rutaDeInicioSesion,
} from './rutas';
export type { EstadoCuenta, SesionActual, Usuario } from './tipos';
