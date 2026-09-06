import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  actualizarContacto,
  crearContacto,
  eliminarContacto,
  listarContactos,
  reordenarContactos,
} from '../api';
import type { MedioContacto } from '../tipos';

/**
 * Estado remoto de los medios de contacto propios.
 *
 * Las mutaciones que devuelven la lista completa —reordenar— la dejan puesta; las que afectan a un
 * solo elemento invalidan la consulta, que es más simple que reconstruir la lista a mano y no
 * arriesga dejarla desalineada con el servidor.
 */

/** Clave con la que React Query guarda los contactos propios. */
export const CLAVE_DE_CONTACTOS = ['prestador', 'contactos'] as const;

/** Los contactos propios, en su orden de visualización. */
export function useContactos(habilitada: boolean) {
  return useQuery({
    queryKey: CLAVE_DE_CONTACTOS,
    queryFn: listarContactos,
    // Sin perfil creado la API responde 404: no se pregunta hasta que exista.
    enabled: habilitada,
    retry: false,
  });
}

export function useCreacionDeContacto() {
  const invalidar = useInvalidarContactos();
  return useMutation({ mutationFn: crearContacto, onSuccess: invalidar });
}

export function useActualizacionDeContacto() {
  const invalidar = useInvalidarContactos();
  return useMutation({
    mutationFn: ({ id, contenido }: { id: number; contenido: string }) =>
      actualizarContacto(id, contenido),
    onSuccess: invalidar,
  });
}

export function useEliminacionDeContacto() {
  const invalidar = useInvalidarContactos();
  return useMutation({ mutationFn: eliminarContacto, onSuccess: invalidar });
}

export function useOrdenDeContactos() {
  const cliente = useQueryClient();

  return useMutation({
    mutationFn: reordenarContactos,
    onSuccess: (contactos: MedioContacto[]) => cliente.setQueryData(CLAVE_DE_CONTACTOS, contactos),
  });
}

function useInvalidarContactos() {
  const cliente = useQueryClient();
  return () => {
    void cliente.invalidateQueries({ queryKey: CLAVE_DE_CONTACTOS });
  };
}
