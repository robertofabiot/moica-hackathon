import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  aceptarSolicitud,
  cancelarSolicitud,
  completarSolicitud,
  crearSolicitud,
  listarEnviadas,
  obtenerCatalogoTerritorial,
  listarRecibidas,
  obtenerSolicitud,
  rechazarSolicitud,
} from '../api';

export const CLAVE_DE_CATALOGO = ['catalogo', 'departamentos'] as const;
export const CLAVE_DE_ENVIADAS = ['solicitud', 'enviadas'] as const;
export const CLAVE_DE_RECIBIDAS = ['solicitud', 'recibidas'] as const;

export function claveDeSolicitud(idSolicitud: number) {
  return ['solicitud', 'detalle', idSolicitud] as const;
}

export function useCatalogoDeMunicipios() {
  return useQuery({
    queryKey: CLAVE_DE_CATALOGO,
    queryFn: obtenerCatalogoTerritorial,
    staleTime: Infinity,
  });
}

export function useSolicitudesEnviadas() {
  return useQuery({
    queryKey: CLAVE_DE_ENVIADAS,
    queryFn: listarEnviadas,
    retry: false,
    staleTime: 30_000,
  });
}

export function useSolicitudesRecibidas() {
  return useQuery({
    queryKey: CLAVE_DE_RECIBIDAS,
    queryFn: listarRecibidas,
    retry: false,
    staleTime: 30_000,
  });
}

export function useSolicitud(idSolicitud: number | undefined) {
  return useQuery({
    queryKey: claveDeSolicitud(idSolicitud ?? 0),
    queryFn: () => obtenerSolicitud(idSolicitud as number),
    enabled: idSolicitud !== undefined,
    retry: false,
  });
}

export function useCreacionDeSolicitud() {
  const invalidar = useInvalidarSolicitudes();
  return useMutation({ mutationFn: crearSolicitud, onSuccess: invalidar });
}

export function useAceptacionDeSolicitud() {
  const invalidar = useInvalidarSolicitudes();
  return useMutation({ mutationFn: aceptarSolicitud, onSuccess: invalidar });
}

export function useRechazoDeSolicitud() {
  const invalidar = useInvalidarSolicitudes();
  return useMutation({ mutationFn: rechazarSolicitud, onSuccess: invalidar });
}

export function useCancelacionDeSolicitud() {
  const invalidar = useInvalidarSolicitudes();
  return useMutation({
    mutationFn: ({ idSolicitud, motivo }: { idSolicitud: number; motivo?: string }) =>
      cancelarSolicitud(idSolicitud, motivo),
    onSuccess: invalidar,
  });
}

export function useCompletadoDeSolicitud() {
  const invalidar = useInvalidarSolicitudes();
  return useMutation({ mutationFn: completarSolicitud, onSuccess: invalidar });
}

function useInvalidarSolicitudes() {
  const cliente = useQueryClient();
  return () => {
    void cliente.invalidateQueries({ queryKey: CLAVE_DE_ENVIADAS });
    void cliente.invalidateQueries({ queryKey: CLAVE_DE_RECIBIDAS });
    void cliente.invalidateQueries({ queryKey: ['solicitud', 'detalle'] });
  };
}
