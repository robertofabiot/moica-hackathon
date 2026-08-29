import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  catalogoDeEjemplo,
  cuerpoDeError,
  instalarApiFalsa,
  resumenDeSolicitudDeEjemplo,
  sesionDeEjemplo,
  solicitudDeServicioDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

describe('Mis solicitudes', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('GET /api/catalogos/departamentos', { estado: 200, cuerpo: catalogoDeEjemplo() });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('muestra el vacío de ambas bandejas', async () => {
    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });

    renderizarConProveedores(<App />, '/solicitudes');

    expect(await screen.findByText('Todavía no has enviado solicitudes.')).toBeVisible();
    expect(screen.getByText('Todavía no has recibido solicitudes.')).toBeVisible();
  });

  it('muestra el estado de carga de las bandejas', async () => {
    api.colgar('GET /api/solicitudes/enviadas');
    api.colgar('GET /api/solicitudes/recibidas');

    renderizarConProveedores(<App />, '/solicitudes');

    expect((await screen.findAllByText('Cargando solicitudes…')).length).toBeGreaterThan(0);
  });

  it('lista enviadas y recibidas y abre el detalle', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/solicitudes/enviadas', {
      estado: 200,
      cuerpo: [resumenDeSolicitudDeEjemplo({ idSolicitudServicio: 21 })],
    });
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [
        resumenDeSolicitudDeEjemplo({ idSolicitudServicio: 22, nombreServicio: 'Electricidad' }),
      ],
    });
    api.responder('GET /api/solicitudes/21', {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/solicitudes');

    expect(await screen.findByText('Reparación de fugas')).toBeVisible();
    expect(screen.getByText('Electricidad')).toBeVisible();

    await persona.click(screen.getByRole('link', { name: /Reparación de fugas/ }));
    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
    expect(screen.getAllByText('Pendiente').length).toBeGreaterThan(0);
  });

  it('muestra el error de red y permite reintentar', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/solicitudes/enviadas', {
      estado: 500,
      cuerpo: cuerpoDeError(500, 'ERROR_INTERNO', 'Algo falló en Moica.'),
    });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });

    renderizarConProveedores(<App />, '/solicitudes');

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo falló en Moica.');

    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    await persona.click(await screen.findByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('Todavía no has enviado solicitudes.')).toBeVisible();
  });

  it('sin sesión redirige a iniciar sesión', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });

    renderizarConProveedores(<App />, '/solicitudes');

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });
});
