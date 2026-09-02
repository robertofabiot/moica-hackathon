import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  catalogoDeCategoriasDeEjemplo,
  instalarApiFalsa,
  sesionDeEjemplo,
  servicioPropioDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

const RUTA_NUEVO = '/prestador/servicios/nuevo';

describe('Asistente de nuevo servicio', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('GET /api/catalogos/categorias', {
      estado: 200,
      cuerpo: catalogoDeCategoriasDeEjemplo(),
    });
    api.responder('GET /api/prestador/servicios', { estado: 200, cuerpo: [] });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('muestra el indicador con Información como paso actual', async () => {
    renderizarConProveedores(<App />, RUTA_NUEVO);

    expect(await screen.findByRole('heading', { name: 'Publicar un servicio' })).toBeVisible();
    const pasos = screen.getByRole('list', { name: 'Pasos de publicación' });
    expect(pasos).toHaveTextContent('Información');
    expect(pasos).toHaveTextContent('Detalles');
    expect(pasos).toHaveTextContent('Precio');
    expect(pasos).toHaveTextContent('Publicar');
    expect(screen.getByRole('listitem', { current: 'step' })).toHaveTextContent('Información');
    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Siguiente' })).toBeVisible();
  });

  it('no avanza ni publica si el paso de información está incompleto', async () => {
    const persona = userEvent.setup();
    renderizarConProveedores(<App />, RUTA_NUEVO);

    await persona.click(await screen.findByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByText('Escribe el nombre del servicio.')).toBeVisible();
    expect(screen.getByText('Elige una categoría.')).toBeVisible();
    expect(screen.getByText('Elige una subcategoría.')).toBeVisible();
    expect(screen.getByRole('listitem', { current: 'step' })).toHaveTextContent('Información');
    expect(screen.queryByLabelText('Descripción')).not.toBeInTheDocument();
    expect(api.ultima('POST /api/prestador/servicios')).toBeUndefined();
  });

  it('vuelve al listado al cancelar desde el primer paso', async () => {
    const persona = userEvent.setup();
    renderizarConProveedores(<App />, RUTA_NUEVO);

    await persona.click(await screen.findByRole('button', { name: 'Cancelar' }));

    expect(await screen.findByRole('heading', { name: 'Tus servicios' })).toBeVisible();
  });

  it('exige una descripción antes de dejar el paso de detalles', async () => {
    const persona = userEvent.setup();
    renderizarConProveedores(<App />, RUTA_NUEVO);

    await completarInformacion(persona);
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByLabelText('Descripción')).toBeVisible();
    expect(screen.getByText('Subir fotos')).toBeVisible();
    expect(screen.getByText(/Arrastra tus imágenes aquí o haz clic para buscarlas/i)).toBeVisible();
    expect(screen.getByText('0/3000')).toBeVisible();

    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByText('Describe el servicio.')).toBeVisible();
    expect(screen.getByRole('listitem', { current: 'step' })).toHaveTextContent('Detalles');
    expect(api.ultima('POST /api/prestador/servicios')).toBeUndefined();
  });

  it('permite adjuntar fotos en el paso de detalles y las sube al publicar', async () => {
    const persona = userEvent.setup();
    const creado = servicioPropioDeEjemplo();
    api.responder('POST /api/prestador/servicios', { estado: 201, cuerpo: creado });
    api.responder('POST /api/prestador/servicios/10/imagenes', {
      estado: 201,
      cuerpo: {
        idImagenServicioPublicado: 101,
        urlImagen: 'https://ejemplo.com/foto.jpg',
        textoAlternativo: null,
        orden: 1,
      },
    });
    api.responder('GET /api/prestador/servicios/10', { estado: 200, cuerpo: creado });

    renderizarConProveedores(<App />, RUTA_NUEVO);

    await completarInformacion(persona);
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));
    await persona.type(
      await screen.findByLabelText('Descripción'),
      'Reparo tuberías y fugas en el hogar.'
    );

    const archivo = new File(['contenido-imagen'], 'foto-tuberias.jpg', { type: 'image/jpeg' });
    const entradaArchivo = screen.getByLabelText(/Fotos del servicio/i);
    await persona.upload(entradaArchivo, archivo);

    expect(await screen.findByText('1 foto seleccionada')).toBeVisible();
    expect(screen.getByText('foto-tuberias.jpg')).toBeVisible();

    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));
    await persona.click(await screen.findByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByText('1 foto lista para subir')).toBeVisible();
    await persona.click(screen.getByRole('button', { name: 'Publicar servicio' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/prestador/servicios/10/imagenes')).toBeDefined();
    });
  });

  it('rechaza un precio inválido y acepta el vacío como A convenir', async () => {
    const persona = userEvent.setup();
    renderizarConProveedores(<App />, RUTA_NUEVO);

    await completarInformacion(persona);
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));
    await persona.type(await screen.findByLabelText('Descripción'), 'Reparo tuberías.');
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByLabelText('Precio de referencia')).toBeVisible();
    expect(
      screen.getByText('Si lo dejas vacío, en la búsqueda pública se mostrará como «A convenir».')
    ).toBeVisible();

    await persona.type(screen.getByLabelText('Precio de referencia'), '0');
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByText('Si indicas un precio, debe ser mayor que cero.')).toBeVisible();
    expect(screen.getByRole('listitem', { current: 'step' })).toHaveTextContent('Precio');

    await persona.clear(screen.getByLabelText('Precio de referencia'));
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));

    expect(await screen.findByRole('heading', { name: 'Revisión y confirmación' })).toBeVisible();
    expect(screen.getByText('A convenir')).toBeVisible();
    expect(screen.getByText('Reparación de fugas')).toBeVisible();
    expect(screen.getByText('Hogar y mantenimiento · Plomería')).toBeVisible();
    expect(screen.getByText('Reparo tuberías.')).toBeVisible();
  });

  it('crea un servicio inactivo y deja el precio vacío como nulo', async () => {
    const persona = userEvent.setup();
    const creado = servicioPropioDeEjemplo();
    api.responder('POST /api/prestador/servicios', { estado: 201, cuerpo: creado });
    api.responder('GET /api/prestador/servicios/10', { estado: 200, cuerpo: creado });

    renderizarConProveedores(<App />, RUTA_NUEVO);

    await completarInformacion(persona);
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));
    await persona.type(
      await screen.findByLabelText('Descripción'),
      'Reparo tuberías y fugas en el hogar.'
    );
    expect(screen.getByText('36/3000')).toBeVisible();
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));
    await persona.click(await screen.findByRole('button', { name: 'Siguiente' }));
    await persona.click(await screen.findByRole('button', { name: 'Publicar servicio' }));

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

  it('conserva los datos al volver atrás y no publica hasta el último paso', async () => {
    const persona = userEvent.setup();
    renderizarConProveedores(<App />, RUTA_NUEVO);

    await completarInformacion(persona);
    await persona.click(screen.getByRole('button', { name: 'Siguiente' }));
    await persona.type(await screen.findByLabelText('Descripción'), 'Reparo tuberías.');
    await persona.click(screen.getByRole('button', { name: 'Atrás' }));

    expect(await screen.findByLabelText('Título del servicio')).toHaveValue('Reparación de fugas');
    expect(screen.getByLabelText('Categoría')).toHaveValue('1');
    expect(screen.getByLabelText('Subcategoría')).toHaveValue('1');
    expect(api.ultima('POST /api/prestador/servicios')).toBeUndefined();
  });
});

async function completarInformacion(persona: ReturnType<typeof userEvent.setup>) {
  await persona.type(await screen.findByLabelText('Título del servicio'), 'Reparación de fugas');
  const categoria = screen.getByLabelText('Categoría');
  await waitFor(() => {
    expect(categoria).toBeEnabled();
  });
  await persona.selectOptions(categoria, '1');
  await persona.selectOptions(screen.getByLabelText('Subcategoría'), '1');
}
