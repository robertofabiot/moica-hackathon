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
