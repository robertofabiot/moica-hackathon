import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  detallePublicoDeServicioDeEjemplo,
  imagenDeServicioDeEjemplo,
  instalarApiFalsa,
  reputacionVaciaDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

describe('Detalle de servicio', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('muestra el estado de carga mientras llega el detalle', () => {
    api.colgar('GET /api/servicios/10');
    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(screen.getByRole('status')).toHaveTextContent('Cargando el servicio…');
  });

  it('muestra el error y permite reintentar', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/servicios/10', {
      estado: 500,
      cuerpo: cuerpoDeError(500, 'ERROR_INTERNO', 'Algo falló en Moica.'),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo falló en Moica.');

    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
  });

  it('pinta migas de pan hacia el inicio y la categoría', async () => {
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByRole('navigation', { name: 'Migas de pan' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: 'Hogar y mantenimiento' })).toHaveAttribute(
      'href',
      '/explorar?idCategoria=1'
    );
  });

  it('muestra la descripción, el pie y la ficha de contratación', async () => {
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
    expect(screen.getByText('Reparo tuberías y fugas en el hogar.')).toBeVisible();
    expect(screen.getByText('A convenir')).toBeVisible();
    expect(screen.getByText('Instalaciones y reparaciones garantizadas')).toBeVisible();
    expect(screen.getByRole('link', { name: 'Iniciar sesión para solicitar' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Guardar' })).toBeVisible();
    expect(screen.getByRole('contentinfo')).toBeVisible();
  });

  it('presenta un precio de referencia como Desde y el monto compacto', async () => {
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo({ precioReferencia: 200 }),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByText('Desde')).toBeVisible();
    expect(screen.getByText('C$200')).toBeVisible();
  });

  it('muestra un placeholder cuando el servicio no tiene imágenes', async () => {
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(
      await screen.findByRole('img', { name: 'Reparación de fugas, sin imágenes' })
    ).toBeVisible();
  });

  it('cambia la imagen principal al elegir una miniatura', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo({
        imagenes: [
          imagenDeServicioDeEjemplo(1, 'Tubería reparada'),
          imagenDeServicioDeEjemplo(2, 'Lavamanos instalado'),
        ],
      }),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByRole('img', { name: 'Tubería reparada' })).toBeVisible();

    await persona.click(screen.getByRole('button', { name: 'Ver imagen 2 de 2' }));

    expect(screen.getByRole('img', { name: 'Lavamanos instalado' })).toBeVisible();
    expect(screen.getByRole('button', { name: 'Ver imagen 2 de 2' })).toHaveAttribute(
      'aria-pressed',
      'true'
    );
  });

  it('enlaza el prestador y conserva la advertencia de la insignia', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByRole('link', { name: /Taller La Esperanza/ })).toHaveAttribute(
      'href',
      '/explorar/prestadores/1'
    );
    await persona.hover(screen.getByRole('button', { name: /Insignia/i }));
    expect(screen.getByText(/No garantiza la calidad futura del trabajo/)).toBeVisible();
  });

  it('muestra el promedio y el desglose reales del prestador', async () => {
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByRole('heading', { name: 'Reseñas' })).toBeVisible();
    // La nota aparece dos veces a propósito: en la ficha de contratación y en el
    // resumen de reseñas. Es la misma cifra del mismo prestador.
    expect(screen.getAllByText('4.3')).toHaveLength(2);
    expect(screen.getByText('De 5')).toBeVisible();
    expect(screen.getByText('3 calificaciones')).toBeVisible();
    // Las cinco filas, incluidas las de dos y una estrella que la maqueta no
    // tenía: el cero es un dato real y el desglose no se inventa.
    expect(screen.getByRole('meter', { name: '5 estrellas' })).toHaveAttribute(
      'aria-valuenow',
      '1'
    );
    expect(screen.getByRole('meter', { name: '4 estrellas' })).toHaveAttribute(
      'aria-valuenow',
      '2'
    );
    expect(screen.getByRole('meter', { name: '3 estrellas' })).toHaveAttribute(
      'aria-valuenow',
      '0'
    );
    expect(screen.getByRole('meter', { name: '2 estrellas' })).toBeVisible();
    expect(screen.getByRole('meter', { name: '1 estrellas' })).toBeVisible();
    expect(screen.getByLabelText('Calificación 4.3 de 5, 3 calificaciones')).toBeVisible();
  });

  it('sin calificaciones no dibuja una nota ni barras vacías', async () => {
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo({
        reputacionPrestador: reputacionVaciaDeEjemplo(),
      }),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(await screen.findByRole('heading', { name: 'Reseñas' })).toBeVisible();
    expect(
      screen.getByText(/Sin calificaciones todavía\. Este prestador aún no completó/)
    ).toBeVisible();
    expect(screen.getByLabelText('Sin calificaciones todavía')).toBeVisible();
    expect(screen.queryByText('0.0')).not.toBeInTheDocument();
    expect(screen.queryByRole('meter')).not.toBeInTheDocument();
  });

  it('no queda ninguna cifra ficticia de la maqueta anterior', async () => {
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    await screen.findByRole('heading', { name: 'Reseñas' });
    expect(screen.queryByText('4.8')).not.toBeInTheDocument();
    expect(screen.queryByText(/120 reseñas/)).not.toBeInTheDocument();
    expect(screen.queryByText(/102/)).not.toBeInTheDocument();
  });

  it('no ofrece solicitar cuando el servicio no admite contratación', async () => {
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo({ admiteContratacion: false }),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    expect(
      await screen.findByText('Este prestador no está disponible para contratar ahora.')
    ).toBeVisible();
    expect(
      screen.queryByRole('link', { name: 'Iniciar sesión para solicitar' })
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Solicitar este servicio' })).not.toBeInTheDocument();
  });

  it('marca el servicio como guardado al pulsar el botón secundario', async () => {
    const persona = userEvent.setup();
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar/servicios/10');

    await persona.click(await screen.findByRole('button', { name: 'Guardar' }));

    expect(screen.getByRole('button', { name: 'Guardado' })).toHaveAttribute(
      'aria-pressed',
      'true'
    );
  });
});
