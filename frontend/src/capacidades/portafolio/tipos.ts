/**
 * Forma de lo que devuelve la API del portafolio.
 *
 * El frontend declara sus propios tipos en lugar de reutilizar los del backend: si un día la
 * respuesta cambia, el compilador debe avisar aquí.
 */

/** Una imagen de un trabajo. La fila guarda su URL pública, nunca el binario. */
export interface ImagenDeTrabajo {
  idImagenTrabajoPortafolio: number;
  urlImagen: string;
  textoAlternativo: string | null;
  ordenVisualizacion: number;
  fechaCreacion: string;
}

/** Un trabajo del portafolio con sus imágenes en orden. */
export interface Trabajo {
  idTrabajo: number;
  titulo: string;
  descripcion: string;
  /** Solo si el prestador quiso indicarla. */
  fechaRealizacion: string | null;
  ordenVisualizacion: number;
  imagenes: ImagenDeTrabajo[];
  fechaCreacion: string;
  fechaActualizacion: string;
}

/** Lo que la API acepta al crear o actualizar un trabajo. */
export interface DatosDeTrabajo {
  titulo: string;
  descripcion: string;
  fechaRealizacion: string | null;
}
