import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../App';
import {
  cuerpoDeError,
  instalarApiFalsa,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../pruebas/apiFalsa';
import { renderizarConProveedores } from '../pruebas/utilidades';

describe('estado de acceso en la pantalla de inicio', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  function sinSesion() {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
  }

  it('ofrece iniciar sesión y crear cuenta cuando no hay sesión', async () => {
    sinSesion();
    renderizarConProveedores(<App />);

    expect(await screen.findByRole('link', { name: 'Iniciar sesión' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Cerrar sesión' })).not.toBeInTheDocument();
  });

  it('saluda a quien tiene la sesión iniciada y le ofrece cerrarla', async () => {
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />);

    expect(await screen.findByText('Erving Miranda')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Crear cuenta' })).not.toBeInTheDocument();
  });

  it('cierra la sesión y devuelve a la pantalla de inicio de sesión', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('DELETE /api/auth/sesion', { estado: 204 });
    renderizarConProveedores(<App />);

    await persona.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión en Moica' })).toBeVisible();
    expect(api.ultima('DELETE /api/auth/sesion')?.cabeceras['X-XSRF-TOKEN']).toBe(
      'token-de-prueba'
    );
  });

  it('avisa de que la sesión venció cuando llega su fecha de expiración', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });

    // Una sesión a la que ya solo le quedan unos segundos.
    const sesion = sesionDeEjemplo();
    sesion.sesion.fechaExpiracion = new Date(Date.now() + 5000).toISOString();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesion });

    renderizarConProveedores(<App />);
    await screen.findByText('Erving Miranda');

    await vi.advanceTimersByTimeAsync(5000);

    expect(await screen.findByRole('status')).toHaveTextContent('Tu sesión venció');
  });
});
