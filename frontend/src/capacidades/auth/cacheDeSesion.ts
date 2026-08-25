import type { QueryClient, QueryKey } from '@tanstack/react-query';

import { MOTIVO_SESION_VENCIDA } from './rutas';
import { CLAVE_DE_SESION } from './hooks/useSesionActual';

/**
 * Qué se olvida cuando una sesión termina, y cómo se explica esa salida.
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

/** Cómo se explica la salida en la pantalla de inicio de sesión. `null` es «sin explicación». */
export type MotivoDeSalida = string | null;

/**
 * El motivo que dejó anotado quien terminó la sesión, hasta que alguien lo recoge.
 *
 * Va por cliente y no en una variable suelta del módulo porque cada prueba estrena el suyo y
 * ninguna debe heredar lo que anotó otra.
 */
const motivosPendientes = new WeakMap<QueryClient, MotivoDeSalida>();

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
 * Quien la termina a propósito anota además **por qué**, pero no navega: de eso se encarga
 * `useVigilanciaDeSesion`, que es el único sitio de la aplicación que lleva a iniciar sesión. Así
 * no hay dos navegaciones compitiendo por la misma salida ni un motivo que dependa de cuál de las
 * dos llegue antes.
 */
export function olvidarSesion(
  cliente: QueryClient,
  motivo: MotivoDeSalida = MOTIVO_SESION_VENCIDA
): void {
  motivosPendientes.set(cliente, motivo);
  olvidarDatosPrivados(cliente);
  cliente.setQueryData(CLAVE_DE_SESION, null);
}

/**
 * Recoge el motivo anotado y lo consume.
 *
 * Sin motivo anotado la sesión desapareció sola —venció, la revocaron desde otro dispositivo o una
 * consulta de fondo recibió un 401—, y eso es exactamente lo que dice {@link MOTIVO_SESION_VENCIDA}.
 */
export function motivoDeLaSalida(cliente: QueryClient): MotivoDeSalida {
  const motivo = motivosPendientes.get(cliente);
  motivosPendientes.delete(cliente);
  return motivo === undefined ? MOTIVO_SESION_VENCIDA : motivo;
}
