import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  calificacionDeEjemplo,
  cuerpoDeError,
  estadoDeCalificacionDeEjemplo,
  instalarApiFalsa,
  sesionDeEjemplo,
  solicitudConHiloDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

const RUTA_DETALLE = '/api/solicitudes/21';
const RUTA_CALIFICACION = '/api/solicitudes/21/calificacion';

/** El cliente de la solicitud de ejemplo tiene el identificador 2; el prestador, el 1. */
const ID_CLIENTE = 2;
const ID_PRESTADOR = 1;

describe('Calificación de una solicitud', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/21/mensajes', { estado: 200, cuerpo: [] });
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

  function conSolicitudCompletada() {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudConHiloDeEjemplo('COMPLETADA'),
    });
  }

  it('no muestra el formulario antes de que la solicitud esté completada', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudConHiloDeEjemplo('ACEPTADA'),
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByRole('heading', { name: 'Acciones' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Calificación' })).not.toBeInTheDocument();
    expect(screen.queryByRole('radio', { name: '5 estrellas' })).not.toBeInTheDocument();
    // Ni siquiera se consulta el estado: no hay nada que decidir todavía.
    expect(api.ultima(`GET ${RUTA_CALIFICACION}`)).toBeUndefined();
  });

  it('en una solicitud completada dice a quién se califica y en qué rol', async () => {
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });

    abrirComo(ID_CLIENTE);

    // El encabezado se pinta antes de que llegue el estado, así que la frase se
    // espera con `findByText` y no con `getByText`.
    expect(
      await screen.findByText(/Calificas a Taller La Esperanza como prestador\./)
    ).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Calificación' })).toBeVisible();
    expect(screen.getByText(/Calificar es opcional/)).toBeVisible();
  });

  it('el selector de estrellas es accesible y operable con el teclado', async () => {
    const persona = userEvent.setup();
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });

    abrirComo(ID_CLIENTE);

    const primera = await screen.findByRole('radio', { name: '1 estrella' });
    expect(screen.getByRole('group', { name: 'Puntuación' })).toBeVisible();
    expect(screen.getAllByRole('radio')).toHaveLength(5);
    expect(screen.getByRole('radio', { name: '5 estrellas' })).toBeVisible();

    // El grupo de radios nativo mueve la selección con las flechas.
    primera.focus();
    await persona.keyboard('{ArrowRight}{ArrowRight}');

    expect(screen.getByRole('radio', { name: '3 estrellas' })).toBeChecked();
    expect(screen.getByRole('button', { name: 'Guardar calificación' })).toBeEnabled();
  });

  it('el botón está deshabilitado mientras no se elige una puntuación', async () => {
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByRole('button', { name: 'Guardar calificación' })).toBeDisabled();
  });

  it('envía la puntuación y el comentario, y el cuerpo no lleva calificado ni rol', async () => {
    const persona = userEvent.setup();
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });
    api.responder(`POST ${RUTA_CALIFICACION}`, {
      estado: 201,
      cuerpo: calificacionDeEjemplo({ puntuacion: 5, comentario: 'Trabajo impecable.' }),
    });

    abrirComo(ID_CLIENTE);

    await persona.click(await screen.findByRole('radio', { name: '5 estrellas' }));
    await persona.type(screen.getByLabelText('Comentario (opcional)'), 'Trabajo impecable.');

    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo({
        puedeCalificar: false,
        calificacionEmitida: calificacionDeEjemplo({
          puntuacion: 5,
          comentario: 'Trabajo impecable.',
        }),
      }),
    });
    await persona.click(screen.getByRole('button', { name: 'Guardar calificación' }));

    await waitFor(() => {
      expect(api.ultima(`POST ${RUTA_CALIFICACION}`)?.cuerpo).toEqual({
        puntuacion: 5,
        comentario: 'Trabajo impecable.',
      });
    });
  });

  it('un comentario en blanco viaja como nulo', async () => {
    const persona = userEvent.setup();
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });
    api.responder(`POST ${RUTA_CALIFICACION}`, {
      estado: 201,
      cuerpo: calificacionDeEjemplo({ puntuacion: 4, comentario: null }),
    });

    abrirComo(ID_CLIENTE);

    await persona.click(await screen.findByRole('radio', { name: '4 estrellas' }));
    await persona.type(screen.getByLabelText('Comentario (opcional)'), '   ');
    await persona.click(screen.getByRole('button', { name: 'Guardar calificación' }));

    await waitFor(() => {
      expect(api.ultima(`POST ${RUTA_CALIFICACION}`)?.cuerpo).toEqual({
        puntuacion: 4,
        comentario: null,
      });
    });
  });

  it('muestra el estado pendiente y no envía dos veces', async () => {
    const persona = userEvent.setup();
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });
    api.colgar(`POST ${RUTA_CALIFICACION}`);

    abrirComo(ID_CLIENTE);

    await persona.click(await screen.findByRole('radio', { name: '5 estrellas' }));
    await persona.click(screen.getByRole('button', { name: 'Guardar calificación' }));

    const guardando = await screen.findByRole('button', { name: 'Guardando…' });
    expect(guardando).toBeDisabled();
    await persona.click(guardando);

    expect(
      api.peticiones.filter((p) => p.metodo === 'POST' && p.ruta === RUTA_CALIFICACION)
    ).toHaveLength(1);
  });

  it('un error conserva la puntuación y el comentario para reintentar', async () => {
    const persona = userEvent.setup();
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });
    api.rechazar(`POST ${RUTA_CALIFICACION}`, new TypeError('Failed to fetch'));

    abrirComo(ID_CLIENTE);

    await persona.click(await screen.findByRole('radio', { name: '3 estrellas' }));
    await persona.type(screen.getByLabelText('Comentario (opcional)'), 'Este texto no se pierde.');
    await persona.click(screen.getByRole('button', { name: 'Guardar calificación' }));

    expect(await screen.findByRole('alert')).toBeVisible();
    expect(screen.getByRole('radio', { name: '3 estrellas' })).toBeChecked();
    expect(screen.getByLabelText('Comentario (opcional)')).toHaveValue('Este texto no se pierde.');
  });

  it('explica el 409 del backend cuando la calificación ya existía', async () => {
    const persona = userEvent.setup();
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });
    api.responder(`POST ${RUTA_CALIFICACION}`, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'CALIFICACION_DUPLICADA',
        'Ya calificaste esta solicitud. Las calificaciones no se editan.'
      ),
    });

    abrirComo(ID_CLIENTE);

    await persona.click(await screen.findByRole('radio', { name: '5 estrellas' }));
    await persona.click(screen.getByRole('button', { name: 'Guardar calificación' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Ya calificaste esta solicitud. Las calificaciones no se editan.'
    );
  });

  it('quien ya calificó ve el resumen inmutable y no el formulario', async () => {
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo({
        puedeCalificar: false,
        calificacionEmitida: calificacionDeEjemplo(),
      }),
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByLabelText('4 estrellas')).toBeVisible();
    expect(screen.getByText('Puntual y ordenado.')).toBeVisible();
    expect(screen.getByText(/Calificaste a Taller La Esperanza como prestador\./)).toBeVisible();
    expect(screen.getByText('Las calificaciones no se editan ni se borran.')).toBeVisible();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Guardar calificación' })).not.toBeInTheDocument();
  });

  it('el prestador califica al cliente en el rol que le corresponde', async () => {
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo({
        idCalificado: ID_CLIENTE,
        nombreCalificado: 'Ana Cliente',
        rolCalificado: 'CLIENTE',
      }),
    });

    abrirComo(ID_PRESTADOR);

    expect(await screen.findByText(/Calificas a Ana Cliente como cliente\./)).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Calificación' })).toBeVisible();
  });

  it('una cuenta restringida no ve el formulario y recibe una explicación', async () => {
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo({ puedeCalificar: false }),
    });

    abrirComo(ID_CLIENTE, 'RESTRINGIDA_TEMPORAL');

    expect(
      await screen.findByText(/Tu cuenta está restringida: por ahora no puedes calificar\./)
    ).toBeVisible();
    expect(screen.queryByRole('radio')).not.toBeInTheDocument();
    expect(api.ultima(`POST ${RUTA_CALIFICACION}`)).toBeUndefined();
  });

  it('muestra el error de carga y permite reintentar', async () => {
    const persona = userEvent.setup();
    conSolicitudCompletada();
    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 500,
      cuerpo: cuerpoDeError(500, 'ERROR_INTERNO', 'Algo falló en Moica.'),
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo falló en Moica.');

    api.responder(`GET ${RUTA_CALIFICACION}`, {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo(),
    });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('radio', { name: '5 estrellas' })).toBeVisible();
  });
});
