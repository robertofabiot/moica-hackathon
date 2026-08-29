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

  /** Sesión administrativa de una cuenta concreta, para distinguir una de otra. */
  function sesionAdministrativaDe(nombreCompleto: string, correoElectronico: string) {
    const sesion = sesionDeEjemplo({
      esAdministrador: true,
      segundoFactorRequerido: true,
      segundoFactorVerificado: true,
    });
    sesion.usuario.nombreCompleto = nombreCompleto;
    sesion.usuario.correoElectronico = correoElectronico;
    return sesion;
  }

  function resumenDe(nombreCompleto: string, correoElectronico: string) {
    return {
      nombreCompleto,
      correoElectronico,
      fechaAsignacion: '2026-08-24T10:00:00-06:00',
    };
  }

  it('lleva a iniciar sesión a quien llega sin sesión', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
    renderizarConProveedores(<App />, '/admin');

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
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

  it('no muestra el resumen de la cuenta anterior a la que entra después', async () => {
    const persona = userEvent.setup();

    // La cuenta A entra, consulta el área administrativa y cierra sesión.
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionAdministrativaDe('Ana Torres', 'ana@moica.test'),
    });
    api.responder('GET /api/admin/resumen', {
      estado: 200,
      cuerpo: resumenDe('Ana Torres', 'ana@moica.test'),
    });
    api.responder('DELETE /api/auth/sesion', { estado: 204 });

    const { cliente } = renderizarConProveedores(<App />, '/admin');
    expect(await screen.findByText('ana@moica.test')).toBeVisible();

    await persona.click(screen.getByRole('link', { name: 'Volver al inicio' }));
    await persona.click(await screen.findByRole('button', { name: /^Hola,/ }));
    await persona.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();

    // La cuenta B entra sin recargar la aplicación y su resumen todavía no responde.
    api.responder('POST /api/auth/sesion', {
      estado: 201,
      cuerpo: sesionAdministrativaDe('Bruno Paz', 'bruno@moica.test'),
    });
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionAdministrativaDe('Bruno Paz', 'bruno@moica.test'),
    });
    api.colgar('GET /api/admin/resumen');

    await persona.type(screen.getByLabelText('Correo electrónico'), 'bruno@moica.test');
    await persona.type(screen.getByLabelText('Contraseña'), 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    await persona.click(await screen.findByRole('button', { name: /^Hola,/ }));
    await persona.click(await screen.findByRole('link', { name: 'Área administrativa' }));

    expect(await screen.findByText('Cargando el área administrativa…')).toBeVisible();
    expect(screen.queryByText('ana@moica.test')).not.toBeInTheDocument();
    expect(screen.queryByText('Ana Torres')).not.toBeInTheDocument();
    expect(cliente.getQueryData(['admin', 'resumen'])).toBeUndefined();
  });

  it('ofrece el área administrativa en el inicio solo a quien tiene el rol', async () => {
    const persona = userEvent.setup();
    sesionAdministrativaVerificada();
    renderizarConProveedores(<App />, '/');

    await persona.click(await screen.findByRole('button', { name: /^Hola,/ }));
    expect(screen.getByRole('link', { name: 'Área administrativa' })).toBeVisible();
  });

  it('no ofrece el área administrativa a una cuenta ordinaria', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />, '/');

    await persona.click(await screen.findByRole('button', { name: /^Hola,/ }));
    expect(screen.getByRole('link', { name: 'Seguridad de la cuenta' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Área administrativa' })).not.toBeInTheDocument();
  });
});
