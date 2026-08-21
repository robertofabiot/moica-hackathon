import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import App from '../../../App';
import {
  cuerpoDeError,
  instalarApiFalsa,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';

describe('pantalla de inicio de sesión', () => {
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

  it('explica que la sesión venció cuando se llega por ese motivo', () => {
    renderizarConProveedores(<App />, '/iniciar-sesion?motivo=sesion-vencida');

    expect(screen.getByRole('status')).toHaveTextContent('Tu sesión venció');
  });

  it('no muestra ningún aviso cuando se entra por decisión propia', () => {
    renderizarConProveedores(<App />, '/iniciar-sesion');

    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('valida el correo antes de llamar a la API', async () => {
    const persona = userEvent.setup();
    renderizarConProveedores(<App />, '/iniciar-sesion');

    await persona.type(screen.getByLabelText('Correo electrónico'), 'esto-no-es-un-correo');
    await persona.type(screen.getByLabelText('Contraseña'), 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByText('Escribe un correo electrónico válido.')).toBeVisible();
    expect(api.ultima('POST /api/auth/sesion')).toBeUndefined();
  });

  it('muestra un mensaje genérico cuando las credenciales no son correctas', async () => {
    const persona = userEvent.setup();
    api.responder('POST /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(
        401,
        'CREDENCIALES_INVALIDAS',
        'El correo o la contraseña no son correctos.'
      ),
    });
    renderizarConProveedores(<App />, '/iniciar-sesion');

    await persona.type(screen.getByLabelText('Correo electrónico'), 'erving@moica.test');
    await persona.type(screen.getByLabelText('Contraseña'), 'incorrecta');
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El correo o la contraseña no son correctos.'
    );
  });

  it('entra y lleva a la pantalla de inicio', async () => {
    const persona = userEvent.setup();
    api.responder('POST /api/auth/sesion', { estado: 201, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />, '/iniciar-sesion');

    await persona.type(screen.getByLabelText('Correo electrónico'), 'erving@moica.test');
    await persona.type(screen.getByLabelText('Contraseña'), 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    expect(await screen.findByRole('heading', { name: 'Moica' })).toBeVisible();
  });

  it('normaliza el correo antes de enviarlo', async () => {
    const persona = userEvent.setup();
    api.responder('POST /api/auth/sesion', { estado: 201, cuerpo: sesionDeEjemplo() });
    renderizarConProveedores(<App />, '/iniciar-sesion');

    await persona.type(screen.getByLabelText('Correo electrónico'), '  Erving@Moica.TEST  ');
    await persona.type(screen.getByLabelText('Contraseña'), 'Moica2026$segura');
    await persona.click(screen.getByRole('button', { name: 'Iniciar sesión' }));

    await screen.findByRole('heading', { name: 'Moica' });
    expect(api.ultima('POST /api/auth/sesion')?.cuerpo).toMatchObject({
      correoElectronico: 'erving@moica.test',
    });
  });
});
