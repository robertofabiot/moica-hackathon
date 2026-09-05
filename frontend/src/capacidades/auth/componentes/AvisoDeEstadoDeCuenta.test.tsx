import { screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { instalarApiFalsa, sesionDeEjemplo, type ApiFalsa } from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import AvisoDeEstadoDeCuenta from './AvisoDeEstadoDeCuenta';

/**
 * El aviso que ve quien arrastra una medida administrativa.
 *
 * Comprueba las dos caras: que explique la situación y ofrezca el canal externo, y que **no** filtre
 * nada del expediente —qué medida, desde qué caso, quién la decidió—, que es información
 * administrativa que esta persona no tiene por qué ver.
 */

const RUTA_SESION = 'GET /api/auth/sesion';

describe('aviso del estado de la cuenta', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('no molesta a una cuenta activa', async () => {
    api.responder(RUTA_SESION, { estado: 200, cuerpo: sesionDeEjemplo() });

    renderizarConProveedores(<AvisoDeEstadoDeCuenta />);

    // Da tiempo a que la consulta se resuelva antes de afirmar que no hay aviso.
    await screen.findByText((_, elemento) => elemento?.tagName === 'BODY', { exact: false });
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('no aparece cuando no hay sesión', async () => {
    api.responder(RUTA_SESION, { estado: 401, cuerpo: null });

    renderizarConProveedores(<AvisoDeEstadoDeCuenta />);

    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('explica una restricción, dice hasta cuándo y ofrece el canal de soporte', async () => {
    api.responder(RUTA_SESION, {
      estado: 200,
      cuerpo: sesionDeEjemplo({
        estadoCuenta: 'RESTRINGIDA_TEMPORAL',
        fechaFinEstadoCuenta: '2026-09-30T18:00:00-06:00',
      }),
    });

    renderizarConProveedores(<AvisoDeEstadoDeCuenta />);

    expect(await screen.findByText(/Tu cuenta está restringida temporalmente/)).toBeInTheDocument();
    expect(screen.getByText(/Termina el/)).toBeInTheDocument();

    const canal = screen.getByRole('link', { name: 'soporte@moica.ni' });
    expect(canal).toHaveAttribute('href', 'mailto:soporte@moica.ni');
  });

  it('dice qué conserva una cuenta restringida y qué no', async () => {
    api.responder(RUTA_SESION, {
      estado: 200,
      cuerpo: sesionDeEjemplo({
        estadoCuenta: 'RESTRINGIDA_TEMPORAL',
        fechaFinEstadoCuenta: '2026-09-30T18:00:00-06:00',
      }),
    });

    renderizarConProveedores(<AvisoDeEstadoDeCuenta />);

    const aviso = await screen.findByRole('status');
    expect(aviso).toHaveTextContent(/consultar tu historial/);
    expect(aviso).toHaveTextContent(/cancelar\s+compromisos/);
    expect(aviso).toHaveTextContent(/no puedes publicar servicios/);
  });

  it('no promete una fecha en una suspensión permanente', async () => {
    api.responder(RUTA_SESION, {
      estado: 200,
      cuerpo: sesionDeEjemplo({ estadoCuenta: 'SUSPENDIDA_PERMANENTE' }),
    });

    renderizarConProveedores(<AvisoDeEstadoDeCuenta />);

    expect(
      await screen.findByText(/suspendida de forma permanente y no se reactiva sola/)
    ).toBeInTheDocument();
    expect(screen.queryByText(/Termina el/)).not.toBeInTheDocument();
  });

  it('no revela qué medida se aplicó, desde qué caso ni quién la decidió', async () => {
    api.responder(RUTA_SESION, {
      estado: 200,
      cuerpo: sesionDeEjemplo({
        estadoCuenta: 'SUSPENDIDA_TEMPORAL',
        fechaFinEstadoCuenta: '2026-09-30T18:00:00-06:00',
      }),
    });

    renderizarConProveedores(<AvisoDeEstadoDeCuenta />);

    const aviso = await screen.findByRole('status');
    expect(aviso).not.toHaveTextContent(/medida/i);
    expect(aviso).not.toHaveTextContent(/caso/i);
    expect(aviso).not.toHaveTextContent(/expediente/i);
    expect(aviso).not.toHaveTextContent(/administrador/i);
  });

  it('no ofrece ningún formulario de apelación: el canal es externo', async () => {
    api.responder(RUTA_SESION, {
      estado: 200,
      cuerpo: sesionDeEjemplo({ estadoCuenta: 'SUSPENDIDA_PERMANENTE' }),
    });

    renderizarConProveedores(<AvisoDeEstadoDeCuenta />);
    await screen.findByRole('status');

    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });
});
