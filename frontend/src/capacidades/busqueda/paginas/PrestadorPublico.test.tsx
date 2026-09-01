import { screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
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

  it('muestra la reputación real del prestador junto a su presentación', async () => {
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    expect(await screen.findByRole('heading', { name: 'Taller La Esperanza' })).toBeVisible();
    expect(
      screen.getAllByLabelText('Calificación 4.3 de 5, 3 calificaciones').length
    ).toBeGreaterThan(0);
    expect(screen.getByText('3 calificaciones de solicitudes completadas')).toBeVisible();
  });

  it('sin calificaciones lo dice y no muestra un promedio de cero', async () => {
    api.responder(RUTA_PERFIL, {
      estado: 200,
      cuerpo: perfilPublicoDeEjemplo(reputacionVaciaDeEjemplo()),
    });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    expect(await screen.findByRole('heading', { name: 'Taller La Esperanza' })).toBeVisible();
    expect(screen.getAllByLabelText('Sin calificaciones todavía').length).toBeGreaterThan(0);
    expect(screen.queryByText('0.0')).not.toBeInTheDocument();
    expect(screen.queryByText(/calificaciones de solicitudes completadas/)).not.toBeInTheDocument();
  });

  it('la reputación del perfil es la misma que la de sus tarjetas', async () => {
    api.responder(RUTA_PERFIL, { estado: 200, cuerpo: perfilPublicoDeEjemplo() });

    renderizarConProveedores(<App />, '/explorar/prestadores/1');

    await screen.findByRole('heading', { name: 'Taller La Esperanza' });
    // La reputación es de la persona, no del servicio: el encabezado del perfil
    // y la tarjeta del servicio muestran exactamente la misma cifra.
    expect(screen.getAllByLabelText('Calificación 4.3 de 5, 3 calificaciones')).toHaveLength(2);
    expect(screen.queryByText('4.8')).not.toBeInTheDocument();
  });
});
