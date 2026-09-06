import type { EstadoServicio } from './tipos';

export function nombreDelEstado(estado: EstadoServicio): string {
  return estado === 'ACTIVO' ? 'Activo' : 'Inactivo';
}

export function precioPropio(precioReferencia: number | null): string {
  return precioReferencia === null ? 'A convenir' : `C$ ${precioReferencia.toFixed(2)}`;
}
