import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it } from 'vitest';

import App from '../../../App';
import {
  catalogoDeEjemplo,
  cuerpoDeError,
  imagenDeEjemplo,
  instalarApiFalsa,
  perfilDeEjemplo,
  sesionDeEjemplo,
  trabajoDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

/**
 * El portafolio desde el navegador: trabajos e imágenes que el prestador administra a mano.
 *
 * Se monta `App` entera en la ruta del perfil, porque el portafolio es una sección suya y no una
 * pantalla aparte.
 */
describe('Portafolio', () => {
  let api: ApiFalsa;

  const RUTA_TRABAJOS = '/api/prestador/portafolio/trabajos';

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('GET /api/catalogos/departamentos', { estado: 200, cuerpo: catalogoDeEjemplo() });
    api.responder('GET /api/prestador/perfil', { estado: 200, cuerpo: perfilDeEjemplo() });
    api.responder('GET /api/prestador/contactos', { estado: 200, cuerpo: [] });
    api.responder(`GET ${RUTA_TRABAJOS}`, { estado: 200, cuerpo: [] });
  });

  /** Un archivo con nombre y tipo reales; el contenido no importa porque valida el backend. */
  function archivoDeImagen(nombre = 'obra.png', tipo = 'image/png') {
    return new File(['contenido-de-prueba'], nombre, { type: tipo });
  }

  it('agrega un trabajo con su fecha opcional', async () => {
    const persona = userEvent.setup();
    api.responder(`POST ${RUTA_TRABAJOS}`, { estado: 201, cuerpo: trabajoDeEjemplo() });

    renderizarConProveedores(<App />, '/prestador');

    await persona.type(await screen.findByLabelText('Título'), 'Instalación eléctrica');
    await persona.type(screen.getByLabelText('Descripción'), 'Instalación completa.');
    await persona.type(screen.getByLabelText('Fecha de realización (opcional)'), '2024-05-20');
    await persona.click(screen.getByRole('button', { name: 'Agregar trabajo' }));

    await waitFor(() => {
      expect(api.ultima(`POST ${RUTA_TRABAJOS}`)?.cuerpo).toEqual({
        titulo: 'Instalación eléctrica',
        descripcion: 'Instalación completa.',
        fechaRealizacion: '2024-05-20',
      });
    });
  });

  it('deja la fecha en nulo cuando no se indica', async () => {
    const persona = userEvent.setup();
    api.responder(`POST ${RUTA_TRABAJOS}`, { estado: 201, cuerpo: trabajoDeEjemplo() });

    renderizarConProveedores(<App />, '/prestador');

    await persona.type(await screen.findByLabelText('Título'), 'Sin fecha');
    await persona.type(screen.getByLabelText('Descripción'), 'Un trabajo sin fecha.');
    await persona.click(screen.getByRole('button', { name: 'Agregar trabajo' }));

    await waitFor(() => {
      expect(api.ultima(`POST ${RUTA_TRABAJOS}`)?.cuerpo).toMatchObject({
        fechaRealizacion: null,
      });
    });
  });

  it('exige título y descripción antes de llamar a la API', async () => {
    const persona = userEvent.setup();

    renderizarConProveedores(<App />, '/prestador');

    await persona.click(await screen.findByRole('button', { name: 'Agregar trabajo' }));

    expect(await screen.findByText('Escribe un título para el trabajo.')).toBeVisible();
    expect(screen.getByText('Describe el trabajo que realizaste.')).toBeVisible();
    expect(api.ultima(`POST ${RUTA_TRABAJOS}`)).toBeUndefined();
  });

  it('reordena los trabajos enviando la lista completa', async () => {
    const persona = userEvent.setup();
    api.responder(`GET ${RUTA_TRABAJOS}`, {
      estado: 200,
      cuerpo: [
        trabajoDeEjemplo({ idTrabajo: 1, titulo: 'Primero' }),
        trabajoDeEjemplo({ idTrabajo: 2, titulo: 'Segundo' }),
      ],
    });
    api.responder(`PUT ${RUTA_TRABAJOS}/orden`, { estado: 200, cuerpo: [] });

    renderizarConProveedores(<App />, '/prestador');

    await persona.click(await screen.findByRole('button', { name: 'Subir el trabajo Segundo' }));

    await waitFor(() => {
      expect(api.ultima(`PUT ${RUTA_TRABAJOS}/orden`)?.cuerpo).toEqual({ idsEnOrden: [2, 1] });
    });
  });

  it('sube una imagen con su texto alternativo en el mismo formulario', async () => {
    const persona = userEvent.setup();
    api.responder(`GET ${RUTA_TRABAJOS}`, {
      estado: 200,
      cuerpo: [trabajoDeEjemplo({ idTrabajo: 7, titulo: 'Con imágenes' })],
    });
    api.responder(`POST ${RUTA_TRABAJOS}/7/imagenes`, {
      estado: 201,
      cuerpo: imagenDeEjemplo(1, 'Fachada terminada'),
    });

    renderizarConProveedores(<App />, '/prestador');

    await persona.type(
      await screen.findByLabelText('Texto alternativo de la imagen nueva'),
      'Fachada terminada'
    );
    await persona.upload(screen.getByLabelText('Agregar imagen'), archivoDeImagen());

    await waitFor(() => {
      expect(api.ultima(`POST ${RUTA_TRABAJOS}/7/imagenes`)).toBeDefined();
    });

    const formulario = api.ultima(`POST ${RUTA_TRABAJOS}/7/imagenes`)?.formulario;
    expect(formulario?.get('textoAlternativo')).toBe('Fachada terminada');
    expect((formulario?.get('archivo') as File).name).toBe('obra.png');
  });

  // El `accept` del campo filtra por tipo declarado, así que lo que llega al
  // backend siempre lo parece: quien lo rechaza por su firma real es el
  // servidor, y aquí se comprueba que ese mensaje se muestre.
  it('explica el rechazo de un archivo que el backend no admite', async () => {
    const persona = userEvent.setup();
    api.responder(`GET ${RUTA_TRABAJOS}`, {
      estado: 200,
      cuerpo: [trabajoDeEjemplo({ idTrabajo: 7 })],
    });
    api.responder(`POST ${RUTA_TRABAJOS}/7/imagenes`, {
      estado: 400,
      cuerpo: cuerpoDeError(
        400,
        'IMAGEN_NO_ADMITIDA',
        'El contenido del archivo no corresponde con una imagen JPEG, PNG o WebP.'
      ),
    });

    renderizarConProveedores(<App />, '/prestador');

    await persona.upload(
      await screen.findByLabelText('Agregar imagen'),
      archivoDeImagen('disfrazada.png', 'image/png')
    );

    expect(
      await screen.findByText(
        'El contenido del archivo no corresponde con una imagen JPEG, PNG o WebP.'
      )
    ).toBeVisible();
  });

  it('explica el rechazo de una imagen demasiado grande', async () => {
    const persona = userEvent.setup();
    api.responder(`GET ${RUTA_TRABAJOS}`, {
      estado: 200,
      cuerpo: [trabajoDeEjemplo({ idTrabajo: 7 })],
    });
    api.responder(`POST ${RUTA_TRABAJOS}/7/imagenes`, {
      estado: 413,
      cuerpo: cuerpoDeError(
        413,
        'IMAGEN_DEMASIADO_GRANDE',
        'La imagen supera el máximo de 5 MB. Reduce su tamaño e inténtalo otra vez.'
      ),
    });

    renderizarConProveedores(<App />, '/prestador');

    await persona.upload(await screen.findByLabelText('Agregar imagen'), archivoDeImagen());

    expect(await screen.findByText(/La imagen supera el máximo de 5 MB/)).toBeVisible();
  });

  it('muestra las imágenes con su texto alternativo como alternativa accesible', async () => {
    api.responder(`GET ${RUTA_TRABAJOS}`, {
      estado: 200,
      cuerpo: [
        trabajoDeEjemplo({
          idTrabajo: 7,
          titulo: 'Con imágenes',
          imagenes: [imagenDeEjemplo(1, 'Fachada pintada de blanco')],
        }),
      ],
    });

    renderizarConProveedores(<App />, '/prestador');

    expect(await screen.findByAltText('Fachada pintada de blanco')).toBeVisible();
  });

  it('elimina una imagen del trabajo', async () => {
    const persona = userEvent.setup();
    api.responder(`GET ${RUTA_TRABAJOS}`, {
      estado: 200,
      cuerpo: [
        trabajoDeEjemplo({ idTrabajo: 7, titulo: 'Con imágenes', imagenes: [imagenDeEjemplo(4)] }),
      ],
    });
    api.responder(`DELETE ${RUTA_TRABAJOS}/7/imagenes/4`, { estado: 204 });

    renderizarConProveedores(<App />, '/prestador');

    await persona.click(
      await screen.findByRole('button', { name: 'Quitar la imagen 1 de Con imágenes' })
    );

    await waitFor(() => {
      expect(api.ultima(`DELETE ${RUTA_TRABAJOS}/7/imagenes/4`)).toBeDefined();
    });
  });

  it('elimina un trabajo completo', async () => {
    const persona = userEvent.setup();
    api.responder(`GET ${RUTA_TRABAJOS}`, {
      estado: 200,
      cuerpo: [trabajoDeEjemplo({ idTrabajo: 9, titulo: 'Se va completo' })],
    });
    api.responder(`DELETE ${RUTA_TRABAJOS}/9`, { estado: 204 });

    renderizarConProveedores(<App />, '/prestador');

    await persona.click(
      await screen.findByRole('button', { name: 'Quitar el trabajo Se va completo' })
    );

    await waitFor(() => {
      expect(api.ultima(`DELETE ${RUTA_TRABAJOS}/9`)).toBeDefined();
    });
  });

  it('edita un trabajo ya guardado con sus datos cargados', async () => {
    const persona = userEvent.setup();
    api.responder(`GET ${RUTA_TRABAJOS}`, {
      estado: 200,
      cuerpo: [
        trabajoDeEjemplo({
          idTrabajo: 5,
          titulo: 'Título original',
          fechaRealizacion: '2023-11-02',
        }),
      ],
    });
    api.responder(`PUT ${RUTA_TRABAJOS}/5`, { estado: 200, cuerpo: trabajoDeEjemplo() });

    renderizarConProveedores(<App />, '/prestador');

    await persona.click(
      await screen.findByRole('button', { name: 'Editar el trabajo Título original' })
    );

    const formulario = screen.getByRole('button', { name: 'Guardar' }).closest('form');
    expect(formulario).not.toBeNull();

    const titulo = within(formulario as HTMLFormElement).getByLabelText('Título');
    expect(titulo).toHaveValue('Título original');

    await persona.clear(titulo);
    await persona.type(titulo, 'Título corregido');
    await persona.click(screen.getByRole('button', { name: 'Guardar' }));

    await waitFor(() => {
      expect(api.ultima(`PUT ${RUTA_TRABAJOS}/5`)?.cuerpo).toMatchObject({
        titulo: 'Título corregido',
        fechaRealizacion: '2023-11-02',
      });
    });
  });
});
