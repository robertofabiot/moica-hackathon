import { useQuery } from '@tanstack/react-query';

import {
  buscarServiciosPublicos,
  listarCategoriasPublicas,
  listarDepartamentosPublicos,
  obtenerPrestadorPublico,
  obtenerServicioPublico,
} from '../api';
import type { FiltrosDeBusqueda } from '../tipos';

export const CLAVE_DE_BUSQUEDA = ['busqueda', 'servicios'] as const;
export const CLAVE_DE_CATEGORIAS_PUBLICAS = ['catalogo', 'categorias'] as const;
export const CLAVE_DE_DEPARTAMENTOS_PUBLICOS = ['catalogo', 'departamentos'] as const;

export function claveDeServicioPublico(idServicio: number) {
  return ['busqueda', 'servicio', idServicio] as const;
}

export function claveDePrestadorPublico(idPrestador: number) {
  return ['busqueda', 'prestador', idPrestador] as const;
}

export function useServiciosPublicos(filtros: FiltrosDeBusqueda) {
  return useQuery({
    queryKey: [...CLAVE_DE_BUSQUEDA, filtros],
    queryFn: () => buscarServiciosPublicos(filtros),
    retry: false,
  });
}

export function useServicioPublico(idServicio: number | undefined) {
  return useQuery({
    queryKey: claveDeServicioPublico(idServicio ?? 0),
    queryFn: () => obtenerServicioPublico(idServicio as number),
    enabled: idServicio !== undefined,
    retry: false,
  });
}

export function usePrestadorPublico(idPrestador: number | undefined) {
  return useQuery({
    queryKey: claveDePrestadorPublico(idPrestador ?? 0),
    queryFn: () => obtenerPrestadorPublico(idPrestador as number),
    enabled: idPrestador !== undefined,
    retry: false,
  });
}

export function useCategoriasPublicas() {
  return useQuery({
    queryKey: CLAVE_DE_CATEGORIAS_PUBLICAS,
    queryFn: listarCategoriasPublicas,
    staleTime: Infinity,
  });
}

export function useDepartamentosPublicos() {
  return useQuery({
    queryKey: CLAVE_DE_DEPARTAMENTOS_PUBLICOS,
    queryFn: listarDepartamentosPublicos,
    staleTime: Infinity,
  });
}
