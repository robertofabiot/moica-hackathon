/**
 * Punto de entrada de la capacidad de acceso.
 *
 * El resto de la aplicación usa solo lo que se publica aquí; nadie importa archivos internos de la
 * capacidad.
 *
 * La infraestructura de red ya no se publica desde aquí: vive en `src/comun/api` y cada capacidad
 * la toma de ahí. Lo que esta capacidad publica son sus propias piezas de acceso.
 */

export { crearClienteDeConsultas } from './clienteDeConsultas';
export { default as AvisoDeEstadoDeCuenta } from './componentes/AvisoDeEstadoDeCuenta';
export { default as RutaProtegida, RutaDeVerificacion } from './componentes/RutaProtegida';
export { useSesionActual } from './hooks/useSesionActual';
export { useCierreSesion, useInicioSesion, useRegistro } from './hooks/useAcceso';
export { useVigilanciaDeSesion } from './hooks/useVigilanciaDeSesion';
export { default as InicioSesion } from './paginas/InicioSesion';
export { default as Registro } from './paginas/Registro';
export { default as SeguridadCuenta } from './paginas/SeguridadCuenta';
export { default as VerificacionSegundoFactor } from './paginas/VerificacionSegundoFactor';
export {
  MOTIVO_CREDENCIALES_CAMBIADAS,
  MOTIVO_CUENTA_CREADA,
  MOTIVO_SESION_VENCIDA,
  PARAMETRO_MOTIVO,
  RUTA_INICIO_SESION,
  RUTA_REGISTRO,
  RUTA_CONFIGURACION,
  RUTA_SEGURIDAD,
  RUTA_VERIFICACION_SEGUNDO_FACTOR,
  rutaDeInicioSesion,
} from './rutas';
export type {
  AvisoDeCuenta,
  EstadoCuenta,
  EstadoSegundoFactor,
  SesionActual,
  Usuario,
} from './tipos';
