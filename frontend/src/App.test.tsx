import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from './App';
import { cuerpoDeError, instalarApiFalsa, type ApiFalsa } from './pruebas/apiFalsa';
import { renderizarConProveedores } from './pruebas/utilidades';

describe('navegación base', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    // La pantalla de inicio consulta la sesión al montarse; en estas pruebas no
    // hay ninguna.
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('muestra la pantalla de inicio en la ruta raíz', () => {
    renderizarConProveedores(<App />, '/');

    expect(
      screen.getByRole('heading', { name: 'Encuentra servicios confiables en tu comunidad' })
    ).toBeInTheDocument();
    expect(screen.getByAltText('Logotipo de Moica')).toBeInTheDocument();
  });

  it('muestra una explicación cuando la dirección no existe', () => {
    renderizarConProveedores(<App />, '/una-direccion-que-no-existe');

    expect(screen.getByRole('heading', { name: '404' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Moica, ir al inicio' })).toBeInTheDocument();
    expect(
      screen.getByRole('heading', { name: 'Ups, parece que nos desconectamos' })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('heading', { name: 'Encuentra servicios confiables en tu comunidad' })
    ).not.toBeInTheDocument();
  });

  it('permite volver al inicio desde una dirección inexistente', async () => {
    const persona = userEvent.setup();
    renderizarConProveedores(<App />, '/una-direccion-que-no-existe');

    await persona.click(screen.getByRole('button', { name: 'Volver a explorar' }));

    expect(
      screen.getByRole('heading', { name: 'Encuentra servicios confiables en tu comunidad' })
    ).toBeInTheDocument();
  });

  it('abre la pantalla de registro en su propia ruta', () => {
    renderizarConProveedores(<App />, '/registro');

    expect(screen.getByRole('heading', { name: 'Crear cuenta' })).toBeInTheDocument();
  });

  it('abre la pantalla de inicio de sesión en su propia ruta', () => {
    renderizarConProveedores(<App />, '/iniciar-sesion');

    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument();
  });

  it('abre la exploración pública de servicios sin exigir sesión', async () => {
    api.responder('GET /api/catalogos/categorias', { estado: 200, cuerpo: [] });
    api.responder('GET /api/catalogos/departamentos', { estado: 200, cuerpo: [] });
    api.responder('GET /api/servicios', { estado: 200, cuerpo: [] });

    renderizarConProveedores(<App />, '/explorar');

    expect(await screen.findByRole('heading', { name: 'Explorar servicios' })).toBeInTheDocument();
  });
});
