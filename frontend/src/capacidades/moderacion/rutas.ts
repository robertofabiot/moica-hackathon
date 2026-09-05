/**
 * Rutas de la revisión administrativa de casos.
 *
 * Cuelgan de `/admin`, que es donde la definición del producto sitúa el área administrativa, junto
 * a la cola de verificaciones documentales.
 */

export const RUTA_ADMIN_CASOS = '/admin/casos';
export const RUTA_ADMIN_EXPEDIENTE = '/admin/casos/:idCaso';

export function rutaDeExpediente(idCaso: number): string {
  return `/admin/casos/${idCaso}`;
}
