/**
 * Punto de entrada de la capacidad de solicitudes de servicio.
 *
 * El resto de la aplicación usa solo lo que se publica aquí; nadie importa archivos internos.
 */

export { default as AccionDeSolicitud } from './componentes/AccionDeSolicitud';
export { default as DetalleDeSolicitud } from './paginas/DetalleDeSolicitud';
export { default as MisSolicitudes } from './paginas/MisSolicitudes';
export { default as NuevaSolicitud } from './paginas/NuevaSolicitud';
export {
  RUTA_DETALLE_SOLICITUD,
  RUTA_NUEVA_SOLICITUD,
  RUTA_SOLICITUDES,
  rutaDeNuevaSolicitud,
  rutaDeSolicitud,
} from './rutas';
