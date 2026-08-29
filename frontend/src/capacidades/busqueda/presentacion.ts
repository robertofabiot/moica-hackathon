/**
 * Cómo se lee un precio o un tipo de prestador en las pantallas públicas.
 *
 * «A convenir» es solo presentación: la API sigue enviando `precioReferencia: null`.
 */

export function precioVisible(precioReferencia: number | null): string {
  return precioReferencia === null ? 'A convenir' : `C$ ${precioReferencia.toFixed(2)}`;
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
