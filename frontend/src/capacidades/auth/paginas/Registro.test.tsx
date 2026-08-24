import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import { cuerpoDeError, instalarApiFalsa, type ApiFalsa } from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

describe('pantalla de registro', () => {
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

  function abrirRegistro() {
    renderizarConProveedores(<App />, '/registro');
  }

  async function rellenar(
    persona: ReturnType<typeof userEvent.setup>,
    clave: string,
    confirmacion = clave
  ) {
    await persona.type(screen.getByLabelText('Nombre completo'), 'Erving Miranda');
    await persona.type(screen.getByLabelText('Correo electrónico'), 'erving@moica.test');
    await persona.type(screen.getByLabelText('Contraseña'), clave);
    await persona.type(screen.getByLabelText('Repetir contraseña'), confirmacion);
  }

  it('rechaza una contraseña que no cumple la política sin llegar a llamar a la API', async () => {
    const persona = userEvent.setup();
    abrirRegistro();

    await rellenar(persona, 'moica2026');
    await persona.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(await screen.findByText('Debe incluir al menos una letra mayúscula.')).toBeVisible();
    expect(api.ultima('POST /api/usuarios')).toBeUndefined();
  });

  it('exige que las dos contraseñas coincidan', async () => {
    const persona = userEvent.setup();
    abrirRegistro();

    await rellenar(persona, 'Moica2026$segura', 'Moica2026$distinta');
    await persona.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(await screen.findByText('Las dos contraseñas deben coincidir.')).toBeVisible();
    expect(api.ultima('POST /api/usuarios')).toBeUndefined();
  });

  it('crea la cuenta y lleva a iniciar sesión sin autenticar', async () => {
    const persona = userEvent.setup();
    api.responder('POST /api/usuarios', {
      estado: 201,
      cuerpo: {
        idUsuario: 1,
        nombreCompleto: 'Erving Miranda',
        correoElectronico: 'erving@moica.test',
        estadoCuenta: 'ACTIVA',
        fechaRegistro: '2026-08-21T10:00:00-06:00',
      },
    });
    abrirRegistro();

    await rellenar(persona, 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(
      await screen.findByText('Tu cuenta quedó creada. Inicia sesión para entrar.')
    ).toBeVisible();
    expect(api.ultima('POST /api/usuarios')?.cuerpo).toEqual({
      nombreCompleto: 'Erving Miranda',
      correoElectronico: 'erving@moica.test',
      clave: 'Moica2026$segura',
    });
  });

  it('muestra el mensaje del backend cuando el correo ya tiene cuenta', async () => {
    const persona = userEvent.setup();
    api.responder('POST /api/usuarios', {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'CORREO_YA_REGISTRADO',
        'Ese correo ya tiene una cuenta en Moica. Inicia sesión o usa otro correo.'
      ),
    });
    abrirRegistro();

    await rellenar(persona, 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Ese correo ya tiene una cuenta');
  });

  it('coloca en su campo el error que devuelve el backend', async () => {
    const persona = userEvent.setup();
    api.responder('POST /api/usuarios', {
      estado: 400,
      cuerpo: cuerpoDeError(400, 'VALIDACION', 'Revisa los datos enviados.', [
        { campo: 'correoElectronico', mensaje: 'Escribe un correo electrónico válido.' },
      ]),
    });
    abrirRegistro();

    await rellenar(persona, 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Crear cuenta' }));

    await waitFor(() =>
      expect(screen.getByLabelText('Correo electrónico')).toHaveAttribute('aria-invalid', 'true')
    );
    expect(screen.getByText('Escribe un correo electrónico válido.')).toBeVisible();
  });
});
