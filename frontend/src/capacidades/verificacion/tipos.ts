/**
 * Forma de lo que devuelve la API de verificación documental.
 *
 * El frontend declara sus propios tipos en lugar de reutilizar los del backend: si un día la
 * respuesta cambia, el compilador debe avisar aquí. Los dominios se repiten tal cual los nombra el
 * diccionario de datos, porque un mismo concepto se llama igual en las tres capas.
 */

/** Coincide con el dominio `NivelVerificacionPrestador` del diccionario. */
export type NivelVerificacion = 'SIN_VERIFICAR' | 'VERIFICADO_BASICO' | 'PROFESIONAL_VERIFICADO';

/** Coincide con el dominio `NivelVerificacionSolicitado` del diccionario. */
export type NivelSolicitado = 'BASICA' | 'PROFESIONAL';

/** Coincide con el dominio `EstadoSolicitudVerificacion` del diccionario. */
export type EstadoDeSolicitud = 'PENDIENTE' | 'EN_REVISION' | 'APROBADA' | 'RECHAZADA' | 'REVOCADA';

/** Coincide con el dominio `TipoDocumentoVerificacion` del diccionario. */
export type TipoDeDocumento =
  'IDENTIDAD' | 'CERTIFICACION' | 'CONSTANCIA' | 'REGISTRO_NEGOCIO' | 'OTRO_RESPALDO';

/** Coincide con el dominio `TipoPrestador` del diccionario. */
export type TipoPrestador = 'INDEPENDIENTE' | 'EMPRENDIMIENTO' | 'PYME';

/**
 * Metadatos de un documento del expediente.
 *
 * Nunca llega la clave de almacenamiento ni una dirección del archivo: el binario solo lo abre una
 * persona administradora, y con un acceso temporal que se pide en cada apertura.
 */
export interface DocumentoDeVerificacion {
  idDocumentoVerificacion: number;
  tipoDocumento: TipoDeDocumento;
  nombreOriginal: string;
  tipoMime: string;
  tamanoBytes: number;
  fechaCarga: string;
}

/** Una solicitud propia, tal como la ve quien la presentó. */
export interface SolicitudDeVerificacion {
  idSolicitudVerificacion: number;
  nivelSolicitado: NivelSolicitado;
  estadoSolicitud: EstadoDeSolicitud;
  /** El motivo del rechazo o de la revocación; `null` en el resto de estados. */
  observacionResolucion: string | null;
  fechaSolicitud: string;
  fechaInicioRevision: string | null;
  fechaResolucion: string | null;
  documentos: DocumentoDeVerificacion[];
}

/** Dónde está el perfil propio dentro del flujo de verificación. */
export interface EstadoDeVerificacion {
  nivelVerificacion: NivelVerificacion;
  significado: string;
  puedeSolicitarBasica: boolean;
  puedeSolicitarProfesional: boolean;
  solicitudAbierta: SolicitudDeVerificacion | null;
}

/** Quién presenta el expediente, tal como lo ve la persona que revisa. */
export interface PrestadorDelExpediente {
  idPrestador: number;
  nombrePublico: string;
  tipoPrestador: TipoPrestador;
  nivelVerificacion: NivelVerificacion;
  nombreCompleto: string;
  correoElectronico: string;
}

/** Una solicitud vista desde el área administrativa. */
export interface Expediente {
  idSolicitudVerificacion: number;
  prestador: PrestadorDelExpediente;
  nivelSolicitado: NivelSolicitado;
  estadoSolicitud: EstadoDeSolicitud;
  observacionResolucion: string | null;
  /** Quién tiene asignada la revisión; `null` mientras está pendiente. */
  idAdministradorRevisor: number | null;
  fechaSolicitud: string;
  fechaInicioRevision: string | null;
  fechaResolucion: string | null;
  documentos: DocumentoDeVerificacion[];
}

/** Un archivo elegido en el navegador y todavía sin enviar. */
export interface DocumentoElegido {
  /** Identificador local, solo para poder quitarlo de la lista y pintar la clave de React. */
  id: string;
  archivo: File;
  tipoDocumento: TipoDeDocumento;
}

/** Qué se pide a la cola administrativa. */
export interface FiltroDeCola {
  estados: EstadoDeSolicitud[];
  nivel: NivelSolicitado | null;
}
