import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { enviarMensaje, listarMensajes, obtenerContactosRevelados } from '../api';

/**
 * Cada cuánto se vuelve a pedir el hilo mientras la pantalla está abierta.
 *
 * Cinco segundos: suficiente para que una conversación se sienta viva y lo bastante espaciado para
 * no castigar una red móvil. El MVP no usa WebSockets a propósito (decisión del plan), así que esta
 * es la única forma de que llegue lo que escribe la contraparte.
 */
export const INTERVALO_DE_MENSAJES_MS = 5_000;

export function claveDeMensajes(idSolicitud: number) {
  return ['solicitud', 'mensajes', idSolicitud] as const;
}

export function claveDeContactos(idSolicitud: number) {
  return ['solicitud', 'contactos', idSolicitud] as const;
}

/**
 * El hilo de una solicitud, refrescado por short polling.
 *
 * `habilitado` es lo que decide si la consulta existe: mientras la solicitud no haya sido aceptada
 * no se pide nada, así que no se golpea un endpoint que responderá 409. React Query no solapa dos
 * peticiones de la misma clave y detiene el temporizador al desmontar, de modo que salir de la
 * pantalla apaga el polling sin que haya que recordarlo a mano.
 *
 * `refetchIntervalInBackground` queda en `false` (el valor por omisión, explícito aquí porque es
 * una decisión): con la pestaña oculta no se sigue consultando.
 *
 * Un fallo puntual no vacía la pantalla: React Query conserva los últimos mensajes que sí llegaron
 * y la vista muestra el aviso encima de ellos.
 */
export function useMensajes(idSolicitud: number, habilitado: boolean) {
  return useQuery({
    queryKey: claveDeMensajes(idSolicitud),
    queryFn: () => listarMensajes(idSolicitud),
    enabled: habilitado,
    retry: false,
    refetchInterval: habilitado ? INTERVALO_DE_MENSAJES_MS : false,
    refetchIntervalInBackground: false,
  });
}

/** Envía un mensaje y deja el hilo pidiendo la versión nueva. */
export function useEnvioDeMensaje(idSolicitud: number) {
  const cliente = useQueryClient();
  return useMutation({
    mutationFn: (contenido: string) => enviarMensaje(idSolicitud, contenido),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: claveDeMensajes(idSolicitud) });
    },
  });
}

/**
 * Los contactos revelados al cliente participante.
 *
 * No se refrescan solos: una vez revelados no cambian con el estado de la solicitud, y el prestador
 * puede editarlos sin que eso sea una novedad que perseguir cada cinco segundos.
 */
export function useContactosRevelados(idSolicitud: number, habilitado: boolean) {
  return useQuery({
    queryKey: claveDeContactos(idSolicitud),
    queryFn: () => obtenerContactosRevelados(idSolicitud),
    enabled: habilitado,
    retry: false,
    staleTime: 5 * 60_000,
  });
}
