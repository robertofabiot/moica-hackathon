/**
 * Forma de lo que devuelve la API del perfil de prestador y del catálogo territorial.
 *
 * El frontend declara sus propios tipos en lugar de reutilizar los del backend: si un día la
 * respuesta cambia, el compilador debe avisar aquí.
 */

/** Coincide con el dominio `TipoPrestador` del diccionario. */
export type TipoPrestador = 'INDEPENDIENTE' | 'EMPRENDIMIENTO' | 'PYME';

/** Coincide con el dominio `EstadoDisponibilidad` del diccionario. */
export type EstadoDisponibilidad = 'DISPONIBLE' | 'NO_DISPONIBLE';

/** Coincide con el dominio `NivelVerificacionPrestador` del diccionario. */
export type NivelVerificacion = 'SIN_VERIFICAR' | 'VERIFICADO_BASICO' | 'PROFESIONAL_VERIFICADO';

/** El municipio principal ya descrito con su departamento, listo para mostrarse. */
export interface MunicipioDelPerfil {
  idMunicipio: number;
  nombreMunicipio: string;
  nombreDepartamento: string;
}

/** El perfil de prestador tal como lo entrega la API a su propietario. */
export interface PerfilPrestador {
  idPrestador: number;
  nombrePublico: string;
  urlImagenPerfil: string | null;
  descripcion: string;
  tipoPrestador: TipoPrestador;
  municipioPrincipal: MunicipioDelPerfil;
  descripcionCobertura: string;
  disponibilidad: EstadoDisponibilidad;
  /** Proyección del flujo de verificación (P4V): aquí solo se lee y se explica. */
  nivelVerificacion: NivelVerificacion;
  fechaCreacion: string;
  fechaActualizacion: string;
}

/** Lo que la API acepta al crear o actualizar el perfil. */
export interface DatosDePerfil {
  nombrePublico: string;
  descripcion: string;
  tipoPrestador: TipoPrestador;
  idMunicipioPrincipal: number;
  descripcionCobertura: string;
}

/** Un medio de contacto propio, oculto para terceros hasta que haya una solicitud aceptada. */
export interface MedioContacto {
  idMedioContactoPrestador: number;
  contenido: string;
  ordenVisualizacion: number;
  fechaCreacion: string;
}

/** Un municipio del catálogo territorial. */
export interface Municipio {
  idMunicipio: number;
  nombre: string;
}

/** Un departamento habilitado con sus municipios, tal como lo entrega el catálogo. */
export interface Departamento {
  idDepartamento: number;
  nombre: string;
  municipios: Municipio[];
}
