import { enviarJson, obtenerJson } from '../../comun/api';
import type {
  Administrador,
  ExpedienteDeCaso,
  FiltroDeBandeja,
  MensajeSolicitud,
  ResultadoDeCaso,
  ResumenDeCaso,
} from './tipos';

/**
 * Llamadas a la API administrativa de casos de moderación.
 *
 * Todas cuelgan de `/api/admin`, así que la cadena de seguridad exige rol administrativo y segundo
 * factor verificado en esta sesión antes de que ninguna llegue a ejecutarse.
 */

const RUTA_CASOS = '/api/admin/casos';
const RUTA_ADMINISTRADORES = '/api/admin/administradores';

/** La bandeja con sus filtros. Sin estados, el backend devuelve lo que espera decisión. */
export function listarCasos(filtro: FiltroDeBandeja): Promise<ResumenDeCaso[]> {
  const parametros = new URLSearchParams();
  for (const estado of filtro.estados) {
    parametros.append('estado', estado);
  }
  if (filtro.soloMios) {
    parametros.append('mios', 'true');
  }

  const consulta = parametros.toString();
  return obtenerJson(consulta === '' ? RUTA_CASOS : `${RUTA_CASOS}?${consulta}`);
}

export function obtenerExpediente(idCaso: number): Promise<ExpedienteDeCaso> {
  return obtenerJson(`${RUTA_CASOS}/${idCaso}`);
}

/**
 * El hilo de la solicitud reportada.
 *
 * Se pide por caso y no por solicitud: sin un expediente que lo justifique, el área administrativa
 * no tiene forma de leer una conversación privada.
 */
export function obtenerMensajesDelCaso(idCaso: number): Promise<MensajeSolicitud[]> {
  return obtenerJson(`${RUTA_CASOS}/${idCaso}/mensajes`);
}

export function asignarCaso(idCaso: number, idAdministrador: number): Promise<ExpedienteDeCaso> {
  return enviarJson('POST', `${RUTA_CASOS}/${idCaso}/asignacion`, { idAdministrador });
}

export function iniciarRevisionDelCaso(idCaso: number): Promise<ExpedienteDeCaso> {
  return enviarJson('POST', `${RUTA_CASOS}/${idCaso}/revision`);
}

export function cerrarCaso(
  idCaso: number,
  resultado: ResultadoDeCaso,
  resolucion: string
): Promise<ExpedienteDeCaso> {
  return enviarJson('POST', `${RUTA_CASOS}/${idCaso}/cierre`, { resultado, resolucion });
}

/** Las personas administradoras entre las que puede repartirse un caso. */
export function listarAdministradores(): Promise<Administrador[]> {
  return obtenerJson(RUTA_ADMINISTRADORES);
}
