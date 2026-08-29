import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  catalogoDeCategoriasDeEjemplo,
  imagenDeServicioDeEjemplo,
  instalarApiFalsa,
  sesionDeEjemplo,
  servicioPropioDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

describe('Servicios propios', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    URL.createObjectURL = vi.fn(() => 'blob:previsualizacion-de-prueba');
    URL.revokeObjectURL = vi.fn();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('GET /api/catalogos/categorias', {
      estado: 200,
      cuerpo: catalogoDeCategoriasDeEjemplo(),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('muestra el vacío cuando todavía no hay publicaciones', async () => {
    api.responder('GET /api/prestador/servicios', { estado: 200, cuerpo: [] });
    renderizarConProveedores(<App />, '/prestador/servicios');

    expect(await screen.findByText(/Todavía no tienes servicios/)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Publicar un servicio' })).toBeVisible();
  });

  it('exige nombre, descripción y subcategoría antes de crear', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/prestador/servicios', { estado: 200, cuerpo: [] });

    renderizarConProveedores(<App />, '/prestador/servicios/nuevo');

    await persona.click(await screen.findByRole('button', { name: 'Crear servicio' }));

    expect(await screen.findByText('Escribe el nombre del servicio.')).toBeVisible();
    expect(screen.getByText('Describe el servicio.')).toBeVisible();
    expect(screen.getByText('Elige una subcategoría.')).toBeVisible();
    expect(api.ultima('POST /api/prestador/servicios')).toBeUndefined();
  });

  it('crea un servicio inactivo y deja el precio vacío como nulo', async () => {
    const persona = userEvent.setup();
    const creado = servicioPropioDeEjemplo();
    api.responder('GET /api/prestador/servicios', { estado: 200, cuerpo: [] });
    api.responder('POST /api/prestador/servicios', { estado: 201, cuerpo: creado });
    api.responder('GET /api/prestador/servicios/10', { estado: 200, cuerpo: creado });

    renderizarConProveedores(<App />, '/prestador/servicios/nuevo');

    await persona.type(await screen.findByLabelText('Nombre'), 'Reparación de fugas');
    await persona.type(
      screen.getByLabelText('Descripción'),
      'Reparo tuberías y fugas en el hogar.'
    );
    await persona.selectOptions(screen.getByLabelText('Subcategoría'), '1');
    await persona.click(screen.getByRole('button', { name: 'Crear servicio' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/prestador/servicios')?.cuerpo).toEqual({
        nombre: 'Reparación de fugas',
        descripcion: 'Reparo tuberías y fugas en el hogar.',
        idSubcategoriaServicio: 1,
        precioReferencia: null,
      });
    });
    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Activar servicio' })).toBeVisible();
  });

  it('muestra A convenir y permite activar o desactivar desde el listado', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/prestador/servicios', {
      estado: 200,
      cuerpo: [servicioPropioDeEjemplo()],
    });
    api.responder('PUT /api/prestador/servicios/10/estado', {
      estado: 200,
      cuerpo: servicioPropioDeEjemplo({ estado: 'ACTIVO' }),
    });

    renderizarConProveedores(<App />, '/prestador/servicios');

    expect(await screen.findByText(/A convenir/)).toBeVisible();
    expect(screen.getByText(/Inactivo/)).toBeVisible();

    await persona.click(screen.getByRole('button', { name: 'Activar' }));

    await waitFor(() => {
      expect(api.ultima('PUT /api/prestador/servicios/10/estado')?.cuerpo).toEqual({
        estado: 'ACTIVO',
      });
    });
  });

  it('conserva la subcategoría al editar cuando el catálogo llega después', async () => {
    const servicio = servicioPropioDeEjemplo();
    api.responder('GET /api/prestador/servicios/10', { estado: 200, cuerpo: servicio });

    renderizarConProveedores(<App />, '/prestador/servicios/10');

    await waitFor(() => {
      expect(screen.getByLabelText('Subcategoría')).toHaveValue('1');
    });
    expect(screen.getByLabelText('Categoría')).toHaveValue('1');
  });

  it('sube una imagen con previsualización y texto alternativo', async () => {
    const persona = userEvent.setup();
    const servicio = servicioPropioDeEjemplo();
    api.responder('GET /api/prestador/servicios/10', { estado: 200, cuerpo: servicio });
    api.responder('POST /api/prestador/servicios/10/imagenes', {
      estado: 201,
      cuerpo: imagenDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/prestador/servicios/10');

    await persona.type(
      await screen.findByLabelText('Texto alternativo de la imagen nueva'),
      'Tubería reparada'
    );
    const archivo = new File(['contenido'], 'obra.png', { type: 'image/png' });
    await persona.upload(screen.getByLabelText('Agregar imagen'), archivo);

    await waitFor(() => {
      const enviada = api.ultima('POST /api/prestador/servicios/10/imagenes')?.formulario;
      expect(enviada?.get('textoAlternativo')).toBe('Tubería reparada');
      expect(enviada?.get('archivo')).toBeInstanceOf(File);
    });
  });
});
