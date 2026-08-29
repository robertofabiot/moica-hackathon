/**
 * Punto de entrada de la capacidad de servicios publicados.
 *
 * El resto de la aplicación usa solo lo que se publica aquí; nadie importa archivos internos.
 */

export { default as EditarServicio } from './paginas/EditarServicio';
export { default as NuevoServicio } from './paginas/NuevoServicio';
export { default as ServiciosPropios } from './paginas/ServiciosPropios';
export { RUTA_EDITAR_SERVICIO, RUTA_NUEVO_SERVICIO, RUTA_SERVICIOS } from './rutas';
export type { CategoriaDeServicio, ServicioPropio } from './tipos';
