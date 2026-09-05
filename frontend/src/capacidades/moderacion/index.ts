/**
 * Punto de entrada de la revisión administrativa de casos de moderación.
 *
 * El resto de la aplicación usa solo lo que se publica aquí; nadie importa archivos internos de la
 * capacidad.
 */

export { default as BandejaDeCasos } from './paginas/BandejaDeCasos';
export { default as CatalogoDeMedidas } from './paginas/CatalogoDeMedidas';
export { default as ExpedienteDeCaso } from './paginas/ExpedienteDeCaso';
export {
  RUTA_ADMIN_CASOS,
  RUTA_ADMIN_EXPEDIENTE,
  RUTA_ADMIN_MEDIDAS,
  rutaDeExpediente,
} from './rutas';
export type {
  EstadoDeCaso,
  ExpedienteDeCaso as DatosDeExpediente,
  MedidaAdministrativa,
  ResumenDeCaso,
} from './tipos';
