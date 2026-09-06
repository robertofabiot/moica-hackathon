/**
 * Punto de entrada de la capacidad de prestador.
 *
 * El resto de la aplicación usa solo lo que se publica aquí; nadie importa archivos internos de la
 * capacidad.
 */

export { default as PerfilPrestador } from './paginas/PerfilPrestador';
export { usePerfilPrestador } from './hooks/usePerfilPrestador';
export { RUTA_PRESTADOR } from './rutas';
export type { EstadoDisponibilidad, NivelVerificacion, TipoPrestador } from './tipos';
