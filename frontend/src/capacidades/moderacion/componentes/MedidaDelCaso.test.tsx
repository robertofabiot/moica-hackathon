import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  casoAdministrativoDeEjemplo,
  cuerpoDeError,
  expedienteDeCasoDeEjemplo,
  instalarApiFalsa,
  medidaDeEjemplo,
  medidaVigenteDeEjemplo,
  sesionDeEjemplo,
  versionDeCasoDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import ExpedienteDeCaso from '../paginas/ExpedienteDeCaso';

/**
 * La medida administrativa dentro del expediente.
 *
 * Comprueba las tres cosas que el criterio de salida exige de esta pantalla: que la medida la elija
 * una persona y no la proponga Moica, que sustituir una vigente pida confirmación explícita, y que
 * un conflicto se explique en lugar de dejar la pantalla cambiando sola.
 */

const RUTA_SESION = 'GET /api/auth/sesion';
const RUTA_EXPEDIENTE = 'GET /api/admin/casos/5';
const RUTA_ADMINISTRADORES = 'GET /api/admin/administradores';
const RUTA_MEDIDAS = 'GET /api/admin/medidas';
const RUTA_APLICAR = 'POST /api/admin/casos/5/medida';
const RUTA_REVOCAR = 'POST /api/admin/casos/5/medida/revocacion';

