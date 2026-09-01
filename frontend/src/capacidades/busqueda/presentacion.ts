/**
 * Cómo se lee un precio, una reputación o un tipo de prestador en las pantallas
 * públicas.
 *
 * «A convenir» es solo presentación: la API sigue enviando `precioReferencia: null`.
 */

import type { ReputacionPorRol } from './tipos';

export function precioVisible(precioReferencia: number | null): string {
  return precioReferencia === null ? 'A convenir' : `C$ ${precioReferencia.toFixed(2)}`;
}

/**
 * Precio compacto de la tarjeta de exploración: «A convenir» o «Desde C$300».
 *
 * El detalle público sigue usando {@link precioVisible}; aquí el número va sin
 * decimales para leerse de un vistazo.
 */
export function precioEnTarjeta(precioReferencia: number | null): {
  prefijo: string | null;
  valor: string;
} {
  if (precioReferencia === null) {
    return { prefijo: null, valor: 'A convenir' };
  }
  return { prefijo: 'Desde', valor: `C$${Math.round(precioReferencia)}` };
}

/**
 * Cómo se lee una reputación real.
 *
 * El backend envía `promedio: null` cuando la persona todavía no recibió
 * calificaciones. Ese caso NO se presenta como `0.0`: una cuenta sin actividad
 * no tiene una nota pésima, no tiene nota. Calificar es opcional y no hacerlo no
 * penaliza a nadie.
 */
export const SIN_CALIFICACIONES = 'Sin calificaciones';

/** «1 calificación» frente a «2 calificaciones». */
export function conteoDeCalificaciones(cantidad: number): string {
  return cantidad === 1 ? '1 calificación' : `${cantidad} calificaciones`;
}

/** La nota con un decimal, o `null` si todavía no hay ninguna calificación. */
export function notaVisible(promedio: number | null): string | null {
  return promedio === null ? null : promedio.toFixed(1);
}

/**
 * Frase completa para lectores de pantalla, para no depender de las estrellas.
 *
 * Es el texto que va en `aria-label`: quien no ve el icono debe recibir la misma
 * información que quien sí lo ve.
 */
export function etiquetaDeReputacion(reputacion: ReputacionPorRol): string {
  const nota = notaVisible(reputacion.promedio);
  if (nota === null) {
    return `${SIN_CALIFICACIONES} todavía`;
  }
  return `Calificación ${nota} de 5, ${conteoDeCalificaciones(reputacion.cantidad)}`;
}

export function nombreDelTipoPrestador(tipo: 'INDEPENDIENTE' | 'EMPRENDIMIENTO' | 'PYME'): string {
  switch (tipo) {
    case 'INDEPENDIENTE':
      return 'Independiente';
    case 'EMPRENDIMIENTO':
      return 'Emprendimiento';
    case 'PYME':
      return 'PyME';
  }
}

export function nombreDeDisponibilidad(disponibilidad: 'DISPONIBLE' | 'NO_DISPONIBLE'): string {
  return disponibilidad === 'DISPONIBLE' ? 'Disponible para contratar' : 'No disponible ahora';
}
