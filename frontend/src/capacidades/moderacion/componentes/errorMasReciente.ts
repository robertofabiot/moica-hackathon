/**
 * El error de la acción que se intentó última, entre varias mutaciones.
 *
 * Se elige la última aunque ya no tenga error, para que una acción exitosa retire el aviso de la
 * anterior. El refresco del expediente no cambia `submittedAt`, así que el aviso sobrevive al
 * refetch el tiempo suficiente para informar.
 */
export function errorMasReciente(
  ...mutaciones: { error: unknown; submittedAt: number }[]
): unknown {
  return mutaciones.sort((una, otra) => otra.submittedAt - una.submittedAt)[0]?.error;
}
