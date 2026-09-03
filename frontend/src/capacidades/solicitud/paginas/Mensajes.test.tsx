import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  instalarApiFalsa,
  mensajeDeEjemplo,
  resumenDeSolicitudDeEjemplo,
  sesionDeEjemplo,
  solicitudConHiloDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import { rutaDeMensajes } from '../rutas';

const ID_CLIENTE = 2;
const ID_PRESTADOR = 1;

describe('Pantalla de mensajes', () => {
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

  function abrirComo(idUsuario: number, ruta = rutaDeMensajes()) {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario }),
    });
    return renderizarConProveedores(<App />, ruta);
  }

  function prepararHilo(
    idSolicitud: number,
    estado: 'ACEPTADA' | 'CANCELADA' | 'COMPLETADA' = 'ACEPTADA',
    mensajes: ReturnType<typeof mensajeDeEjemplo>[] = []
  ) {
    api.responder(`GET /api/solicitudes/${idSolicitud}`, {
      estado: 200,
      cuerpo: {
        ...solicitudConHiloDeEjemplo(estado),
        idSolicitudServicio: idSolicitud,
      },
    });
    api.responder(`GET /api/solicitudes/${idSolicitud}/mensajes`, {
      estado: 200,
      cuerpo: mensajes,
    });
  }

  it('sin sesión redirige a iniciar sesión', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });

    renderizarConProveedores(<App />, rutaDeMensajes());

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });

  it('muestra el vacío de la bandeja y el estado vacío del hilo', async () => {
    abrirComo(ID_PRESTADOR);

    expect(
      await screen.findByText(
        'Todavía no tienes conversaciones. Aparecerán aquí cuando una solicitud sea aceptada.'
      )
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Mensajes' })).toBeVisible();
    expect(
      screen.getByText('Selecciona una conversación para ver los mensajes')
    ).toBeInTheDocument();
  });

  it('anuncia la carga de la bandeja', async () => {
    api.colgar('GET /api/solicitudes/enviadas');
    api.colgar('GET /api/solicitudes/recibidas');

    abrirComo(ID_PRESTADOR);

    expect(await screen.findByText('Cargando conversaciones…')).toBeVisible();
  });

  it('deja fuera pendientes y rechazadas y lista aceptadas, completadas y canceladas', async () => {
    api.responder('GET /api/solicitudes/enviadas', {
      estado: 200,
      cuerpo: [
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 21,
          estadoActual: 'ACEPTADA',
          idCliente: ID_PRESTADOR,
          nombrePublicoPrestador: 'Electricista Norte',
          nombreServicio: 'Instalación eléctrica',
        }),
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 24,
          estadoActual: 'PENDIENTE',
          nombreServicio: 'No debe verse pendiente',
        }),
      ],
    });
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 22,
          estadoActual: 'COMPLETADA',
          nombreCliente: 'Bruno Pérez',
          nombreServicio: 'Pintura',
        }),
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 23,
          estadoActual: 'CANCELADA',
          nombreCliente: 'Carla Ruiz',
          nombreServicio: 'Jardinería',
        }),
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 25,
          estadoActual: 'RECHAZADA',
          nombreServicio: 'No debe verse rechazada',
        }),
      ],
    });

    abrirComo(ID_PRESTADOR);

    expect(await screen.findByText('Electricista Norte')).toBeVisible();
    expect(screen.getByText('Bruno Pérez')).toBeVisible();
    expect(screen.getByText('Carla Ruiz')).toBeVisible();
    expect(screen.queryByText('No debe verse pendiente')).not.toBeInTheDocument();
    expect(screen.queryByText('No debe verse rechazada')).not.toBeInTheDocument();
  });

  it('filtra la bandeja por nombre de la contraparte o del servicio', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [
        resumenDeSolicitudDeEjemplo({ estadoActual: 'ACEPTADA' }),
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 22,
          estadoActual: 'ACEPTADA',
          nombreCliente: 'Bruno Pérez',
          nombreServicio: 'Electricidad',
        }),
      ],
    });

    abrirComo(ID_PRESTADOR);

    expect(await screen.findByText('Ana Cliente')).toBeVisible();
    expect(screen.getByText('Bruno Pérez')).toBeVisible();

    await persona.type(
      screen.getByRole('searchbox', { name: 'Buscar conversaciones por nombre o servicio' }),
      'bruno'
    );

    expect(screen.getByText('Bruno Pérez')).toBeVisible();
    expect(screen.queryByText('Ana Cliente')).not.toBeInTheDocument();

    await persona.clear(
      screen.getByRole('searchbox', { name: 'Buscar conversaciones por nombre o servicio' })
    );
    await persona.type(
      screen.getByRole('searchbox', { name: 'Buscar conversaciones por nombre o servicio' }),
      'fugas'
    );

    expect(screen.getByText('Ana Cliente')).toBeVisible();
    expect(screen.queryByText('Bruno Pérez')).not.toBeInTheDocument();

    await persona.clear(
      screen.getByRole('searchbox', { name: 'Buscar conversaciones por nombre o servicio' })
    );
    await persona.type(
      screen.getByRole('searchbox', { name: 'Buscar conversaciones por nombre o servicio' }),
      'zzzz'
    );

    expect(screen.getByText('Ninguna conversación coincide con la búsqueda.')).toBeVisible();
  });

  it('abre el hilo al elegir una conversación y distingue propios de ajenos', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [resumenDeSolicitudDeEjemplo({ estadoActual: 'ACEPTADA' })],
    });
    prepararHilo(21, 'ACEPTADA', [
      mensajeDeEjemplo({ idMensajeSolicitud: 1, idRemitente: ID_CLIENTE }),
      mensajeDeEjemplo({
        idMensajeSolicitud: 2,
        idRemitente: ID_PRESTADOR,
        nombreRemitente: 'Erving Miranda',
        contenido: 'Llego a las tres.',
      }),
    ]);

    abrirComo(ID_PRESTADOR);

    await persona.click(await screen.findByRole('button', { name: /Ana Cliente/ }));

    const hilo = await screen.findByRole('log', { name: 'Mensajes de la conversación' });
    const burbujas = hilo.querySelectorAll('article');
    expect(burbujas).toHaveLength(2);
    expect(burbujas[0]).toHaveTextContent('¿A qué hora puede llegar?');
    expect(burbujas[1]).toHaveTextContent('Llego a las tres.');
    expect(screen.getByRole('heading', { name: 'Ana Cliente' })).toBeVisible();
    expect(screen.getByText('En línea')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ver detalle del servicio' })).toHaveAttribute(
      'href',
      '/solicitudes/21'
    );
  });

  it('abre el hilo desde la dirección y deja el ítem de mensajes activo', async () => {
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [resumenDeSolicitudDeEjemplo({ estadoActual: 'ACEPTADA' })],
    });
    prepararHilo(21, 'ACEPTADA', [mensajeDeEjemplo()]);

    abrirComo(ID_PRESTADOR, rutaDeMensajes(21));

    expect(await screen.findByText('¿A qué hora puede llegar?')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Mensajes', hidden: true })).toHaveAttribute(
      'aria-current',
      'page'
    );
  });

  it('envía un mensaje y limpia el campo solo tras la confirmación', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [resumenDeSolicitudDeEjemplo({ estadoActual: 'ACEPTADA' })],
    });
    prepararHilo(21, 'ACEPTADA', []);
    api.responder('POST /api/solicitudes/21/mensajes', {
      estado: 201,
      cuerpo: mensajeDeEjemplo({ contenido: 'Voy en camino.' }),
    });

    abrirComo(ID_PRESTADOR, rutaDeMensajes(21));

    const campo = await screen.findByLabelText('Mensaje');
    await persona.type(campo, 'Voy en camino.');
    api.responder('GET /api/solicitudes/21/mensajes', {
      estado: 200,
      cuerpo: [mensajeDeEjemplo({ contenido: 'Voy en camino.' })],
    });
    await persona.click(screen.getByRole('button', { name: 'Enviar' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/solicitudes/21/mensajes')?.cuerpo).toEqual({
        contenido: 'Voy en camino.',
      });
    });
    await waitFor(() => {
      expect(screen.getByLabelText('Mensaje')).toHaveValue('');
    });
  });

  it('conserva lo escrito cuando el envío falla', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [resumenDeSolicitudDeEjemplo({ estadoActual: 'ACEPTADA' })],
    });
    prepararHilo(21, 'ACEPTADA', []);
    api.rechazar('POST /api/solicitudes/21/mensajes', new TypeError('Failed to fetch'));

    abrirComo(ID_PRESTADOR, rutaDeMensajes(21));

    const campo = await screen.findByLabelText('Mensaje');
    await persona.type(campo, 'Este texto no se debe perder.');
    await persona.click(screen.getByRole('button', { name: 'Enviar' }));

    expect(await screen.findByRole('alert')).toBeVisible();
    expect(screen.getByLabelText('Mensaje')).toHaveValue('Este texto no se debe perder.');
  });

  it('no deja escribir un mensaje en blanco', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [resumenDeSolicitudDeEjemplo({ estadoActual: 'ACEPTADA' })],
    });
    prepararHilo(21, 'ACEPTADA', []);

    abrirComo(ID_PRESTADOR, rutaDeMensajes(21));

    const campo = await screen.findByLabelText('Mensaje');
    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled();

    await persona.type(campo, '    ');
    expect(screen.getByRole('button', { name: 'Enviar' })).toBeDisabled();
    expect(api.ultima('POST /api/solicitudes/21/mensajes')).toBeUndefined();
  });

  it('deja el hilo en solo lectura cuando la solicitud se completó', async () => {
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [resumenDeSolicitudDeEjemplo({ estadoActual: 'COMPLETADA' })],
    });
    prepararHilo(21, 'COMPLETADA', [mensajeDeEjemplo()]);

    abrirComo(ID_PRESTADOR, rutaDeMensajes(21));

    expect(await screen.findByText('¿A qué hora puede llegar?')).toBeVisible();
    expect(
      screen.getByText('Esta conversación ha finalizado y permanece en solo lectura')
    ).toBeVisible();
    expect(screen.queryByLabelText('Mensaje')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Enviar' })).not.toBeInTheDocument();
  });

  it('una cuenta restringida lee el hilo pero no ve el formulario', async () => {
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [resumenDeSolicitudDeEjemplo({ estadoActual: 'ACEPTADA' })],
    });
    prepararHilo(21, 'ACEPTADA', [mensajeDeEjemplo()]);
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({
        idUsuario: ID_PRESTADOR,
        estadoCuenta: 'RESTRINGIDA_TEMPORAL',
      }),
    });

    renderizarConProveedores(<App />, rutaDeMensajes(21));

    expect(await screen.findByText('¿A qué hora puede llegar?')).toBeVisible();
    expect(
      screen.getByText(
        'Tu cuenta está restringida: puedes leer el historial, pero por ahora no puedes enviar mensajes.'
      )
    ).toBeVisible();
    expect(screen.queryByLabelText('Mensaje')).not.toBeInTheDocument();
  });

  it('muestra un error recuperable en la bandeja y vuelve a intentar', async () => {
    const persona = userEvent.setup();
    api.rechazar('GET /api/solicitudes/enviadas', new TypeError('Failed to fetch'));
    api.rechazar('GET /api/solicitudes/recibidas', new TypeError('Failed to fetch'));

    abrirComo(ID_PRESTADOR);

    expect(await screen.findByRole('alert')).toBeVisible();

    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(
      await screen.findByText(
        'Todavía no tienes conversaciones. Aparecerán aquí cuando una solicitud sea aceptada.'
      )
    ).toBeVisible();
  });
});
