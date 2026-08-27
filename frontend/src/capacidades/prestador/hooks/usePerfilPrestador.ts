import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  actualizarPerfil,
  cambiarDisponibilidad,
  crearPerfil,
  eliminarImagenDePerfil,
  obtenerCatalogoTerritorial,
  obtenerPerfil,
  subirImagenDePerfil,
} from '../api';
import type { PerfilPrestador } from '../tipos';

/**
 * Estado remoto del perfil de prestador propio.
 *
 * Todas las mutaciones devuelven el perfil resultante y lo dejan en la misma clave de consulta,
 * así que ninguna pantalla necesita refetch para verse al día. Al cerrar sesión, la limpieza
 * general de `cacheDeSesion` descarta estas claves como cualquier otra privada.
 */

/** Clave con la que React Query guarda el perfil propio. */
export const CLAVE_DE_PERFIL = ['prestador', 'perfil'] as const;

/** Clave del catálogo territorial. */
export const CLAVE_DE_CATALOGO = ['catalogo', 'departamentos'] as const;

/** El perfil propio; `data` vale `null` mientras la cuenta no lo haya creado. */
export function usePerfilPrestador() {
  return useQuery({
    queryKey: CLAVE_DE_PERFIL,
    queryFn: obtenerPerfil,
    retry: false,
    staleTime: 60_000,
  });
}

/**
 * El catálogo de departamentos habilitados con sus municipios.
 *
 * Cambia por migración, no por uso, así que se conserva fresco durante toda la visita.
 */
export function useCatalogoTerritorial() {
  return useQuery({
    queryKey: CLAVE_DE_CATALOGO,
    queryFn: obtenerCatalogoTerritorial,
    staleTime: Infinity,
  });
}

export function useCreacionDePerfil() {
  const guardar = useGuardarPerfil();
  return useMutation({ mutationFn: crearPerfil, onSuccess: guardar });
}

export function useActualizacionDePerfil() {
  const guardar = useGuardarPerfil();
  return useMutation({ mutationFn: actualizarPerfil, onSuccess: guardar });
}

export function useDisponibilidad() {
  const guardar = useGuardarPerfil();
  return useMutation({ mutationFn: cambiarDisponibilidad, onSuccess: guardar });
}

export function useSubidaDeImagenDePerfil() {
  const guardar = useGuardarPerfil();
  return useMutation({ mutationFn: subirImagenDePerfil, onSuccess: guardar });
}

export function useEliminacionDeImagenDePerfil() {
  const guardar = useGuardarPerfil();
  return useMutation({ mutationFn: eliminarImagenDePerfil, onSuccess: guardar });
}

/** Deja el perfil que devolvió la API como estado vigente de la consulta. */
function useGuardarPerfil() {
  const cliente = useQueryClient();
  return (perfil: PerfilPrestador) => cliente.setQueryData(CLAVE_DE_PERFIL, perfil);
}
