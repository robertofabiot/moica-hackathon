export type EstadoSolicitud = 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'CANCELADA' | 'COMPLETADA';

export interface CambioEstadoSolicitud {
  idCambioEstadoSolicitud: number;
  estadoAnterior: EstadoSolicitud | null;
  estadoNuevo: EstadoSolicitud;
  idActor: number;
  nombreActor: string;
  motivo: string | null;
  fechaCambio: string;
}

export interface ResumenDeSolicitudServicio {
  idSolicitudServicio: number;
  idServicioPublicado: number;
  nombreServicio: string;
  idCliente: number;
  nombreCliente: string;
  idPrestador: number;
  nombrePublicoPrestador: string;
  idMunicipio: number;
  nombreMunicipio: string;
  estadoActual: EstadoSolicitud;
  fechaPreferida: string | null;
  fechaCreacion: string;
}

export interface DatosDeSolicitudServicio extends ResumenDeSolicitudServicio {
  nombreDepartamento: string;
  descripcionNecesidad: string;
  indicacionUbicacion: string;
  fechaActualizacion: string;
  historial: CambioEstadoSolicitud[];
}

export interface MunicipioDeCatalogo {
  idMunicipio: number;
  nombre: string;
}

export interface DepartamentoDeCatalogo {
  idDepartamento: number;
  nombre: string;
  municipios: MunicipioDeCatalogo[];
}

export interface DatosDeContratacion {
  idServicioPublicado: number;
  descripcionNecesidad: string;
  idMunicipio: number;
  indicacionUbicacion: string;
  fechaPreferida: string | null;
}

export interface MensajeSolicitud {
  idMensajeSolicitud: number;
  idSolicitudServicio: number;
  idRemitente: number;
  nombreRemitente: string;
  contenido: string;
  fechaEnvio: string;
}

export interface ContactoRevelado {
  idMedioContactoPrestador: number;
  contenido: string;
  ordenVisualizacion: number;
}

/** Rol en que quedó calificada la contraparte. Lo deriva el backend, nunca el navegador. */
export type RolCalificado = 'CLIENTE' | 'PRESTADOR';

/** Una calificación ya emitida. En el MVP no se edita ni se borra. */
export interface DatosDeCalificacion {
  idCalificacionUsuario: number;
  idSolicitudServicio: number;
  idCalificador: number;
  idCalificado: number;
  rolCalificado: RolCalificado;
  puntuacion: number;
  comentario: string | null;
  fechaCreacion: string;
}

/**
 * Qué puede hacer la sesión con la calificación de una solicitud.
 *
 * `puedeCalificar` lo decide el servidor —solicitud completada, sin calificación
 * previa y cuenta activa—. Ocultar el formulario no autoriza nada: el envío se
 * vuelve a comprobar en el backend.
 */
export interface EstadoDeCalificacion {
  idSolicitudServicio: number;
  solicitudCompletada: boolean;
  idCalificado: number;
  nombreCalificado: string;
  rolCalificado: RolCalificado;
  puedeCalificar: boolean;
  calificacionEmitida: DatosDeCalificacion | null;
}

/** Lo único que el navegador envía al calificar: el resto lo pone el servidor. */
export interface CalificacionAEmitir {
  puntuacion: number;
  comentario: string | null;
}
