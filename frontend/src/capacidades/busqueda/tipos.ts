import type { NivelVerificacion } from '../verificacion';

/** Cuántas calificaciones recibió una puntuación concreta. */
export interface TramoDeReputacion {
  estrellas: number;
  cantidad: number;
}

/**
 * Reputación de una persona en un rol, calculada por el backend.
 *
 * `promedio` es `null` cuando todavía no hay calificaciones: la interfaz lo
 * presenta como «Sin calificaciones» y nunca dibuja un `0.0`. El desglose llega
 * siempre con las cinco filas, de cinco a una estrella.
 */
export interface ReputacionPorRol {
  rol: 'CLIENTE' | 'PRESTADOR';
  promedio: number | null;
  cantidad: number;
  desglose: TramoDeReputacion[];
}

export interface ImagenPublicaDeServicio {
  idImagenServicioPublicado: number;
  urlImagen: string;
  textoAlternativo: string | null;
  ordenVisualizacion: number;
  fechaCreacion: string;
}

export interface MunicipioPublico {
  idMunicipio: number;
  nombreMunicipio: string;
  nombreDepartamento: string;
}

export interface PrestadorPublico {
  idPrestador: number;
  nombrePublico: string;
  urlImagenPerfil: string | null;
  descripcion: string;
  tipoPrestador: 'INDEPENDIENTE' | 'EMPRENDIMIENTO' | 'PYME';
  municipioPrincipal: MunicipioPublico;
  descripcionCobertura: string;
  disponibilidad: 'DISPONIBLE' | 'NO_DISPONIBLE';
  nivelVerificacion: NivelVerificacion;
  significadoVerificacion: string;
  advertenciaDeInsignia: string;
}

export interface ResumenPublicoDeServicio {
  idServicioPublicado: number;
  nombre: string;
  descripcion: string;
  precioReferencia: number | null;
  idCategoriaServicio: number;
  nombreCategoria: string;
  idSubcategoriaServicio: number;
  nombreSubcategoria: string;
  imagenPrincipal: ImagenPublicaDeServicio | null;
  prestador: PrestadorPublico;
  reputacionPrestador: ReputacionPorRol;
}

export interface DetallePublicoDeServicio {
  idServicioPublicado: number;
  nombre: string;
  descripcion: string;
  precioReferencia: number | null;
  idCategoriaServicio: number;
  nombreCategoria: string;
  idSubcategoriaServicio: number;
  nombreSubcategoria: string;
  imagenes: ImagenPublicaDeServicio[];
  admiteContratacion: boolean;
  prestador: PrestadorPublico;
  reputacionPrestador: ReputacionPorRol;
}

export interface TrabajoPublico {
  idTrabajo: number;
  titulo: string;
  descripcion: string;
  fechaRealizacion: string | null;
  ordenVisualizacion: number;
  imagenes: Array<{
    idImagenTrabajoPortafolio: number;
    urlImagen: string;
    textoAlternativo: string | null;
    ordenVisualizacion: number;
    fechaCreacion: string;
  }>;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface PerfilPublico {
  prestador: PrestadorPublico;
  portafolio: TrabajoPublico[];
  servicios: ResumenPublicoDeServicio[];
  admiteContratacion: boolean;
  reputacionPrestador: ReputacionPorRol;
}

export interface FiltrosDeBusqueda {
  texto: string;
  idCategoria: string;
  idSubcategoria: string;
  idMunicipio: string;
  precioMaximo?: string;
}

export interface DepartamentoPublico {
  idDepartamento: number;
  nombre: string;
  municipios: Array<{ idMunicipio: number; nombre: string }>;
}

export interface SubcategoriaPublica {
  idSubcategoriaServicio: number;
  nombre: string;
  descripcion: string | null;
}

export interface CategoriaPublica {
  idCategoriaServicio: number;
  nombre: string;
  descripcion: string | null;
  subcategorias: SubcategoriaPublica[];
}
