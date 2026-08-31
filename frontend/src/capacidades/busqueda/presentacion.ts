/**
 * Cómo se lee un precio o un tipo de prestador en las pantallas públicas.
 *
 * «A convenir» es solo presentación: la API sigue enviando `precioReferencia: null`.
 */

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
 * Maqueta visual de reputación. El listado público todavía no publica
 * calificaciones; no se inventa una nota distinta por servicio.
 */
export const CALIFICACION_DE_MUESTRA = 4.8;
export const RESENAS_DE_MUESTRA = 102;

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
