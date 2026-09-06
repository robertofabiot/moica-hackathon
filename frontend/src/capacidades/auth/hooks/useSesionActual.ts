import { useQuery } from '@tanstack/react-query';

import { obtenerSesionActual } from '../api';

/** Clave con la que React Query guarda la sesión en curso. */
export const CLAVE_DE_SESION = ['auth', 'sesion'] as const;

/**
 * Estado de la sesión de quien está usando Moica.
 *
 * `data` vale `null` cuando no hay sesión vigente, que es lo que responde la API con 401. No se
 * reintenta: la ausencia de sesión no es un fallo pasajero.
 */
export function useSesionActual() {
  return useQuery({
    queryKey: CLAVE_DE_SESION,
    queryFn: obtenerSesionActual,
    retry: false,
    staleTime: 60_000,
  });
}
