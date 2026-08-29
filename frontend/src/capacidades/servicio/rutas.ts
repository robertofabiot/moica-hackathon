export const RUTA_SERVICIOS = '/prestador/servicios';
export const RUTA_NUEVO_SERVICIO = '/prestador/servicios/nuevo';
export const RUTA_EDITAR_SERVICIO = '/prestador/servicios/:idServicio';

export function rutaDeEdicionDeServicio(idServicio: number): string {
  return `/prestador/servicios/${idServicio}`;
}
