export type EstadoServicio = 'ACTIVO' | 'INACTIVO';

export interface ImagenDeServicio {
  idImagenServicioPublicado: number;
  urlImagen: string;
  textoAlternativo: string | null;
  ordenVisualizacion: number;
  fechaCreacion: string;
}

export interface ServicioPropio {
  idServicioPublicado: number;
  nombre: string;
  descripcion: string;
  precioReferencia: number | null;
  estado: EstadoServicio;
  idCategoriaServicio: number;
  nombreCategoria: string;
  idSubcategoriaServicio: number;
  nombreSubcategoria: string;
  imagenes: ImagenDeServicio[];
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface DatosDeServicio {
  nombre: string;
  descripcion: string;
  idSubcategoriaServicio: number;
  precioReferencia: number | null;
}

export interface CategoriaDeServicio {
  idCategoriaServicio: number;
  nombre: string;
  descripcion: string | null;
  subcategorias: SubcategoriaDeServicio[];
}

export interface SubcategoriaDeServicio {
  idSubcategoriaServicio: number;
  nombre: string;
  descripcion: string | null;
}
