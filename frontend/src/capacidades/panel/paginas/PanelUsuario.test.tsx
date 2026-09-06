import { screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  instalarApiFalsa,
  perfilDeEjemplo,
  prestadorPublicoDeEjemplo,
  reputacionDeEjemplo,
  reputacionVaciaDeEjemplo,
  resumenDeSolicitudDeEjemplo,
  sesionDeEjemplo,
  servicioPropioDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

function perfilPublicoDeEjemplo(
  reputacionPrestador: ReturnType<typeof reputacionDeEjemplo> = reputacionDeEjemplo()
) {
  return {
    prestador: prestadorPublicoDeEjemplo(),
    portafolio: [],
    servicios: [],
    admiteContratacion: true,
    reputacionPrestador,
  };
}

describe('Panel de usuario', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/solicitudes/enviadas', { estado: 200, cuerpo: [] });
    api.responder('GET /api/solicitudes/recibidas', { estado: 200, cuerpo: [] });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function abrirComo(idUsuario = 1) {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ idUsuario }),
    });
    return renderizarConProveedores(<App />, '/panel');
  }

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

  it('sin sesión redirige a iniciar sesión', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });

    renderizarConProveedores(<App />, '/panel');

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });

  it('sin perfil de prestador redirige a explorar servicios', async () => {
    sinPerfil();
    abrirComo();

    expect(await screen.findByRole('heading', { name: 'Explorar servicios' })).toBeVisible();

    await waitFor(() => {
      expect(api.ultima('GET /api/prestador/perfil')).toBeDefined();
    });
    expect(api.ultima('GET /api/prestador/servicios')).toBeUndefined();
    expect(api.ultima('GET /api/prestadores/1')).toBeUndefined();
  });

  it('deja el ítem de inicio activo en la barra lateral para un prestador', async () => {
    api.responder('GET /api/prestador/perfil', {
      estado: 200,
      cuerpo: perfilDeEjemplo(),
    });
    api.responder('GET /api/prestador/servicios', { estado: 200, cuerpo: [] });
    api.responder('GET /api/prestadores/1', {
      estado: 200,
      cuerpo: perfilPublicoDeEjemplo(reputacionVaciaDeEjemplo()),
    });

    abrirComo();

    expect(await screen.findByRole('heading', { name: '¡Hola, Erving! 👋' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Inicio' })).toHaveAttribute('aria-current', 'page');
  });

  it('cuenta servicios activos, hilos y contrataciones con datos reales', async () => {
    api.responder('GET /api/prestador/perfil', {
      estado: 200,
      cuerpo: perfilDeEjemplo({ nivelVerificacion: 'VERIFICADO_BASICO' }),
    });
    api.responder('GET /api/prestador/servicios', {
      estado: 200,
      cuerpo: [
        servicioPropioDeEjemplo({ estado: 'ACTIVO' }),
        servicioPropioDeEjemplo({ idServicioPublicado: 11, estado: 'INACTIVO' }),
        servicioPropioDeEjemplo({ idServicioPublicado: 12, estado: 'ACTIVO' }),
      ],
    });
    api.responder('GET /api/prestadores/1', {
      estado: 200,
      cuerpo: perfilPublicoDeEjemplo(reputacionDeEjemplo()),
    });
    api.responder('GET /api/solicitudes/enviadas', {
      estado: 200,
      cuerpo: [
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 21,
          estadoActual: 'ACEPTADA',
          idCliente: 1,
        }),
      ],
    });
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 22,
          estadoActual: 'PENDIENTE',
          nombreServicio: 'Pintura',
        }),
      ],
    });

    abrirComo();

    expect(await screen.findByRole('link', { name: 'Servicios publicados 2' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Mensajes 1' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'Contrataciones 2' })).toBeVisible();
    expect(screen.getByLabelText('Calificación 4.3 de 5, 3 calificaciones')).toBeVisible();
    expect(
      screen.getByRole('link', { name: 'Tienes 1 solicitud pendiente de respuesta' })
    ).toHaveAttribute('href', '/solicitudes');
    expect(screen.getByRole('link', { name: 'Publicar servicio' })).toHaveAttribute(
      'href',
      '/prestador/servicios/nuevo'
    );
  });

  it('ordena la actividad reciente y no pasa de cuatro filas', async () => {
    api.responder('GET /api/prestador/perfil', {
      estado: 200,
      cuerpo: perfilDeEjemplo(),
    });
    api.responder('GET /api/prestador/servicios', { estado: 200, cuerpo: [] });
    api.responder('GET /api/prestadores/1', {
      estado: 200,
      cuerpo: perfilPublicoDeEjemplo(reputacionVaciaDeEjemplo()),
    });
    api.responder('GET /api/solicitudes/enviadas', {
      estado: 200,
      cuerpo: [
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 1,
          nombreServicio: 'Primera',
          fechaCreacion: '2026-08-01T10:00:00-06:00',
        }),
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 2,
          nombreServicio: 'Segunda',
          fechaCreacion: '2026-08-03T10:00:00-06:00',
        }),
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 3,
          nombreServicio: 'Tercera',
          fechaCreacion: '2026-08-05T10:00:00-06:00',
        }),
      ],
    });
    api.responder('GET /api/solicitudes/recibidas', {
      estado: 200,
      cuerpo: [
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 4,
          nombreServicio: 'Cuarta',
          fechaCreacion: '2026-08-04T10:00:00-06:00',
        }),
        resumenDeSolicitudDeEjemplo({
          idSolicitudServicio: 5,
          nombreServicio: 'Quinta',
          fechaCreacion: '2026-08-02T10:00:00-06:00',
        }),
      ],
    });

    abrirComo();

    expect(await screen.findByRole('heading', { name: '¡Hola, Erving! 👋' })).toBeVisible();
    const filas = await screen.findAllByRole('link', { name: /Solicitud para/ });
    expect(filas).toHaveLength(4);
    expect(filas[0]).toHaveTextContent('Tercera');
    expect(filas[1]).toHaveTextContent('Cuarta');
    expect(filas[2]).toHaveTextContent('Segunda');
    expect(filas[3]).toHaveTextContent('Quinta');
    expect(screen.queryByText(/Solicitud para Primera/)).not.toBeInTheDocument();
    expect(filas[0]).toHaveAttribute('href', '/solicitudes/3');
  });

  it('pide verificar el perfil cuando el prestador sigue sin verificar', async () => {
    api.responder('GET /api/prestador/perfil', {
      estado: 200,
      cuerpo: perfilDeEjemplo({ nivelVerificacion: 'SIN_VERIFICAR' }),
    });
    api.responder('GET /api/prestador/servicios', { estado: 200, cuerpo: [] });
    api.responder('GET /api/prestadores/1', {
      estado: 200,
      cuerpo: perfilPublicoDeEjemplo(reputacionVaciaDeEjemplo()),
    });

    abrirComo();

    expect(
      await screen.findByRole('link', {
        name: 'Envía tu documentación para verificar tu perfil',
      })
    ).toHaveAttribute('href', '/prestador');
    expect(screen.getByText('Calificación').closest('div')).toHaveTextContent('—');
  });

  it('pide publicar el primer servicio cuando el prestador ya está verificado', async () => {
    api.responder('GET /api/prestador/perfil', {
      estado: 200,
      cuerpo: perfilDeEjemplo({ nivelVerificacion: 'VERIFICADO_BASICO' }),
    });
    api.responder('GET /api/prestador/servicios', { estado: 200, cuerpo: [] });
    api.responder('GET /api/prestadores/1', {
      estado: 200,
      cuerpo: perfilPublicoDeEjemplo(reputacionVaciaDeEjemplo()),
    });

    abrirComo();

    expect(
      await screen.findByRole('link', {
        name: 'Publica tu primer servicio para recibir clientes',
      })
    ).toHaveAttribute('href', '/prestador/servicios/nuevo');
  });
});
