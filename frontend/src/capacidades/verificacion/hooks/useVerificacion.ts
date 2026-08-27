import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { enviarExpediente, listarSolicitudesPropias, obtenerEstadoDeVerificacion } from '../api';
import type { DocumentoElegido, NivelSolicitado } from '../tipos';

/**
 * Estado remoto de la verificación propia.
 *
 * Al cerrar sesión, la limpieza general de `cacheDeSesion` descarta estas claves como cualquier
 * otra privada: un expediente nunca debe verse un instante al entrar con otra cuenta.
 */

/** Clave con la que React Query guarda el estado de verificación propio. */
export const CLAVE_DE_VERIFICACION = ['verificacion', 'estado'] as const;

/** Clave del historial propio de solicitudes. */
export const CLAVE_DE_SOLICITUDES = ['verificacion', 'solicitudes'] as const;

export function useEstadoDeVerificacion() {
  return useQuery({
    queryKey: CLAVE_DE_VERIFICACION,
    queryFn: obtenerEstadoDeVerificacion,
    retry: false,
  });
}

export function useSolicitudesPropias() {
  return useQuery({
    queryKey: CLAVE_DE_SOLICITUDES,
    queryFn: listarSolicitudesPropias,
    retry: false,
  });
}

/**
 * Envía el expediente y deja al día las dos consultas.
 *
 * El envío cambia a la vez el historial y lo que el perfil puede solicitar, así que se invalidan
 * ambas en lugar de escribir a mano un estado que el servidor acaba de recalcular.
 */
export function useEnvioDeExpediente() {
  const cliente = useQueryClient();

  return useMutation({
    mutationFn: ({
      nivel,
      documentos,
    }: {
      nivel: NivelSolicitado;
      documentos: DocumentoElegido[];
    }) => enviarExpediente(nivel, documentos),
    onSuccess: async () => {
      await Promise.all([
        cliente.invalidateQueries({ queryKey: CLAVE_DE_VERIFICACION }),
        cliente.invalidateQueries({ queryKey: CLAVE_DE_SOLICITUDES }),
      ]);
    },
  });
}
