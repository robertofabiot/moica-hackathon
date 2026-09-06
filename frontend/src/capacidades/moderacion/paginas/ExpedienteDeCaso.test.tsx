import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Route, Routes } from 'react-router';

import {
  casoAdministrativoDeEjemplo,
  cuerpoDeError,
  expedienteDeCasoDeEjemplo,
  instalarApiFalsa,
  sesionDeEjemplo,
  versionDeCasoDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import ExpedienteDeCaso from './ExpedienteDeCaso';

/**
 * El expediente: qué se lee, qué acciones se ofrecen y qué pasa cuando alguien se adelanta.
 *
 * Que un botón aparezca o no es experiencia, no seguridad: el backend rechaza igual a quien no
 * lleva el caso. Lo que se comprueba aquí es que la pantalla no proponga lo que la API va a
 * rechazar, que el hilo privado no se descargue sin que nadie lo pida y que un conflicto se explique
 * en lugar de dejar el estado viejo.
 */

const RUTA_SESION = 'GET /api/auth/sesion';
const RUTA_EXPEDIENTE = 'GET /api/admin/casos/5';
const RUTA_MENSAJES = 'GET /api/admin/casos/5/mensajes';
const RUTA_ADMINISTRADORES = 'GET /api/admin/administradores';
const RUTA_ASIGNACION = 'POST /api/admin/casos/5/asignacion';
const RUTA_REVISION = 'POST /api/admin/casos/5/revision';
const RUTA_CIERRE = 'POST /api/admin/casos/5/cierre';

function montar() {
  return renderizarConProveedores(
    <Routes>
      <Route path="/admin/casos/:idCaso" element={<ExpedienteDeCaso />} />
    </Routes>,
    '/admin/casos/5'
  );
}

describe('expediente de un caso de moderación', () => {
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
    api.responder(RUTA_ADMINISTRADORES, {
      estado: 200,
      cuerpo: [{ idAdministrador: 9, nombreCompleto: 'Lucía Moderadora' }],
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('reúne el reporte, la solicitud, las evidencias y el historial', async () => {
    api.responder(RUTA_EXPEDIENTE, { estado: 200, cuerpo: expedienteDeCasoDeEjemplo() });

    montar();

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' })
    ).toBeInTheDocument();
    expect(screen.getByText('Usó insultos y no terminó el trabajo acordado.')).toBeInTheDocument();
    expect(screen.getByText('Reparación de fugas')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Tubería reparada' })).toBeInTheDocument();
    expect(screen.getByText('Caso abierto')).toBeInTheDocument();
    expect(screen.getByText('Versión vigente')).toBeInTheDocument();
  });

  it('no descarga el hilo privado hasta que alguien pide verlo', async () => {
    api.responder(RUTA_EXPEDIENTE, { estado: 200, cuerpo: expedienteDeCasoDeEjemplo() });
    api.responder(RUTA_MENSAJES, {
      estado: 200,
      cuerpo: [
        {
          idMensajeSolicitud: 1,
          idSolicitudServicio: 21,
          idRemitente: 2,
          nombreRemitente: 'Ana Cliente',
          contenido: '¿A qué hora llegas?',
          fechaEnvio: '2026-08-29T11:00:00-06:00',
        },
      ],
    });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    expect(api.ultima(RUTA_MENSAJES)).toBeUndefined();

    await userEvent.click(screen.getByRole('button', { name: 'Ver los mensajes' }));

    expect(await screen.findByText('¿A qué hora llegas?')).toBeInTheDocument();
    expect(api.ultima(RUTA_MENSAJES)).toBeDefined();
  });

  it('no ofrece revisar ni cerrar a quien no lleva el caso', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        puedeResolver: false,
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    expect(screen.queryByRole('button', { name: 'Iniciar la revisión' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cerrar el caso' })).not.toBeInTheDocument();
    expect(
      screen.getByText(/Solo quien lo tiene asignado puede iniciar la revisión/)
    ).toBeInTheDocument();
  });

  it('asigna el caso a la persona elegida', async () => {
    api.responder(RUTA_EXPEDIENTE, { estado: 200, cuerpo: expedienteDeCasoDeEjemplo() });
    api.responder(RUTA_ASIGNACION, { estado: 200, cuerpo: expedienteDeCasoDeEjemplo() });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    // El desplegable nace deshabilitado hasta que llega la lista de personas.
    await screen.findByRole('option', { name: 'Lucía Moderadora' });
    await userEvent.selectOptions(screen.getByLabelText('Asignar responsable'), 'Lucía Moderadora');
    await userEvent.click(screen.getByRole('button', { name: 'Asignar' }));

    await waitFor(() => expect(api.ultima(RUTA_ASIGNACION)).toBeDefined());
    expect(api.ultima(RUTA_ASIGNACION)?.cuerpo).toEqual({ idAdministrador: 9 });
  });

  it('quien lleva el caso puede iniciar la revisión', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        puedeResolver: true,
      }),
    });
    api.responder(RUTA_REVISION, { estado: 200, cuerpo: expedienteDeCasoDeEjemplo() });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    await userEvent.click(screen.getByRole('button', { name: 'Iniciar la revisión' }));

    await waitFor(() => expect(api.ultima(RUTA_REVISION)).toBeDefined());
  });

  it('el cierre envía resultado y resolución juntos, y no se activa sin texto', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          estadoActual: 'EN_REVISION',
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        puedeResolver: true,
      }),
    });
    api.responder(RUTA_CIERRE, { estado: 200, cuerpo: expedienteDeCasoDeEjemplo() });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    const cerrar = screen.getByRole('button', { name: 'Cerrar el caso' });
    expect(cerrar).toBeDisabled();

    await userEvent.click(screen.getByRole('radio', { name: /Desestimado/ }));
    await userEvent.type(screen.getByLabelText('Resolución'), 'No se acreditó la conducta.');
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar el caso' }));

    await waitFor(() => expect(api.ultima(RUTA_CIERRE)).toBeDefined());
    expect(api.ultima(RUTA_CIERRE)?.cuerpo).toEqual({
      resultado: 'DESESTIMADO',
      resolucion: 'No se acreditó la conducta.',
    });
  });

  it('advierte que cerrar no aplica ninguna medida', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          estadoActual: 'EN_REVISION',
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        puedeResolver: true,
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    expect(
      screen.getByText(/No aplica ninguna medida ni cambia el estado de la cuenta reportada/)
    ).toBeInTheDocument();
  });

  it('explica el conflicto cuando otra persona se adelantó', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        puedeResolver: true,
      }),
    });
    api.responder(RUTA_REVISION, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'TRANSICION_NO_PERMITIDA',
        'Esa acción no está disponible: el caso está en estado CERRADO.'
      ),
    });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    await userEvent.click(screen.getByRole('button', { name: 'Iniciar la revisión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Esa acción no está disponible: el caso está en estado CERRADO.'
    );
  });

  it('muestra la resolución vigente de un caso ya cerrado y no ofrece decidir', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          estadoActual: 'CERRADO',
          resultadoActual: 'PROCEDENTE',
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        resolucionActual: 'La conducta reportada quedó acreditada.',
        puedeResolver: true,
        historial: [
          versionDeCasoDeEjemplo({
            esVersionActual: false,
            fechaFinVigencia: '2026-08-30T13:00:00-06:00',
          }),
          versionDeCasoDeEjemplo({
            idHistorialCaso: 91,
            numeroVersion: 2,
            tipoEvento: 'RESOLUCION_REGISTRADA',
            tipoActor: 'ADMINISTRADOR',
            nombreActor: 'Lucía Moderadora',
            estadoCaso: 'CERRADO',
            resultadoCaso: 'PROCEDENTE',
            detalleCambio: 'El caso se cerró con resultado PROCEDENTE y su resolución.',
            fechaInicioVigencia: '2026-08-30T13:00:00-06:00',
          }),
        ],
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    expect(screen.getByText('La conducta reportada quedó acreditada.')).toBeInTheDocument();
    // El estado y el resultado se pintan en el mismo párrafo, en dos nodos.
    expect(screen.getByText(/Cerrado/).textContent).toContain('Procedente');
    expect(screen.queryByRole('button', { name: 'Cerrar el caso' })).not.toBeInTheDocument();
    // Un caso cerrado tampoco se reasigna: la API responde 409.
    expect(screen.queryByLabelText(/Reasignar/)).not.toBeInTheDocument();
  });
  it('el historial nombra al responsable de cada versión, aparte de quien la originó', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        historial: [
          versionDeCasoDeEjemplo({
            esVersionActual: false,
            fechaFinVigencia: '2026-08-30T13:00:00-06:00',
          }),
          versionDeCasoDeEjemplo({
            idHistorialCaso: 91,
            numeroVersion: 2,
            tipoEvento: 'RESPONSABLE_ASIGNADO',
            tipoActor: 'ADMINISTRADOR',
            idActor: 9,
            nombreActor: 'Lucía Moderadora',
            idAdministradorResponsable: 12,
            nombreAdministradorResponsable: 'Carlos Moderador',
            detalleCambio: 'El caso se asignó a una persona administradora responsable.',
            fechaInicioVigencia: '2026-08-30T13:00:00-06:00',
          }),
        ],
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 1, name: 'Trato irrespetuoso' });

    // Quien ejecutó la acción y quien quedó a cargo son personas distintas, y el
    // historial tiene que poder distinguirlas.
    const asignacion = screen.getByText('Responsable asignado').closest('li');
    expect(asignacion).not.toBeNull();
    expect(asignacion).toHaveTextContent('Lucía Moderadora');
    expect(asignacion).toHaveTextContent('Responsable entonces: Carlos Moderador');
    expect(asignacion).not.toHaveTextContent('Responsable entonces: Lucía Moderadora');

    // La apertura no tuvo responsable: no se inventa ninguno.
    const apertura = screen.getByText('Caso abierto').closest('li');
    expect(apertura).not.toHaveTextContent('Responsable entonces:');
  });

  it('el aviso del 409 sobrevive al refresco que oculta la acción', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        puedeResolver: true,
      }),
    });
    montar();
    await screen.findByRole('button', { name: 'Iniciar la revisión' });

    // Otra persona cerró el caso entre que se pintó la pantalla y se pulsó: la
    // acción responde 409 y el refresco trae ya el caso cerrado.
    api.responder(RUTA_REVISION, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'TRANSICION_NO_PERMITIDA',
        'Esa acción no está disponible: el caso está en estado CERRADO.'
      ),
    });
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          estadoActual: 'CERRADO',
          resultadoActual: 'PROCEDENTE',
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        resolucionActual: 'Otra persona ya lo resolvió.',
        puedeResolver: true,
      }),
    });

    await userEvent.click(screen.getByRole('button', { name: 'Iniciar la revisión' }));

    // El refresco ya terminó: el caso se ve cerrado y las acciones desaparecieron.
    await screen.findByText('Otra persona ya lo resolvió.');
    expect(screen.queryByRole('button', { name: 'Iniciar la revisión' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/Reasignar|Asignar responsable/)).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cerrar el caso' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Asignar' })).not.toBeInTheDocument();

    // Y aun así el aviso sigue en pantalla: es lo único que explica por qué.
    expect(screen.getByRole('alert')).toHaveTextContent(
      'Esa acción no está disponible: el caso está en estado CERRADO.'
    );
  });

  it('el aviso del 403 sobrevive al refresco que quita el permiso de resolver', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          estadoActual: 'EN_REVISION',
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
        puedeResolver: true,
      }),
    });
    montar();
    await screen.findByRole('button', { name: 'Cerrar el caso' });

    // El caso se reasignó mientras se escribía la resolución: cerrar responde 403
    // y el refresco trae ya `puedeResolver: false`.
    api.responder(RUTA_CIERRE, {
      estado: 403,
      cuerpo: cuerpoDeError(
        403,
        'CASO_DE_OTRO_ADMINISTRADOR',
        'Este caso lo lleva otra persona administradora.'
      ),
    });
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          estadoActual: 'EN_REVISION',
          idAdministradorResponsable: 12,
          nombreAdministradorResponsable: 'Carlos Moderador',
        }),
        puedeResolver: false,
      }),
    });

    await userEvent.type(screen.getByLabelText('Resolución'), 'Quedó acreditado.');
    await userEvent.click(screen.getByRole('button', { name: 'Cerrar el caso' }));

    // El refresco ya terminó: el formulario de cierre desapareció con el permiso.
    await screen.findByText(/Responsable: Carlos Moderador/);
    expect(screen.queryByRole('button', { name: 'Cerrar el caso' })).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Resolución')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Iniciar la revisión' })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Reasignar a otra persona')).toBeEnabled();

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Este caso lo lleva otra persona administradora.'
    );

    // Recuperar la asignación es una acción posterior válida. Su éxito no debe
    // seguir acompañado del error de cierre anterior.
    const recuperado = expedienteDeCasoDeEjemplo({
      caso: casoAdministrativoDeEjemplo({
        estadoActual: 'EN_REVISION',
        idAdministradorResponsable: 9,
        nombreAdministradorResponsable: 'Lucía Moderadora',
      }),
      puedeResolver: true,
    });
    api.responder(RUTA_ASIGNACION, { estado: 200, cuerpo: recuperado });
    api.responder(RUTA_EXPEDIENTE, { estado: 200, cuerpo: recuperado });
    await userEvent.selectOptions(screen.getByLabelText('Reasignar a otra persona'), '9');
    expect(screen.getByRole('button', { name: 'Asignar' })).toBeEnabled();
    await userEvent.click(screen.getByRole('button', { name: 'Asignar' }));
    await screen.findByText(/Responsable: Lucía Moderadora/);
    await waitFor(() => expect(screen.getByRole('button', { name: 'Asignar' })).toBeEnabled());
    expect(screen.getByRole('button', { name: 'Cerrar el caso' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
