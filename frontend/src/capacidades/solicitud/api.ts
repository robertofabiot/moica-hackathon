import { enviarJson, obtenerJson } from '../../comun/api';
import type {
  ContactoRevelado,
  DatosDeContratacion,
  DatosDeSolicitudServicio,
  DepartamentoDeCatalogo,
  MensajeSolicitud,
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
