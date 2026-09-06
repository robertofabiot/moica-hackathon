import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  cuerpoDeError,
  instalarApiFalsa,
  medidaDeEjemplo,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import CatalogoDeMedidas from './CatalogoDeMedidas';

/**
 * El catálogo de medidas: qué se lee, qué se administra y qué nunca se puede hacer.
 *
 * Lo que más importa comprobar aquí es lo que **no** existe: ninguna forma de eliminar una medida.
 * Una citada por un caso o por el historial es la evidencia de una decisión, así que el negocio
 * «elimina» deshabilitando.
 */

const RUTA_SESION = 'GET /api/auth/sesion';
const RUTA_MEDIDAS = 'GET /api/admin/medidas';
const RUTA_CREAR = 'POST /api/admin/medidas';
const RUTA_EDITAR = 'PUT /api/admin/medidas/1';
const RUTA_HABILITACION = 'PUT /api/admin/medidas/1/habilitacion';

describe('catálogo de medidas administrativas', () => {
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

  it('explica que el catálogo está vacío en lugar de mostrar una lista sin filas', async () => {
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [] });

    renderizarConProveedores(<CatalogoDeMedidas />);

    expect(
      await screen.findByText(/Todavía no hay ninguna medida/, { exact: false })
    ).toBeInTheDocument();
  });

  it('muestra cada medida con su código, severidad y efecto sobre la cuenta', async () => {
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
          nivelSeveridad: 1,
        }),
      ],
    });

    renderizarConProveedores(<CatalogoDeMedidas />);

    expect(await screen.findByText('Restricción temporal')).toBeInTheDocument();
    expect(
      screen.getByText(/RESTRICCION_TEMPORAL · Severidad 2 · deja la cuenta restringida/)
    ).toBeInTheDocument();
    expect(screen.getByText(/ADVERTENCIA · Severidad 1 · no cambia el acceso/)).toBeInTheDocument();
  });

  it('no ofrece ninguna forma de eliminar una medida', async () => {
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [medidaDeEjemplo()] });

    renderizarConProveedores(<CatalogoDeMedidas />);
    await screen.findByText('Restricción temporal');

    expect(screen.queryByRole('button', { name: /Eliminar/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Borrar/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dejar de ofrecerla' })).toBeInTheDocument();
  });

  it('deshabilita una medida sin borrarla y ofrece volver a habilitarla', async () => {
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [medidaDeEjemplo()] });
    api.responder(RUTA_HABILITACION, {
      estado: 200,
      cuerpo: medidaDeEjemplo({ habilitada: false }),
    });

    renderizarConProveedores(<CatalogoDeMedidas />);
    await screen.findByText('Restricción temporal');

    api.responder(RUTA_MEDIDAS, {
      estado: 200,
      cuerpo: [medidaDeEjemplo({ habilitada: false })],
    });
    await userEvent.click(screen.getByRole('button', { name: 'Dejar de ofrecerla' }));

    expect(await screen.findByRole('button', { name: 'Volver a ofrecerla' })).toBeInTheDocument();
    expect(screen.getByText('Deshabilitada')).toBeInTheDocument();
    // Sigue en la lista: deshabilitar no es borrar.
    expect(screen.getByText('Restricción temporal')).toBeInTheDocument();
    expect(api.ultima(RUTA_HABILITACION)?.cuerpo).toEqual({ habilitada: false });
  });

  it('crea una medida y deriva el plazo del estado que impone', async () => {
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [] });
    api.responder(RUTA_CREAR, { estado: 201, cuerpo: medidaDeEjemplo() });

    renderizarConProveedores(<CatalogoDeMedidas />);
    await screen.findByText(/Todavía no hay ninguna medida/);

    await userEvent.type(screen.getByLabelText('Código'), 'restriccion_temporal');
    await userEvent.type(screen.getByLabelText('Nombre'), 'Restricción temporal');
    await userEvent.selectOptions(
      screen.getByLabelText('Estado en el que deja la cuenta'),
      'RESTRINGIDA_TEMPORAL'
    );
    await userEvent.click(screen.getByRole('button', { name: 'Añadir la medida' }));

    await waitFor(() => expect(api.ultima(RUTA_CREAR)).toBeDefined());
    expect(api.ultima(RUTA_CREAR)?.cuerpo).toMatchObject({
      // El código se normaliza antes de enviarlo.
      codigo: 'RESTRICCION_TEMPORAL',
      nombre: 'Restricción temporal',
      estadoCuentaResultante: 'RESTRINGIDA_TEMPORAL',
      // Un estado temporal implica plazo: no se pregunta, se deriva.
      requiereFechaFin: true,
    });
  });

  it('no pide plazo para una medida que no termina sola', async () => {
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [] });
    api.responder(RUTA_CREAR, { estado: 201, cuerpo: medidaDeEjemplo() });

    renderizarConProveedores(<CatalogoDeMedidas />);
    await screen.findByText(/Todavía no hay ninguna medida/);

    await userEvent.type(screen.getByLabelText('Código'), 'ADVERTENCIA');
    await userEvent.type(screen.getByLabelText('Nombre'), 'Advertencia');
    await userEvent.click(screen.getByRole('button', { name: 'Añadir la medida' }));

    await waitFor(() => expect(api.ultima(RUTA_CREAR)).toBeDefined());
    expect(api.ultima(RUTA_CREAR)?.cuerpo).toMatchObject({
      estadoCuentaResultante: null,
      requiereFechaFin: false,
    });
  });

  it('no envía nada mientras falten el código o el nombre', async () => {
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [] });

    renderizarConProveedores(<CatalogoDeMedidas />);
    await screen.findByText(/Todavía no hay ninguna medida/);

    expect(screen.getByRole('button', { name: 'Añadir la medida' })).toBeDisabled();

    await userEvent.type(screen.getByLabelText('Código'), 'ADVERTENCIA');
    expect(screen.getByRole('button', { name: 'Añadir la medida' })).toBeDisabled();

    await userEvent.type(screen.getByLabelText('Nombre'), 'Advertencia');
    expect(screen.getByRole('button', { name: 'Añadir la medida' })).toBeEnabled();
  });

  it('explica el conflicto de un código repetido sin perder lo escrito', async () => {
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [medidaDeEjemplo()] });
    api.responder(RUTA_CREAR, {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'MEDIDA_DUPLICADA',
        'Ya existe una medida con ese código o ese nombre.'
      ),
    });

    renderizarConProveedores(<CatalogoDeMedidas />);
    await screen.findByText('Restricción temporal');

    await userEvent.type(screen.getByLabelText('Código'), 'RESTRICCION_TEMPORAL');
    await userEvent.type(screen.getByLabelText('Nombre'), 'Otra cosa');
    await userEvent.click(screen.getByRole('button', { name: 'Añadir la medida' }));

    expect(
      await screen.findByText('Ya existe una medida con ese código o ese nombre.')
    ).toBeInTheDocument();
  });

  it('edita una medida sin ofrecer cambiar su código', async () => {
    api.responder(RUTA_MEDIDAS, { estado: 200, cuerpo: [medidaDeEjemplo()] });
    api.responder(RUTA_EDITAR, { estado: 200, cuerpo: medidaDeEjemplo({ nivelSeveridad: 5 }) });

    renderizarConProveedores(<CatalogoDeMedidas />);
    await screen.findByText('Restricción temporal');

    await userEvent.click(screen.getByRole('button', { name: 'Editar' }));

    // El formulario de edición no tiene campo de código: identifica decisiones ya tomadas.
    const codigos = screen.queryAllByLabelText('Código');
    expect(codigos).toHaveLength(1);

    await userEvent.click(screen.getByRole('button', { name: 'Guardar los cambios' }));

    await waitFor(() => expect(api.ultima(RUTA_EDITAR)).toBeDefined());
    expect(api.ultima(RUTA_EDITAR)?.cuerpo).not.toHaveProperty('codigo');
  });

  it('explica el fallo de carga y deja reintentar', async () => {
    api.responder(RUTA_MEDIDAS, {
      estado: 500,
      cuerpo: cuerpoDeError(
        500,
        'ERROR_INTERNO',
        'No pudimos completar la operación. Inténtalo de nuevo.'
      ),
    });

    renderizarConProveedores(<CatalogoDeMedidas />);

    expect(
      await screen.findByText('No pudimos completar la operación. Inténtalo de nuevo.')
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeInTheDocument();
  });
});
