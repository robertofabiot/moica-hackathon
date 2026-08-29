import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import App from '../../../App';
import { definirTiempoDeEsperaMs, TIEMPO_DE_ESPERA_MS } from '../../../comun/api';
import {
  catalogoDeEjemplo,
  cuerpoDeError,
  estadoDeVerificacionDeEjemplo,
  instalarApiFalsa,
  perfilDeEjemplo,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

/**
 * El perfil de prestador desde el navegador: crearlo, editarlo y administrar lo que cuelga de él.
 *
 * Se monta `App` entera y se navega por sus rutas, de modo que lo que se comprueba es lo que hace
 * la aplicación y no una pantalla montada a mano.
 */
describe('Perfil de prestador', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('GET /api/catalogos/departamentos', { estado: 200, cuerpo: catalogoDeEjemplo() });
    api.responder('GET /api/prestador/contactos', { estado: 200, cuerpo: [] });
    api.responder('GET /api/prestador/portafolio/trabajos', { estado: 200, cuerpo: [] });
    // La sección de verificación es parte de la pantalla desde P4V: sin su
    // respuesta, el perfil se pintaría con un error que no es el que cada
    // prueba viene a comprobar.
    api.responder('GET /api/prestador/verificacion', {
      estado: 200,
      cuerpo: estadoDeVerificacionDeEjemplo(),
    });
    api.responder('GET /api/prestador/verificacion/solicitudes', { estado: 200, cuerpo: [] });
  });

  afterEach(() => {
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  /** Quien todavía no creó su perfil recibe 404 con el código que lo distingue. */
  function sinPerfil() {
    api.responder('GET /api/prestador/perfil', {
      estado: 404,
      cuerpo: cuerpoDeError(
        404,
        'PERFIL_NO_ENCONTRADO',
        'Esta cuenta todavía no tiene un perfil de prestador.'
      ),
    });
  }

  function conPerfil(cambios = {}) {
    api.responder('GET /api/prestador/perfil', {
      estado: 200,
      cuerpo: perfilDeEjemplo(cambios),
    });
  }

  it('crea el perfil con el tipo y el municipio elegidos', async () => {
    const persona = userEvent.setup();
    sinPerfil();
    api.responder('POST /api/prestador/perfil', { estado: 201, cuerpo: perfilDeEjemplo() });

    renderizarConProveedores(<App />, '/prestador');

    expect(await screen.findByRole('heading', { name: 'Crea tu perfil' })).toBeVisible();

    await persona.type(screen.getByLabelText('Nombre público'), 'Taller La Esperanza');
    await persona.click(screen.getByLabelText('Emprendimiento'));
    await persona.selectOptions(screen.getByLabelText('Municipio principal'), '8');
    await persona.type(screen.getByLabelText('Presentación'), 'Diez años reparando instalaciones.');
    await persona.type(screen.getByLabelText('Cobertura'), 'Distritos I y II.');
    await persona.click(screen.getByRole('button', { name: 'Crear perfil' }));

    await waitFor(() => {
      expect(api.ultima('POST /api/prestador/perfil')).toBeDefined();
    });

    expect(api.ultima('POST /api/prestador/perfil')?.cuerpo).toEqual({
      nombrePublico: 'Taller La Esperanza',
      descripcion: 'Diez años reparando instalaciones.',
      tipoPrestador: 'EMPRENDIMIENTO',
      // El selector entrega texto y el esquema lo convierte antes de enviarlo.
      idMunicipioPrincipal: 8,
      descripcionCobertura: 'Distritos I y II.',
    });
  });

  it('exige los campos obligatorios antes de llamar a la API', async () => {
    const persona = userEvent.setup();
    sinPerfil();

    renderizarConProveedores(<App />, '/prestador');
    await screen.findByRole('heading', { name: 'Crea tu perfil' });

    await persona.click(screen.getByRole('button', { name: 'Crear perfil' }));

    expect(await screen.findByText('Escribe el nombre con el que quieres aparecer.')).toBeVisible();
    expect(screen.getByText('Elige tu municipio principal.')).toBeVisible();
    expect(screen.getByText('Cuenta quién eres y qué ofreces.')).toBeVisible();
    expect(api.ultima('POST /api/prestador/perfil')).toBeUndefined();
  });

  it('muestra el detalle por campo que devuelve el backend', async () => {
    const persona = userEvent.setup();
    sinPerfil();
    api.responder('POST /api/prestador/perfil', {
      estado: 400,
      cuerpo: cuerpoDeError(400, 'VALIDACION', 'Revisa los datos enviados.', [
        { campo: 'nombrePublico', mensaje: 'Ese nombre no está permitido.' },
      ]),
    });

    renderizarConProveedores(<App />, '/prestador');
    await screen.findByRole('heading', { name: 'Crea tu perfil' });

    await persona.type(screen.getByLabelText('Nombre público'), 'Taller');
    await persona.selectOptions(screen.getByLabelText('Municipio principal'), '3');
    await persona.type(screen.getByLabelText('Presentación'), 'Presentación de prueba.');
    await persona.type(screen.getByLabelText('Cobertura'), 'Managua.');
    await persona.click(screen.getByRole('button', { name: 'Crear perfil' }));

    expect(await screen.findByText('Ese nombre no está permitido.')).toBeVisible();
  });

  it('avisa que el perfil sin verificar todavía es privado', async () => {
    conPerfil();

    renderizarConProveedores(<App />, '/prestador');

    const aviso = await screen.findByText(/Tu perfil todavía es privado/);
    expect(aviso).toBeVisible();
    expect(aviso.parentElement).toHaveTextContent(
      'Nadie más puede verlo mientras esté sin verificar.'
    );
    // Nada en la pantalla debe sugerir que ya se ve públicamente.
    expect(screen.queryByText(/tu perfil ya es público/i)).not.toBeInTheDocument();
  });

  it('edita el perfil existente con sus datos ya cargados', async () => {
    const persona = userEvent.setup();
    conPerfil();
    api.responder('PUT /api/prestador/perfil', {
      estado: 200,
      cuerpo: perfilDeEjemplo({ nombrePublico: 'Taller La Esperanza y Familia' }),
    });

    renderizarConProveedores(<App />, '/prestador');

    const nombre = await screen.findByLabelText('Nombre público');
    expect(nombre).toHaveValue('Taller La Esperanza');
    expect(screen.getByLabelText('Municipio principal')).toHaveValue('3');
    expect(screen.getByLabelText('Independiente')).toBeChecked();

    await persona.clear(nombre);
    await persona.type(nombre, 'Taller La Esperanza y Familia');
    await persona.click(screen.getByRole('button', { name: 'Guardar cambios' }));

    expect(await screen.findByText('Guardamos tus cambios.')).toBeVisible();
    expect(api.ultima('PUT /api/prestador/perfil')?.cuerpo).toMatchObject({
      nombrePublico: 'Taller La Esperanza y Familia',
    });
  });

  // El catálogo llega después que el formulario y sustituye las opciones del
  // selector. Un `select` no controlado descarta en ese momento el valor que ya
  // no encuentra entre sus hijos y se queda con el primero, así que el perfil
  // aparecía con un municipio que no era el suyo.
  it('conserva el municipio del perfil cuando llegan las opciones del catálogo', async () => {
    conPerfil();

    renderizarConProveedores(<App />, '/prestador');

    const municipio = await screen.findByLabelText('Municipio principal');

    // Se espera a que las opciones reales estén puestas, que es cuando ocurría
    // la pérdida del valor.
    expect(await screen.findByRole('option', { name: 'Tipitapa' })).toBeInTheDocument();
    expect(municipio).toHaveValue('3');
    expect(screen.getByRole('option', { name: 'Managua', selected: true })).toBeInTheDocument();
  });

  it('alterna la disponibilidad y refleja el estado devuelto', async () => {
    const persona = userEvent.setup();
    conPerfil();
    api.responder('PUT /api/prestador/disponibilidad', {
      estado: 200,
      cuerpo: perfilDeEjemplo({ disponibilidad: 'NO_DISPONIBLE' }),
    });

    renderizarConProveedores(<App />, '/prestador');

    await persona.click(await screen.findByRole('button', { name: 'Marcarme como no disponible' }));

    expect(await screen.findByText('No disponible')).toBeVisible();
    expect(api.ultima('PUT /api/prestador/disponibilidad')?.cuerpo).toEqual({
      disponibilidad: 'NO_DISPONIBLE',
    });
    expect(await screen.findByRole('button', { name: 'Volver a estar disponible' })).toBeVisible();
  });

  it('agrega un contacto y lo muestra en la lista', async () => {
    const persona = userEvent.setup();
    conPerfil();
    api.responder('POST /api/prestador/contactos', {
      estado: 201,
      cuerpo: {
        idMedioContactoPrestador: 1,
        contenido: 'WhatsApp 8888-8888',
        ordenVisualizacion: 0,
        fechaCreacion: '2026-08-25T10:00:00-06:00',
      },
    });

    renderizarConProveedores(<App />, '/prestador');

    await persona.type(await screen.findByLabelText('Agregar un contacto'), 'WhatsApp 8888-8888');

    api.responder('GET /api/prestador/contactos', {
      estado: 200,
      cuerpo: [
        {
          idMedioContactoPrestador: 1,
          contenido: 'WhatsApp 8888-8888',
          ordenVisualizacion: 0,
          fechaCreacion: '2026-08-25T10:00:00-06:00',
        },
      ],
    });
    await persona.click(screen.getByRole('button', { name: 'Agregar contacto' }));

    expect(await screen.findByText('WhatsApp 8888-8888')).toBeVisible();
  });

  it('reordena los contactos enviando la lista completa', async () => {
    const persona = userEvent.setup();
    conPerfil();
    api.responder('GET /api/prestador/contactos', {
      estado: 200,
      cuerpo: [
        {
          idMedioContactoPrestador: 1,
          contenido: 'Primero',
          ordenVisualizacion: 0,
          fechaCreacion: '2026-08-25T10:00:00-06:00',
        },
        {
          idMedioContactoPrestador: 2,
          contenido: 'Segundo',
          ordenVisualizacion: 1,
          fechaCreacion: '2026-08-25T10:00:00-06:00',
        },
      ],
    });
    api.responder('PUT /api/prestador/contactos/orden', { estado: 200, cuerpo: [] });

    renderizarConProveedores(<App />, '/prestador');

    await persona.click(await screen.findByRole('button', { name: 'Subir el contacto Segundo' }));

    await waitFor(() => {
      expect(api.ultima('PUT /api/prestador/contactos/orden')?.cuerpo).toEqual({
        idsEnOrden: [2, 1],
      });
    });
  });

  it('explica el error cuando no se pueden cargar los contactos', async () => {
    conPerfil();
    api.responder('GET /api/prestador/contactos', {
      estado: 503,
      cuerpo: cuerpoDeError(503, 'ERROR_INTERNO', 'No pudimos completar la operación.'),
    });

    renderizarConProveedores(<App />, '/prestador');

    expect(await screen.findByText(/No pudimos cargar tus contactos/)).toBeVisible();
    expect(screen.getByRole('button', { name: 'Reintentar' })).toBeVisible();
  });

  it('lleva a iniciar sesión a quien llega sin sesión', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'No hay sesión.'),
    });

    renderizarConProveedores(<App />, '/prestador');

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
    expect(
      screen.queryByRole('heading', { name: 'Tu perfil de prestador' })
    ).not.toBeInTheDocument();
  });

  it('no muestra el perfil de la cuenta anterior a la que entra después', async () => {
    const persona = userEvent.setup();

    // La cuenta A entra, ve su perfil y cierra sesión.
    conPerfil({ nombrePublico: 'Taller de Ana' });
    api.responder('DELETE /api/auth/sesion', { estado: 204 });

    const { cliente } = renderizarConProveedores(<App />, '/prestador');
    expect(await screen.findByDisplayValue('Taller de Ana')).toBeVisible();

    await persona.click(screen.getByRole('link', { name: 'Volver al inicio' }));
    await persona.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();

    // La cuenta B entra sin recargar y su perfil todavía no responde.
    api.responder('POST /api/auth/sesion', { estado: 201, cuerpo: sesionDeEjemplo() });
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.colgar('GET /api/prestador/perfil');

    await persona.type(screen.getByLabelText('Correo electrónico'), 'bruno@moica.test');
    await persona.type(screen.getByLabelText('Contraseña'), 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    await persona.click(await screen.findByRole('link', { name: 'Mi perfil de prestador' }));

    expect(await screen.findByText('Cargando tu perfil…')).toBeVisible();
    expect(screen.queryByDisplayValue('Taller de Ana')).not.toBeInTheDocument();
    expect(cliente.getQueryData(['prestador', 'perfil'])).toBeUndefined();
  });
});
