import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  actualizarTextoAlternativo,
  actualizarTrabajo,
  crearTrabajo,
  eliminarImagenDeTrabajo,
  eliminarTrabajo,
  listarTrabajos,
  reordenarImagenes,
  reordenarTrabajos,
  subirImagenDeTrabajo,
} from '../api';
import type { DatosDeTrabajo, Trabajo } from '../tipos';

/**
 * Estado remoto del portafolio propio.
 *
 * La consulta trae los trabajos con sus imágenes, así que cualquier cambio sobre una imagen
 * invalida la lista entera: es una sola petición y evita reconstruir a mano un estado anidado que
 * podría quedar desalineado con el servidor.
 */

/** Clave con la que React Query guarda el portafolio propio. */
export const CLAVE_DE_PORTAFOLIO = ['portafolio', 'trabajos'] as const;

/** Los trabajos propios en su orden. Sin perfil creado la API responde 404. */
export function usePortafolio(habilitada: boolean) {
  return useQuery({
    queryKey: CLAVE_DE_PORTAFOLIO,
    queryFn: listarTrabajos,
    enabled: habilitada,
    retry: false,
  });
}

export function useCreacionDeTrabajo() {
  const invalidar = useInvalidarPortafolio();
  return useMutation({ mutationFn: crearTrabajo, onSuccess: invalidar });
}

export function useActualizacionDeTrabajo() {
  const invalidar = useInvalidarPortafolio();
  return useMutation({
    mutationFn: ({ id, datos }: { id: number; datos: DatosDeTrabajo }) =>
      actualizarTrabajo(id, datos),
    onSuccess: invalidar,
  });
}

export function useEliminacionDeTrabajo() {
  const invalidar = useInvalidarPortafolio();
  return useMutation({ mutationFn: eliminarTrabajo, onSuccess: invalidar });
}

export function useOrdenDeTrabajos() {
  const cliente = useQueryClient();

  return useMutation({
    mutationFn: reordenarTrabajos,
    onSuccess: (trabajos: Trabajo[]) => cliente.setQueryData(CLAVE_DE_PORTAFOLIO, trabajos),
  });
}

export function useSubidaDeImagenDeTrabajo() {
  const invalidar = useInvalidarPortafolio();
  return useMutation({
    mutationFn: ({
      idTrabajo,
      archivo,
      textoAlternativo,
    }: {
      idTrabajo: number;
      archivo: File;
      textoAlternativo: string;
    }) => subirImagenDeTrabajo(idTrabajo, archivo, textoAlternativo),
    onSuccess: invalidar,
  });
}

export function useTextoAlternativo() {
  const invalidar = useInvalidarPortafolio();
  return useMutation({
    mutationFn: ({
      idTrabajo,
      idImagen,
      textoAlternativo,
    }: {
      idTrabajo: number;
      idImagen: number;
      textoAlternativo: string;
    }) => actualizarTextoAlternativo(idTrabajo, idImagen, textoAlternativo),
    onSuccess: invalidar,
  });
}

export function useEliminacionDeImagen() {
  const invalidar = useInvalidarPortafolio();
  return useMutation({
    mutationFn: ({ idTrabajo, idImagen }: { idTrabajo: number; idImagen: number }) =>
      eliminarImagenDeTrabajo(idTrabajo, idImagen),
    onSuccess: invalidar,
  });
}

export function useOrdenDeImagenes() {
  const invalidar = useInvalidarPortafolio();
  return useMutation({
    mutationFn: ({ idTrabajo, idsEnOrden }: { idTrabajo: number; idsEnOrden: number[] }) =>
      reordenarImagenes(idTrabajo, idsEnOrden),
    onSuccess: invalidar,
  });
}

function useInvalidarPortafolio() {
  const cliente = useQueryClient();
  return () => {
    void cliente.invalidateQueries({ queryKey: CLAVE_DE_PORTAFOLIO });
  };
}
