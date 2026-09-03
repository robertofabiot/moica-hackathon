import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ErrorDeApi } from '../../../comun/api';
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
 *
 * Un conflicto se invalida igual que un acierto. Un 409 significa que el servidor sabe algo que
 * esta pantalla ya no: que el caso existe —lo abrió otra pestaña o un envío anterior— o que la
 * solicitud dejó de admitir el reporte. Sin volver a preguntar, la caché seguiría ofreciendo un
 * formulario que ya no puede funcionar hasta que venciera `staleTime`.
 */
export function useEnvioDeReporte(idSolicitud: number) {
  const cliente = useQueryClient();
  const volverAPedirElEstado = () =>
    void cliente.invalidateQueries({ queryKey: claveDeReporte(idSolicitud) });

  return useMutation({
    mutationFn: (reporte: ReporteAPresentar) => enviarReporte(idSolicitud, reporte),
    onSuccess: volverAPedirElEstado,
    onError: (fallo) => {
      if (fallo instanceof ErrorDeApi && fallo.estado === 409) {
        volverAPedirElEstado();
      }
    },
  });
}
