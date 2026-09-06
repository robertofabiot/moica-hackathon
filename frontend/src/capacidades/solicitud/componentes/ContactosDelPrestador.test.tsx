import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  contactoReveladoDeEjemplo,
  cuerpoDeError,
  estadoDeCalificacionDeEjemplo,
  estadoDeReporteDeEjemplo,
  instalarApiFalsa,
  sesionDeEjemplo,
  solicitudConHiloDeEjemplo,
  solicitudDeServicioDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

const RUTA_CONTACTOS = '/api/solicitudes/21/contactos';
const RUTA_DETALLE = '/api/solicitudes/21';

const ID_CLIENTE = 2;
const ID_PRESTADOR = 1;

describe('Contactos revelados al cliente', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/21/mensajes', { estado: 200, cuerpo: [] });
    // El detalle monta también la calificación de P8; aquí se prueban los contactos.
    api.responder('GET /api/solicitudes/21/calificacion', {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo({ puedeCalificar: false, solicitudCompletada: false }),
    });
    // El detalle monta también el reporte de P9; aquí no se prueba.
    api.responder('GET /api/solicitudes/21/caso-moderacion', {
      estado: 200,
      cuerpo: estadoDeReporteDeEjemplo({ solicitudReportable: false, puedeReportar: false }),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function abrirComo(idUsuario: number, estadoCuenta?: 'RESTRINGIDA_TEMPORAL') {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario, estadoCuenta }),
    });
    return renderizarConProveedores(<App />, '/solicitudes/21');
  }

  it('el cliente ve los contactos de una solicitud aceptada, en su orden', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_CONTACTOS}`, {
      estado: 200,
      cuerpo: [
        contactoReveladoDeEjemplo(1, 'WhatsApp 8888-8888', 0),
        contactoReveladoDeEjemplo(2, 'taller.esperanza@correo.test', 1),
      ],
    });

    abrirComo(ID_CLIENTE);

    const lista = await screen.findByRole('list', {
      name: 'Medios de contacto del prestador',
    });
    const entradas = lista.querySelectorAll('li');

    expect(entradas[0]).toHaveTextContent('WhatsApp 8888-8888');
    expect(entradas[1]).toHaveTextContent('taller.esperanza@correo.test');
    expect(screen.getByRole('heading', { name: 'Contactos de Taller La Esperanza' })).toBeVisible();
  });

  it('los contactos se muestran como texto y no como enlaces', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_CONTACTOS}`, {
      estado: 200,
      cuerpo: [contactoReveladoDeEjemplo(1, 'https://ejemplo.test/taller', 0)],
    });

    abrirComo(ID_CLIENTE);

    const lista = await screen.findByRole('list', {
      name: 'Medios de contacto del prestador',
    });
    expect(lista).toHaveTextContent('https://ejemplo.test/taller');
    expect(lista.querySelectorAll('a')).toHaveLength(0);
  });

  it('anuncia un estado vacío honesto cuando el prestador no publicó contactos', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_CONTACTOS}`, { estado: 200, cuerpo: [] });

    abrirComo(ID_CLIENTE);

    expect(
      await screen.findByText(
        'Este prestador todavía no publicó ningún medio de contacto. Puedes coordinar el trabajo por los mensajes de esta solicitud.'
      )
    ).toBeVisible();
    expect(
      screen.queryByRole('list', { name: 'Medios de contacto del prestador' })
    ).not.toBeInTheDocument();
  });

  it('el prestador no ve la sección ni pide el recurso', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_CONTACTOS}`, {
      estado: 200,
      cuerpo: [contactoReveladoDeEjemplo()],
    });

    abrirComo(ID_PRESTADOR);

    expect(await screen.findByRole('heading', { name: 'Mensajes' })).toBeVisible();
    expect(screen.queryByText(/Contactos de/)).not.toBeInTheDocument();
    expect(screen.queryByText('WhatsApp 8888-8888')).not.toBeInTheDocument();
    expect(api.ultima(`GET ${RUTA_CONTACTOS}`)).toBeUndefined();
  });

  it('no hay contactos en una solicitud pendiente', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudDeServicioDeEjemplo() });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText('Reparación de fugas')).toBeVisible();
    expect(screen.queryByText(/Contactos de/)).not.toBeInTheDocument();
    expect(api.ultima(`GET ${RUTA_CONTACTOS}`)).toBeUndefined();
  });

  it('no hay contactos en una solicitud cancelada antes de aceptarse', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo({ estadoActual: 'CANCELADA' }),
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText('Reparación de fugas')).toBeVisible();
    expect(screen.queryByText(/Contactos de/)).not.toBeInTheDocument();
    expect(api.ultima(`GET ${RUTA_CONTACTOS}`)).toBeUndefined();
  });

  it('cancelar o completar después de aceptar no vuelve a ocultar los contactos', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudConHiloDeEjemplo('COMPLETADA'),
    });
    api.responder(`GET ${RUTA_CONTACTOS}`, {
      estado: 200,
      cuerpo: [contactoReveladoDeEjemplo()],
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText('WhatsApp 8888-8888')).toBeVisible();
  });

  it('muestra un error recuperable y vuelve a intentar', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_CONTACTOS}`, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'CONTACTOS_NO_REVELADOS',
        'Los contactos del prestador se revelan cuando acepta la solicitud.'
      ),
    });

    const persona = userEvent.setup();
    abrirComo(ID_CLIENTE);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Los contactos del prestador se revelan cuando acepta la solicitud.'
    );

    api.responder(`GET ${RUTA_CONTACTOS}`, {
      estado: 200,
      cuerpo: [contactoReveladoDeEjemplo()],
    });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('WhatsApp 8888-8888')).toBeVisible();
  });
});
