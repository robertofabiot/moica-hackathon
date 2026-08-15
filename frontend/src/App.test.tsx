import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';

import App from './App';

function montarEn(rutaInicial: string) {
  return render(
    <MemoryRouter initialEntries={[rutaInicial]}>
      <App />
    </MemoryRouter>
  );
}

describe('navegación base', () => {
  it('muestra la pantalla de inicio en la ruta raíz', () => {
    montarEn('/');

    expect(screen.getByRole('heading', { name: 'Moica' })).toBeInTheDocument();
    expect(screen.getByAltText('Logotipo de Moica')).toBeInTheDocument();
  });

  it('muestra una explicación cuando la dirección no existe', () => {
    montarEn('/una-direccion-que-no-existe');

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Moica' })).not.toBeInTheDocument();
  });

  it('permite volver al inicio desde una dirección inexistente', async () => {
    const usuario = userEvent.setup();
    montarEn('/una-direccion-que-no-existe');

    await usuario.click(screen.getByRole('link', { name: 'Volver al inicio' }));

    expect(screen.getByRole('heading', { name: 'Moica' })).toBeInTheDocument();
  });
});
