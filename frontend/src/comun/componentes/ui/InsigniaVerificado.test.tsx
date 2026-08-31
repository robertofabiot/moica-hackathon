import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { InsigniaVerificado } from './InsigniaVerificado';
import estilos from './InsigniaVerificado.module.css';

describe('InsigniaVerificado', () => {
  it('anuncia el estado verificado con texto y no solo con color', () => {
    render(<InsigniaVerificado />);

    expect(screen.getByText('Verificado')).toBeVisible();
    expect(screen.getByText('Verificado').parentElement).toHaveClass(estilos.insignia ?? '');
  });
});
