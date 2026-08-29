import { obtenerJson } from '../../comun/api';
import type {
  CategoriaPublica,
  DepartamentoPublico,
  DetallePublicoDeServicio,
  FiltrosDeBusqueda,
  PerfilPublico,
  ResumenPublicoDeServicio,
} from './tipos';

export async function buscarServiciosPublicos(
  filtros: FiltrosDeBusqueda
): Promise<ResumenPublicoDeServicio[]> {
  const parametros = new URLSearchParams();
  if (filtros.texto.trim() !== '') {
    parametros.set('texto', filtros.texto.trim());
  }
  if (filtros.idCategoria !== '') {
    parametros.set('idCategoria', filtros.idCategoria);
  }
  if (filtros.idSubcategoria !== '') {
    parametros.set('idSubcategoria', filtros.idSubcategoria);
  }
  if (filtros.idMunicipio !== '') {
    parametros.set('idMunicipio', filtros.idMunicipio);
  }
  const consulta = parametros.toString();
  return obtenerJson<ResumenPublicoDeServicio[]>(
    consulta === '' ? '/api/servicios' : `/api/servicios?${consulta}`
  );
}

export async function obtenerServicioPublico(
  idServicio: number
): Promise<DetallePublicoDeServicio> {
  return obtenerJson<DetallePublicoDeServicio>(`/api/servicios/${idServicio}`);
}

export async function obtenerPrestadorPublico(idPrestador: number): Promise<PerfilPublico> {
  return obtenerJson<PerfilPublico>(`/api/prestadores/${idPrestador}`);
}

export async function listarDepartamentosPublicos(): Promise<DepartamentoPublico[]> {
  return obtenerJson<DepartamentoPublico[]>('/api/catalogos/departamentos');
}

export async function listarCategoriasPublicas(): Promise<CategoriaPublica[]> {
  return obtenerJson<CategoriaPublica[]>('/api/catalogos/categorias');
}
