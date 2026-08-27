import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import {
  definirTiempoDeEsperaDeArchivoMs,
  TIEMPO_DE_ESPERA_DE_ARCHIVO_MS,
  TIEMPO_DE_ESPERA_MS,
} from '../../../comun/api';
import {
  cuerpoDeError,
  instalarApiFalsa,
  perfilDeEjemplo,
  type ApiFalsa,
} from '../../../pruebas/apiFalsa';
import { renderizarConProveedores } from '../../../pruebas/utilidades';
import { usePerfilPrestador } from '../hooks/usePerfilPrestador';
import ImagenDePerfil from './ImagenDePerfil';

/**
 * La previsualización de la imagen de perfil, que es del archivo local y no de lo ya guardado.
 *
 * Se comprueba el ciclo completo de la URL temporal —crearla, sustituirla y liberarla— y que la
 * pantalla nunca presente como guardado un archivo que el backend todavía no aceptó.
 *
 * `URL.createObjectURL` se sustituye por un doble que apunta las URL creadas y revocadas: jsdom no
 * pinta imágenes, así que lo comprobable es exactamente eso, qué URL se crea, cuál se muestra y
 * cuál se libera.
 */

const RUTA_PERFIL = '/api/prestador/perfil';
const RUTA_IMAGEN = '/api/prestador/perfil/imagen';

const ALT_GUARDADA = 'Imagen de perfil de Taller La Esperanza';
const ALT_LOCAL = 'Imagen elegida para el perfil de Taller La Esperanza, todavía sin guardar';

const URL_VIEJA = 'https://imagenes.moica.test/perfiles/vieja.png';
const URL_NUEVA = 'https://imagenes.moica.test/perfiles/nueva.png';

/** Alimenta al componente como lo hace la pantalla real: desde la consulta del perfil. */
function PantallaDeImagen() {
  const { data } = usePerfilPrestador();

  if (data === undefined || data === null) {
    return <p>Cargando…</p>;
  }
  return <ImagenDePerfil perfil={data} />;
}