const CASO_PROCEDENTE = casoAdministrativoDeEjemplo({
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

describe('medida administrativa de un caso', () => {
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
    api.responder(RUTA_MEDIDAS, {
      estado: 200,
      cuerpo: [
        medidaDeEjemplo(),
        medidaDeEjemplo({
          idMedidaAdministrativa: 2,
          codigo: 'ADVERTENCIA',
          nombre: 'Advertencia',
          estadoCuentaResultante: null,
          requiereFechaFin: false,
        }),
      ],
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('no propone ninguna medida: el desplegable empieza sin elegir', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: true }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Medida administrativa' });

    const desplegable = await screen.findByLabelText('Medida que vas a aplicar');
    expect(desplegable).toHaveValue('');
    expect(screen.getByRole('button', { name: 'Aplicar la medida' })).toBeDisabled();
  });

  it('no ofrece las medidas deshabilitadas', async () => {
    api.responder(RUTA_MEDIDAS, {
      estado: 200,
      cuerpo: [
        medidaDeEjemplo(),
        medidaDeEjemplo({
          idMedidaAdministrativa: 3,
          codigo: 'VIEJA',
          nombre: 'Medida retirada',
          habilitada: false,
        }),
      ],
    });
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: true }),
    });

    montar();
    await screen.findByLabelText('Medida que vas a aplicar');

    expect(await screen.findByRole('option', { name: 'Restricción temporal' })).toBeInTheDocument();
    expect(screen.queryByRole('option', { name: 'Medida retirada' })).not.toBeInTheDocument();
  });

  it('pide el plazo solo cuando la medida elegida lo exige', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: true }),
    });

    montar();
    const desplegable = await screen.findByLabelText('Medida que vas a aplicar');
    // El desplegable nace deshabilitado hasta que llega el catálogo.
    await screen.findByRole('option', { name: 'Advertencia' });

    await userEvent.selectOptions(desplegable, '2');
    expect(screen.queryByLabelText('Hasta cuándo')).not.toBeInTheDocument();

    await userEvent.selectOptions(desplegable, '1');
    expect(screen.getByLabelText('Hasta cuándo')).toBeInTheDocument();
  });

  it('aplica la medida con su plazo y su justificación', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: true }),
    });
    api.responder(RUTA_APLICAR, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_PROCEDENTE,
        puedeResolver: true,
        estadoCuentaReportada: 'RESTRINGIDA_TEMPORAL',
        medidaVigente: medidaVigenteDeEjemplo(),
      }),
    });

    montar();
    const desplegable = await screen.findByLabelText('Medida que vas a aplicar');
    await screen.findByRole('option', { name: 'Restricción temporal' });

    await userEvent.selectOptions(desplegable, '1');
    await userEvent.type(screen.getByLabelText('Hasta cuándo'), '2026-12-31T12:00');
    await userEvent.type(screen.getByLabelText('Por qué la aplicas'), 'Conducta acreditada.');
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar la medida' }));

    await waitFor(() => expect(api.ultima(RUTA_APLICAR)).toBeDefined());
    expect(api.ultima(RUTA_APLICAR)?.cuerpo).toMatchObject({
      idMedidaAdministrativa: 1,
      justificacion: 'Conducta acreditada.',
      // Sin medida vigente no hay nada que sustituir.
      confirmaReemplazo: false,
    });
  });

  it('advierte de la medida vigente de otro caso y exige confirmar la sustitución', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_PROCEDENTE,
        puedeResolver: true,
        estadoCuentaReportada: 'RESTRINGIDA_TEMPORAL',
        medidaVigente: medidaVigenteDeEjemplo({ idCasoModeracion: 8, esDeEsteCaso: false }),
      }),
    });
    api.responder(RUTA_APLICAR, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: true }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Medida administrativa' });

    expect(screen.getByText(/Esa medida la impuso el caso 8/)).toBeInTheDocument();

    await screen.findByRole('option', { name: 'Advertencia' });
    await userEvent.selectOptions(screen.getByLabelText('Medida que vas a aplicar'), '2');
    await userEvent.type(screen.getByLabelText('Por qué la aplicas'), 'Procede sustituirla.');

    // Sin marcar la casilla no se puede enviar.
    const boton = screen.getByRole('button', { name: 'Sustituir la medida vigente' });
    expect(boton).toBeDisabled();

    await userEvent.click(screen.getByRole('checkbox'));
    expect(boton).toBeEnabled();
    await userEvent.click(boton);

    await waitFor(() => expect(api.ultima(RUTA_APLICAR)).toBeDefined());
    expect(api.ultima(RUTA_APLICAR)?.cuerpo).toMatchObject({ confirmaReemplazo: true });
  });

  it('cancelar la confirmación deja la medida vigente intacta', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_PROCEDENTE,
        puedeResolver: true,
        estadoCuentaReportada: 'RESTRINGIDA_TEMPORAL',
        medidaVigente: medidaVigenteDeEjemplo(),
      }),
    });

    montar();
    await screen.findByRole('option', { name: 'Advertencia' });
    await userEvent.selectOptions(screen.getByLabelText('Medida que vas a aplicar'), '2');
    await userEvent.type(screen.getByLabelText('Por qué la aplicas'), 'Lo pensaré.');

    await userEvent.click(screen.getByRole('checkbox'));
    await userEvent.click(screen.getByRole('checkbox'));

    expect(screen.getByRole('button', { name: 'Sustituir la medida vigente' })).toBeDisabled();
    expect(api.ultima(RUTA_APLICAR)).toBeUndefined();
  });

  it('un conflicto de reemplazo se explica y pasa a exigir confirmación', async () => {
    // La pantalla se cargó sin medida vigente: alguien sancionó mientras tanto.
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: true }),
    });
    api.responder(RUTA_APLICAR, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'MEDIDA_VIGENTE_EXISTENTE',
        'Esta cuenta ya tiene una medida vigente impuesta por el caso 8. Confirma la sustitución.'
      ),
    });

    montar();
    await screen.findByRole('option', { name: 'Advertencia' });
    await userEvent.selectOptions(screen.getByLabelText('Medida que vas a aplicar'), '2');
    await userEvent.type(screen.getByLabelText('Por qué la aplicas'), 'Procede.');
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar la medida' }));

    expect(
      await screen.findByText(/ya tiene una medida vigente impuesta por el caso 8/)
    ).toBeInTheDocument();
    // Y ahora sí pide confirmar, aunque el expediente siga diciendo que no hay ninguna.
    expect(await screen.findByRole('checkbox')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sustituir la medida vigente' })).toBeDisabled();
  });

  it('el aviso del conflicto sobrevive al refresco del expediente', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: true }),
    });
    api.responder(RUTA_APLICAR, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'MEDIDA_DESHABILITADA',
        'Esa medida ya no está disponible en el catálogo. Elige otra.'
      ),
    });

    montar();
    await screen.findByRole('option', { name: 'Advertencia' });
    await userEvent.selectOptions(screen.getByLabelText('Medida que vas a aplicar'), '2');
    await userEvent.type(screen.getByLabelText('Por qué la aplicas'), 'Procede.');

    // El refresco que sigue al fallo trae el caso ya sin permiso para resolver:
    // el formulario desaparece y el aviso tiene que seguir explicando por qué.
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: false }),
    });
    await userEvent.click(screen.getByRole('button', { name: 'Aplicar la medida' }));

    expect(
      await screen.findByText('Esa medida ya no está disponible en el catálogo. Elige otra.')
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.queryByLabelText('Medida que vas a aplicar')).not.toBeInTheDocument()
    );
    expect(
      screen.getByText('Esa medida ya no está disponible en el catálogo. Elige otra.')
    ).toBeInTheDocument();
  });

  it('revoca la medida de este caso con su motivo', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_PROCEDENTE,
        puedeResolver: true,
        estadoCuentaReportada: 'RESTRINGIDA_TEMPORAL',
        medidaVigente: medidaVigenteDeEjemplo(),
      }),
    });
    api.responder(RUTA_REVOCAR, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({ caso: CASO_PROCEDENTE, puedeResolver: true }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Medida administrativa' });

    await userEvent.type(
      screen.getByLabelText('Motivo para revocar la medida'),
      'La persona aportó pruebas.'
    );
    await userEvent.click(screen.getByRole('button', { name: 'Revocar la medida' }));

    await waitFor(() => expect(api.ultima(RUTA_REVOCAR)).toBeDefined());
    expect(api.ultima(RUTA_REVOCAR)?.cuerpo).toEqual({ motivo: 'La persona aportó pruebas.' });
  });

  it('no ofrece revocar una medida que sostiene otro expediente', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_PROCEDENTE,
        puedeResolver: true,
        estadoCuentaReportada: 'RESTRINGIDA_TEMPORAL',
        medidaVigente: medidaVigenteDeEjemplo({ idCasoModeracion: 8, esDeEsteCaso: false }),
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Medida administrativa' });

    expect(screen.queryByRole('button', { name: 'Revocar la medida' })).not.toBeInTheDocument();
  });

  it('no ofrece aplicar nada sobre un caso desestimado', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: casoAdministrativoDeEjemplo({
          estadoActual: 'CERRADO',
          resultadoActual: 'DESESTIMADO',
          idAdministradorResponsable: 1,
        }),
        puedeResolver: true,
      }),
    });

    montar();
    await screen.findByRole('heading', { level: 2, name: 'Medida administrativa' });

    expect(screen.queryByLabelText('Medida que vas a aplicar')).not.toBeInTheDocument();
    expect(screen.getByText(/El caso se desestimó/)).toBeInTheDocument();
  });

  it('el historial nombra la medida que cada versión retrataba', async () => {
    api.responder(RUTA_EXPEDIENTE, {
      estado: 200,
      cuerpo: expedienteDeCasoDeEjemplo({
        caso: CASO_PROCEDENTE,
        puedeResolver: true,
        historial: [
          versionDeCasoDeEjemplo(),
          versionDeCasoDeEjemplo({
            idHistorialCaso: 91,
            numeroVersion: 2,
            tipoEvento: 'MEDIDA_APLICADA',
            tipoActor: 'ADMINISTRADOR',
            idMedidaAdministrativa: 1,
            nombreMedida: 'Restricción temporal',
            estadoCuenta: 'RESTRINGIDA_TEMPORAL',
          }),
        ],
      }),
    });

    montar();

    expect(await screen.findByText('Medida aplicada')).toBeInTheDocument();
  });
});
