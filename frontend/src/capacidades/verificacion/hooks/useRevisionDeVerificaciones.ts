import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  aprobarExpediente,
  listarExpedientes,
  rechazarExpediente,
  revocarExpediente,
  tomarExpediente,
} from '../api';
import type { FiltroDeCola } from '../tipos';

/**
 * Estado remoto de la cola administrativa.
 *
 * Toda resolución vuelve a pedir la cola en lugar de reescribirla a mano: entre que se pintó y que
 * se resolvió, otra persona administradora pudo tomar o resolver algo, y la cola dejaría de decir
 * la verdad.
 *
 * La cola se vuelve a pedir tanto si la acción salió bien como si falló. Un conflicto es
 * justamente el caso en el que lo que hay en pantalla ya no es cierto: quien recibe el 409 debe ver
 * de inmediato quién tiene la revisión, no quedarse mirando el estado viejo con un mensaje encima.
 */

/** Clave de la cola, parametrizada por el filtro que se está viendo. */
export function claveDeCola(filtro: FiltroDeCola) {
  return ['admin', 'verificaciones', filtro.estados.join(','), filtro.nivel ?? 'TODOS'] as const;
}

/** Prefijo común a todas las colas, para invalidarlas juntas tras una resolución. */
const PREFIJO_DE_COLA = ['admin', 'verificaciones'] as const;

export function useColaDeVerificaciones(filtro: FiltroDeCola) {
  return useQuery({
    queryKey: claveDeCola(filtro),
    queryFn: () => listarExpedientes(filtro),
    retry: false,
  });
}

export function useTomaDeExpediente() {
  return useResolucion(tomarExpediente);
}

export function useAprobacionDeExpediente() {
  return useResolucion(aprobarExpediente);
}

export function useRechazoDeExpediente() {
  return useResolucionConMotivo(rechazarExpediente);
}

export function useRevocacionDeExpediente() {
  return useResolucionConMotivo(revocarExpediente);
}

function useResolucion<T>(accion: (idSolicitudVerificacion: number) => Promise<T>) {
  const invalidar = useInvalidarCola();
  return useMutation({ mutationFn: accion, onSettled: invalidar });
}

function useResolucionConMotivo<T>(
  accion: (idSolicitudVerificacion: number, observacion: string) => Promise<T>
) {
  const invalidar = useInvalidarCola();
  return useMutation({
    mutationFn: ({
      idSolicitudVerificacion,
      observacion,
    }: {
      idSolicitudVerificacion: number;
      observacion: string;
    }) => accion(idSolicitudVerificacion, observacion),
    onSettled: invalidar,
  });
}

function useInvalidarCola() {
  const cliente = useQueryClient();
  return () => cliente.invalidateQueries({ queryKey: PREFIJO_DE_COLA });
}
