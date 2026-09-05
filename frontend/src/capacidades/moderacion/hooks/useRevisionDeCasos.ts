import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  asignarCaso,
  cerrarCaso,
  iniciarRevisionDelCaso,
  listarAdministradores,
  listarCasos,
  obtenerExpediente,
  obtenerMensajesDelCaso,
} from '../api';
import type { FiltroDeBandeja, ResultadoDeCaso } from '../tipos';

/**
 * Estado remoto de la revisión administrativa de casos.
 *
 * Toda decisión vuelve a pedir el expediente y la bandeja en lugar de reescribirlos a mano: entre
 * que se pintó la pantalla y se resolvió, otra persona administradora pudo reasignar o cerrar, y lo
 * que hay en pantalla dejaría de decir la verdad.
 *
 * Se refrescan tanto si la acción salió bien como si falló. Un conflicto es justamente el caso en
 * el que lo mostrado ya no es cierto: quien recibe el 409 debe ver de inmediato el estado real, no
 * quedarse mirando el viejo con un mensaje encima.
 */

const PREFIJO = ['admin', 'casos'] as const;

export function claveDeBandeja(filtro: FiltroDeBandeja) {
  return [...PREFIJO, 'bandeja', filtro.estados.join(','), filtro.soloMios] as const;
}

export function claveDeExpediente(idCaso: number) {
  return [...PREFIJO, idCaso] as const;
}

export function useBandejaDeCasos(filtro: FiltroDeBandeja) {
  return useQuery({
    queryKey: claveDeBandeja(filtro),
    queryFn: () => listarCasos(filtro),
    retry: false,
  });
}

export function useExpedienteDeCaso(idCaso: number) {
  return useQuery({
    queryKey: claveDeExpediente(idCaso),
    queryFn: () => obtenerExpediente(idCaso),
    retry: false,
  });
}

/**
 * El hilo de la solicitud reportada, que solo se pide cuando alguien decide leerlo.
 *
 * `habilitada` mantiene la consulta apagada hasta entonces: abrir un expediente no debe descargar
 * de paso una conversación privada que quizá nadie va a mirar.
 */
export function useMensajesDelCaso(idCaso: number, habilitada: boolean) {
  return useQuery({
    queryKey: [...claveDeExpediente(idCaso), 'mensajes'],
    queryFn: () => obtenerMensajesDelCaso(idCaso),
    enabled: habilitada,
    retry: false,
  });
}

export function useAdministradores() {
  return useQuery({ queryKey: ['admin', 'administradores'], queryFn: listarAdministradores });
}

export function useAsignacionDeCaso(idCaso: number) {
  const refrescar = useRefrescoDelCaso();
  return useMutation({
    mutationFn: (idAdministrador: number) => asignarCaso(idCaso, idAdministrador),
    onSettled: refrescar,
  });
}

export function useInicioDeRevision(idCaso: number) {
  const refrescar = useRefrescoDelCaso();
  return useMutation({ mutationFn: () => iniciarRevisionDelCaso(idCaso), onSettled: refrescar });
}

export function useCierreDeCaso(idCaso: number) {
  const refrescar = useRefrescoDelCaso();
  return useMutation({
    mutationFn: ({ resultado, resolucion }: { resultado: ResultadoDeCaso; resolucion: string }) =>
      cerrarCaso(idCaso, resultado, resolucion),
    onSettled: refrescar,
  });
}

/** Invalida el expediente y todas las bandejas a la vez: comparten prefijo. */
function useRefrescoDelCaso() {
  const cliente = useQueryClient();
  return () => cliente.invalidateQueries({ queryKey: PREFIJO });
}
