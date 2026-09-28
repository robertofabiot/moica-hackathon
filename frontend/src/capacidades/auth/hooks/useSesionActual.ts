import { useQuery } from '@tanstack/react-query';

import { obtenerSesionActual } from '../api';
import { olvidarDatosPrivados } from '../cacheDeSesion';

/** Clave con la que React Query guarda la sesión en curso. */
export const CLAVE_DE_SESION = ['auth', 'sesion'] as const;

/**
 * Estado de la sesión de quien está usando Moica.
 *
 * `data` vale `null` cuando no hay sesión vigente, que es lo que responde la API con 401. No se
 * reintenta: la ausencia de sesión no es un fallo pasajero.
 *
 * Se vuelve a pedir al recuperar el foco porque la cookie es de todo el navegador: si en otra
 * pestaña salió una cuenta y entró otra, esta solo se entera así. Cuando la respuesta trae a otra
 * persona, lo que quedaba en caché de la anterior se descarta **antes** de publicar la sesión
 * nueva, para que ninguna pantalla llegue a pintarlo bajo la cuenta que acaba de llegar.
 */
export function useSesionActual() {
  return useQuery({
    queryKey: CLAVE_DE_SESION,
    queryFn: async ({ client }) => {
      const sesion = await obtenerSesionActual();
      const anterior = client.getQueryData<typeof sesion>(CLAVE_DE_SESION);
      if (anterior && sesion && anterior.usuario.idUsuario !== sesion.usuario.idUsuario) {
        olvidarDatosPrivados(client);
      }
      return sesion;
    },
    retry: false,
    staleTime: 60_000,
    refetchOnWindowFocus: true,
  });
}
