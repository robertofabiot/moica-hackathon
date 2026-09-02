import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { enviarReporte, obtenerEstadoDeReporte } from '../api';
import type { ReporteAPresentar } from '../tipos';

export function claveDeReporte(idSolicitud: number) {
  return ['solicitud', 'reporte', idSolicitud] as const;
}

/**
 * El estado del reporte de la sesión sobre una solicitud.
 *
 * `habilitado` es lo que decide si la consulta existe: en una solicitud que nunca llegó a
 * aceptarse no se pide nada, porque no hay nada que decidir.
 *
 * No lleva `refetchInterval`. Que la solicitud haya llegado a estar aceptada no deja de ser cierto,
 * y el caso propio solo aparece cuando esta misma persona lo abre, lo que ya invalida la clave.
 */
export function useReporte(idSolicitud: number, habilitado: boolean) {
  return useQuery({
    queryKey: claveDeReporte(idSolicitud),
    queryFn: () => obtenerEstadoDeReporte(idSolicitud),
    enabled: habilitado,
    retry: false,
    staleTime: 5 * 60_000,
  });
}

/**
 * Abre el caso y deja pidiendo el estado nuevo.
 *
 * Solo se invalida la clave del reporte: abrir un caso no cambia la solicitud, ninguna cuenta ni
 * ninguna reputación, así que invalidar más caché haría recargar pantallas que no cambiaron.
 */
export function useEnvioDeReporte(idSolicitud: number) {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: (reporte: ReporteAPresentar) => enviarReporte(idSolicitud, reporte),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: claveDeReporte(idSolicitud) });
    },
  });
}
