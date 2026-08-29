import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  catalogoDeEjemplo,
  cuerpoDeError,
  detallePublicoDeServicioDeEjemplo,
  instalarApiFalsa,
  sesionDeEjemplo,
  solicitudDeServicioDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

describe('Nueva solicitud', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 2 }),
    });
    api.responder('GET /api/catalogos/departamentos', { estado: 200, cuerpo: catalogoDeEjemplo() });
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('exige descripción, municipio y ubicación antes de enviar', async () => {
    const persona = userEvent.setup();
    renderizarConProveedores(<App />, '/explorar/servicios/10/solicitar');

    await persona.click(await screen.findByRole('button', { name: 'Enviar solicitud' }));

    expect(await screen.findByText('Describe lo que necesitas.')).toBeVisible();
    expect(screen.getByText('Elige el municipio.')).toBeVisible();
    expect(screen.getByText('Indica la dirección, el sector o una referencia.')).toBeVisible();
    expect(api.ultima('POST /api/solicitudes')).toBeUndefined();
  });

  it('envía una solicitud válida y abre el detalle', async () => {
    const persona = userEvent.setup();
    const creada = solicitudDeServicioDeEjemplo();
    api.responder('POST /api/solicitudes', { estado: 201, cuerpo: creada });
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: creada });

    renderizarConProveedores(<App />, '/explorar/servicios/10/solicitar');

    await persona.type(
      await screen.findByLabelText('Qué necesitas'),
      'Se fugará el lavamanos del baño principal.'
    );
    await persona.selectOptions(screen.getByLabelText('Municipio'), '3');
    await persona.type(
      screen.getByLabelText('Dirección, sector o referencia'),
      'De la UCA dos cuadras al lago, portón verde.'
    );
    await persona.type(screen.getByLabelText('Fecha preferida (opcional)'), '2026-09-15');
    await persona.click(screen.getByRole('button', { name: 'Enviar solicitud' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/solicitudes')?.cuerpo).toEqual({
        idServicioPublicado: 10,
        descripcionNecesidad: 'Se fugará el lavamanos del baño principal.',
        idMunicipio: 3,
        indicacionUbicacion: 'De la UCA dos cuadras al lago, portón verde.',
        fechaPreferida: '2026-09-15',
      });
    });

    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
  });

  it('muestra el error de negocio del backend', async () => {
    const persona = userEvent.setup();
    api.responder('POST /api/solicitudes', {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'SERVICIO_PROPIO',
        'No puedes solicitar un servicio publicado por tu propia cuenta.'
      ),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10/solicitar');

    await persona.type(await screen.findByLabelText('Qué necesitas'), 'Necesito una visita.');
    await persona.selectOptions(screen.getByLabelText('Municipio'), '3');
    await persona.type(screen.getByLabelText('Dirección, sector o referencia'), 'Portón verde');
    await persona.click(screen.getByRole('button', { name: 'Enviar solicitud' }));

    expect(
      await screen.findByText('No puedes solicitar un servicio publicado por tu propia cuenta.')
    ).toBeVisible();
  });

  it('desde el detalle público invita a solicitar con sesión', async () => {
    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByRole('link', { name: 'Solicitar este servicio' })).toBeVisible();
  });

  it('desde el detalle no ofrece solicitar el servicio propio', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 1 }),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(
      await screen.findByText('No puedes solicitar un servicio publicado por tu propia cuenta.')
    ).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Solicitar este servicio' })).not.toBeInTheDocument();
  });
});
