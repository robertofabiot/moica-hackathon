import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../App';
import { definirTiempoDeEsperaMs, TIEMPO_DE_ESPERA_MS } from '../capacidades/auth/api';
import {
  catalogoDeCategoriasDeEjemplo,
  catalogoDeEjemplo,
  cuerpoDeError,
  instalarApiFalsa,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../pruebas/apiFalsa';
import { renderizarConProveedores } from '../pruebas/utilidades';

describe('estado de acceso en la pantalla de inicio', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
    vi.restoreAllMocks();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  function sinSesion() {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
  }

  async function conSesionIniciada() {
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />);
    expect(await screen.findByRole('button', { name: 'Hola, Erving' })).toBeVisible();
  }

  function permaneceAutenticadoYPuedeReintentar() {
    expect(screen.getByRole('button', { name: 'Hola, Erving' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeEnabled();
    expect(screen.queryByRole('heading', { name: 'Iniciar sesión' })).not.toBeInTheDocument();
  }

  async function abrirMenuDeSesion(persona: ReturnType<typeof userEvent.setup>) {
    if (!screen.queryByRole('button', { name: 'Cerrar sesión' })) {
      await persona.click(screen.getByRole('button', { name: /^Hola,/ }));
    }
  }

  async function reintentaYCierra(persona: ReturnType<typeof userEvent.setup>) {
    api.responder('DELETE /api/auth/sesion', { estado: 204 });
    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));
    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Hola, Erving' })).not.toBeInTheDocument();
  }

  it('ofrece iniciar sesión y registrarse cuando no hay sesión', async () => {
    sinSesion();
    renderizarConProveedores(<App />);

    expect(await screen.findByRole('button', { name: 'Iniciar sesión' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Regístrate' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Explorar' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Cerrar sesión' })).not.toBeInTheDocument();
  });

  it('lleva a iniciar sesión desde el encabezado', async () => {
    const persona = userEvent.setup();
    sinSesion();
    renderizarConProveedores(<App />);

    await persona.click(await screen.findByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });

  it('muestra el hero, la búsqueda y las categorías populares', async () => {
    sinSesion();
    renderizarConProveedores(<App />);

    expect(
      await screen.findByRole('heading', {
        name: 'Encuentra servicios confiables en tu comunidad',
      })
    ).toBeVisible();
    expect(screen.getByRole('search')).toBeVisible();
    expect(screen.getByPlaceholderText('¿Qué servicio necesitas?')).toBeVisible();
    expect(screen.getByDisplayValue('Managua, NIC')).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Categorías populares' })).toBeVisible();
    const categorias = within(screen.getByRole('region', { name: 'Categorías populares' }));
    expect(categorias.getByText('Hogar')).toBeVisible();
    expect(categorias.getByText('Construcción')).toBeVisible();
    expect(categorias.getByText('Transporte')).toBeVisible();
    expect(categorias.getByText('Tecnología')).toBeVisible();
    expect(categorias.getByText('Eventos')).toBeVisible();
    expect(categorias.getByText('Más')).toBeVisible();
    expect(screen.getByRole('contentinfo')).toBeVisible();
    expect(screen.getByText('© 2026 Moica. Todos los derechos reservados.')).toBeVisible();
  });

  it('saluda a quien tiene la sesión iniciada y le ofrece cerrarla', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />);

    expect(await screen.findByRole('button', { name: 'Hola, Erving' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Cerrar sesión' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Regístrate' })).not.toBeInTheDocument();
    expect(screen.getByText(/Bienvenido de nuevo, Erving/)).toBeVisible();
    expect(screen.getByRole('link', { name: 'Ir a tu Panel principal →' })).toHaveAttribute(
      'href',
      '/panel'
    );

    await persona.click(screen.getByRole('button', { name: 'Hola, Erving' }));
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
  });

  it('cierra la sesión y devuelve a la pantalla de inicio de sesión', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('DELETE /api/auth/sesion', { estado: 204 });
    renderizarConProveedores(<App />);

    await persona.click(await screen.findByRole('button', { name: 'Hola, Erving' }));
    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
    expect(api.ultima('DELETE /api/auth/sesion')?.cabeceras['X-XSRF-TOKEN']).toBe(
      'token-de-prueba'
    );
  });

  it('trata el 401 al cerrar como sesión vencida y lleva a iniciar sesión', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    api.responder('DELETE /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });

    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Tu sesión venció');
    expect(screen.queryByRole('button', { name: 'Hola, Erving' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cerrar sesión' })).not.toBeInTheDocument();
  });

  it('conserva la sesión si el navegador está sin conexión y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);

    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(screen.queryByRole('button', { name: 'Cerrando sesión…' })).not.toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos comunicarnos con Moica. Revisa tu conexión e inténtalo otra vez.'
    );
    expect(api.ultima('DELETE /api/auth/sesion')).toBeUndefined();
    permaneceAutenticadoYPuedeReintentar();

    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
    await reintentaYCierra(persona);
  });

  it('abandona el cierre si fetch queda colgado como en Chrome offline', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    definirTiempoDeEsperaMs(20);
    vi.stubGlobal(
      'fetch',
      vi.fn(() => new Promise<Response>(() => undefined))
    );

    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Tardamos demasiado en obtener respuesta. Revisa tu conexión e inténtalo otra vez.'
    );
    expect(screen.queryByRole('button', { name: 'Cerrando sesión…' })).not.toBeInTheDocument();
    permaneceAutenticadoYPuedeReintentar();
  });

  it('conserva la sesión si no hay conexión y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    api.rechazar('DELETE /api/auth/sesion');

    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No pudimos comunicarnos con Moica. Revisa tu conexión e inténtalo otra vez.'
    );
    permaneceAutenticadoYPuedeReintentar();
    await reintentaYCierra(persona);
  });

  it('conserva la sesión ante un 403 al cerrar y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    api.responder('DELETE /api/auth/sesion', {
      estado: 403,
      cuerpo: cuerpoDeError(
        403,
        'ACCESO_DENEGADO',
        'No se pudo validar la petición. Recarga la página e inténtalo otra vez.'
      ),
    });

    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se pudo validar la petición. Recarga la página e inténtalo otra vez.'
    );
    permaneceAutenticadoYPuedeReintentar();
    await reintentaYCierra(persona);
  });

  it('conserva la sesión ante un 500 al cerrar y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    api.responder('DELETE /api/auth/sesion', {
      estado: 500,
      cuerpo: cuerpoDeError(
        500,
        'ERROR_INTERNO',
        'Algo falló en Moica. Inténtalo de nuevo en unos minutos.'
      ),
    });

    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Algo falló en Moica. Inténtalo de nuevo en unos minutos.'
    );
    permaneceAutenticadoYPuedeReintentar();
    await reintentaYCierra(persona);
  });

  it('abandona el cierre si la petición no responde y permite reintentar', async () => {
    const persona = userEvent.setup();
    await conSesionIniciada();
    definirTiempoDeEsperaMs(20);
    api.colgar('DELETE /api/auth/sesion');

    await abrirMenuDeSesion(persona);
    await persona.click(screen.getByRole('button', { name: 'Cerrar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Tardamos demasiado en obtener respuesta. Revisa tu conexión e inténtalo otra vez.'
    );
    permaneceAutenticadoYPuedeReintentar();
    await reintentaYCierra(persona);
  });

  it('avisa de que la sesión venció cuando llega su fecha de expiración', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });

    // Una sesión a la que ya solo le quedan unos segundos.
    const sesion = sesionDeEjemplo();
    sesion.sesion.fechaExpiracion = new Date(Date.now() + 5000).toISOString();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesion });

    renderizarConProveedores(<App />);
    await screen.findByRole('button', { name: 'Hola, Erving' });

    await vi.advanceTimersByTimeAsync(5000);

    expect(await screen.findByRole('status')).toHaveTextContent('Tu sesión venció');
  });

  it('lleva Explorar al descubrimiento público', async () => {
    const persona = userEvent.setup();
    sinSesion();
    api.responder('GET /api/catalogos/categorias', { estado: 200, cuerpo: [] });
    api.responder('GET /api/catalogos/departamentos', { estado: 200, cuerpo: [] });
    api.responder('GET /api/servicios', { estado: 200, cuerpo: [] });
    renderizarConProveedores(<App />);

    await persona.click(await screen.findByRole('link', { name: 'Explorar' }));

    expect(await screen.findByRole('heading', { name: 'Explorar servicios' })).toBeVisible();
  });

  it('lleva el texto del hero a explorar y conserva el filtro', async () => {
    const persona = userEvent.setup();
    sinSesion();
    api.responder('GET /api/catalogos/categorias', {
      estado: 200,
      cuerpo: catalogoDeCategoriasDeEjemplo(),
    });
    api.responder('GET /api/catalogos/departamentos', {
      estado: 200,
      cuerpo: catalogoDeEjemplo(),
    });
    api.responder('GET /api/servicios?texto=fuga', { estado: 200, cuerpo: [] });
    renderizarConProveedores(<App />);

    await persona.type(await screen.findByLabelText('Qué servicio necesitas'), 'fuga');
    await persona.click(screen.getByRole('button', { name: 'Buscar' }));

    expect(await screen.findByRole('heading', { name: 'Explorar servicios' })).toBeVisible();
    expect(screen.getByLabelText('Buscar servicios')).toHaveValue('fuga');
    expect(api.ultima('GET /api/servicios?texto=fuga')).toBeDefined();
  });

  it('abre explorar sin consulta cuando el hero se envía vacío', async () => {
    const persona = userEvent.setup();
    sinSesion();
    api.responder('GET /api/catalogos/categorias', { estado: 200, cuerpo: [] });
    api.responder('GET /api/catalogos/departamentos', { estado: 200, cuerpo: [] });
    api.responder('GET /api/servicios', { estado: 200, cuerpo: [] });
    renderizarConProveedores(<App />);

    await persona.click(await screen.findByRole('button', { name: 'Buscar' }));

    expect(await screen.findByRole('heading', { name: 'Explorar servicios' })).toBeVisible();
    expect(screen.getByLabelText('Buscar servicios')).toHaveValue('');
    expect(api.ultima('GET /api/servicios')).toBeDefined();
  });

  it('ofrece Mis servicios en el menú de una sesión plena', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />);

    await persona.click(await screen.findByRole('button', { name: 'Hola, Erving' }));

    expect(screen.getByRole('link', { name: 'Panel principal' })).toHaveAttribute('href', '/panel');
    expect(screen.getByRole('link', { name: 'Mi perfil de prestador' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Mis servicios' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Mis solicitudes' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Seguridad de la cuenta' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Área administrativa' })).not.toBeInTheDocument();
  });

  it('limita el menú pendiente de segundo factor a verificarlo o salir', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ segundoFactorRequerido: true }),
    });
    renderizarConProveedores(<App />);

    await persona.click(await screen.findByRole('button', { name: 'Hola, Erving' }));

    expect(screen.getByRole('link', { name: 'Verificar segundo factor' })).toBeVisible();
    expect(screen.queryByRole('link', { name: 'Panel principal' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Ir a tu Panel principal →' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Mis servicios' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Mis solicitudes' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Mi perfil de prestador' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeVisible();
  });
});
