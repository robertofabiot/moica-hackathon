export const RUTA_EXPLORAR = '/explorar';
export const RUTA_DETALLE_SERVICIO = '/explorar/servicios/:idServicio';
export const RUTA_PRESTADOR_PUBLICO = '/explorar/prestadores/:idPrestador';

export function rutaDeDetalleDeServicio(idServicio: number): string {
  return `/explorar/servicios/${idServicio}`;
}

export function rutaDePrestadorPublico(idPrestador: number): string {
  return `/explorar/prestadores/${idPrestador}`;
}
