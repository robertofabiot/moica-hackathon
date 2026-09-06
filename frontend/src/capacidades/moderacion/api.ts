import { enviarJson, obtenerJson } from '../../comun/api';
import type {
  Administrador,
  ExpedienteDeCaso,
  FiltroDeBandeja,
  MedidaAAplicar,
  MedidaACrear,
  MedidaAEditar,
  MedidaAdministrativa,
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
const RUTA_MEDIDAS = '/api/admin/medidas';

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

// --- Catálogo de medidas ---------------------------------------------------

/** El catálogo entero, de la más leve a la más grave, habilitadas y deshabilitadas. */
export function listarMedidas(): Promise<MedidaAdministrativa[]> {
  return obtenerJson(RUTA_MEDIDAS);
}

export function crearMedida(medida: MedidaACrear): Promise<MedidaAdministrativa> {
  return enviarJson('POST', RUTA_MEDIDAS, medida);
}

export function editarMedida(
  idMedida: number,
  medida: MedidaAEditar
): Promise<MedidaAdministrativa> {
  return enviarJson('PUT', `${RUTA_MEDIDAS}/${idMedida}`, medida);
}

/**
 * Habilita o deshabilita una medida.
 *
 * Es lo que el negocio llama «eliminar»: la API no expone ningún `DELETE`, porque una medida citada
 * por un caso o por el historial es la evidencia de una decisión.
 */
export function cambiarHabilitacionDeMedida(
  idMedida: number,
  habilitada: boolean
): Promise<MedidaAdministrativa> {
  return enviarJson('PUT', `${RUTA_MEDIDAS}/${idMedida}/habilitacion`, { habilitada });
}

// --- Medidas y apelaciones de un caso --------------------------------------

/**
 * Aplica una medida a la cuenta reportada.
 *
 * Si la cuenta ya sostiene otra, responde 409 `MEDIDA_VIGENTE_EXISTENTE` y no cambia nada; hay que
 * reenviar con `confirmaReemplazo` para sustituirla.
 */
export function aplicarMedida(idCaso: number, medida: MedidaAAplicar): Promise<ExpedienteDeCaso> {
  return enviarJson('POST', `${RUTA_CASOS}/${idCaso}/medida`, medida);
}

export function revocarMedida(idCaso: number, motivo: string): Promise<ExpedienteDeCaso> {
  return enviarJson('POST', `${RUTA_CASOS}/${idCaso}/medida/revocacion`, { motivo });
}

/** Registra en el expediente una apelación recibida por el canal externo de soporte. */
export function registrarApelacion(idCaso: number, relato: string): Promise<ExpedienteDeCaso> {
  return enviarJson('POST', `${RUTA_CASOS}/${idCaso}/apelacion`, { relato });
}

export function resolverApelacion(
  idCaso: number,
  aceptada: boolean,
  resolucion: string
): Promise<ExpedienteDeCaso> {
  return enviarJson('POST', `${RUTA_CASOS}/${idCaso}/apelacion/resolucion`, {
    aceptada,
    resolucion,
  });
}

/** Devuelve a revisión un caso cerrado cuya apelación fue aceptada. */
export function reabrirCaso(idCaso: number, motivo: string): Promise<ExpedienteDeCaso> {
  return enviarJson('POST', `${RUTA_CASOS}/${idCaso}/reapertura`, { motivo });
}
