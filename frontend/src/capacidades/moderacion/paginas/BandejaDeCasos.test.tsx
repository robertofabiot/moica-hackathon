import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  casoAdministrativoDeEjemplo,
  cuerpoDeError,
  instalarApiFalsa,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import BandejaDeCasos from './BandejaDeCasos';

/**
 * La bandeja administrativa: qué se ve y cómo se acota.
 *
 * Nada de lo que se comprueba aquí es un control de seguridad —eso lo aplica el backend en cada
 * petición—, sino que la pantalla pida lo que dice pedir y que un error se explique en lugar de
 * dejar la tabla vacía sin motivo.
 */

const RUTA_SESION = 'GET /api/auth/sesion';
const RUTA_PENDIENTES = 'GET /api/admin/casos?estado=ABIERTO&estado=EN_REVISION&estado=REABIERTO';
const RUTA_CERRADOS = 'GET /api/admin/casos?estado=CERRADO';
const RUTA_MIOS =
  'GET /api/admin/casos?estado=ABIERTO&estado=EN_REVISION&estado=REABIERTO&mios=true';

describe('bandeja de casos de moderación', () => {
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
  });

  it('pide lo que espera decisión y muestra quién reportó a quién', async () => {
    api.responder(RUTA_PENDIENTES, {
      estado: 200,
      cuerpo: [casoAdministrativoDeEjemplo()],
    });

    renderizarConProveedores(<BandejaDeCasos />);

    expect(await screen.findByText('Trato irrespetuoso')).toBeInTheDocument();
    expect(screen.getByText(/Ana Cliente reportó a Taller La Esperanza/)).toBeInTheDocument();
    expect(screen.getByText('Abierto')).toBeInTheDocument();
    expect(screen.getByText('Sin asignar')).toBeInTheDocument();
  });

  it('el enlace del caso lleva a su expediente', async () => {
    api.responder(RUTA_PENDIENTES, {
      estado: 200,
      cuerpo: [casoAdministrativoDeEjemplo({ idCasoModeracion: 12 })],
    });

    renderizarConProveedores(<BandejaDeCasos />);

    const enlace = await screen.findByRole('link', { name: 'Trato irrespetuoso' });
    expect(enlace).toHaveAttribute('href', '/admin/casos/12');
  });

  it('muestra el responsable y el resultado cuando el caso ya se resolvió', async () => {
    api.responder(RUTA_PENDIENTES, { estado: 200, cuerpo: [] });
    api.responder(RUTA_CERRADOS, {
      estado: 200,
      cuerpo: [
        casoAdministrativoDeEjemplo({
          estadoActual: 'CERRADO',
          resultadoActual: 'DESESTIMADO',
          idAdministradorResponsable: 9,
          nombreAdministradorResponsable: 'Lucía Moderadora',
        }),
      ],
    });

    renderizarConProveedores(<BandejaDeCasos />);
    await screen.findByText('No hay casos con estos filtros.');

    await userEvent.click(screen.getByRole('button', { name: 'Cerrados' }));

    expect(await screen.findByText('Cerrado')).toBeInTheDocument();
    expect(screen.getByText('Desestimado')).toBeInTheDocument();
    expect(screen.getByText('Lucía Moderadora')).toBeInTheDocument();
  });

  it('el filtro de los propios añade el parámetro «mios»', async () => {
    api.responder(RUTA_PENDIENTES, { estado: 200, cuerpo: [casoAdministrativoDeEjemplo()] });
    api.responder(RUTA_MIOS, { estado: 200, cuerpo: [] });

    renderizarConProveedores(<BandejaDeCasos />);
    await screen.findByText('Trato irrespetuoso');

    await userEvent.click(screen.getByRole('button', { name: 'Solo los míos' }));

    await waitFor(() => expect(api.ultima(RUTA_MIOS)).toBeDefined());
    expect(await screen.findByText('No hay casos con estos filtros.')).toBeInTheDocument();
  });

  it('los filtros dicen cuál está activo a quien no ve el estilo', async () => {
    api.responder(RUTA_PENDIENTES, { estado: 200, cuerpo: [] });

    renderizarConProveedores(<BandejaDeCasos />);
    await screen.findByText('No hay casos con estos filtros.');

    expect(screen.getByRole('button', { name: 'Esperando decisión' })).toHaveAttribute(
      'aria-pressed',
      'true'
    );
    expect(screen.getByRole('button', { name: 'Cerrados' })).toHaveAttribute(
      'aria-pressed',
      'false'
    );
    expect(screen.getByRole('button', { name: 'Solo los míos' })).toHaveAttribute(
      'aria-pressed',
      'false'
    );
  });

  it('explica el error de la API y deja reintentar', async () => {
    api.responder(RUTA_PENDIENTES, {
      estado: 403,
      cuerpo: cuerpoDeError(403, 'ACCESO_DENEGADO', 'Esta cuenta no tiene permisos.'),
    });

    renderizarConProveedores(<BandejaDeCasos />);

    const aviso = await screen.findByRole('alert');
    expect(aviso).toHaveTextContent('Esta cuenta no tiene permisos.');
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
