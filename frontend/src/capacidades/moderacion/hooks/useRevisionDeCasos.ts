import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  aplicarMedida,
  asignarCaso,
  cambiarHabilitacionDeMedida,
  cerrarCaso,
  crearMedida,
  editarMedida,
  iniciarRevisionDelCaso,
  listarAdministradores,
  listarCasos,
  listarMedidas,
  obtenerExpediente,
  obtenerMensajesDelCaso,
  reabrirCaso,
  registrarApelacion,
  resolverApelacion,
  revocarMedida,
} from '../api';
import type {
  FiltroDeBandeja,
  MedidaAAplicar,
  MedidaACrear,
  MedidaAEditar,
  ResultadoDeCaso,
} from '../tipos';

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

// --- Catálogo de medidas ---------------------------------------------------

/** Clave con la que React Query guarda el catálogo. */
export const CLAVE_DE_MEDIDAS = ['admin', 'medidas'] as const;

export function useCatalogoDeMedidas() {
  return useQuery({ queryKey: CLAVE_DE_MEDIDAS, queryFn: listarMedidas, retry: false });
}

export function useCreacionDeMedida() {
  const refrescar = useRefrescoDelCatalogo();
  return useMutation({
    mutationFn: (medida: MedidaACrear) => crearMedida(medida),
    onSettled: refrescar,
  });
}

export function useEdicionDeMedida() {
  const refrescar = useRefrescoDelCatalogo();
  return useMutation({
    mutationFn: ({ idMedida, medida }: { idMedida: number; medida: MedidaAEditar }) =>
      editarMedida(idMedida, medida),
    onSettled: refrescar,
  });
}

export function useHabilitacionDeMedida() {
  const refrescar = useRefrescoDelCatalogo();
  return useMutation({
    mutationFn: ({ idMedida, habilitada }: { idMedida: number; habilitada: boolean }) =>
      cambiarHabilitacionDeMedida(idMedida, habilitada),
    onSettled: refrescar,
  });
}

// --- Medidas y apelaciones de un caso --------------------------------------

export function useAplicacionDeMedida(idCaso: number) {
  const refrescar = useRefrescoDelCaso();
  return useMutation({
    mutationFn: (medida: MedidaAAplicar) => aplicarMedida(idCaso, medida),
    onSettled: refrescar,
  });
}

export function useRevocacionDeMedida(idCaso: number) {
  const refrescar = useRefrescoDelCaso();
  return useMutation({
    mutationFn: (motivo: string) => revocarMedida(idCaso, motivo),
    onSettled: refrescar,
  });
}

export function useRegistroDeApelacion(idCaso: number) {
  const refrescar = useRefrescoDelCaso();
  return useMutation({
    mutationFn: (relato: string) => registrarApelacion(idCaso, relato),
    onSettled: refrescar,
  });
}

export function useResolucionDeApelacion(idCaso: number) {
  const refrescar = useRefrescoDelCaso();
  return useMutation({
    mutationFn: ({ aceptada, resolucion }: { aceptada: boolean; resolucion: string }) =>
      resolverApelacion(idCaso, aceptada, resolucion),
    onSettled: refrescar,
  });
}

export function useReaperturaDeCaso(idCaso: number) {
  const refrescar = useRefrescoDelCaso();
  return useMutation({
    mutationFn: (motivo: string) => reabrirCaso(idCaso, motivo),
    onSettled: refrescar,
  });
}

/**
 * Invalida el catálogo tras administrarlo.
 *
 * También invalida los expedientes: el desplegable de medidas aplicables sale del catálogo, y
 * deshabilitar una debe retirarla de las pantallas abiertas en lugar de dejar ofrecer algo que el
 * backend ya rechaza.
 */
function useRefrescoDelCatalogo() {
  const cliente = useQueryClient();
  return () => {
    void cliente.invalidateQueries({ queryKey: CLAVE_DE_MEDIDAS });
    void cliente.invalidateQueries({ queryKey: PREFIJO });
  };
}
