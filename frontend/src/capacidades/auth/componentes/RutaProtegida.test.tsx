import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  instalarApiFalsa,
  segundoFactorDeEjemplo,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

/**
 * Un fallo al comprobar la sesión no es lo mismo que no tenerla: sin red o con un 5xx la cookie
 * puede seguir vigente, así que no se lleva a iniciar sesión.
 */
describe('rutas que exigen sesión', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('ante un 5xx al comprobar la sesión permite reintentar en lugar de pedir entrar', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/auth/sesion', {
      estado: 502,
      cuerpo: cuerpoDeError(502, 'ERROR_INTERNO', 'Algo falló en Moica.'),
    });
    api.responder('GET /api/auth/segundo-factor', {
      estado: 200,
      cuerpo: segundoFactorDeEjemplo(null),
    });

    renderizarConProveedores(<App />, '/seguridad');

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo falló en Moica.');
    expect(screen.queryByRole('heading', { name: 'Iniciar sesión' })).not.toBeInTheDocument();

    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('heading', { name: 'Seguridad de tu cuenta' })).toBeVisible();
  });

  it('sin red en la verificación del segundo factor tampoco pide entrar', async () => {
    api.rechazar('GET /api/auth/sesion');

    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos comunicarnos con Moica. Revisa tu conexión e inténtalo otra vez.'
    );
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Iniciar sesión' })).not.toBeInTheDocument();
  });
});
