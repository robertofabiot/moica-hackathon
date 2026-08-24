import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import {
  cuerpoDeError,
  instalarApiFalsa,
  sesionDeEjemplo,
  type ApiFalsa,
} from '../../pruebas/apiFalsa';
import {
  cerrarSesion,
  definirTiempoDeEsperaMs,
  ErrorDeApi,
  iniciarSesion,
  obtenerSesionActual,
  registrarUsuario,
  TIEMPO_DE_ESPERA_MS,
} from './api';

describe('llamadas a la API de acceso', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=;expires=Thu, 01 Jan 1970 00:00:00 GMT';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
  });

  it('devuelve la sesión cuando hay una vigente', async () => {
    const sesion = sesionDeEjemplo();
    api.responder('GET /api/auth/sesion', { estado: 200, cuerpo: sesion });

    await expect(obtenerSesionActual()).resolves.toEqual(sesion);
  });

  it('entiende el 401 como que no hay sesión, no como un fallo', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', 'Tu sesión no está activa.'),
    });

    await expect(obtenerSesionActual()).resolves.toBeNull();
  });

  it('devuelve el token CSRF en la cabecera de una operación mutable', async () => {
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('POST /api/usuarios', { estado: 201, cuerpo: { idUsuario: 1 } });

    await registrarUsuario({
      nombreCompleto: 'Erving Miranda',
      correoElectronico: 'erving@moica.test',
      clave: 'Moica2026$segura',
    });

    expect(api.ultima('POST /api/usuarios')?.cabeceras['X-XSRF-TOKEN']).toBe('token-de-prueba');
  });

  it('pide el token CSRF al backend cuando el navegador todavía no lo tiene', async () => {
    api.responder('GET /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(401, 'NO_AUTENTICADO', ''),
    });
    api.responder('POST /api/usuarios', { estado: 201, cuerpo: { idUsuario: 1 } });

    await registrarUsuario({
      nombreCompleto: 'Erving Miranda',
      correoElectronico: 'erving@moica.test',
      clave: 'Moica2026$segura',
    });

    expect(api.peticiones.map((peticion) => `${peticion.metodo} ${peticion.ruta}`)).toEqual([
      'GET /api/auth/sesion',
      'POST /api/usuarios',
    ]);
  });

  it('convierte un correo repetido en un error con su código y su mensaje', async () => {
    api.responder('POST /api/usuarios', {
      estado: 409,
      cuerpo: cuerpoDeError(
        409,
        'CORREO_YA_REGISTRADO',
        'Ese correo ya tiene una cuenta en Moica.'
      ),
    });

    await expect(
      registrarUsuario({
        nombreCompleto: 'Erving Miranda',
        correoElectronico: 'erving@moica.test',
        clave: 'Moica2026$segura',
      })
    ).rejects.toMatchObject({
      estado: 409,
      codigo: 'CORREO_YA_REGISTRADO',
      message: 'Ese correo ya tiene una cuenta en Moica.',
    });
  });

  it('conserva el detalle por campo de un error de validación', async () => {
    api.responder('POST /api/usuarios', {
      estado: 400,
      cuerpo: cuerpoDeError(400, 'VALIDACION', 'Revisa los datos enviados.', [
        { campo: 'clave', mensaje: 'La contraseña debe incluir al menos un símbolo.' },
      ]),
    });

    const fallo = await registrarUsuario({
      nombreCompleto: 'Erving Miranda',
      correoElectronico: 'erving@moica.test',
      clave: 'moica2026',
    }).catch((error: unknown) => error);

    expect(fallo).toBeInstanceOf(ErrorDeApi);
    expect((fallo as ErrorDeApi).errores).toEqual([
      { campo: 'clave', mensaje: 'La contraseña debe incluir al menos un símbolo.' },
    ]);
  });

  it('da un mensaje genérico ante unas credenciales incorrectas', async () => {
    api.responder('POST /api/auth/sesion', {
      estado: 401,
      cuerpo: cuerpoDeError(
        401,
        'CREDENCIALES_INVALIDAS',
        'El correo o la contraseña no son correctos.'
      ),
    });

    await expect(
      iniciarSesion({ correoElectronico: 'erving@moica.test', clave: 'incorrecta' })
    ).rejects.toMatchObject({ codigo: 'CREDENCIALES_INVALIDAS' });
  });

  it('cierra la sesión sin devolver cuerpo', async () => {
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder('DELETE /api/auth/sesion', { estado: 204 });

    await expect(cerrarSesion()).resolves.toBeUndefined();
    expect(api.ultima('DELETE /api/auth/sesion')).toBeDefined();
  });

  it('avisa cuando el navegador declara que no hay conexión', async () => {
    vi.stubGlobal('navigator', { ...navigator, onLine: false });

    await expect(obtenerSesionActual()).rejects.toMatchObject({ codigo: 'SIN_RESPUESTA' });
    expect(api.peticiones).toHaveLength(0);
  });

  it('avisa cuando no se pudo hablar con el servidor', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new TypeError('Failed to fetch')))
    );

    await expect(obtenerSesionActual()).rejects.toMatchObject({ codigo: 'SIN_RESPUESTA' });
  });

  it('abandona la petición si el servidor no responde a tiempo', async () => {
    vi.useFakeTimers();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    vi.stubGlobal(
      'fetch',
      vi.fn(
        (_entrada: unknown, opciones?: RequestInit) =>
          new Promise<Response>((_resolve, reject) => {
            opciones?.signal?.addEventListener(
              'abort',
              () => {
                reject(new DOMException('The operation was aborted.', 'AbortError'));
              },
              { once: true }
            );
          })
      )
    );

    const cierre = cerrarSesion();
    const expectativa = expect(cierre).rejects.toMatchObject({ codigo: 'TIEMPO_AGOTADO' });
    await vi.advanceTimersByTimeAsync(TIEMPO_DE_ESPERA_MS);
    await expectativa;
  });

  it('abandona aunque fetch ignore por completo el aborto', async () => {
    vi.useFakeTimers();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    vi.stubGlobal(
      'fetch',
      vi.fn(() => new Promise<Response>(() => undefined))
    );

    const cierre = cerrarSesion();
    const expectativa = expect(cierre).rejects.toMatchObject({ codigo: 'TIEMPO_AGOTADO' });
    await vi.advanceTimersByTimeAsync(TIEMPO_DE_ESPERA_MS);
    await expectativa;
  });
});
