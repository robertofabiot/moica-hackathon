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
import { definirTiempoDeEsperaMs, TIEMPO_DE_ESPERA_MS } from '../api';

/**
 * La sesión se vigila desde `App`, así que su fin debe resolverse igual en cualquier ruta.
 *
 * Estas pruebas recorren las pantallas a las que se llega **después** del inicio —`/seguridad` y
 * `/admin`—, porque es justo donde antes no había nadie escuchando: el temporizador vivía en la
 * pantalla de inicio y se cancelaba al navegar.
 */
describe('vigilancia de la sesión', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  /** Una sesión a la que solo le quedan unos segundos, para no esperar siete días. */
  function sesionQueEstaPorVencer(opciones: Parameters<typeof sesionDeEjemplo>[0] = {}) {
    const sesion = sesionDeEjemplo(opciones);
    sesion.sesion.fechaExpiracion = new Date(Date.now() + 5000).toISOString();
    return sesion;
  }

  function sesionAdministrativa() {
    return sesionQueEstaPorVencer({
      esAdministrador: true,
      segundoFactorRequerido: true,
      segundoFactorVerificado: true,
    });
  }

  async function esperarElInicioDeSesionConMotivo(texto: string) {
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión en Moica' })).toBeVisible();
    expect(screen.getByRole('status')).toHaveTextContent(texto);
    expect(screen.queryByText('Cerrando tu sesión…')).not.toBeInTheDocument();
  }

  it('avisa del vencimiento mientras se está en la seguridad de la cuenta', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionQueEstaPorVencer() });
    api.responder('GET /api/auth/segundo-factor', {
      estado: 200,
      cuerpo: segundoFactorDeEjemplo(null),
    });

    renderizarConProveedores(<App />, '/seguridad');
    expect(await screen.findByRole('heading', { name: 'Seguridad de tu cuenta' })).toBeVisible();

    await vi.advanceTimersByTimeAsync(5000);

    await esperarElInicioDeSesionConMotivo('Tu sesión venció');
  });

  it('avisa del vencimiento mientras se está en el área administrativa', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionAdministrativa() });
    api.responder('GET /api/admin/resumen', {
      estado: 200,
      cuerpo: {
        nombreCompleto: 'Erving Miranda',
        correoElectronico: 'erving@moica.test',
        fechaAsignacion: '2026-08-24T10:00:00-06:00',
      },
    });

    renderizarConProveedores(<App />, '/admin');
    expect(await screen.findByRole('heading', { name: 'Área administrativa' })).toBeVisible();

    await vi.advanceTimersByTimeAsync(5000);

    await esperarElInicioDeSesionConMotivo('Tu sesión venció');
  });

  it('resuelve la revocación que descubre una consulta de fondo, no solo la que vence', async () => {
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    // La sesión se revocó desde otro dispositivo entre la consulta de la sesión
    // y la del segundo factor.
    api.responder('GET /api/auth/segundo-factor', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });

    renderizarConProveedores(<App />, '/seguridad');

    await esperarElInicioDeSesionConMotivo('Tu sesión venció');
  });

  it('resuelve la revocación que descubre el área administrativa', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({
        esAdministrador: true,
        segundoFactorRequerido: true,
        segundoFactorVerificado: true,
      }),
    });
    api.responder('GET /api/admin/resumen', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });

    renderizarConProveedores(<App />, '/admin');

    await esperarElInicioDeSesionConMotivo('Tu sesión venció');
  });

  it('no confunde unas credenciales equivocadas con una sesión perdida', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
    api.responder('POST /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(
        401,
        'CREDENCIALES_INVALIDAS',
        'El correo o la contraseña no son correctos.'
      ),
    });

    renderizarConProveedores(<App />, '/iniciar-sesion');

    await persona.type(await screen.findByLabelText('Correo electrónico'), 'persona@moica.test');
    await persona.type(screen.getByLabelText('Contraseña'), 'Moica2026$equivocada');
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El correo o la contraseña no son correctos.'
    );
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });
});
