import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

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
import ExpedienteDeCaso from '../paginas/ExpedienteDeCaso';

/**
 * Las apelaciones dentro del expediente.
 *
 * Lo primero que comprueba es lo que **no** hay: ninguna vía para que la persona sancionada apele
 * desde Moica. La apelación llega por el canal externo y aquí solo se registra lo recibido.
 */

const RUTA_SESION = 'GET /api/auth/sesion';
const RUTA_EXPEDIENTE = 'GET /api/admin/casos/5';
const RUTA_ADMINISTRADORES = 'GET /api/admin/administradores';
const RUTA_MEDIDAS = 'GET /api/admin/medidas';
const RUTA_REGISTRAR = 'POST /api/admin/casos/5/apelacion';
const RUTA_RESOLVER = 'POST /api/admin/casos/5/apelacion/resolucion';
const RUTA_REABRIR = 'POST /api/admin/casos/5/reapertura';

const CASO_CERRADO = casoAdministrativoDeEjemplo({
  estadoActual: 'CERRADO',
  resultadoActual: 'PROCEDENTE',
  idAdministradorResponsable: 1,
  nombreAdministradorResponsable: 'Erving Miranda',
});

function montar() {
  return renderizarConProveedores(
    <Routes>
      <Route path="/admin/casos/:idCaso" element={<ExpedienteDeCaso />} />
    </Routes>,
    '/admin/casos/5'
  );
}

