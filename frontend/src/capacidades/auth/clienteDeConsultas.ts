import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query';

import { ErrorDeApi } from './api';
import { CLAVE_DE_SESION } from './hooks/useSesionActual';

/**
 * El cliente de React Query de Moica, con la única regla transversal que tiene el estado remoto:
 * **un 401 en una operación autenticada significa que la sesión ya no existe**.
 *
 * Sin esta regla, descubrir que la sesión fue revocada dependía de qué pantalla estuviera abierta:
 * el estado del segundo factor mostraba «no pudimos consultarlo» con un botón de reintentar, y el
 * área administrativa se quedaba igual, sin que nadie llevara a iniciar sesión. Al anotarlo aquí,
 * cualquier consulta o mutación que reciba un 401 deja la sesión en `null` y quien la vigila hace
 * lo mismo que ante una expiración.
 *
 * El 401 del inicio de sesión público no entra: allí no hay ninguna sesión en curso —lo que falló
 * fueron las credenciales escritas— y confundirlos haría que un intento fallido se explicara como
 * «tu sesión venció». Distinguirlos no necesita mirar la ruta: basta con exigir que hubiera una
 * sesión antes del fallo.
 *
 * Lo construye una función y no un módulo con una instancia suelta para que cada prueba estrene la
 * suya y ninguna herede la caché de otra.
 */
export function crearClienteDeConsultas(): QueryClient {
  const olvidarLaSesionQueElServidorYaNoReconoce = (fallo: unknown): void => {
    if (!(fallo instanceof ErrorDeApi) || fallo.estado !== 401) {
      return;
    }
    if (!cliente.getQueryData(CLAVE_DE_SESION)) {
      return;
    }
    cliente.setQueryData(CLAVE_DE_SESION, null);
  };

  const cliente = new QueryClient({
    queryCache: new QueryCache({ onError: olvidarLaSesionQueElServidorYaNoReconoce }),
    mutationCache: new MutationCache({ onError: olvidarLaSesionQueElServidorYaNoReconoce }),
    defaultOptions: {
      queries: {
        // Una peticion fallida se reintenta una vez; mas reintentos solo
        // retrasarian el mensaje de error en una conexion mala.
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  });

  return cliente;
}
