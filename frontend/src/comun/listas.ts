/**
 * Utilidades de orden para las listas que el propietario reordena a mano.
 *
 * Los controles son «subir» y «bajar», así que un movimiento siempre es un intercambio entre dos
 * posiciones contiguas. Se expresa como tal en lugar de recortar y reinsertar: dice lo que hace y
 * no deja huecos si alguna posición fuera inválida.
 */

/**
 * La lista con dos posiciones intercambiadas, o `null` si alguna cae fuera.
 *
 * Devolver `null` en lugar de lanzar deja que quien llama simplemente no haga nada: los botones ya
 * están deshabilitados en los extremos, así que ese caso solo ocurriría por una carrera.
 */
export function intercambiar<T>(lista: readonly T[], desde: number, hasta: number): T[] | null {
  const primero = lista[desde];
  const segundo = lista[hasta];

  if (primero === undefined || segundo === undefined) {
    return null;
  }

  const reordenada = [...lista];
  reordenada[desde] = segundo;
  reordenada[hasta] = primero;
  return reordenada;
}
