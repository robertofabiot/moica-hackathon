/**
 * Forma de lo que devuelve la API administrativa de casos de moderación.
 *
 * El frontend declara sus propios tipos en lugar de reutilizar los del backend: si un día la
 * respuesta cambia, el compilador debe avisar aquí. Los dominios se repiten tal cual los nombra el
 * diccionario de datos, porque un mismo concepto se llama igual en las tres capas.
 *
 * Lo que ya pertenece a la capacidad de solicitudes —el detalle de la solicitud y sus mensajes— se
 * toma de su superficie pública y no se redeclara.
 */

import type { DatosDeSolicitudServicio, MensajeSolicitud } from '../solicitud';

/** Coincide con el dominio `EstadoCasoModeracion` del diccionario. */
export type EstadoDeCaso = 'ABIERTO' | 'EN_REVISION' | 'CERRADO' | 'REABIERTO';

/** Coincide con el dominio `ResultadoCasoModeracion` del diccionario. */
export type ResultadoDeCaso = 'PROCEDENTE' | 'DESESTIMADO';

/** Coincide con el dominio `TipoActorHistorial` del diccionario. */
export type TipoDeActor = 'USUARIO' | 'ADMINISTRADOR' | 'SISTEMA';

/** Coincide con el dominio `TipoEventoHistorial` del diccionario. */
export type TipoDeEvento =
  | 'CASO_ABIERTO'
  | 'RESPONSABLE_ASIGNADO'
  | 'ESTADO_CASO_CAMBIADO'
  | 'RESOLUCION_REGISTRADA'
  | 'MEDIDA_APLICADA'
  | 'MEDIDA_REVOCADA'
  | 'MEDIDA_EXPIRADA'
  | 'ESTADO_CUENTA_CAMBIADO'
  | 'APELACION_PRESENTADA'
  | 'APELACION_ACEPTADA'
  | 'APELACION_RECHAZADA'
  | 'CASO_REABIERTO';

/** Coincide con el dominio `EstadoCuenta` del diccionario. */
export type EstadoDeCuenta =
  'ACTIVA' | 'RESTRINGIDA_TEMPORAL' | 'SUSPENDIDA_TEMPORAL' | 'SUSPENDIDA_PERMANENTE';

/** Una fila de la bandeja: lo justo para priorizar y elegir. */
export interface ResumenDeCaso {
  idCasoModeracion: number;
  idSolicitudServicio: number;
  idReportante: number;
  nombreReportante: string;
  idReportado: number;
  nombreReportado: string;
  motivo: string;
  estadoActual: EstadoDeCaso;
  resultadoActual: ResultadoDeCaso | null;
  idAdministradorResponsable: number | null;
  nombreAdministradorResponsable: string | null;
  fechaApertura: string;
  fechaActualizacion: string;
}

/** Una imagen del servicio contratado: la evidencia material que ya existía del trato. */
export interface ImagenDeServicio {
  idImagenServicioPublicado: number;
  urlImagen: string;
  textoAlternativo: string | null;
  ordenVisualizacion: number;
  fechaCreacion: string;
}

/** Una versión SCD2 del caso, con la fotografía completa de aquel momento. */
export interface VersionDeCaso {
  idHistorialCaso: number;
  numeroVersion: number;
  tipoEvento: TipoDeEvento;
  tipoActor: TipoDeActor;
  idActor: number | null;
  nombreActor: string | null;
  /** Quién respondía por el caso en esta versión. Nulo mientras nadie lo tuviera asignado. */
  idAdministradorResponsable: number | null;
  nombreAdministradorResponsable: string | null;
  estadoCaso: EstadoDeCaso;
  resultadoCaso: ResultadoDeCaso | null;
  estadoCuenta: EstadoDeCuenta;
  resolucion: string | null;
  detalleCambio: string;
  fechaInicioVigencia: string;
  fechaFinVigencia: string | null;
  esVersionActual: boolean;
}

/** El expediente completo. Los mensajes no viajan aquí: tienen su propia consulta. */
export interface ExpedienteDeCaso {
  caso: ResumenDeCaso;
  descripcion: string;
  resolucionActual: string | null;
  solicitud: DatosDeSolicitudServicio;
  imagenesDelServicio: ImagenDeServicio[];
  historial: VersionDeCaso[];
  /** Si esta sesión es la responsable y puede, por tanto, revisar y resolver. */
  puedeResolver: boolean;
}

/** Una persona administradora, para elegirla al asignar. */
export interface Administrador {
  idAdministrador: number;
  nombreCompleto: string;
}

/** Lo que la bandeja está mostrando ahora mismo. */
export interface FiltroDeBandeja {
  estados: EstadoDeCaso[];
  soloMios: boolean;
}

export type { MensajeSolicitud };
