import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  actualizarServicio,
  actualizarTextoAlternativoDeServicio,
  cambiarEstadoDeServicio,
  crearServicio,
  eliminarImagenDeServicio,
  listarCategoriasDeServicio,
  listarServiciosPropios,
  obtenerServicioPropio,
  reordenarImagenesDeServicio,
  subirImagenDeServicio,
} from '../api';
import type { DatosDeServicio, ServicioPropio } from '../tipos';

export const CLAVE_DE_SERVICIOS_PROPIOS = ['servicio', 'propios'] as const;
export const CLAVE_DE_CATEGORIAS = ['catalogo', 'categorias'] as const;

export function claveDeServicioPropio(idServicio: number) {
  return ['servicio', 'propio', idServicio] as const;
}

export function useServiciosPropios(opciones?: { enabled?: boolean }) {
  return useQuery({
    queryKey: CLAVE_DE_SERVICIOS_PROPIOS,
    queryFn: listarServiciosPropios,
    retry: false,
    staleTime: 60_000,
    enabled: opciones?.enabled ?? true,
  });
}

export function useServicioPropio(idServicio: number | undefined) {
  return useQuery({
    queryKey: claveDeServicioPropio(idServicio ?? 0),
    queryFn: () => obtenerServicioPropio(idServicio as number),
    enabled: idServicio !== undefined,
    retry: false,
  });
}

export function useCategoriasDeServicio() {
  return useQuery({
    queryKey: CLAVE_DE_CATEGORIAS,
    queryFn: listarCategoriasDeServicio,
    staleTime: Infinity,
  });
}

export function useCreacionDeServicio() {
  const invalidar = useInvalidarServicios();
  return useMutation({ mutationFn: crearServicio, onSuccess: invalidar });
}

export function useActualizacionDeServicio() {
  const invalidar = useInvalidarServicios();
  return useMutation({
    mutationFn: ({ idServicio, datos }: { idServicio: number; datos: DatosDeServicio }) =>
      actualizarServicio(idServicio, datos),
    onSuccess: invalidar,
  });
}

export function useCambioDeEstadoDeServicio() {
  const invalidar = useInvalidarServicios();
  return useMutation({
    mutationFn: ({
      idServicio,
      estado,
    }: {
      idServicio: number;
      estado: ServicioPropio['estado'];
    }) => cambiarEstadoDeServicio(idServicio, estado),
    onSuccess: invalidar,
  });
}

export function useSubidaDeImagenDeServicio() {
  const invalidar = useInvalidarServicios();
  return useMutation({
    mutationFn: ({
      idServicio,
      archivo,
      textoAlternativo,
    }: {
      idServicio: number;
      archivo: File;
      textoAlternativo: string;
    }) => subirImagenDeServicio(idServicio, archivo, textoAlternativo),
    onSuccess: invalidar,
  });
}

export function useTextoAlternativoDeServicio() {
  const invalidar = useInvalidarServicios();
  return useMutation({
    mutationFn: ({
      idServicio,
      idImagen,
      textoAlternativo,
    }: {
      idServicio: number;
      idImagen: number;
      textoAlternativo: string | null;
    }) => actualizarTextoAlternativoDeServicio(idServicio, idImagen, textoAlternativo),
    onSuccess: invalidar,
  });
}

export function useOrdenDeImagenesDeServicio() {
  const invalidar = useInvalidarServicios();
  return useMutation({
    mutationFn: ({ idServicio, idsEnOrden }: { idServicio: number; idsEnOrden: number[] }) =>
      reordenarImagenesDeServicio(idServicio, idsEnOrden),
    onSuccess: invalidar,
  });
}

export function useEliminacionDeImagenDeServicio() {
  const invalidar = useInvalidarServicios();
  return useMutation({
    mutationFn: ({ idServicio, idImagen }: { idServicio: number; idImagen: number }) =>
      eliminarImagenDeServicio(idServicio, idImagen),
    onSuccess: invalidar,
  });
}

function useInvalidarServicios() {
  const cliente = useQueryClient();
  return () => {
    void cliente.invalidateQueries({ queryKey: CLAVE_DE_SERVICIOS_PROPIOS });
    void cliente.invalidateQueries({ queryKey: ['servicio', 'propio'] });
  };
}
