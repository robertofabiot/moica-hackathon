import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  instalarApiFalsa,
  sesionDeEjemplo,
  solicitudDeServicioDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

describe('Detalle de solicitud', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('el prestador ve aceptar y rechazar en una pendiente', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 1 }),
    });
    api.responder('GET /api/solicitudes/21', {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/solicitudes/21');

    expect(await screen.findByRole('button', { name: 'Aceptar' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Rechazar' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Cancelar solicitud' })).not.toBeInTheDocument();
    expect(screen.getAllByText('Pendiente').length).toBeGreaterThan(0);
  });

  it('el cliente ve cancelar y no aceptar', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 2 }),
    });
    api.responder('GET /api/solicitudes/21', {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/solicitudes/21');

    expect(await screen.findByRole('button', { name: 'Cancelar solicitud' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Aceptar' })).not.toBeInTheDocument();
  });

  it('rechaza y actualiza el estado', async () => {
    const persona = userEvent.setup();
    const pendiente = solicitudDeServicioDeEjemplo();
    const rechazada = solicitudDeServicioDeEjemplo({
      estadoActual: 'RECHAZADA',
      historial: [
        ...pendiente.historial,
        {
          idCambioEstadoSolicitud: 2,
          estadoAnterior: 'PENDIENTE',
          estadoNuevo: 'RECHAZADA',
          idActor: 1,
          nombreActor: 'Erving Miranda',
          motivo: null,
          fechaCambio: '2026-08-29T11:00:00-06:00',
        },
      ],
    });
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 1 }),
    });
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: pendiente });
    api.responder('POST /api/solicitudes/21/rechazo', { estado: 200, cuerpo: rechazada });

    renderizarConProveedores(<App />, '/solicitudes/21');
    await persona.click(await screen.findByRole('button', { name: 'Rechazar' }));
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: rechazada });
    await persona.click(screen.getByRole('button', { name: 'Sí, rechazar' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/solicitudes/21/rechazo')).toBeDefined();
    });
    expect(await screen.findByText(/ya está cerrada/)).toBeVisible();
  });

  it('acepta y actualiza el estado', async () => {
    const persona = userEvent.setup();
    const pendiente = solicitudDeServicioDeEjemplo();
    const aceptada = solicitudDeServicioDeEjemplo({
      estadoActual: 'ACEPTADA',
      historial: [
        ...pendiente.historial,
        {
          idCambioEstadoSolicitud: 2,
          estadoAnterior: 'PENDIENTE',
          estadoNuevo: 'ACEPTADA',
          idActor: 1,
          nombreActor: 'Erving Miranda',
          motivo: null,
          fechaCambio: '2026-08-29T11:00:00-06:00',
        },
      ],
    });
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 1 }),
    });
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: pendiente });
    api.responder('POST /api/solicitudes/21/aceptacion', { estado: 200, cuerpo: aceptada });

    renderizarConProveedores(<App />, '/solicitudes/21');
    await persona.click(await screen.findByRole('button', { name: 'Aceptar' }));
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: aceptada });
    await persona.click(screen.getByRole('button', { name: 'Sí, aceptar' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/solicitudes/21/aceptacion')).toBeDefined();
    });
    expect(await screen.findByText('Aceptada')).toBeVisible();
    expect(await screen.findByRole('button', { name: 'Marcar como completada' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancelar con motivo' })).toBeVisible();
  });

  it('exige motivo al cancelar una aceptada', async () => {
    const persona = userEvent.setup();
    const aceptada = solicitudDeServicioDeEjemplo({ estadoActual: 'ACEPTADA' });
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 2 }),
    });
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: aceptada });

    renderizarConProveedores(<App />, '/solicitudes/21');
    await persona.click(await screen.findByRole('button', { name: 'Cancelar con motivo' }));
    await persona.click(screen.getByRole('button', { name: 'Confirmar cancelación' }));

    expect(await screen.findByText('Indica el motivo de la cancelación.')).toBeVisible();
    expect(api.ultima('POST /api/solicitudes/21/cancelacion')).toBeUndefined();

    await persona.type(screen.getByLabelText('Motivo'), 'Ya no necesito la visita.');
    const cancelada = solicitudDeServicioDeEjemplo({ estadoActual: 'CANCELADA' });
    api.responder('POST /api/solicitudes/21/cancelacion', { estado: 200, cuerpo: cancelada });
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: cancelada });
    await persona.click(await screen.findByRole('button', { name: 'Confirmar cancelación' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/solicitudes/21/cancelacion')?.cuerpo).toEqual({
        motivo: 'Ya no necesito la visita.',
      });
    });
  });

  it('completa como prestador y presenta el historial en orden', async () => {
    const persona = userEvent.setup();
    const aceptada = solicitudDeServicioDeEjemplo({
      estadoActual: 'ACEPTADA',
      historial: [
        solicitudDeServicioDeEjemplo().historial[0] ?? {
          idCambioEstadoSolicitud: 1,
          estadoAnterior: null,
          estadoNuevo: 'PENDIENTE',
          idActor: 2,
          nombreActor: 'Ana Cliente',
          motivo: null,
          fechaCambio: '2026-08-29T10:00:00-06:00',
        },
        {
          idCambioEstadoSolicitud: 2,
          estadoAnterior: 'PENDIENTE',
          estadoNuevo: 'ACEPTADA',
          idActor: 1,
          nombreActor: 'Erving Miranda',
          motivo: null,
          fechaCambio: '2026-08-29T11:00:00-06:00',
        },
      ],
    });
    const completada = {
      ...aceptada,
      estadoActual: 'COMPLETADA' as const,
      historial: [
        ...aceptada.historial,
        {
          idCambioEstadoSolicitud: 3,
          estadoAnterior: 'ACEPTADA',
          estadoNuevo: 'COMPLETADA',
          idActor: 1,
          nombreActor: 'Erving Miranda',
          motivo: null,
          fechaCambio: '2026-08-29T12:00:00-06:00',
        },
      ],
    };
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 1 }),
    });
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: aceptada });
    api.responder('POST /api/solicitudes/21/completado', { estado: 200, cuerpo: completada });

    renderizarConProveedores(<App />, '/solicitudes/21');

    const historial = await screen.findByRole('list');
    const entradas = historial.querySelectorAll('li');
    expect(entradas[0]).toHaveTextContent('Pendiente');
    expect(entradas[1]).toHaveTextContent('Pendiente → Aceptada');

    await persona.click(screen.getByRole('button', { name: 'Marcar como completada' }));
    api.responder('GET /api/solicitudes/21', { estado: 200, cuerpo: completada });
    await persona.click(screen.getByRole('button', { name: 'Sí, completar' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/solicitudes/21/completado')).toBeDefined();
    });
    expect(await screen.findByText(/se marcó como completado/)).toBeVisible();
  });

  it('muestra un 403 y un fallo de red', async () => {
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('GET /api/solicitudes/21', {
      estado: 403,
      cuerpo: cuerpoDeError(403, 'CUENTA_RESTRINGIDA', 'Tu cuenta está restringida.'),
    });

    renderizarConProveedores(<App />, '/solicitudes/21');
    expect(await screen.findByRole('alert')).toHaveTextContent('Tu cuenta está restringida.');

    api.rechazar('GET /api/solicitudes/22', new TypeError('Failed to fetch'));
    renderizarConProveedores(<App />, '/solicitudes/22');
    expect(await screen.findByRole('alert')).toBeVisible();
  });

  it('un prestador restringido no ve aceptar ni rechazar una pendiente', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 1, estadoCuenta: 'RESTRINGIDA_TEMPORAL' }),
    });
    api.responder('GET /api/solicitudes/21', {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/solicitudes/21');

    expect(
      await screen.findByText(
        'Tu cuenta está restringida y por ahora no puede aceptar ni rechazar solicitudes.'
      )
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Aceptar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Rechazar' })).not.toBeInTheDocument();
    expect(api.ultima('POST /api/solicitudes/21/rechazo')).toBeUndefined();
    expect(api.ultima('POST /api/solicitudes/21/aceptacion')).toBeUndefined();
  });

  it('un cliente restringido conserva cancelar una pendiente', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 2, estadoCuenta: 'RESTRINGIDA_TEMPORAL' }),
    });
    api.responder('GET /api/solicitudes/21', {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/solicitudes/21');

    expect(await screen.findByRole('button', { name: 'Cancelar solicitud' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Aceptar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Rechazar' })).not.toBeInTheDocument();
  });

  it('un prestador restringido conserva cancelar y no completa una aceptada', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 1, estadoCuenta: 'RESTRINGIDA_TEMPORAL' }),
    });
    api.responder('GET /api/solicitudes/21', {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo({ estadoActual: 'ACEPTADA' }),
    });

    renderizarConProveedores(<App />, '/solicitudes/21');

    expect(
      await screen.findByText(
        'Tu cuenta está restringida y por ahora no puede completar solicitudes.'
      )
    ).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cancelar con motivo' })).toBeVisible();
    expect(
      screen.queryByRole('button', { name: 'Marcar como completada' })
    ).not.toBeInTheDocument();
    expect(api.ultima('POST /api/solicitudes/21/completado')).toBeUndefined();
  });

  it('una rechazada no muestra acciones de cambio', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario: 1 }),
    });
    api.responder('GET /api/solicitudes/21', {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo({ estadoActual: 'RECHAZADA' }),
    });

    renderizarConProveedores(<App />, '/solicitudes/21');

    expect(await screen.findByText(/ya está cerrada/)).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Aceptar' })).not.toBeInTheDocument();
  });
});
