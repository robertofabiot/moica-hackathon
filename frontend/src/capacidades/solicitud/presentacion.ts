import type { EstadoSolicitud } from './tipos';

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