describe('apelación de un caso de moderación', () => {
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
    api.responder(RUTA_ADMINISTRADORES, { estado: 200, cuerpo: [] });
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [] });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('explica que la apelación llega por un canal externo, no desde Moica', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_CERRADO, puedeResolver: true }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    expect(screen.getByText(/Nadie ha apelado esta decisión/)).toBeInTheDocument();
    expect(
      screen.getByText(/llegan por el canal externo de soporte, no desde\s+Moica/)
    ).toBeInTheDocument();
  });

  it('registra lo recibido y lo manda tal cual', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_CERRADO, puedeResolver: true }),
    });
    api.responder(RUTA_REGISTRAR, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'PENDIENTE',
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    await userEvent.type(
      screen.getByLabelText('Qué expuso la persona'),
      'Escribió a soporte con capturas.'
    );
    await userEvent.click(screen.getByRole('button', { name: 'Registrar la apelación' }));

    await waitFor(() => expect(api.ultima(RUTA_REGISTRAR)).toBeDefined());
    expect(api.ultima(RUTA_REGISTRAR)?.cuerpo).toEqual({
      relato: 'Escribió a soporte con capturas.',
    });
  });

  it('no ofrece registrar una apelación sobre un caso sin decisión vigente', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          estadoActual: 'EN_REVISION',
          idAdministradorResponsable: 1,
        }),
        puedeResolver: true,
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    expect(screen.queryByLabelText('Qué expuso la persona')).not.toBeInTheDocument();
    expect(
      screen.getByText(/Solo se registra una apelación sobre un caso cerrado/)
    ).toBeInTheDocument();
  });

  it('con una apelación pendiente ofrece resolverla y no registrar otra', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'PENDIENTE',
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    expect(screen.queryByLabelText('Qué expuso la persona')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Resolución')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: /Aceptarla/ })).toBeInTheDocument();
  });

  it('acepta la apelación sin reabrir el caso por sí sola', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'PENDIENTE',
      }),
    });
    api.responder(RUTA_RESOLVER, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'ACEPTADA',
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    await userEvent.type(screen.getByLabelText('Resolución'), 'Las capturas la respaldan.');
    // El refresco que sigue a la mutación vuelve a pedir el expediente.
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'ACEPTADA',
      }),
    });
    await userEvent.click(screen.getByRole('button', { name: 'Registrar la decisión' }));

    await waitFor(() => expect(api.ultima(RUTA_RESOLVER)).toBeDefined());
    expect(api.ultima(RUTA_RESOLVER)?.cuerpo).toEqual({
      aceptada: true,
      resolucion: 'Las capturas la respaldan.',
    });
    // El caso sigue cerrado: reabrirlo es otra decisión.
    expect(await screen.findByRole('button', { name: 'Reabrir el caso' })).toBeInTheDocument();
  });

  it('rechaza la apelación cuando se elige esa opción', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'PENDIENTE',
      }),
    });
    api.responder(RUTA_RESOLVER, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'RECHAZADA',
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    await userEvent.click(screen.getByRole('radio', { name: /Rechazarla/ }));
    await userEvent.type(screen.getByLabelText('Resolución'), 'No aporta nada nuevo.');
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'RECHAZADA',
      }),
    });
    await userEvent.click(screen.getByRole('button', { name: 'Registrar la decisión' }));

    await waitFor(() => expect(api.ultima(RUTA_RESOLVER)).toBeDefined());
    expect(api.ultima(RUTA_RESOLVER)?.cuerpo).toMatchObject({ aceptada: false });
    expect(
      await screen.findByText(/La apelación se evaluó y la decisión se mantuvo/)
    ).toBeInTheDocument();
  });

  it('solo ofrece reabrir cuando la apelación fue aceptada', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'RECHAZADA',
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    expect(screen.queryByRole('button', { name: 'Reabrir el caso' })).not.toBeInTheDocument();
  });

  it('reabre el mismo expediente y avisa de que la resolución anterior se conserva', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'ACEPTADA',
        resolucionActual: 'La conducta quedó acreditada.',
      }),
    });
    api.responder(RUTA_REABRIR, { estado: 200, cuerpo: expedienteReabierto() });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    expect(
      screen.getByText(/La resolución anterior\s+se conserva en el historial/)
    ).toBeInTheDocument();

    await userEvent.type(
      screen.getByLabelText('Motivo para reabrir el caso'),
      'Hay que revisar de nuevo.'
    );
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteReabierto(),
    });
    await userEvent.click(screen.getByRole('button', { name: 'Reabrir el caso' }));

    await waitFor(() => expect(api.ultima(RUTA_REABRIR)).toBeDefined());
    // Es el mismo caso, no otro expediente.
    expect(api.ultima(RUTA_REABRIR)?.ruta).toBe('/api/admin/casos/5/reapertura');
    expect(await screen.findByText('Caso reabierto')).toBeInTheDocument();
    // La resolución anterior sobrevive en el historial aunque el caso ya no la muestre.
    expect(screen.getByText('Resolución registrada')).toBeInTheDocument();
  });

  it('explica un conflicto al reabrir en lugar de dejar la pantalla como estaba', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: true,
        apelacion: 'ACEPTADA',
      }),
    });
    api.responder(RUTA_REABRIR, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'APELACION_NO_ACEPTADA',
        'Un caso solo se reabre cuando su apelación fue aceptada.'
      ),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    await userEvent.type(screen.getByLabelText('Motivo para reabrir el caso'), 'Procede.');
    await userEvent.click(screen.getByRole('button', { name: 'Reabrir el caso' }));

    expect(
      await screen.findByText('Un caso solo se reabre cuando su apelación fue aceptada.')
    ).toBeInTheDocument();
  });

  it('no ofrece nada a quien no lleva el caso', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_CERRADO,
        puedeResolver: false,
        apelacion: 'PENDIENTE',
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Apelación' });

    expect(screen.queryByLabelText('Qué expuso la persona')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Resolución')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reabrir el caso' })).not.toBeInTheDocument();
  });
});

/**
 * El expediente tal como queda tras reabrirlo.
 *
 * La resolución sale de la fila del caso —{@code ck_caso_moderacion_cierre} solo la admite en
 * {@code CERRADO}— pero sigue intacta en la versión que la registró. Es exactamente lo que la
 * prueba comprueba.
 */
function expedienteReabierto() {
  return expedienteDeCasoDeEjemplo({
    caso: casoAdministrativoDeEjemplo({
      estadoActual: 'REABIERTO',
      idAdministradorResponsable: 1,
    }),
    puedeResolver: true,
    apelacion: 'SIN_APELACION',
    resolucionActual: null,
    historial: [
      versionDeCasoDeEjemplo(),
      versionDeCasoDeEjemplo({
        idHistorialCaso: 92,
        numeroVersion: 2,
        tipoEvento: 'RESOLUCION_REGISTRADA',
        tipoActor: 'ADMINISTRADOR',
        estadoCaso: 'CERRADO',
        resultadoCaso: 'PROCEDENTE',
        resolucion: 'La conducta quedó acreditada.',
      }),
      versionDeCasoDeEjemplo({
        idHistorialCaso: 93,
        numeroVersion: 3,
        tipoEvento: 'CASO_REABIERTO',
        tipoActor: 'ADMINISTRADOR',
        estadoCaso: 'REABIERTO',
      }),
    ],
  });
}
