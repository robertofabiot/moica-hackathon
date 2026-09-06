import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { EstrellasCalificacion } from './EstrellasCalificacion';
import estilos from './EstrellasCalificacion.module.css';

describe('EstrellasCalificacion', () => {
  it('muestra la nota en negrita y el recuento de calificaciones', () => {
    render(<EstrellasCalificacion calificacion={4.8} totalCalificaciones={120} />);

    expect(screen.getByLabelText('Calificación 4.8 de 5, 120 calificaciones')).toBeVisible();
    expect(screen.getByText('4.8')).toHaveClass(estilos.puntuacion ?? '');
    expect(screen.getByText('(120)')).toHaveClass(estilos.resenas ?? '');
  });

  it('usa el singular cuando hay una sola calificación', () => {
    render(<EstrellasCalificacion calificacion={5} totalCalificaciones={1} />);

    expect(screen.getByLabelText('Calificación 5.0 de 5, 1 calificación')).toBeVisible();
  });

  it('omite el recuento cuando no llega', () => {
    render(<EstrellasCalificacion calificacion={5} />);

    expect(screen.getByLabelText('Calificación 5.0 de 5')).toBeVisible();
    expect(screen.getByText('5.0')).toBeVisible();
    expect(screen.queryByText(/^\(/)).not.toBeInTheDocument();
  });

  it('sin calificaciones no dibuja una nota de cero', () => {
    render(<EstrellasCalificacion calificacion={null} />);

    expect(screen.getByLabelText('Sin calificaciones todavía')).toBeVisible();
    expect(screen.getByText('Sin calificaciones')).toBeVisible();
    expect(screen.queryByText('0.0')).not.toBeInTheDocument();
  });
});
