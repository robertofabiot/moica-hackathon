import type { EstadoCuenta } from '../auth';
import type { DatosDeSolicitudServicio, EstadoSolicitud, RolCalificado } from './tipos';

/** Enviar, aceptar, rechazar y completar exigen cuenta activa. Cancelar no. */
export function cuentaEstaActiva(estadoCuenta: EstadoCuenta | undefined): boolean {
  return estadoCuenta === 'ACTIVA';
}

const ESTADOS: Record<EstadoSolicitud, string> = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  RECHAZADA: 'Rechazada',
  CANCELADA: 'Cancelada',
  COMPLETADA: 'Completada',
};

export function nombreDelEstado(estado: EstadoSolicitud): string {
  return ESTADOS[estado];
}

export function fechaVisible(valor: string): string {
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    return valor;
  }
  return new Intl.DateTimeFormat('es-NI', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha);
}

export function diaVisible(valor: string): string {
  const [anioTexto, mesTexto, diaTexto] = valor.split('-');
  const anio = Number(anioTexto);
  const mes = Number(mesTexto ?? '1');
  const dia = Number(diaTexto ?? '1');
  const fecha = new Date(anio, mes - 1, dia);
  if (Number.isNaN(fecha.getTime())) {
    return valor;
  }
  return new Intl.DateTimeFormat('es-NI', { dateStyle: 'medium' }).format(fecha);
}

/**
 * Si la solicitud llegó a estar aceptada y, por tanto, tiene hilo.
 *
 * El estado vigente no basta: una `CANCELADA` puede venir de `PENDIENTE` —y entonces nunca hubo
 * chat— o de `ACEPTADA`, y en ese caso el historial queda visible en solo lectura. La diferencia
 * está en el historial, que el detalle ya trae. El backend aplica la misma regla: esto solo decide
 * qué se pinta.
 */
export function hiloHabilitado(solicitud: DatosDeSolicitudServicio): boolean {
  return (
    solicitud.estadoActual === 'ACEPTADA' ||
    solicitud.historial.some((cambio) => cambio.estadoNuevo === 'ACEPTADA')
  );
}

/** Solo se escribe mientras el compromiso sigue vivo. Cancelar o completar lo cierra. */
export function admiteMensajesNuevos(solicitud: DatosDeSolicitudServicio): boolean {
  return solicitud.estadoActual === 'ACEPTADA';
}

/** La revelación de contactos pertenece al cliente participante, nunca al prestador. */
export function puedeVerContactos(
  solicitud: DatosDeSolicitudServicio,
  idUsuario: number | undefined
): boolean {
  return idUsuario === solicitud.idCliente && hiloHabilitado(solicitud);
}

/**
 * Si la solicitud admite calificación.
 *
 * Solo `COMPLETADA`, que es el único estado que cierra el servicio y además es definitivo. Antes de
 * eso no se pinta ni el formulario ni la sección: no hay nada que calificar todavía. Quién puede
 * calificar de verdad lo decide el backend; esto solo evita ofrecer una acción falsa.
 */
export function admiteCalificacion(solicitud: DatosDeSolicitudServicio): boolean {
  return solicitud.estadoActual === 'COMPLETADA';
}

/** Cómo se nombra en pantalla el rol en que quedó calificada la contraparte. */
export function nombreDelRol(rol: RolCalificado): string {
  return rol === 'PRESTADOR' ? 'prestador' : 'cliente';
}
