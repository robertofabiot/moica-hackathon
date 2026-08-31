import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  catalogoDeCategoriasDeEjemplo,
  catalogoDeEjemplo,
  cuerpoDeError,
  detallePublicoDeServicioDeEjemplo,
  instalarApiFalsa,
  servicioPublicoDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

describe('Explorar servicios', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
    api.responder('GET /api/catalogos/categorias', {
      estado: 200,
      cuerpo: catalogoDeCategoriasDeEjemplo(),
    });
    api.responder('GET /api/catalogos/departamentos', {
      estado: 200,
      cuerpo: catalogoDeEjemplo(),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('muestra el estado de carga mientras llega el listado', () => {
    api.colgar('GET /api/servicios');
    renderizarConProveedores(<App />, '/explorar');

    expect(screen.getByRole('status')).toHaveTextContent('Buscando servicios…');
  });

  it('muestra el error y permite reintentar', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/servicios', {
      estado: 500,
      cuerpo: cuerpoDeError(500, 'ERROR_INTERNO', 'Algo falló en Moica.'),
    });

    renderizarConProveedores(<App />, '/explorar');

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo falló en Moica.');

    api.responder('GET /api/servicios', { estado: 200, cuerpo: [] });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByText(/No hay servicios que coincidan/)).toBeVisible();
  });

  it('muestra el vacío cuando no hay resultados', async () => {
    api.responder('GET /api/servicios', { estado: 200, cuerpo: [] });
    renderizarConProveedores(<App />, '/explorar');

    expect(await screen.findByText(/No hay servicios que coincidan/)).toBeVisible();
  });

  it('presenta un precio nulo como A convenir y no muestra contactos', async () => {
    api.responder('GET /api/servicios', {
      estado: 200,
      cuerpo: [
        servicioPublicoDeEjemplo(),
        servicioPublicoDeEjemplo({
          idServicioPublicado: 11,
          nombre: 'Instalación eléctrica',
          precioReferencia: 450,
          nombreSubcategoria: 'Electricidad',
        }),
      ],
    });

    renderizarConProveedores(<App />, '/explorar');

    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
    expect(screen.getByText('A convenir')).toBeVisible();
    expect(screen.getByText('C$450')).toBeVisible();
    expect(screen.getAllByText('Verificado').length).toBeGreaterThan(0);
    expect(screen.queryByText(/@moica\.test/)).not.toBeInTheDocument();
    expect(screen.queryByText(/correo/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/teléfono/i)).not.toBeInTheDocument();
  });

  it('lee el texto inicial desde la dirección y pide ese filtro', async () => {
    api.responder('GET /api/servicios?texto=fuga', { estado: 200, cuerpo: [] });
    renderizarConProveedores(<App />, '/explorar?texto=fuga');

    expect(await screen.findByLabelText('Qué buscas')).toHaveValue('fuga');
    expect(api.ultima('GET /api/servicios?texto=fuga')).toBeDefined();
  });

  it('limpia la dirección al quitar filtros', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/servicios?texto=fuga', { estado: 200, cuerpo: [] });
    api.responder('GET /api/servicios', { estado: 200, cuerpo: [] });
    renderizarConProveedores(<App />, '/explorar?texto=fuga');
    expect(await screen.findByLabelText('Qué buscas')).toHaveValue('fuga');

    await persona.click(screen.getByRole('button', { name: 'Quitar filtros' }));

    expect(screen.getByLabelText('Qué buscas')).toHaveValue('');
    expect(api.ultima('GET /api/servicios')).toBeDefined();
  });

  it('combina texto, categoría y municipio al buscar', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/servicios', { estado: 200, cuerpo: [] });
    api.responder('GET /api/servicios?texto=fuga&idCategoria=1&idMunicipio=3', {
      estado: 200,
      cuerpo: [servicioPublicoDeEjemplo()],
    });

    renderizarConProveedores(<App />, '/explorar');
    await screen.findByText(/No hay servicios que coincidan/);

    await persona.type(screen.getByLabelText('Qué buscas'), 'fuga');
    await persona.click(screen.getByRole('button', { name: 'Hogar' }));
    await persona.selectOptions(screen.getByLabelText('Ubicación'), '3');
    await persona.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
    expect(api.ultima('GET /api/servicios?texto=fuga&idCategoria=1&idMunicipio=3')).toBeDefined();
  });

  it('abre el detalle con la advertencia de la insignia', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/servicios', {
      estado: 200,
      cuerpo: [servicioPublicoDeEjemplo()],
    });
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar');
    await persona.click(await screen.findByRole('link', { name: /Reparación de fugas/ }));

    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
    expect(screen.getByText('A convenir')).toBeVisible();
    expect(screen.getByText(/No garantiza la calidad futura del trabajo/)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Iniciar sesión para solicitar' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Solicitar este servicio' })).not.toBeInTheDocument();
  });
});
