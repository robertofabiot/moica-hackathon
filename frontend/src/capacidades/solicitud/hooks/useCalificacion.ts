import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { enviarCalificacion, obtenerEstadoDeCalificacion } from '../api';
import type { CalificacionAEmitir } from '../tipos';

export function claveDeCalificacion(idSolicitud: number) {
  return ['solicitud', 'calificacion', idSolicitud] as const;
}

/**
 * El estado de la calificación de la sesión sobre una solicitud.
 *
 * `habilitado` es lo que decide si la consulta existe: antes de que la solicitud esté completada no
 * se pide nada, porque no hay nada que decidir todavía.
 *
 * No lleva `refetchInterval`: `COMPLETADA` es un estado definitivo y la calificación es inmutable,
 * así que nada cambia por su cuenta mientras la pantalla está abierta. Solo cambia si esta misma
 * persona califica, y eso ya invalida la clave.
 */
export function useCalificacion(idSolicitud: number, habilitado: boolean) {
  return useQuery({
    queryKey: claveDeCalificacion(idSolicitud),
    queryFn: () => obtenerEstadoDeCalificacion(idSolicitud),
    enabled: habilitado,
    retry: false,
    staleTime: 5 * 60_000,
  });
}

/**
 * Emite la calificación y deja pidiendo el estado nuevo.
 *
 * Se invalida también el detalle de la solicitud: la reputación que se muestra en las pantallas
 * públicas cambió, y la caché de búsqueda debe dejar de dar por buena la anterior.
 */
export function useEnvioDeCalificacion(idSolicitud: number) {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: (calificacion: CalificacionAEmitir) =>
      enviarCalificacion(idSolicitud, calificacion),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: claveDeCalificacion(idSolicitud) });
      void cliente.invalidateQueries({ queryKey: ['solicitud', 'detalle'] });
      void cliente.invalidateQueries({ queryKey: ['busqueda'] });
    },
  });
}
