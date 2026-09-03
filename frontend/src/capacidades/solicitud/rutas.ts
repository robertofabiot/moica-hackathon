export const RUTA_SOLICITUDES = '/solicitudes';
export const RUTA_DETALLE_SOLICITUD = '/solicitudes/:idSolicitud';
export const RUTA_NUEVA_SOLICITUD = '/explorar/servicios/:idServicio/solicitar';
export const RUTA_MENSAJES = '/mensajes';

export function rutaDeSolicitud(idSolicitud: number): string {
  return `/solicitudes/${idSolicitud}`;
}

export function rutaDeNuevaSolicitud(idServicio: number): string {
  return `/explorar/servicios/${idServicio}/solicitar`;
}

export function rutaDeMensajes(idSolicitud?: number): string {
  return idSolicitud === undefined ? RUTA_MENSAJES : `${RUTA_MENSAJES}?solicitud=${idSolicitud}`;
}

export function idSeleccionadoDeParametros(parametros: URLSearchParams): number | undefined {
  const crudo = parametros.get('solicitud');
  if (crudo === null || crudo === '') {
    return undefined;
  }
  const id = Number(crudo);
  return Number.isInteger(id) && id > 0 ? id : undefined;
}
