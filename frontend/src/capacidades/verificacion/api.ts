import { enviarArchivo, enviarJson, obtenerJson } from '../../comun/api';
import type {
  DocumentoElegido,
  EstadoDeVerificacion,
  Expediente,
  FiltroDeCola,
  NivelSolicitado,
  SolicitudDeVerificacion,
} from './tipos';

/** Llamadas a la API de verificación documental, la propia y la administrativa. */

const RUTA_PROPIA = '/api/prestador/verificacion';
const RUTA_SOLICITUDES_PROPIAS = '/api/prestador/verificacion/solicitudes';
const RUTA_REVISION = '/api/admin/verificaciones';

/** Nivel vigente del perfil propio, qué significa y qué puede solicitarse ahora. */
export function obtenerEstadoDeVerificacion(): Promise<EstadoDeVerificacion> {
  return obtenerJson(RUTA_PROPIA);
}

/** Todas las solicitudes propias, la más reciente primero. */
export function listarSolicitudesPropias(): Promise<SolicitudDeVerificacion[]> {
  return obtenerJson(RUTA_SOLICITUDES_PROPIAS);
}

/**
 * Envía la solicitud con su expediente completo en una sola petición.
 *
 * Los archivos viajan en partes `archivo` y sus tipos en campos `tipoDocumento`, en el mismo orden.
 * Es una sola petición a propósito: la solicitud y su expediente nacen juntos o no nacen.
 */
export function enviarExpediente(
  nivelSolicitado: NivelSolicitado,
  documentos: DocumentoElegido[]
): Promise<SolicitudDeVerificacion> {
  const formulario = new FormData();
  formulario.append('nivelSolicitado', nivelSolicitado);

  for (const documento of documentos) {
    formulario.append('archivo', documento.archivo);
  }
  for (const documento of documentos) {
    formulario.append('tipoDocumento', documento.tipoDocumento);
  }

  return enviarArchivo('POST', RUTA_SOLICITUDES_PROPIAS, formulario);
}

/** La cola administrativa con sus filtros. Sin estados, el backend devuelve lo que espera decisión. */
export function listarExpedientes(filtro: FiltroDeCola): Promise<Expediente[]> {
  const parametros = new URLSearchParams();
  for (const estado of filtro.estados) {
    parametros.append('estado', estado);
  }
  if (filtro.nivel !== null) {
    parametros.append('nivel', filtro.nivel);
  }

  const consulta = parametros.toString();
  return obtenerJson(consulta === '' ? RUTA_REVISION : `${RUTA_REVISION}?${consulta}`);
}

export function tomarExpediente(idSolicitudVerificacion: number): Promise<Expediente> {
  return enviarJson('POST', `${RUTA_REVISION}/${idSolicitudVerificacion}/toma`);
}

export function aprobarExpediente(idSolicitudVerificacion: number): Promise<Expediente> {
  return enviarJson('POST', `${RUTA_REVISION}/${idSolicitudVerificacion}/aprobacion`);
}

export function rechazarExpediente(
  idSolicitudVerificacion: number,
  observacion: string
): Promise<Expediente> {
  return enviarJson('POST', `${RUTA_REVISION}/${idSolicitudVerificacion}/rechazo`, {
    observacion,
  });
}

export function revocarExpediente(
  idSolicitudVerificacion: number,
  observacion: string
): Promise<Expediente> {
  return enviarJson('POST', `${RUTA_REVISION}/${idSolicitudVerificacion}/revocacion`, {
    observacion,
  });
}

/**
 * Dirección del endpoint que abre un documento del expediente.
 *
 * No devuelve el archivo ni una dirección firmada: el backend comprueba los permisos y responde una
 * redirección hacia un acceso temporal. Por eso lo que se abre es esta ruta de Moica y no una URL
 * del proveedor, que así nunca pasa por el JavaScript de la aplicación ni queda en su caché.
 */
export function rutaDeAccesoADocumento(
  idSolicitudVerificacion: number,
  idDocumentoVerificacion: number
): string {
  return `${RUTA_REVISION}/${idSolicitudVerificacion}/documentos/${idDocumentoVerificacion}/acceso`;
}
