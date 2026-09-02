import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  casoDeModeracionDeEjemplo,
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

const RUTA_DETALLE = '/api/solicitudes/21';
const RUTA_REPORTE = '/api/solicitudes/21/caso-moderacion';

/** El cliente de la solicitud de ejemplo tiene el identificador 2; el prestador, el 1. */
const ID_CLIENTE = 2;

describe('Reporte de una solicitud', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/21/mensajes', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/21/contactos', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/21/calificacion', {
      estado: 200,
      cuerpo: estadoDeCalificacionDeEjemplo({ puedeCalificar: false }),
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

  function conSolicitud(estado: 'ACEPTADA' | 'CANCELADA' | 'COMPLETADA' = 'ACEPTADA') {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudConHiloDeEjemplo(estado),
    });
  }

  function conEstadoDeReporte(cambios: Parameters<typeof estadoDeReporteDeEjemplo>[0] = {}) {
    api.responder(`GET ${RUTA_REPORTE}`, {
      estado: 200,
      cuerpo: estadoDeReporteDeEjemplo(cambios),
    });
  }

  async function abrirFormulario(persona: ReturnType<typeof userEvent.setup>) {
    await persona.click(await screen.findByRole('button', { name: 'Reportar un problema' }));
    return screen.findByRole('textbox', { name: 'Motivo' });
  }

  // --- Visibilidad de la acción -------------------------------------------

  it('no ofrece reportar en una solicitud que nunca fue aceptada', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo({ estadoActual: 'PENDIENTE' }),
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByRole('heading', { name: 'Acciones' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Reportar un problema' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reportar un problema' })).not.toBeInTheDocument();
    // Ni siquiera se consulta el estado: no hay nada que decidir todavía.
    expect(api.ultima(`GET ${RUTA_REPORTE}`)).toBeUndefined();
  });

  it('no ofrece reportar en una cancelada que nunca llegó a aceptarse', async () => {
    api.responder(`GET ${RUTA_DETALLE}`, {
      estado: 200,
      cuerpo: solicitudDeServicioDeEjemplo({ estadoActual: 'CANCELADA' }),
    });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByRole('heading', { name: 'Acciones' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'Reportar un problema' })).not.toBeInTheDocument();
    expect(api.ultima(`GET ${RUTA_REPORTE}`)).toBeUndefined();
  });

  it.each(['ACEPTADA', 'COMPLETADA', 'CANCELADA'] as const)(
    'ofrece reportar en una solicitud que llegó a aceptarse y quedó %s',
    async (estado) => {
      conSolicitud(estado);
      conEstadoDeReporte();

      abrirComo(ID_CLIENTE);

      expect(await screen.findByRole('button', { name: 'Reportar un problema' })).toBeVisible();
      expect(screen.getByRole('heading', { name: 'Reportar un problema' })).toBeVisible();
    }
  );

  it('explica que se abrirá un caso y que reportar no sanciona automáticamente', async () => {
    conSolicitud();
    conEstadoDeReporte();

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText(/Se abrirá un caso/)).toBeVisible();
    expect(screen.getByText(/no sanciona automáticamente/)).toBeVisible();
  });

  it('avisa cuando la solicitud ya no admite el reporte de esta persona', async () => {
    conSolicitud();
    conEstadoDeReporte({ puedeReportar: false });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText('Esta solicitud ya no admite tu reporte.')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Reportar un problema' })).not.toBeInTheDocument();
  });

  // --- Formulario y accesibilidad -----------------------------------------

  it('el formulario tiene etiquetas visibles y campos con nombre accesible', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    abrirComo(ID_CLIENTE);

    await abrirFormulario(persona);

    expect(screen.getByRole('textbox', { name: 'Motivo' })).toBeVisible();
    expect(screen.getByRole('textbox', { name: 'Descripción' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Enviar reporte' })).toBeEnabled();
    expect(screen.getByText(/Reportas a Taller La Esperanza\./)).toBeVisible();
    expect(screen.getByText(/0 de 3000 caracteres/)).toBeVisible();
  });

  it('exige motivo y descripción, y lo dice de forma accesible', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    abrirComo(ID_CLIENTE);
    await abrirFormulario(persona);

    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    expect(await screen.findByText('Indica el motivo del reporte.')).toBeVisible();
    expect(screen.getByText('Describe lo que ocurrió.')).toBeVisible();
    expect(screen.getByRole('textbox', { name: 'Motivo' })).toHaveAttribute('aria-invalid', 'true');
    // El contador va tambien en la descripcion accesible: es lo que ata ese
    // «de 3000 caracteres» a este campo y no al de motivo.
    expect(screen.getByRole('textbox', { name: 'Descripción' })).toHaveAccessibleDescription(
      'Describe lo que ocurrió. 0 de 3000 caracteres'
    );
    expect(api.ultima(`POST ${RUTA_REPORTE}`)).toBeUndefined();
  });

  it('cuenta los caracteres de la descripción mientras se escribe', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    abrirComo(ID_CLIENTE);
    await abrirFormulario(persona);

    await persona.type(screen.getByRole('textbox', { name: 'Descripción' }), 'Doce chars.');

    expect(await screen.findByText(/11 de 3000 caracteres/)).toBeVisible();
  });

  // --- Envío ---------------------------------------------------------------

  it('envía solo el motivo y la descripción, y el reportado lo pone el backend', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    api.responder(`POST ${RUTA_REPORTE}`, { estado: 201, cuerpo: casoDeModeracionDeEjemplo() });
    abrirComo(ID_CLIENTE);
    await abrirFormulario(persona);

    await persona.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Trato irrespetuoso');
    await persona.type(screen.getByRole('textbox', { name: 'Descripción' }), 'Usó insultos.');
    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    await waitFor(() => expect(api.ultima(`POST ${RUTA_REPORTE}`)).toBeDefined());
    expect(api.ultima(`POST ${RUTA_REPORTE}`)?.cuerpo).toEqual({
      motivo: 'Trato irrespetuoso',
      descripcion: 'Usó insultos.',
    });
  });

  it('muestra el resumen del caso y ya no ofrece un segundo reporte', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    api.responder(`POST ${RUTA_REPORTE}`, { estado: 201, cuerpo: casoDeModeracionDeEjemplo() });
    abrirComo(ID_CLIENTE);
    await abrirFormulario(persona);

    await persona.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Trato irrespetuoso');
    await persona.type(screen.getByRole('textbox', { name: 'Descripción' }), 'Usó insultos.');

    // La consulta responde ya con el caso abierto, como haría el backend.
    conEstadoDeReporte({ puedeReportar: false, casoAbierto: casoDeModeracionDeEjemplo() });
    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    expect(await screen.findByText(/Reportaste a Taller La Esperanza\./)).toBeVisible();
    expect(screen.getByText('Abierto')).toBeVisible();
    expect(screen.getByText('Trato irrespetuoso')).toBeVisible();
    expect(screen.getByText('Usó insultos y no terminó el trabajo acordado.')).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Enviar reporte' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reportar un problema' })).not.toBeInTheDocument();
  });

  it('al volver a abrir la pantalla muestra el caso ya presentado', async () => {
    conSolicitud();
    conEstadoDeReporte({ puedeReportar: false, casoAbierto: casoDeModeracionDeEjemplo() });

    abrirComo(ID_CLIENTE);

    expect(await screen.findByText(/Reportaste a Taller La Esperanza\./)).toBeVisible();
    expect(screen.getByText(/Un reporte no se edita ni se retira/)).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Reportar un problema' })).not.toBeInTheDocument();
  });

  // --- Error y reintento ---------------------------------------------------

  it('muestra el mensaje del backend y conserva lo escrito para reintentar', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    api.responder(`POST ${RUTA_REPORTE}`, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'REPORTE_DUPLICADO',
        'Ya reportaste esta solicitud. Tu caso sigue abierto y no se presenta dos veces.'
      ),
    });
    abrirComo(ID_CLIENTE);
    await abrirFormulario(persona);

    await persona.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Trato irrespetuoso');
    await persona.type(screen.getByRole('textbox', { name: 'Descripción' }), 'Usó insultos.');
    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya reportaste esta solicitud.');
    expect(screen.getByRole('textbox', { name: 'Motivo' })).toHaveValue('Trato irrespetuoso');
    expect(screen.getByRole('textbox', { name: 'Descripción' })).toHaveValue('Usó insultos.');

    // Y se puede volver a enviar sin rehacer el formulario.
    api.responder(`POST ${RUTA_REPORTE}`, { estado: 201, cuerpo: casoDeModeracionDeEjemplo() });
    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    await waitFor(() =>
      expect(
        api.peticiones.filter((p) => p.metodo === 'POST' && p.ruta === RUTA_REPORTE)
      ).toHaveLength(2)
    );
  });

  it('tras un conflicto vuelve a pedir el estado y muestra el caso que ya existía', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    abrirComo(ID_CLIENTE);
    await abrirFormulario(persona);

    await persona.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Trato irrespetuoso');
    await persona.type(screen.getByRole('textbox', { name: 'Descripción' }), 'Usó insultos.');

    // El caso ya lo abrió otra pestaña: el envío choca con la unicidad y la
    // pantalla tiene que enterarse, no quedarse ofreciendo el formulario.
    api.responder(`POST ${RUTA_REPORTE}`, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'REPORTE_DUPLICADO',
        'Ya reportaste esta solicitud. Tu caso sigue abierto y no se presenta dos veces.'
      ),
    });
    conEstadoDeReporte({ puedeReportar: false, casoAbierto: casoDeModeracionDeEjemplo() });

    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    expect(await screen.findByText(/^Reportaste a /)).toBeVisible();
    expect(screen.queryByRole('textbox', { name: 'Motivo' })).toBeNull();
  });

  it('deja reintentar cuando falla la consulta del estado', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    api.responder(`GET ${RUTA_REPORTE}`, {
      estado: 500,
      cuerpo: cuerpoDeError(500, 'ERROR_INTERNO', 'No pudimos completar la operación.'),
    });
    abrirComo(ID_CLIENTE);

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos completar la operación.'
    );

    conEstadoDeReporte();
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('button', { name: 'Reportar un problema' })).toBeVisible();
  });

  it('impide el doble envío mientras la petición está en curso', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    api.colgar(`POST ${RUTA_REPORTE}`);
    abrirComo(ID_CLIENTE);
    await abrirFormulario(persona);

    await persona.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Trato irrespetuoso');
    await persona.type(screen.getByRole('textbox', { name: 'Descripción' }), 'Usó insultos.');
    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    const boton = await screen.findByRole('button', { name: 'Enviando…' });
    expect(boton).toBeDisabled();
    expect(screen.getByRole('textbox', { name: 'Motivo' })).toBeDisabled();

    await persona.click(boton);

    expect(
      api.peticiones.filter((p) => p.metodo === 'POST' && p.ruta === RUTA_REPORTE)
    ).toHaveLength(1);
  });

  it('no admite un segundo envío entre la confirmación y el refresco del estado', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    api.responder(`POST ${RUTA_REPORTE}`, { estado: 201, cuerpo: casoDeModeracionDeEjemplo() });
    abrirComo(ID_CLIENTE);
    await abrirFormulario(persona);

    await persona.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Trato irrespetuoso');
    await persona.type(screen.getByRole('textbox', { name: 'Descripción' }), 'Usó insultos.');

    // El estado nuevo se queda colgado: es exactamente la ventana en la que el
    // caso ya existe pero la pantalla todavia muestra el formulario.
    api.colgar(`GET ${RUTA_REPORTE}`);
    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    await waitFor(() => expect(api.ultima(`POST ${RUTA_REPORTE}`)).toBeDefined());
    const boton = await screen.findByRole('button', { name: 'Enviando…' });
    expect(boton).toBeDisabled();

    await persona.click(boton);

    expect(
      api.peticiones.filter((p) => p.metodo === 'POST' && p.ruta === RUTA_REPORTE)
    ).toHaveLength(1);
  });

  // --- Cuenta restringida --------------------------------------------------

  it('una cuenta restringida conserva el reporte', async () => {
    const persona = userEvent.setup();
    conSolicitud();
    conEstadoDeReporte();
    api.responder(`POST ${RUTA_REPORTE}`, { estado: 201, cuerpo: casoDeModeracionDeEjemplo() });

    abrirComo(ID_CLIENTE, 'RESTRINGIDA_TEMPORAL');
    await abrirFormulario(persona);

    await persona.type(screen.getByRole('textbox', { name: 'Motivo' }), 'Trato irrespetuoso');
    await persona.type(screen.getByRole('textbox', { name: 'Descripción' }), 'Usó insultos.');
    await persona.click(screen.getByRole('button', { name: 'Enviar reporte' }));

    await waitFor(() => expect(api.ultima(`POST ${RUTA_REPORTE}`)).toBeDefined());
  });
});
