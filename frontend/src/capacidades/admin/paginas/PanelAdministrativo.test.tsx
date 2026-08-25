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
import { definirTiempoDeEsperaMs, TIEMPO_DE_ESPERA_MS } from '../../auth/api';

describe('área administrativa', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  function sesionAdministrativaVerificada() {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({
        esAdministrador: true,
        segundoFactorRequerido: true,
        segundoFactorVerificado: true,
      }),
    });
  }

  it('lleva a iniciar sesión a quien llega sin sesión', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
    renderizarConProveedores(<App />, '/admin');

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión en Moica' })).toBeVisible();
    expect(api.ultima('GET /api/admin/resumen')).toBeUndefined();
  });

  it('explica el acceso denegado a una cuenta ordinaria', async () => {
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />, '/admin');

    expect(await screen.findByRole('heading', { name: 'Acceso denegado' })).toBeVisible();
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Esta cuenta no tiene permisos administrativos'
    );
    expect(api.ultima('GET /api/admin/resumen')).toBeUndefined();
  });

  it('explica que falta verificar el segundo factor a un administrador que no lo activó', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ esAdministrador: true }),
    });
    renderizarConProveedores(<App />, '/admin');

    expect(await screen.findByRole('heading', { name: 'Acceso denegado' })).toBeVisible();
    expect(screen.getByRole('alert')).toHaveTextContent('exige el segundo factor verificado');
  });

  it('lleva a verificar a un administrador con la sesión provisional', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ esAdministrador: true, segundoFactorRequerido: true }),
    });
    renderizarConProveedores(<App />, '/admin');

    expect(
      await screen.findByRole('heading', { name: 'Verifica tu segundo factor' })
    ).toBeVisible();
  });

  it('muestra el área a un administrador con el segundo factor verificado', async () => {
    sesionAdministrativaVerificada();
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
    expect(await screen.findByText('erving@moica.test')).toBeVisible();
  });

  it('muestra el mensaje del backend si aun así deniega el acceso', async () => {
    sesionAdministrativaVerificada();
    api.responder('GET /api/admin/resumen', {
      estado: 403,
      cuerpo: cuerpoDeError(403, 'ACCESO_DENEGADO', 'No pudimos completar la operación.'),
    });
    renderizarConProveedores(<App />, '/admin');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos completar la operación.'
    );
  });

  it('permite reintentar cuando falla la red', async () => {
    const persona = userEvent.setup();
    sesionAdministrativaVerificada();
    api.rechazar('GET /api/admin/resumen');
    renderizarConProveedores(<App />, '/admin');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos comunicarnos con Moica.'
    );

    api.responder('GET /api/admin/resumen', {
      estado: 200,
      cuerpo: {
        nombreCompleto: 'Erving Miranda',
        correoElectronico: 'erving@moica.test',
        fechaAsignacion: '2026-08-24T10:00:00-06:00',
      },
    });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('erving@moica.test')).toBeVisible();
  });

  it('ofrece el área administrativa en el inicio solo a quien tiene el rol', async () => {
    sesionAdministrativaVerificada();
    renderizarConProveedores(<App />, '/');

    expect(await screen.findByRole('link', { name: 'Área administrativa' })).toBeVisible();
  });

  it('no ofrece el área administrativa a una cuenta ordinaria', async () => {
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />, '/');

    expect(await screen.findByRole('link', { name: 'Seguridad de la cuenta' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Área administrativa' })).not.toBeInTheDocument();
  });
});
