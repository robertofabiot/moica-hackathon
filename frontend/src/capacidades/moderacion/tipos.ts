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
  /** La medida que el caso sostenía en esa versión. `null` cuando no sostenía ninguna. */
  idMedidaAdministrativa: number | null;
  /**
   * Nombre de aquella medida.
   *
   * Se resuelve aunque la medida se haya deshabilitado después: una medida retirada del catálogo
   * sigue describiendo correctamente las decisiones que la citaron, porque nunca se borra.
   */
  nombreMedida: string | null;
  fechaFinMedida: string | null;
  resolucion: string | null;
  detalleCambio: string;
  fechaInicioVigencia: string;
  fechaFinVigencia: string | null;
  esVersionActual: boolean;
}

/** Una medida del catálogo, tal como la administra el área administrativa. */
export interface MedidaAdministrativa {
  idMedidaAdministrativa: number;
  codigo: string;
  nombre: string;
  descripcion: string | null;
  /**
   * Ordena las medidas para quien decide y nada más.
   *
   * Es descriptivo: Moica no recomienda medidas ni escala sanciones por severidad.
   */
  nivelSeveridad: number;
  /** `null` cuando la medida no cambia el acceso, como una advertencia. */
  estadoCuentaResultante: EstadoDeCuenta | null;
  requiereFechaFin: boolean;
  /** Una deshabilitada sigue describiendo decisiones pasadas pero ya no se ofrece. */
  habilitada: boolean;
}

/**
 * La única medida que la cuenta reportada sostiene ahora mismo.
 *
 * La regla de una sola medida vigente es de la **cuenta**, no del expediente, así que puede venir de
 * otro caso. Es lo que permite advertir del reemplazo antes de que el backend responda 409.
 */
export interface MedidaVigenteDeCuenta {
  idCasoModeracion: number;
  esDeEsteCaso: boolean;
  idMedidaAdministrativa: number;
  codigo: string;
  nombre: string;
  estadoCuentaResultante: EstadoDeCuenta | null;
  fechaFinMedida: string | null;
}

/** En qué punto va la apelación del caso, deducida de su historial. */
export type EstadoDeApelacion = 'SIN_APELACION' | 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA';

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
  /** El estado operativo que la cuenta reportada tiene ahora mismo. */
  estadoCuentaReportada: EstadoDeCuenta;
  /** `null` si la cuenta no sostiene ninguna medida. */
  medidaVigente: MedidaVigenteDeCuenta | null;
  apelacion: EstadoDeApelacion;
}

/** Lo que se envía para aplicar una medida a la cuenta reportada. */
export interface MedidaAAplicar {
  idMedidaAdministrativa: number;
  /** Obligatoria cuando la medida lo exige y prohibida cuando no. */
  fechaFinMedida: string | null;
  justificacion: string;
  /**
   * La confirmación explícita que exige sustituir una medida vigente.
   *
   * Sin ella, una segunda aplicación responde 409 y no sustituye nada.
   */
  confirmaReemplazo: boolean;
}

/** Lo que se envía para crear una medida del catálogo. */
export interface MedidaACrear {
  codigo: string;
  nombre: string;
  descripcion: string | null;
  nivelSeveridad: number;
  estadoCuentaResultante: EstadoDeCuenta | null;
  requiereFechaFin: boolean;
}

/** Lo que se envía para reescribir una medida. El código no se toca: identifica decisiones. */
export type MedidaAEditar = Omit<MedidaACrear, 'codigo'>;

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
