import type { QueryClient } from '@tanstack/react-query';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  activacionDeEjemplo,
  cuerpoDeError,
  instalarApiFalsa,
  segundoFactorDeEjemplo,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import { definirTiempoDeEsperaMs, TIEMPO_DE_ESPERA_MS } from '../api';

/** El secreto que devuelve {@link activacionDeEjemplo}, para poder buscarlo literalmente. */
const SECRETO_DE_ACTIVACION = 'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP';

describe('seguridad de la cuenta', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
    api.responder('GET /api/auth/segundo-factor', {
      estado: 200,
      cuerpo: segundoFactorDeEjemplo(null),
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  async function abrirSeguridad() {
    const montado = renderizarConProveedores(<App />, '/seguridad');
    expect(await screen.findByRole('heading', { name: 'Seguridad de tu cuenta' })).toBeVisible();
    return montado;
  }

  /** Todo lo que queda guardado de las mutaciones, para buscar en ello un secreto. */
  function memoriaDeLasMutaciones(cliente: QueryClient): string {
    return JSON.stringify(
      cliente
        .getMutationCache()
        .getAll()
        .map((mutacion) => mutacion.state)
    );
  }

  describe('cambio de contraseña', () => {
    it('valida en el navegador antes de llamar a la API', async () => {
      const persona = userEvent.setup();
      await abrirSeguridad();

      await persona.type(screen.getByLabelText('Contraseña actual'), 'Moica2026$segura');
      await persona.type(screen.getByLabelText('Contraseña nueva'), 'corta');
      await persona.type(screen.getByLabelText('Repetir contraseña nueva'), 'corta');
      await persona.click(screen.getByRole('button', { name: 'Cambiar contraseña' }));

      expect(
        await screen.findByText('La contraseña debe tener entre 8 y 72 caracteres.')
      ).toBeVisible();
      expect(api.ultima('PUT /api/auth/clave')).toBeUndefined();
    });

    it('exige que las dos contraseñas nuevas coincidan', async () => {
      const persona = userEvent.setup();
      await abrirSeguridad();

      await persona.type(screen.getByLabelText('Contraseña actual'), 'Moica2026$segura');
      await persona.type(screen.getByLabelText('Contraseña nueva'), 'Moica2026$nueva');
      await persona.type(screen.getByLabelText('Repetir contraseña nueva'), 'Moica2026$otra');
      await persona.click(screen.getByRole('button', { name: 'Cambiar contraseña' }));

      expect(await screen.findByText('Las dos contraseñas deben coincidir.')).toBeVisible();
      expect(api.ultima('PUT /api/auth/clave')).toBeUndefined();
    });

    it('cambia la contraseña y devuelve al inicio de sesión porque ya no hay sesión vigente', async () => {
      const persona = userEvent.setup();
      api.responder('PUT /api/auth/clave', { estado: 204 });
      await abrirSeguridad();

      await cambiarContrasena(persona);

      expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
      expect(screen.getByRole('status')).toHaveTextContent('Cambiaste tus credenciales');
      expect(api.ultima('PUT /api/auth/clave')?.cuerpo).toEqual({
        claveActual: 'Moica2026$segura',
        claveNueva: 'Moica2026$nueva',
      });
    });

    it('muestra el mensaje del backend cuando la contraseña actual no es correcta', async () => {
      const persona = userEvent.setup();
      api.responder('PUT /api/auth/clave', {
        estado: 403,
        cuerpo: cuerpoDeError(
          403,
          'CREDENCIALES_INVALIDAS',
          'La contraseña actual no es correcta.'
        ),
      });
      await abrirSeguridad();

      await cambiarContrasena(persona);

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'La contraseña actual no es correcta.'
      );
      expect(screen.getByRole('heading', { name: 'Seguridad de tu cuenta' })).toBeVisible();
    });

    it('coloca en el campo el detalle de validación que devuelve el backend', async () => {
      const persona = userEvent.setup();
      api.responder('PUT /api/auth/clave', {
        estado: 400,
        cuerpo: cuerpoDeError(400, 'VALIDACION', 'Revisa los datos enviados.', [
          { campo: 'claveNueva', mensaje: 'La contraseña es demasiado larga.' },
        ]),
      });
      await abrirSeguridad();

      await cambiarContrasena(persona);

      expect(await screen.findByText('La contraseña es demasiado larga.')).toBeVisible();
    });

    it('no deja la interfaz colgada cuando falla la red', async () => {
      const persona = userEvent.setup();
      api.rechazar('PUT /api/auth/clave');
      await abrirSeguridad();

      await cambiarContrasena(persona);

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'No pudimos comunicarnos con Moica.'
      );
      expect(screen.getByRole('button', { name: 'Cambiar contraseña' })).toBeEnabled();
    });
  });

  describe('segundo factor', () => {
    it('describe que la cuenta todavía no lo tiene configurado', async () => {
      await abrirSeguridad();

      expect(await screen.findByText('Sin configurar')).toBeVisible();
      expect(screen.getByRole('button', { name: 'Activar el segundo factor' })).toBeVisible();
    });

    it('entrega la clave manual y el código QR al empezar la activación', async () => {
      const persona = userEvent.setup();
      api.responder('POST /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: activacionDeEjemplo(),
      });
      await abrirSeguridad();

      await persona.click(await screen.findByRole('button', { name: 'Activar el segundo factor' }));

      expect(await screen.findByText('JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP')).toBeVisible();
      expect(
        screen.getByTitle('Código QR para configurar tu aplicación autenticadora')
      ).toBeInTheDocument();
      expect(screen.getByLabelText('Código de verificación')).toBeVisible();
    });

    it('olvida el secreto al salir de la pantalla y no lo reaparece al volver', async () => {
      const persona = userEvent.setup();
      api.responder('POST /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: activacionDeEjemplo(),
      });
      const { cliente } = await abrirSeguridad();

      await persona.click(await screen.findByRole('button', { name: 'Activar el segundo factor' }));
      expect(await screen.findByText(SECRETO_DE_ACTIVACION)).toBeVisible();

      await persona.click(screen.getByRole('link', { name: 'Volver al inicio' }));
      expect(
        await screen.findByRole('heading', {
          name: 'Encuentra servicios confiables en tu comunidad',
        })
      ).toBeVisible();

      expect(memoriaDeLasMutaciones(cliente)).not.toContain(SECRETO_DE_ACTIVACION);

      await persona.click(await screen.findByRole('button', { name: /^Hola,/ }));
      await persona.click(await screen.findByRole('link', { name: 'Seguridad de la cuenta' }));

      expect(
        await screen.findByRole('button', { name: 'Activar el segundo factor' })
      ).toBeVisible();
      expect(screen.queryByText(SECRETO_DE_ACTIVACION)).not.toBeInTheDocument();
    });

    it('deja de mostrar el secreto en cuanto el segundo factor queda activo', async () => {
      const persona = userEvent.setup();
      api.responder('POST /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: activacionDeEjemplo(),
      });
      api.responder('POST /api/auth/segundo-factor/activacion', {
        estado: 200,
        cuerpo: segundoFactorDeEjemplo('ACTIVO'),
      });
      const { cliente } = await abrirSeguridad();

      await persona.click(await screen.findByRole('button', { name: 'Activar el segundo factor' }));
      await persona.type(await screen.findByLabelText('Código de verificación'), '123456');
      await persona.click(screen.getByRole('button', { name: 'Confirmar activación' }));

      expect(await screen.findByText('Activo')).toBeVisible();
      expect(screen.queryByText(SECRETO_DE_ACTIVACION)).not.toBeInTheDocument();
      expect(memoriaDeLasMutaciones(cliente)).not.toContain(SECRETO_DE_ACTIVACION);
    });

    it('activa el segundo factor con el primer código válido', async () => {
      const persona = userEvent.setup();
      api.responder('POST /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: activacionDeEjemplo(),
      });
      api.responder('POST /api/auth/segundo-factor/activacion', {
        estado: 200,
        cuerpo: segundoFactorDeEjemplo('ACTIVO'),
      });
      await abrirSeguridad();

      await persona.click(await screen.findByRole('button', { name: 'Activar el segundo factor' }));
      await persona.type(await screen.findByLabelText('Código de verificación'), '123 456');
      await persona.click(screen.getByRole('button', { name: 'Confirmar activación' }));

      expect(await screen.findByText('Activo')).toBeVisible();
      expect(api.ultima('POST /api/auth/segundo-factor/activacion')?.cuerpo).toEqual({
        codigo: '123456',
      });
    });

    it('explica que el código no es válido sin activar nada', async () => {
      const persona = userEvent.setup();
      api.responder('POST /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: activacionDeEjemplo(),
      });
      api.responder('POST /api/auth/segundo-factor/activacion', {
        estado: 403,
        cuerpo: cuerpoDeError(403, 'CODIGO_INVALIDO', 'El código no es válido.'),
      });
      await abrirSeguridad();

      await persona.click(await screen.findByRole('button', { name: 'Activar el segundo factor' }));
      await persona.type(await screen.findByLabelText('Código de verificación'), '000000');
      await persona.click(screen.getByRole('button', { name: 'Confirmar activación' }));

      expect(await screen.findByRole('alert')).toHaveTextContent('El código no es válido.');
      expect(screen.getByRole('button', { name: 'Confirmar activación' })).toBeEnabled();
    });

    it('ofrece desactivarlo cuando ya está activo y devuelve al inicio de sesión', async () => {
      const persona = userEvent.setup();
      api.responder('GET /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: segundoFactorDeEjemplo('ACTIVO'),
      });
      api.responder('POST /api/auth/segundo-factor/desactivacion', { estado: 204 });
      await abrirSeguridad();

      await persona.type(
        await screen.findByLabelText('Contraseña actual', {
          selector: '#claveActualParaDesactivar',
        }),
        'Moica2026$segura'
      );
      await persona.type(screen.getByLabelText('Código de verificación'), '123456');
      await persona.click(screen.getByRole('button', { name: 'Desactivar el segundo factor' }));

      expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
      expect(api.ultima('POST /api/auth/segundo-factor/desactivacion')?.cuerpo).toEqual({
        claveActual: 'Moica2026$segura',
        codigo: '123456',
      });
    });

    it('no ofrece desactivarlo a una cuenta administrativa', async () => {
      api.responder('GET /api/auth/sesion', {
        estado: 200,
        cuerpo: sesionDeEjemplo({
          esAdministrador: true,
          segundoFactorRequerido: true,
          segundoFactorVerificado: true,
        }),
      });
      api.responder('GET /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: segundoFactorDeEjemplo('ACTIVO', true),
      });
      await abrirSeguridad();

      expect(await screen.findByText(/el segundo factor es obligatorio/i)).toBeVisible();
      expect(
        screen.queryByRole('button', { name: 'Desactivar el segundo factor' })
      ).not.toBeInTheDocument();
    });

    it('no muestra el estado de la cuenta anterior a la que entra después', async () => {
      const persona = userEvent.setup();

      // La cuenta A tiene el segundo factor activo y lo consulta.
      api.responder('GET /api/auth/sesion', {
        estado: 200,
        cuerpo: sesionDeEjemplo({ segundoFactorRequerido: true, segundoFactorVerificado: true }),
      });
      api.responder('GET /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: segundoFactorDeEjemplo('ACTIVO'),
      });
      api.responder('DELETE /api/auth/sesion', { estado: 204 });

      const { cliente } = await abrirSeguridad();
      expect(await screen.findByText('Activo')).toBeVisible();

      await persona.click(screen.getByRole('link', { name: 'Volver al inicio' }));
      await persona.click(await screen.findByRole('button', { name: /^Hola,/ }));
      await persona.click(await screen.findByRole('button', { name: 'Cerrar sesión' }));
      expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();

      // La cuenta B entra sin recargar y su consulta todavía no ha respondido.
      api.responder('POST /api/auth/sesion', { estado: 201, cuerpo: sesionDeEjemplo() });
      api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesionDeEjemplo() });
      api.colgar('GET /api/auth/segundo-factor');

      await persona.type(screen.getByLabelText('Correo electrónico'), 'otra@moica.test');
      await persona.type(screen.getByLabelText('Contraseña'), 'Moica2026$segura');
      await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

      await persona.click(await screen.findByRole('button', { name: /^Hola,/ }));
      await persona.click(await screen.findByRole('link', { name: 'Seguridad de la cuenta' }));

      expect(await screen.findByText('Consultando el estado de tu segundo factor…')).toBeVisible();
      expect(screen.queryByText('Activo')).not.toBeInTheDocument();
      expect(cliente.getQueryData(['auth', 'segundo-factor'])).toBeUndefined();
    });

    it('permite reintentar cuando no se pudo consultar su estado', async () => {
      const persona = userEvent.setup();
      api.rechazar('GET /api/auth/segundo-factor');
      await abrirSeguridad();

      expect(await screen.findByRole('alert')).toHaveTextContent(
        'No pudimos comunicarnos con Moica.'
      );

      api.responder('GET /api/auth/segundo-factor', {
        estado: 200,
        cuerpo: segundoFactorDeEjemplo(null),
      });
      await persona.click(screen.getByRole('button', { name: 'Reintentar' }));

      expect(await screen.findByText('Sin configurar')).toBeVisible();
    });
  });

  it('lleva a iniciar sesión a quien llega sin sesión', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });
    renderizarConProveedores(<App />, '/seguridad');

    expect(await screen.findByRole('heading', { name: 'Iniciar sesión' })).toBeVisible();
  });

  it('lleva a verificar el segundo factor a una sesión provisional', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 200,
      cuerpo: sesionDeEjemplo({ segundoFactorRequerido: true }),
    });
    renderizarConProveedores(<App />, '/seguridad');

    expect(
      await screen.findByRole('heading', { name: 'Verifica tu segundo factor' })
    ).toBeVisible();
  });

  async function cambiarContrasena(persona: ReturnType<typeof userEvent.setup>) {
    await persona.type(screen.getByLabelText('Contraseña actual'), 'Moica2026$segura');
    await persona.type(screen.getByLabelText('Contraseña nueva'), 'Moica2026$nueva');
    await persona.type(screen.getByLabelText('Repetir contraseña nueva'), 'Moica2026$nueva');
    await persona.click(screen.getByRole('button', { name: 'Cambiar contraseña' }));
  }
});
