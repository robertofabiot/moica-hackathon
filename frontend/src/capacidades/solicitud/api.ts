import { enviarJson, obtenerJson } from '../../comun/api';
import type {
  CalificacionAEmitir,
  ContactoRevelado,
  DatosDeCasoModeracion,
  DatosDeContratacion,
  DatosDeSolicitudServicio,
  DepartamentoDeCatalogo,
  EstadoDeCalificacion,
  EstadoDeReporte,
  DatosDeCalificacion,
  MensajeSolicitud,
  ReporteAPresentar,
  ResumenDeSolicitudServicio,
} from './tipos';

const RUTA = '/api/solicitudes';
const RUTA_CATALOGO = '/api/catalogos/departamentos';

export function obtenerCatalogoTerritorial(): Promise<DepartamentoDeCatalogo[]> {
  return obtenerJson(RUTA_CATALOGO);
}

export function listarEnviadas(): Promise<ResumenDeSolicitudServicio[]> {
  return obtenerJson(`${RUTA}/enviadas`);
}

export function listarRecibidas(): Promise<ResumenDeSolicitudServicio[]> {
  return obtenerJson(`${RUTA}/recibidas`);
}

export function obtenerSolicitud(idSolicitud: number): Promise<DatosDeSolicitudServicio> {
  return obtenerJson(`${RUTA}/${idSolicitud}`);
}

export function crearSolicitud(datos: DatosDeContratacion): Promise<DatosDeSolicitudServicio> {
  return enviarJson('POST', RUTA, datos);
}

export function aceptarSolicitud(idSolicitud: number): Promise<DatosDeSolicitudServicio> {
  return enviarJson('POST', `${RUTA}/${idSolicitud}/aceptacion`, {});
}

export function rechazarSolicitud(idSolicitud: number): Promise<DatosDeSolicitudServicio> {
  return enviarJson('POST', `${RUTA}/${idSolicitud}/rechazo`, {});
}

export function cancelarSolicitud(
  idSolicitud: number,
  motivo?: string
): Promise<DatosDeSolicitudServicio> {
  return enviarJson(
    'POST',
    `${RUTA}/${idSolicitud}/cancelacion`,
    motivo === undefined ? {} : { motivo }
  );
}

export function completarSolicitud(idSolicitud: number): Promise<DatosDeSolicitudServicio> {
  return enviarJson('POST', `${RUTA}/${idSolicitud}/completado`, {});
}

/** El hilo completo de una solicitud, en orden cronológico. */
export function listarMensajes(idSolicitud: number): Promise<MensajeSolicitud[]> {
  return obtenerJson(`${RUTA}/${idSolicitud}/mensajes`);
}

/** Agrega un mensaje. El remitente lo pone el backend a partir de la sesión. */
export function enviarMensaje(idSolicitud: number, contenido: string): Promise<MensajeSolicitud> {
  return enviarJson('POST', `${RUTA}/${idSolicitud}/mensajes`, { contenido });
}

/**
 * Los contactos externos del prestador, revelados al cliente de una solicitud aceptada.
 *
 * Es una superficie propia: no hay ninguna ruta para consultar los contactos de un prestador
 * cualquiera, y por eso tampoco viajan en el detalle ni en las bandejas.
 */
export function obtenerContactosRevelados(idSolicitud: number): Promise<ContactoRevelado[]> {
  return obtenerJson(`${RUTA}/${idSolicitud}/contactos`);
}

/** A quién califica la sesión, en qué rol y si todavía puede hacerlo. */
export function obtenerEstadoDeCalificacion(idSolicitud: number): Promise<EstadoDeCalificacion> {
  return obtenerJson(`${RUTA}/${idSolicitud}/calificacion`);
}

/** Emite la calificación. El calificado y su rol los pone el backend, no este cuerpo. */
export function enviarCalificacion(
  idSolicitud: number,
  calificacion: CalificacionAEmitir
): Promise<DatosDeCalificacion> {
  return enviarJson('POST', `${RUTA}/${idSolicitud}/calificacion`, calificacion);
}

/** A quién puede reportar la sesión, si la solicitud lo admite y qué caso abrió, si abrió uno. */
export function obtenerEstadoDeReporte(idSolicitud: number): Promise<EstadoDeReporte> {
  return obtenerJson(`${RUTA}/${idSolicitud}/caso-moderacion`);
}

/** Abre el caso de moderación. El reportado lo pone el backend, no este cuerpo. */
export function enviarReporte(
  idSolicitud: number,
  reporte: ReporteAPresentar
): Promise<DatosDeCasoModeracion> {
  return enviarJson('POST', `${RUTA}/${idSolicitud}/caso-moderacion`, reporte);
}
