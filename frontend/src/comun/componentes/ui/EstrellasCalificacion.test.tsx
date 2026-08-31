import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { EstrellasCalificacion } from './EstrellasCalificacion';
import estilos from './EstrellasCalificacion.module.css';

describe('EstrellasCalificacion', () => {
  it('muestra la nota en negrita y el recuento de reseñas', () => {
    render(<EstrellasCalificacion calificacion={4.8} totalResenas={120} />);

    expect(screen.getByLabelText('Calificación 4.8 de 5, 120 reseñas')).toBeVisible();
    expect(screen.getByText('4.8')).toHaveClass(estilos.puntuacion ?? '');
    expect(screen.getByText('(120)')).toHaveClass(estilos.resenas ?? '');
  });

  it('omite el recuento cuando no hay reseñas', () => {
    render(<EstrellasCalificacion calificacion={5} />);

    expect(screen.getByLabelText('Calificación 5.0 de 5')).toBeVisible();
    expect(screen.getByText('5.0')).toBeVisible();
    expect(screen.queryByText(/^\(/)).not.toBeInTheDocument();
  });
});