describe('Previsualización de la imagen de perfil', () => {
  let api: ApiFalsa;
  let creadas: string[];
  let revocadas: string[];

  const crearOriginal = URL.createObjectURL;
  const revocarOriginal = URL.revokeObjectURL;

  beforeEach(() => {
    api = instalarApiFalsa();
    document.cookie = 'XSRF-TOKEN=token-de-prueba';
    api.responder(`GET ${RUTA_PERFIL}`, { estado: 200, cuerpo: perfilDeEjemplo() });

    // Una carga que se deja colgada no debe quedar noventa segundos esperando
    // después de que la prueba termine; diez es la misma espera con la que ya
    // trabajan las demás pruebas de red.
    definirTiempoDeEsperaDeArchivoMs(TIEMPO_DE_ESPERA_MS);

    creadas = [];
    revocadas = [];
    URL.createObjectURL = () => {
      const url = `blob:moica/${creadas.length + 1}`;
      creadas.push(url);
      return url;
    };
    URL.revokeObjectURL = (url: string) => {
      revocadas.push(url);
    };
  });

  afterEach(() => {
    URL.createObjectURL = crearOriginal;
    URL.revokeObjectURL = revocarOriginal;
    definirTiempoDeEsperaDeArchivoMs(TIEMPO_DE_ESPERA_DE_ARCHIVO_MS);
  });

  function archivoDeImagen(nombre = 'retrato.png') {
    return new File([new Uint8Array(8)], nombre, { type: 'image/png' });
  }

  function conImagenGuardada() {
    api.responder(`GET ${RUTA_PERFIL}`, {
      estado: 200,
      cuerpo: perfilDeEjemplo({ urlImagenPerfil: URL_VIEJA }),
    });
  }

  function rechazarLaSubida() {
    api.responder(`PUT ${RUTA_IMAGEN}`, {
      estado: 400,
      cuerpo: cuerpoDeError(
        400,
        'IMAGEN_NO_ADMITIDA',
        'El contenido del archivo no corresponde con una imagen JPEG, PNG o WebP.'
      ),
    });
  }

  it('muestra el archivo elegido desde el navegador mientras se sube', async () => {
    const persona = userEvent.setup();
    api.colgar(`PUT ${RUTA_IMAGEN}`);

    renderizarConProveedores(<PantallaDeImagen />);

    await persona.upload(await screen.findByLabelText('Subir una imagen'), archivoDeImagen());

    const local = await screen.findByAltText(ALT_LOCAL);
    expect(local).toHaveAttribute('src', creadas[0]);
    expect(creadas, 'una sola URL temporal por archivo elegido').toHaveLength(1);
    expect(screen.getByText('Elegida, subiendo…')).toBeVisible();
    expect(screen.getByRole('status')).toHaveTextContent('Subiendo la imagen…');
    expect(
      screen.getByLabelText('Subir una imagen'),
      'no se admite otro archivo hasta que este termine'
    ).toBeDisabled();
  });

  it('sustituye la previsualización y libera la URL de la anterior', async () => {
    const persona = userEvent.setup();
    rechazarLaSubida();

    renderizarConProveedores(<PantallaDeImagen />);
    const campo = await screen.findByLabelText('Subir una imagen');

    await persona.upload(campo, archivoDeImagen('primera.png'));
    await screen.findByRole('alert');

    await persona.upload(campo, archivoDeImagen('segunda.png'));

    await waitFor(() => expect(creadas).toHaveLength(2));
    expect(revocadas, 'la URL de la primera se libera al elegir la segunda').toEqual([creadas[0]]);
    expect(await screen.findByAltText(ALT_LOCAL)).toHaveAttribute('src', creadas[1]);
  });

  it('libera la URL temporal al desmontar la pantalla', async () => {
    const persona = userEvent.setup();
    rechazarLaSubida();

    const { unmount } = renderizarConProveedores(<PantallaDeImagen />);

    await persona.upload(await screen.findByLabelText('Subir una imagen'), archivoDeImagen());
    await screen.findByRole('alert');
    expect(revocadas).toEqual([]);

    unmount();

    expect(revocadas).toEqual([creadas[0]]);
  });

  it('tras guardarse deja de verse la copia local y queda la imagen del servidor', async () => {
    const persona = userEvent.setup();
    api.responder(`PUT ${RUTA_IMAGEN}`, {
      estado: 200,
      cuerpo: perfilDeEjemplo({ urlImagenPerfil: URL_NUEVA }),
    });

    renderizarConProveedores(<PantallaDeImagen />);

    await persona.upload(await screen.findByLabelText('Subir una imagen'), archivoDeImagen());

    expect(await screen.findByAltText(ALT_GUARDADA)).toHaveAttribute('src', URL_NUEVA);
    expect(screen.queryByAltText(ALT_LOCAL)).not.toBeInTheDocument();
    expect(screen.queryByText('Imagen actual')).not.toBeInTheDocument();
    expect(revocadas, 'la URL temporal no sobrevive a la imagen que ya sirve el backend').toEqual([
      creadas[0],
    ]);
    expect(screen.getByLabelText('Sustituir la imagen')).toBeEnabled();
  });

  it('ante un error explica qué pasó, conserva la imagen guardada y permite reintentar', async () => {
    const persona = userEvent.setup();
    conImagenGuardada();
    rechazarLaSubida();

    renderizarConProveedores(<PantallaDeImagen />);

    await persona.upload(
      await screen.findByLabelText('Sustituir la imagen'),
      archivoDeImagen('disfrazada.png')
    );

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'El contenido del archivo no corresponde con una imagen JPEG, PNG o WebP.'
    );
    expect(
      screen.getByAltText(ALT_GUARDADA),
      'la imagen anterior sigue siendo la vigente'
    ).toHaveAttribute('src', URL_VIEJA);
    expect(screen.getByText('Imagen actual')).toBeVisible();
    expect(screen.getByText('Elegida, sin guardar')).toBeVisible();
    expect(screen.queryByRole('status'), 'ya no hay nada procesándose').not.toBeInTheDocument();

    // Reintentar reenvía el mismo archivo, sin obligar a volver a elegirlo.
    api.responder(`PUT ${RUTA_IMAGEN}`, {
      estado: 200,
      cuerpo: perfilDeEjemplo({ urlImagenPerfil: URL_NUEVA }),
    });
    await persona.click(screen.getByRole('button', { name: 'Reintentar la subida' }));

    await waitFor(() => {
      expect(screen.getByAltText(ALT_GUARDADA)).toHaveAttribute('src', URL_NUEVA);
    });
    expect(api.peticiones.filter((peticion) => peticion.ruta === RUTA_IMAGEN)).toHaveLength(2);
    expect(creadas, 'reintentar no vuelve a leer el archivo').toHaveLength(1);
    expect(revocadas).toEqual([creadas[0]]);
  });

  it('no presenta como aceptado un archivo que el navegador no puede mostrar', async () => {
    const persona = userEvent.setup();
    rechazarLaSubida();

    renderizarConProveedores(<PantallaDeImagen />);

    await persona.upload(
      await screen.findByLabelText('Subir una imagen'),
      archivoDeImagen('documento.png')
    );

    // El navegador avisa de que no pudo pintarlo: la imagen rota se sustituye
    // por un texto, y el backend sigue teniendo la última palabra.
    fireEvent.error(await screen.findByAltText(ALT_LOCAL));

    expect(await screen.findByText('No pudimos mostrar este archivo')).toBeVisible();
    expect(screen.queryByAltText(ALT_LOCAL)).not.toBeInTheDocument();
    expect(await screen.findByRole('alert')).toHaveTextContent('El contenido del archivo');
  });
});
