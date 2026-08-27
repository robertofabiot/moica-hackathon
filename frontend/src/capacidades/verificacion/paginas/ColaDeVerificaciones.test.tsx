import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { definirTiempoDeEsperaMs, TIEMPO_DE_ESPERA_MS } from '../../../comun/api';
import {
  cuerpoDeError,
  documentoDeVerificacionDeEjemplo,
  expedienteDeEjemplo,
  instalarApiFalsa,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import ColaDeVerificaciones from './ColaDeVerificaciones';

/**
 * La cola administrativa: qué se ve, qué se puede resolver y qué pasa cuando alguien se adelanta.
 *
 * Nada de lo que se comprueba aquí es un control de seguridad —eso lo aplica el backend en cada
 * petición—, sino que la pantalla no proponga lo que la API va a rechazar y que un conflicto se
 * explique en lugar de dejar el estado viejo en pantalla.
 */

const RUTA_SESION = 'GET /api/auth/sesion';
const RUTA_ABIERTAS = 'GET /api/admin/verificaciones?estado=PENDIENTE&estado=EN_REVISION';
const RUTA_APROBADAS = 'GET /api/admin/verificaciones?estado=APROBADA';
const RUTA_BASICAS =
  'GET /api/admin/verificaciones?estado=PENDIENTE&estado=EN_REVISION&nivel=BASICA';

const ID_ADMINISTRADORA = 1;

describe('cola de verificaciones', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder(RUTA_SESION, {
      estado: 200,
      cuerpo: sesionDeEjemplo({
        esAdministrador: true,
        segundoFactorRequerido: true,
        segundoFactorVerificado: true,
      }),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  function conCola(...expedientes: unknown[]) {
    api.responder(RUTA_ABIERTAS, { estado: 200, cuerpo: expedientes });
  }

  async function abrirElExpediente(nombre = 'Taller La Esperanza') {
    await userEvent.click(
      await screen.findByRole('button', { name: `Abrir el expediente de ${nombre}` })
    );
  }

  it('muestra la cola de lo que espera decisión con sus datos', async () => {
    conCola(expedienteDeEjemplo());
    renderizarConProveedores(<ColaDeVerificaciones />);

    expect(await screen.findByRole('table')).toBeInTheDocument();
    expect(screen.getByRole('rowheader', { name: 'Taller La Esperanza' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Verificación básica' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Pendiente de revisión' })).toBeInTheDocument();
  });

  it('los filtros piden a la API exactamente lo que se eligió', async () => {
    conCola(expedienteDeEjemplo());
    api.responder(RUTA_APROBADAS, { estado: 200, cuerpo: [] });
    api.responder(RUTA_BASICAS, { estado: 200, cuerpo: [expedienteDeEjemplo()] });
    renderizarConProveedores(<ColaDeVerificaciones />);

    await screen.findByRole('table');

    await userEvent.selectOptions(screen.getByLabelText('Nivel'), 'BASICA');
    await waitFor(() => expect(api.ultima(RUTA_BASICAS)).toBeDefined());

    await userEvent.selectOptions(screen.getByLabelText('Nivel'), '');
    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'APROBADA');
    await waitFor(() => expect(api.ultima(RUTA_APROBADAS)).toBeDefined());
    expect(await screen.findByText('No hay solicitudes con estos filtros.')).toBeInTheDocument();
  });

  it('el expediente se abre con los metadatos y un enlace hacia Moica, no hacia el proveedor', async () => {
    conCola(
      expedienteDeEjemplo({
        documentos: [documentoDeVerificacionDeEjemplo(9, 'IDENTIDAD', 'cedula.png')],
      })
    );
    renderizarConProveedores(<ColaDeVerificaciones />);
    await abrirElExpediente();

    expect(screen.getByText('Documento de identidad')).toBeInTheDocument();
    expect(screen.getByText('liz@moica.test', { exact: false })).toBeInTheDocument();

    const enlace = screen.getByRole('link', { name: 'Abrir cedula.png' });
    expect(enlace).toHaveAttribute('href', '/api/admin/verificaciones/1/documentos/9/acceso');
    expect(enlace.getAttribute('href')).not.toContain('r2.cloudflarestorage');
  });

  it('toma una solicitud pendiente', async () => {
    conCola(expedienteDeEjemplo());
    api.responder('POST /api/admin/verificaciones/1/toma', {
      estado: 200,
      cuerpo: expedienteDeEjemplo({
        estadoSolicitud: 'EN_REVISION',
        idAdministradorRevisor: ID_ADMINISTRADORA,
      }),
    });
    renderizarConProveedores(<ColaDeVerificaciones />);
    await abrirElExpediente();

    await userEvent.click(screen.getByRole('button', { name: 'Tomar para revisar' }));

    await waitFor(() => expect(api.ultima('POST /api/admin/verificaciones/1/toma')).toBeDefined());
  });

  it('una toma que llega tarde se explica y la cola se vuelve a pedir', async () => {
    conCola(expedienteDeEjemplo());
    api.responder('POST /api/admin/verificaciones/1/toma', {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'SOLICITUD_YA_TOMADA',
        'Otra persona administradora ya tomó esta solicitud.'
      ),
    });
    renderizarConProveedores(<ColaDeVerificaciones />);
    await abrirElExpediente();

    const peticionesAntes = api.peticiones.length;
    await userEvent.click(screen.getByRole('button', { name: 'Tomar para revisar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Otra persona administradora ya tomó esta solicitud.'
    );
    await waitFor(() => expect(api.peticiones.length).toBeGreaterThan(peticionesAntes + 1));
  });

  it('solo ofrece resolver a quien tomó la revisión', async () => {
    conCola(expedienteDeEjemplo({ estadoSolicitud: 'EN_REVISION', idAdministradorRevisor: 99 }));
    renderizarConProveedores(<ColaDeVerificaciones />);
    await abrirElExpediente();

    expect(screen.queryByRole('button', { name: 'Aprobar' })).not.toBeInTheDocument();
    expect(screen.getByText(/la está revisando otra persona administradora/i)).toBeInTheDocument();
  });

  it('aprueba una solicitud que se tomó en esta sesión', async () => {
    conCola(
      expedienteDeEjemplo({
        estadoSolicitud: 'EN_REVISION',
        idAdministradorRevisor: ID_ADMINISTRADORA,
      })
    );
    api.responder('POST /api/admin/verificaciones/1/aprobacion', {
      estado: 200,
      cuerpo: expedienteDeEjemplo({ estadoSolicitud: 'APROBADA' }),
    });
    renderizarConProveedores(<ColaDeVerificaciones />);
    await abrirElExpediente();

    await userEvent.click(screen.getByRole('button', { name: 'Aprobar' }));

    await waitFor(() =>
      expect(api.ultima('POST /api/admin/verificaciones/1/aprobacion')).toBeDefined()
    );
  });

  it('el rechazo exige un motivo antes de llamar a la API', async () => {
    conCola(
      expedienteDeEjemplo({
        estadoSolicitud: 'EN_REVISION',
        idAdministradorRevisor: ID_ADMINISTRADORA,
      })
    );
    renderizarConProveedores(<ColaDeVerificaciones />);
    await abrirElExpediente();

    await userEvent.click(screen.getByRole('button', { name: 'Rechazar con motivo' }));
    await userEvent.click(screen.getByRole('button', { name: 'Rechazar' }));

    expect(await screen.findByText(/Escribe el motivo/i)).toBeInTheDocument();
    expect(api.ultima('POST /api/admin/verificaciones/1/rechazo')).toBeUndefined();
  });

  it('rechaza con el motivo escrito', async () => {
    conCola(
      expedienteDeEjemplo({
        estadoSolicitud: 'EN_REVISION',
        idAdministradorRevisor: ID_ADMINISTRADORA,
      })
    );
    api.responder('POST /api/admin/verificaciones/1/rechazo', {
      estado: 200,
      cuerpo: expedienteDeEjemplo({ estadoSolicitud: 'RECHAZADA' }),
    });
    renderizarConProveedores(<ColaDeVerificaciones />);
    await abrirElExpediente();

    await userEvent.click(screen.getByRole('button', { name: 'Rechazar con motivo' }));
    await userEvent.type(screen.getByLabelText('Motivo'), 'El documento está ilegible.');
    await userEvent.click(screen.getByRole('button', { name: 'Rechazar' }));

    await waitFor(() =>
      expect(api.ultima('POST /api/admin/verificaciones/1/rechazo')?.cuerpo).toEqual({
        observacion: 'El documento está ilegible.',
      })
    );
  });

  it('revocar avisa de sus efectos y exige motivo y confirmación explícita', async () => {
    api.responder(RUTA_APROBADAS, {
      estado: 200,
      cuerpo: [
        expedienteDeEjemplo({
          estadoSolicitud: 'APROBADA',
          prestador: {
            ...expedienteDeEjemplo().prestador,
            nivelVerificacion: 'VERIFICADO_BASICO',
          },
        }),
      ],
    });
    conCola();
    api.responder('POST /api/admin/verificaciones/1/revocacion', {
      estado: 200,
      cuerpo: expedienteDeEjemplo({ estadoSolicitud: 'REVOCADA' }),
    });
    renderizarConProveedores(<ColaDeVerificaciones />);

    await screen.findByText('No hay solicitudes con estos filtros.');
    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'APROBADA');
    await abrirElExpediente();

    await userEvent.click(screen.getByRole('button', { name: 'Revocar con motivo' }));
    expect(
      screen.getByText(/si tenía la profesional, esa también queda sin efecto/i)
    ).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Motivo'), 'El documento no era auténtico.');
    await userEvent.click(screen.getByRole('button', { name: 'Sí, revocar la verificación' }));

    expect(await screen.findByText(/Marca la casilla para confirmar/i)).toBeInTheDocument();
    expect(api.ultima('POST /api/admin/verificaciones/1/revocacion')).toBeUndefined();

    await userEvent.click(screen.getByLabelText('Entiendo que este perfil perderá su insignia.'));
    await userEvent.click(screen.getByRole('button', { name: 'Sí, revocar la verificación' }));

    await waitFor(() =>
      expect(api.ultima('POST /api/admin/verificaciones/1/revocacion')?.cuerpo).toEqual({
        observacion: 'El documento no era auténtico.',
      })
    );
  });

  it('si la cola no carga lo dice y permite reintentar', async () => {
    api.responder(RUTA_ABIERTAS, {
      estado: 500,
      cuerpo: cuerpoDeError(500, 'ERROR_INTERNO', 'No pudimos completar la operación.'),
    });
    renderizarConProveedores(<ColaDeVerificaciones />);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos completar la operación.'
    );
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });

  it('sin conexión lo explica en lugar de quedarse cargando', async () => {
    api.rechazar(RUTA_ABIERTAS);
    renderizarConProveedores(<ColaDeVerificaciones />);

    expect(await screen.findByRole('alert')).toHaveTextContent(/Revisa tu conexión/i);
  });

  it('un 401 en la cola da la sesión por terminada', async () => {
    // Primero se carga la pantalla con la sesión viva, y solo después llega el
    // 401: la regla del cliente de consultas distingue «la sesión murió» de «no
    // había ninguna», y probarla exige que antes hubiera una.
    conCola(expedienteDeEjemplo());
    api.responder(RUTA_APROBADAS, {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Inicia sesión para continuar.'),
    });
    const { cliente } = renderizarConProveedores(<ColaDeVerificaciones />);

    await screen.findByRole('table');
    await waitFor(() => expect(cliente.getQueryData(['auth', 'sesion'])).toBeTruthy());

    await userEvent.selectOptions(screen.getByLabelText('Estado'), 'APROBADA');

    await waitFor(() => expect(cliente.getQueryData(['auth', 'sesion'])).toBeNull());
  });
});
