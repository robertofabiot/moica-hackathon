import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it } from 'vitest';

import AccesoNoAutorizado from './AccesoNoAutorizado';

function pintar(ui: Parameters<typeof render>[0]) {
  return render(<MemoryRouter>{ui}</MemoryRouter>);
}

describe('AccesoNoAutorizado', () => {
  it('muestra el título y las rutas de una sesión inactiva (401)', () => {
    pintar(<AccesoNoAutorizado tipo="sesion-expirada" />);

    expect(screen.getByRole('heading', { name: 'Tu sesión no está activa' })).toBeVisible();
    expect(screen.getByText('HTTP 401 · SESIÓN REQUERIDA')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toHaveAttribute(
      'href',
      '/iniciar-sesion'
    );
    expect(screen.getByRole('link', { name: 'Volver a explorar' })).toHaveAttribute(
      'href',
      '/explorar'
    );
  });

  it('infiere el caso 401 cuando solo llega el código', () => {
    pintar(<AccesoNoAutorizado codigo={401} />);

    expect(screen.getByRole('heading', { name: 'Tu sesión no está activa' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toHaveAttribute(
      'href',
      '/iniciar-sesion'
    );
  });

  it('muestra el título y las rutas de un 403 por permisos', () => {
    pintar(<AccesoNoAutorizado tipo="permisos-insuficientes" />);

    expect(
      screen.getByRole('heading', { name: 'Esta zona requiere otros permisos' })
    ).toBeVisible();
    expect(screen.getByText('HTTP 403 · ACCESO RESTRINGIDO')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Volver a explorar' })).toHaveAttribute(
      'href',
      '/explorar'
    );
  });

  it('usa permisos insuficientes por omisión y respeta destinoRetorno', () => {
    pintar(<AccesoNoAutorizado destinoRetorno="/panel" />);

    expect(
      screen.getByRole('heading', { name: 'Esta zona requiere otros permisos' })
    ).toBeVisible();
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute(
      'href',
      '/panel'
    );
  });

  it('lleva a configurar seguridad cuando falta el segundo factor', () => {
    pintar(<AccesoNoAutorizado tipo="requiere-segundo-factor" />);

    expect(screen.getByRole('heading', { name: 'Verificación adicional requerida' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Configurar seguridad' })).toHaveAttribute(
      'href',
      '/seguridad'
    );
    expect(screen.getByRole('alert')).toHaveTextContent(
      'segundo factor de autenticación verificado'
    );
  });

  it('sustituye la explicación cuando llega un mensaje personalizado', () => {
    pintar(
      <AccesoNoAutorizado
        tipo="permisos-insuficientes"
        mensajePersonalizado="Solo el equipo de confianza puede abrir esta zona."
      />
    );

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Solo el equipo de confianza puede abrir esta zona.'
    );
  });
});
