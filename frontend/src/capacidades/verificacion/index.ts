/**
 * Punto de entrada de la capacidad de verificación.
 *
 * El resto de la aplicación usa solo lo que se publica aquí; nadie importa archivos internos de la
 * capacidad.
 *
 * {@link InsigniaDeVerificacion} se publica aunque hoy solo la use el perfil propio: es lo único
 * del flujo que llegará a ser público, y cuando P5 abra el descubrimiento debe decir exactamente lo
 * mismo que dice aquí.
 */

export { default as InsigniaDeVerificacion } from './componentes/InsigniaDeVerificacion';
export { default as Verificacion } from './componentes/Verificacion';
export type {
  EstadoDeSolicitud,
  EstadoDeVerificacion,
  NivelSolicitado,
  NivelVerificacion,
  SolicitudDeVerificacion,
  TipoDeDocumento,
} from './tipos';
