import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';

import { PieDePagina } from './PieDePagina';

describe('PieDePagina', () => {
  it('muestra el lockup, el lema y el aviso de derechos', () => {
    render(
      <MemoryRouter>
        <PieDePagina />
      </MemoryRouter>
    );

    expect(screen.getByRole('contentinfo')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Moica, ir al inicio' })).toHaveAttribute('href', '/');
    expect(
      screen.getByText('La confianza se construye entre todos. Únete a la comunidad.')
    ).toBeVisible();
    expect(screen.getByText('© 2026 Moica. Todos los derechos reservados.')).toBeVisible();
  });

  it('ofrece las cuatro columnas de enlaces institucionales', () => {
    render(
      <MemoryRouter>
        <PieDePagina />
      </MemoryRouter>
    );

    expect(screen.getByRole('navigation', { name: 'Enlaces institucionales' })).toBeVisible();
    expect(screen.getByText('Moica')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Sobre nosotros' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Cómo funciona' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Trabaja con nosotros' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Blog' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Centro de ayuda' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Términos y condiciones' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Privacidad' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Para empresas' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Conviértete en proveedor' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Recomendaciones' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Eventos' })).toBeVisible();
  });

  it('expone las redes con nombre accesible y destino externo', () => {
    render(
      <MemoryRouter>
        <PieDePagina />
      </MemoryRouter>
    );

    const facebook = screen.getByRole('link', { name: 'Facebook' });
    expect(facebook).toHaveAttribute('href', 'https://www.facebook.com/');
    expect(facebook).toHaveAttribute('target', '_blank');
    expect(screen.getByRole('link', { name: 'Instagram' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'YouTube' })).toBeVisible();
  });
});
