import { enviarJson, obtenerJson } from '../../comun/api';
import type {
  DatosDeContratacion,
  DatosDeSolicitudServicio,
  DepartamentoDeCatalogo,
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
