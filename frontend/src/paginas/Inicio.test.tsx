import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../App';
import { definirTiempoDeEsperaMs, TIEMPO_DE_ESPERA_MS } from '../capacidades/auth/api';
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
    vi.restoreAllMocks();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  function sinSesion() {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
  }

  async function conSesionIniciada() {
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />);
    expect(await screen.findByText('Erving Miranda')).toBeVisible();
  }

  function permaneceAutenticadoYPuedeReintentar() {
    expect(screen.getByText('Erving Miranda')).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeEnabled();
    expect(screen.queryByRole('heading', { name: 'Iniciar sesión' })).not.toBeInTheDocument();
  }

  async function reintentaYCierra(persona: ReturnType<typeof userEvent.setup>) {
    api.responder('DELETE /api/auth/sesion', { estado: 204 });
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
    expect(screen.queryByText('Erving Miranda')).not.toBeInTheDocument();
  }

  it('ofrece iniciar sesión y crear cuenta cuando no hay sesión', async () => {
    sinSesion();
    renderizarConProveedores(<App />);

    expect(await screen.findByRole('link', { name: 'Iniciar sesión' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Crear cuenta' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Explorar servicios' })).toBeVisible();
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

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
    expect(api.ultima('DELETE /api/auth/sesion')?.cabeceras['X-XSRF-TOKEN']).toBe(
      'token-de-prueba'
    );
  });

  it('trata el 401 al cerrar como sesión vencida y lleva a iniciar sesión', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    api.responder('DELETE /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });

    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Tu sesión venció');
    expect(screen.queryByText('Erving Miranda')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cerrar sesión' })).not.toBeInTheDocument();
  });

  it('conserva la sesión si el navegador está sin conexión y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);

    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(screen.queryByRole('button', { name: 'Cerrando sesión…' })).not.toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos comunicarnos con Moica. Revisa tu conexión e inténtalo otra vez.'
    );
    expect(api.ultima('DELETE /api/auth/sesion')).toBeUndefined();
    permaneceAutenticadoYPuedeReintentar();

    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
    await reintentaYCierra(persona);
  });

  it('abandona el cierre si fetch queda colgado como en Chrome offline', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    definirTiempoDeEsperaMs(20);
    vi.stubGlobal(
      'fetch',
      vi.fn(() => new Promise<Response>(() => undefined))
    );

    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Tardamos demasiado en obtener respuesta. Revisa tu conexión e inténtalo otra vez.'
    );
    expect(screen.queryByRole('button', { name: 'Cerrando sesión…' })).not.toBeInTheDocument();
    permaneceAutenticadoYPuedeReintentar();
  });

  it('conserva la sesión si no hay conexión y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    api.rechazar('DELETE /api/auth/sesion');

    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos comunicarnos con Moica. Revisa tu conexión e inténtalo otra vez.'
    );
    permaneceAutenticadoYPuedeReintentar();
    await reintentaYCierra(persona);
  });

  it('conserva la sesión ante un 403 al cerrar y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    api.responder('DELETE /api/auth/sesion', {
      estado: 403,
      cuerpo: cuerpoDeError(
        403,
        'ACCESO_DENEGADO',
        'No se pudo validar la petición. Recarga la página e inténtalo otra vez.'
      ),
    });

    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo validar la petición. Recarga la página e inténtalo otra vez.'
    );
    permaneceAutenticadoYPuedeReintentar();
    await reintentaYCierra(persona);
  });

  it('conserva la sesión ante un 500 al cerrar y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    api.responder('DELETE /api/auth/sesion', {
      estado: 500,
      cuerpo: cuerpoDeError(
        500,
        'ERROR_INTERNO',
        'Algo falló en Moica. Inténtalo de nuevo en unos minutos.'
      ),
    });

    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Algo falló en Moica. Inténtalo de nuevo en unos minutos.'
    );
    permaneceAutenticadoYPuedeReintentar();
    await reintentaYCierra(persona);
  });

  it('abandona el cierre si la petición no responde y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    definirTiempoDeEsperaMs(20);
    api.colgar('DELETE /api/auth/sesion');

    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Tardamos demasiado en obtener respuesta. Revisa tu conexión e inténtalo otra vez.'
    );
    permaneceAutenticadoYPuedeReintentar();
    await reintentaYCierra(persona);
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
