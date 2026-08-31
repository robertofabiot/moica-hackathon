import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  instalarApiFalsa,
  mensajeDeEjemplo,
  sesionDeEjemplo,
  solicitudConHiloDeEjemplo,
  solicitudDeServicioDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import { INTERVALO_DE_MENSAJES_MS } from '../hooks/useChat';

const RUTA_MENSAJES = '/api/solicitudes/21/mensajes';
const RUTA_DETALLE = '/api/solicitudes/21';

/** El cliente de la solicitud de ejemplo tiene el identificador 2; el prestador, el 1. */
const ID_CLIENTE = 2;
const ID_PRESTADOR = 1;

describe('Chat de una solicitud', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/21/contactos', { estado: 200, cuerpo: [] });
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

  it('anuncia la carga y luego el estado vacío', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [] });

    abrirComo(ID_CLIENTE);

    expect(
      await screen.findByText(
        'Todavía no hay mensajes. Escribe el primero para coordinar el trabajo.'
      )
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Mensajes' })).toBeVisible();
  });

  it('distingue los mensajes propios de los de la contraparte', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_MENSAJES}`, {
      estado: 200,
      cuerpo: [
        mensajeDeEjemplo({ idMensajeSolicitud: 1, idRemitente: ID_CLIENTE }),
        mensajeDeEjemplo({
          idMensajeSolicitud: 2,
          idRemitente: ID_PRESTADOR,
          nombreRemitente: 'Erving Miranda',
          contenido: 'Llego a las tres.',
        }),
      ],
    });

    abrirComo(ID_CLIENTE);

    const hilo = await screen.findByRole('log', { name: 'Mensajes de la solicitud' });
    const burbujas = hilo.querySelectorAll('article');

    expect(burbujas).toHaveLength(2);
    // El propio se rotula «Tú» y el ajeno con el nombre: la distinción no
    // depende solo del color ni del lado.
    expect(burbujas[0]).toHaveTextContent('Tú');
    expect(burbujas[0]).toHaveTextContent('¿A qué hora puede llegar?');
    expect(burbujas[1]).toHaveTextContent('Erving Miranda');
    expect(burbujas[1]).toHaveTextContent('Llego a las tres.');
  });

  it('muestra un error recuperable y vuelve a intentar', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.rechazar(`GET ${RUTA_MENSAJES}`, new TypeError('Failed to fetch'));

    const persona = userEvent.setup();
    abrirComo(ID_CLIENTE);

    const aviso = await screen.findByRole('alert');
    expect(aviso).toBeVisible();

    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [mensajeDeEjemplo()] });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText('¿A qué hora puede llegar?')).toBeVisible();
  });

  it('envía un mensaje y limpia el campo solo tras la confirmación', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [] });
    api.responder(`POST ${RUTA_MENSAJES}`, {
      estado: 201,
      cuerpo: mensajeDeEjemplo({ contenido: 'Voy en camino.' }),
    });

    const persona = userEvent.setup();
    abrirComo(ID_CLIENTE);

    const campo = await screen.findByLabelText('Mensaje');
    await persona.type(campo, 'Voy en camino.');
    api.responder(`GET ${RUTA_MENSAJES}`, {
      estado: 200,
      cuerpo: [mensajeDeEjemplo({ contenido: 'Voy en camino.' })],
    });
    await persona.click(screen.getByRole('button', { name: 'Enviar' }));

    await waitFor(() => {
      expect(api.ultima(`POST ${RUTA_MENSAJES}`)?.cuerpo).toEqual({ contenido: 'Voy en camino.' });
    });
    await waitFor(() => {
      expect(screen.getByLabelText('Mensaje')).toHaveValue('');
    });
  });

  it('conserva lo escrito cuando el envío falla', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [] });
    api.rechazar(`POST ${RUTA_MENSAJES}`, new TypeError('Failed to fetch'));

    const persona = userEvent.setup();
    abrirComo(ID_CLIENTE);

    const campo = await screen.findByLabelText('Mensaje');
    await persona.type(campo, 'Este texto no se debe perder.');
    await persona.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(await screen.findByRole('alert')).toBeVisible();
    expect(screen.getByLabelText('Mensaje')).toHaveValue('Este texto no se debe perder.');
  });

  it('no envía dos veces el mismo mensaje mientras la petición está en curso', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [] });
    api.colgar(`POST ${RUTA_MENSAJES}`);

    const persona = userEvent.setup();
    abrirComo(ID_CLIENTE);

    await persona.type(await screen.findByLabelText('Mensaje'), 'Un solo envío.');
    await persona.click(screen.getByRole('button', { name: 'Enviar' }));

    const enviando = await screen.findByRole('button', { name: 'Enviando…' });
    expect(enviando).toBeDisabled();
    await persona.click(enviando);

    expect(
      api.peticiones.filter((p) => p.metodo === 'POST' && p.ruta === RUTA_MENSAJES)
    ).toHaveLength(1);
  });

  it('no deja escribir un mensaje en blanco', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [] });

    const persona = userEvent.setup();
    abrirComo(ID_CLIENTE);

    const campo = await screen.findByLabelText('Mensaje');
    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled();

    await persona.type(campo, '    ');
    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled();
    expect(api.ultima(`POST ${RUTA_MENSAJES}`)).toBeUndefined();
  });

  it('refresca el hilo por short polling y apaga el temporizador al salir', async () => {
    vi.useFakeTimers();
    try {
      api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
      api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [] });

      const { unmount } = abrirComo(ID_CLIENTE);

      await vi.advanceTimersByTimeAsync(50);
      expect(lecturasDelHilo()).toBe(1);

      await vi.advanceTimersByTimeAsync(INTERVALO_DE_MENSAJES_MS + 50);
      expect(lecturasDelHilo()).toBe(2);

      await vi.advanceTimersByTimeAsync(INTERVALO_DE_MENSAJES_MS);
      expect(lecturasDelHilo()).toBe(3);

      unmount();
      const alSalir = lecturasDelHilo();
      await vi.advanceTimersByTimeAsync(INTERVALO_DE_MENSAJES_MS * 3);

      expect(lecturasDelHilo()).toBe(alSalir);
    } finally {
      vi.useRealTimers();
    }
  });

  it('deja el hilo en solo lectura cuando la solicitud se canceló tras aceptarse', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudConHiloDeEjemplo('CANCELADA'),
    });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [mensajeDeEjemplo()] });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText('¿A qué hora puede llegar?')).toBeVisible();
    expect(
      screen.getByText(
        'Esta solicitud ya se cerró. El historial queda visible, pero no admite mensajes nuevos.'
      )
    ).toBeVisible();
    expect(screen.queryByLabelText('Mensaje')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Enviar' })).not.toBeInTheDocument();
  });

  it('deja el hilo en solo lectura cuando la solicitud se completó', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudConHiloDeEjemplo('COMPLETADA'),
    });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [mensajeDeEjemplo()] });

    abrirComo(ID_PRESTADOR);

    expect(await screen.findByText('¿A qué hora puede llegar?')).toBeVisible();
    expect(screen.queryByLabelText('Mensaje')).not.toBeInTheDocument();
  });

  it('no muestra el chat cuando la solicitud nunca llegó a aceptarse', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo({ estadoActual: 'CANCELADA' }),
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText('Reparación de fugas')).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Mensajes' })).not.toBeInTheDocument();
    expect(api.ultima(`GET ${RUTA_MENSAJES}`)).toBeUndefined();
  });

  it('no muestra el chat en una solicitud pendiente', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudDeServicioDeEjemplo() });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText('Reparación de fugas')).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Mensajes' })).not.toBeInTheDocument();
    expect(api.ultima(`GET ${RUTA_MENSAJES}`)).toBeUndefined();
  });

  it('una cuenta restringida lee el hilo pero no ve el formulario', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [mensajeDeEjemplo()] });

    abrirComo(ID_CLIENTE, 'RESTRINGIDA_TEMPORAL');

    expect(await screen.findByText('¿A qué hora puede llegar?')).toBeVisible();
    expect(
      screen.getByText(
        'Tu cuenta está restringida: puedes leer el historial, pero por ahora no puedes enviar mensajes.'
      )
    ).toBeVisible();
    expect(screen.queryByLabelText('Mensaje')).not.toBeInTheDocument();
    expect(api.ultima(`POST ${RUTA_MENSAJES}`)).toBeUndefined();
  });

  it('explica el 409 del backend si el hilo dejó de admitir mensajes', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, { estado: 200, cuerpo: solicitudConHiloDeEjemplo() });
    api.responder(`GET ${RUTA_MENSAJES}`, { estado: 200, cuerpo: [] });
    api.responder(`POST ${RUTA_MENSAJES}`, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'CHAT_SOLO_LECTURA',
        'Esta solicitud ya se cerró: el historial sigue visible, pero no admite mensajes nuevos.'
      ),
    });

    const persona = userEvent.setup();
    abrirComo(ID_CLIENTE);

    await persona.type(await screen.findByLabelText('Mensaje'), 'Llego tarde.');
    await persona.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Esta solicitud ya se cerró: el historial sigue visible, pero no admite mensajes nuevos.'
    );
    expect(screen.getByLabelText('Mensaje')).toHaveValue('Llego tarde.');
  });

  function lecturasDelHilo() {
    return api.peticiones.filter((p) => p.metodo === 'GET' && p.ruta === RUTA_MENSAJES).length;
  }
});
