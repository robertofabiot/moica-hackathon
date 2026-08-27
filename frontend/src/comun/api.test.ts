import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { instalarApiFalsa, type ApiFalsa } from '../pruebas/apiFalsa';
import {
  definirTiempoDeEsperaDeArchivoMs,
  definirTiempoDeEsperaMs,
  enviar,
  enviarArchivo,
  TIEMPO_DE_ESPERA_DE_ARCHIVO_MS,
  TIEMPO_DE_ESPERA_MS,
} from './api';

/**
 * Los tiempos de espera de la red compartida, que no son uno solo.
 *
 * Una petición JSON que tarda más de diez segundos ya no va a llegar. Una imagen de hasta 5 MB por
 * una red móvil, en cambio, puede tardar bastante más, y el backend se reserva hasta un minuto solo
 * para su llamada al almacenamiento: cortarla a los diez segundos mostraría un error por una imagen
 * que el servidor sí estaba guardando. Por eso la carga de archivos tiene su propia espera, más
 * larga que ese máximo, y estas pruebas fijan las dos con temporizadores simulados.
 */

const RUTA_PERFIL = '/api/prestador/perfil';
const RUTA_IMAGEN = '/api/prestador/perfil/imagen';

/**
 * El detector de rechazos sin dueño de Node, tipado al mínimo.
 *
 * `tsconfig.app.json` no incluye los tipos de Node porque este código corre en el navegador; aquí
 * solo se necesita para comprobar que nada escapa cuando `fetch` responde tardísimo.
 */
interface DetectorDeRechazos {
  on(evento: 'unhandledRejection', oyente: (razon: unknown) => void): void;
  off(evento: 'unhandledRejection', oyente: (razon: unknown) => void): void;
}

describe('tiempos de espera de la red compartida', () => {
  let api: ApiFalsa;

  beforeEach(() => {
    api = instalarApiFalsa();
    // Con la cookie ya puesta, ninguna operación mutable gasta una petición
    // previa en conseguir el token CSRF.
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
    definirTiempoDeEsperaMs(TIEMPO_DE_ESPERA_MS);
    definirTiempoDeEsperaDeArchivoMs(TIEMPO_DE_ESPERA_DE_ARCHIVO_MS);
  });

  it('una petición ordinaria conserva su espera de diez segundos', async () => {
    vi.useFakeTimers();
    api.colgar(`PUT ${RUTA_PERFIL}`);

    const peticion = enviar('PUT', RUTA_PERFIL, { nombrePublico: 'Taller La Esperanza' });
    const estado = seguir(peticion);

    await vi.advanceTimersByTimeAsync(TIEMPO_DE_ESPERA_MS - 1);
    expect(estado.asentada).toBe(false);

    const expectativa = expect(peticion).rejects.toMatchObject({ codigo: 'TIEMPO_AGOTADO' });
    await vi.advanceTimersByTimeAsync(1);
    await expectativa;
  });

  it('la carga de un archivo no expira a los diez segundos', async () => {
    vi.useFakeTimers();
    api.colgar(`PUT ${RUTA_IMAGEN}`);

    const carga = enviarArchivo('PUT', RUTA_IMAGEN, formularioConImagen());
    const estado = seguir(carga);

    await vi.advanceTimersByTimeAsync(TIEMPO_DE_ESPERA_MS);

    expect(
      estado.asentada,
      'a los diez segundos la imagen puede seguir subiendo perfectamente'
    ).toBe(false);

    // La prueba no puede terminar con una carga colgada: se la deja expirar.
    const expectativa = expect(carga).rejects.toMatchObject({ codigo: 'TIEMPO_AGOTADO' });
    await vi.advanceTimersByTimeAsync(TIEMPO_DE_ESPERA_DE_ARCHIVO_MS);
    await expectativa;
  });

  it('la carga de un archivo sí expira al alcanzar su propia espera', async () => {
    vi.useFakeTimers();
    api.colgar(`PUT ${RUTA_IMAGEN}`);

    const carga = enviarArchivo('PUT', RUTA_IMAGEN, formularioConImagen());
    const estado = seguir(carga);

    await vi.advanceTimersByTimeAsync(TIEMPO_DE_ESPERA_DE_ARCHIVO_MS - 1);
    expect(estado.asentada).toBe(false);

    const expectativa = expect(carga).rejects.toMatchObject({ codigo: 'TIEMPO_AGOTADO' });
    await vi.advanceTimersByTimeAsync(1);
    await expectativa;
  });

  it('sin conexión la carga falla al instante, sin armar ningún temporizador', async () => {
    vi.useFakeTimers();
    vi.stubGlobal('navigator', { ...navigator, onLine: false });

    await expect(enviarArchivo('PUT', RUTA_IMAGEN, formularioConImagen())).rejects.toMatchObject({
      codigo: 'SIN_RESPUESTA',
    });

    expect(api.peticiones, 'no se llega a llamar a la red').toHaveLength(0);
    expect(vi.getTimerCount(), 'nadie espera noventa segundos para saber que no hay red').toBe(0);
  });

  it('absorbe el rechazo tardío de fetch cuando la carga ya se abandonó', async () => {
    // Con temporizadores reales y una espera corta: lo que se comprueba aquí es
    // qué pasa después del abandono, y eso ocurre fuera de todo temporizador.
    definirTiempoDeEsperaDeArchivoMs(20);

    let fallar: (razon: unknown) => void = () => undefined;
    vi.stubGlobal(
      'fetch',
      vi.fn(
        () =>
          new Promise<Response>((_resolve, reject) => {
            fallar = reject;
          })
      )
    );

    const sinDuenio: unknown[] = [];
    const anotar = (razon: unknown) => sinDuenio.push(razon);
    const proceso = (globalThis as { process?: DetectorDeRechazos }).process;
    proceso?.on('unhandledRejection', anotar);

    try {
      await expect(enviarArchivo('PUT', RUTA_IMAGEN, formularioConImagen())).rejects.toMatchObject({
        codigo: 'TIEMPO_AGOTADO',
      });

      // `fetch` responde mucho después, cuando ya nadie lo espera: si su rechazo
      // escapara, el navegador lo anunciaría como error no controlado.
      fallar(new TypeError('Failed to fetch'));
      await respirar();
      await respirar();
    } finally {
      proceso?.off('unhandledRejection', anotar);
    }

    expect(sinDuenio).toEqual([]);
  });
});

/** Deja ver si una promesa ya terminó sin obligar a esperarla. */
function seguir(promesa: Promise<unknown>) {
  const estado = { asentada: false };
  const marcar = () => {
    estado.asentada = true;
  };
  promesa.then(marcar, marcar);
  return estado;
}

/** Un formulario con la forma exacta que envía la subida de una imagen. */
function formularioConImagen(): FormData {
  const formulario = new FormData();
  formulario.append('archivo', new File([new Uint8Array(8)], 'foto.png', { type: 'image/png' }));
  return formulario;
}

/** Cede el turno al bucle de eventos para que Node pueda anunciar lo que quedó sin dueño. */
function respirar(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}
