import type { QueryClient, QueryKey } from '@tanstack/react-query';

import { CLAVE_DE_SESION } from './hooks/useSesionActual';

/**
 * Qué se olvida cuando una sesión termina.
 *
 * Moica es una sola aplicación que no se recarga entre una cuenta y la siguiente: cerrar sesión y
 * volver a entrar con otra cuenta ocurre sin que el navegador vacíe nada. La caché de React Query,
 * en cambio, sobrevive a esa transición, y todo lo que guarda de una ruta autenticada pertenece a
 * la cuenta que la pidió.
 *
 * Por eso el criterio no es una lista de claves conocidas —que habría que acordarse de ampliar en
 * cada incremento— sino el contrario: **se descarta todo menos la sesión**. Una consulta que se
 * olvide de más se vuelve a pedir; una que se olvide de menos enseña datos ajenos.
 *
 * La sesión se conserva y se pone en `null` en lugar de retirarse: es la consulta que todas las
 * pantallas observan, y quitarla las dejaría creyendo que todavía se está comprobando.
 */

function esConsultaDeSesion(clave: QueryKey): boolean {
  return (
    clave.length === CLAVE_DE_SESION.length &&
    clave.every((parte, posicion) => parte === CLAVE_DE_SESION[posicion])
  );
}

/**
 * Descarta toda la respuesta privada que quede en memoria, sin tocar la sesión.
 *
 * Se usa al entrar: aunque la salida anterior debió limpiarlo todo, quien empieza una sesión no
 * puede heredar nada de la anterior por un camino que no se previó.
 */
export function olvidarDatosPrivados(cliente: QueryClient): void {
  cliente.removeQueries({ predicate: (consulta) => !esConsultaDeSesion(consulta.queryKey) });
}

/**
 * Da la sesión por terminada: no queda en memoria nada de la cuenta que la tenía.
 *
 * Es el único sitio por el que se olvida una sesión, sea cual sea el motivo —cierre voluntario,
 * expiración, revocación, cambio de contraseña o desactivación del segundo factor—, de modo que
 * ninguno de esos caminos pueda limpiar menos que los demás.
 */
export function olvidarSesion(cliente: QueryClient): void {
  olvidarDatosPrivados(cliente);
  cliente.setQueryData(CLAVE_DE_SESION, null);
}
