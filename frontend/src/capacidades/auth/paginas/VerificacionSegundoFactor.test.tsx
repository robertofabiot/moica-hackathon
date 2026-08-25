import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  instalarApiFalsa,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import { definirTiempoDeEsperaMs, TIEMPO_DE_ESPERA_MS } from '../api';

describe('verificación del segundo factor', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  function sinSesion() {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
  }

  function conSesionProvisional() {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ segundoFactorRequerido: true }),
    });
  }

  it('lleva a verificar cuando el inicio de sesión abre una sesión provisional', async () => {
    const persona = userEvent.setup();
    sinSesion();
    api.responder('POST /api/auth/sesion', {
      estado: 201,
      cuerpo: sesionDeEjemplo({ segundoFactorRequerido: true }),
    });
    renderizarConProveedores(<App />, '/iniciar-sesion');

    await persona.type(screen.getByLabelText('Correo electrónico'), 'erving@moica.test');
    await persona.type(screen.getByLabelText('Contraseña'), 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(
      await screen.findByRole('heading', { name: 'Verifica tu segundo factor' })
    ).toBeVisible();
  });

  it('entra a la aplicación cuando el código es correcto', async () => {
    const persona = userEvent.setup();
    conSesionProvisional();
    api.responder('POST /api/auth/sesion/segundo-factor', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ segundoFactorRequerido: true, segundoFactorVerificado: true }),
    });
    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    await persona.type(await screen.findByLabelText('Código de verificación'), '123 456');
    await persona.click(screen.getByRole('button', { name: 'Verificar y entrar' }));

    expect(await screen.findByRole('heading', { name: 'Moica' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Seguridad de la cuenta' })).toBeVisible();
    expect(api.ultima('POST /api/auth/sesion/segundo-factor')?.cuerpo).toEqual({
      codigo: '123456',
    });
  });

  it('explica que el código no es válido y deja reintentar', async () => {
    const persona = userEvent.setup();
    conSesionProvisional();
    api.responder('POST /api/auth/sesion/segundo-factor', {
      estado: 403,
      cuerpo: cuerpoDeError(403, 'CODIGO_INVALIDO', 'El código no es válido.'),
    });
    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    await persona.type(await screen.findByLabelText('Código de verificación'), '000000');
    await persona.click(screen.getByRole('button', { name: 'Verificar y entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('El código no es válido.');
    expect(screen.getByRole('heading', { name: 'Verifica tu segundo factor' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Verificar y entrar' })).toBeEnabled();
  });

  it('exige escribir algún código antes de llamar a la API', async () => {
    const persona = userEvent.setup();
    conSesionProvisional();
    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    await persona.click(await screen.findByRole('button', { name: 'Verificar y entrar' }));

    expect(
      await screen.findByText('Escribe el código de tu aplicación autenticadora.')
    ).toBeVisible();
    expect(api.ultima('POST /api/auth/sesion/segundo-factor')).toBeUndefined();
  });

  it('vuelve a iniciar sesión si la sesión provisional muere mientras se verifica', async () => {
    const persona = userEvent.setup();
    conSesionProvisional();
    api.responder('POST /api/auth/sesion/segundo-factor', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    await persona.type(await screen.findByLabelText('Código de verificación'), '123456');
    await persona.click(screen.getByRole('button', { name: 'Verificar y entrar' }));

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión en Moica' })).toBeVisible();
    expect(screen.getByRole('status')).toHaveTextContent('Tu sesión venció');
  });

  it('no deja la interfaz colgada cuando falla la red', async () => {
    const persona = userEvent.setup();
    conSesionProvisional();
    api.rechazar('POST /api/auth/sesion/segundo-factor');
    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    await persona.type(await screen.findByLabelText('Código de verificación'), '123456');
    await persona.click(screen.getByRole('button', { name: 'Verificar y entrar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos comunicarnos con Moica.'
    );
    expect(screen.getByRole('button', { name: 'Verificar y entrar' })).toBeEnabled();
  });

  it('permite salir sin verificar', async () => {
    const persona = userEvent.setup();
    conSesionProvisional();
    api.responder('DELETE /api/auth/sesion', { estado: 204 });
    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    await persona.click(await screen.findByRole('button', { name: 'Salir de la sesión' }));

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión en Moica' })).toBeVisible();
  });

  it('lleva a iniciar sesión a quien llega sin ninguna sesión', async () => {
    sinSesion();
    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión en Moica' })).toBeVisible();
  });

  it('devuelve al inicio a quien ya no tiene nada que verificar', async () => {
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />, '/verificar-segundo-factor');

    expect(await screen.findByRole('heading', { name: 'Moica' })).toBeVisible();
  });

  it('avisa en el inicio de que la sesión sigue pendiente de verificarse', async () => {
    conSesionProvisional();
    renderizarConProveedores(<App />, '/');

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Falta verificar tu segundo factor'
    );
    expect(screen.getByRole('link', { name: 'Verificar segundo factor' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Seguridad de la cuenta' })).not.toBeInTheDocument();
  });
});
