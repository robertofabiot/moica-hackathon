import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  detallePublicoDeServicioDeEjemplo,
  instalarApiFalsa,
  prestadorPublicoDeEjemplo,
  reputacionDeEjemplo,
  reputacionVaciaDeEjemplo,
  servicioPublicoDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

const RUTA_PERFIL = 'GET /api/prestadores/1';

/** El perfil público tal como lo devuelve `GET /api/prestadores/{id}`. */
function perfilPublicoDeEjemplo(
  reputacionPrestador: ReturnType<typeof reputacionDeEjemplo> = reputacionDeEjemplo()
) {
  return {
    prestador: prestadorPublicoDeEjemplo(),
    portafolio: [],
    servicios: [servicioPublicoDeEjemplo({ reputacionPrestador })],
    admiteContratacion: true,
    reputacionPrestador,
  };
}

describe('Perfil público del prestador', () => {
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

  it('muestra el estado de carga mientras llega el perfil', () => {
    api.colgar(RUTA_PERFIL);
    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    expect(screen.getByRole('status')).toHaveTextContent('Cargando el perfil…');
  });

  it('muestra el error y permite reintentar', async () => {
    const persona = userEvent.setup();
    api.responder(RUTA_PERFIL, {
      estado: 500,
      cuerpo: cuerpoDeError(500, 'ERROR_INTERNO', 'Algo falló en Moica.'),
    });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    expect(await screen.findByRole('alert')).toHaveTextContent('Algo falló en Moica.');

    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });
    await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

    expect(await screen.findByRole('heading', { name: 'Taller La Esperanza' })).toBeVisible();
  });

  it('muestra la reputación real del prestador junto a su presentación', async () => {
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    expect(await screen.findByRole('heading', { name: 'Taller La Esperanza' })).toBeVisible();
    expect(screen.getByText('Plomería')).toBeVisible();
    expect(screen.getByText('Managua, NIC')).toBeVisible();
    expect(screen.getByText('Verificado')).toBeVisible();
    expect(screen.getByLabelText('Calificación 4.3 de 5, 3 calificaciones')).toBeVisible();
  });

  it('sin calificaciones lo dice y no muestra un promedio de cero', async () => {
    api.responder(RUTA_PERFIL, {
      estado: 200,
      cuerpo: perfilPublicoDeEjemplo(reputacionVaciaDeEjemplo()),
    });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    expect(await screen.findByRole('heading', { name: 'Taller La Esperanza' })).toBeVisible();
    expect(screen.getByLabelText('Sin calificaciones todavía')).toBeVisible();
    expect(screen.queryByText('0.0')).not.toBeInTheDocument();
  });

  it('lista reseñas individuales con nombre, estrellas y comentario', async () => {
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    await screen.findByRole('heading', { name: 'Reseñas de clientes' });
    expect(screen.getByText('María Gómez')).toBeVisible();
    expect(screen.getByText(/100% recomendado/)).toBeVisible();
    expect(screen.getByText('Ana López')).toBeVisible();
    expect(screen.getByText('Luis Martínez')).toBeVisible();
    expect(screen.getByText('Carmen Ruiz')).toBeVisible();
    expect(screen.getAllByLabelText('5 de 5 estrellas').length).toBeGreaterThan(0);
    expect(screen.getByLabelText('4 de 5 estrellas')).toBeVisible();
    expect(screen.queryByText('Clientes de Moica')).not.toBeInTheDocument();
    expect(screen.queryByText(/calificaciones de solicitudes completadas/)).not.toBeInTheDocument();
  });

  it('no pinta las cifras ficticias de la maqueta en la cabecera', async () => {
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    await screen.findByRole('heading', { name: 'Taller La Esperanza' });
    expect(screen.getByLabelText('Calificación 4.3 de 5, 3 calificaciones')).toBeVisible();
    expect(screen.queryByText('4.8')).not.toBeInTheDocument();
    expect(screen.queryByText(/120 reseñas/)).not.toBeInTheDocument();
    expect(screen.queryByText(/5\+ años/)).not.toBeInTheDocument();
    expect(screen.queryByText('98%')).not.toBeInTheDocument();
  });

  it('enlaza cada servicio al detalle público con su precio', async () => {
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    const servicio = await screen.findByRole('link', { name: /Reparación de fugas/ });
    expect(servicio).toHaveAttribute('href', '/explorar/servicios/10');
    expect(screen.getByText('A convenir')).toBeVisible();
  });

  it('pinta el portafolio con miniaturas cuando hay trabajos', async () => {
    api.responder(RUTA_PERFIL, {
      estado: 200,
      cuerpo: {
        ...perfilPublicoDeEjemplo(),
        portafolio: [
          {
            idTrabajo: 1,
            titulo: 'Instalación en residencial',
            descripcion: 'Tablero nuevo en el Distrito I.',
            fechaRealizacion: '2026-03-01',
            ordenVisualizacion: 0,
            imagenes: [
              {
                idImagenTrabajoPortafolio: 1,
                urlImagen: 'https://imagenes.moica.test/trabajos/1.png',
                textoAlternativo: 'Tablero instalado',
                ordenVisualizacion: 0,
                fechaCreacion: '2026-03-01T10:00:00-06:00',
              },
            ],
            fechaCreacion: '2026-03-01T10:00:00-06:00',
            fechaActualizacion: '2026-03-01T10:00:00-06:00',
          },
        ],
      },
    });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    expect(await screen.findByRole('heading', { name: 'Portafolio' })).toBeVisible();
    expect(screen.getByText('Instalación en residencial')).toBeVisible();
    expect(screen.getByRole('img', { name: 'Tablero instalado' })).toBeVisible();
  });

  it('muestra el marco con encabezado, pie y navegación', async () => {
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    await screen.findByRole('heading', { name: 'Taller La Esperanza' });
    expect(
      screen
        .getAllByRole('link', { name: 'Moica, ir al inicio' })
        .every((enlace) => enlace.getAttribute('href') === '/')
    ).toBe(true);
    expect(screen.getByRole('button', { name: 'Notificaciones, hay avisos' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Iniciar sesión' })).toBeVisible();
    expect(screen.getByRole('contentinfo')).toBeVisible();
    expect(
      screen.getByRole('navigation', { name: 'Navegación principal', hidden: true })
    ).toBeInTheDocument();
  });

  it('marca al prestador como seguido al pulsar el botón de contorno', async () => {
    const persona = userEvent.setup();
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    await persona.click(await screen.findByRole('button', { name: 'Seguir' }));

    expect(screen.getByRole('button', { name: 'Siguiendo' })).toHaveAttribute(
      'aria-pressed',
      'true'
    );
  });

  it('contactar abre el detalle del primer servicio', async () => {
    const persona = userEvent.setup();
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });
    api.responder('GET /api/servicios/10', {
      estado: 200,
      cuerpo: detallePublicoDeServicioDeEjemplo(),
    });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    await persona.click(await screen.findByRole('button', { name: 'Contactar' }));

    expect(await screen.findByRole('heading', { name: 'Reparación de fugas' })).toBeVisible();
  });
});
