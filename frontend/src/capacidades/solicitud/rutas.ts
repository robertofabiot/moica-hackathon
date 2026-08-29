export const RUTA_SOLICITUDES = '/solicitudes';
export const RUTA_DETALLE_SOLICITUD = '/solicitudes/:idSolicitud';
export const RUTA_NUEVA_SOLICITUD = '/explorar/servicios/:idServicio/solicitar';

export function rutaDeSolicitud(idSolicitud: number): string {
  return `/solicitudes/${idSolicitud}`;
}

export function rutaDeNuevaSolicitud(idServicio: number): string {
  return `/explorar/servicios/${idServicio}/solicitar`;
}
