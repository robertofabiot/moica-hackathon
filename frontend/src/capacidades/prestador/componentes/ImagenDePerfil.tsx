import { useCallback, useEffect, useRef, useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, IconoCamara, IconoUsuario } from '../../../comun/componentes/ui';
import {
  useEliminacionDeImagenDePerfil,
  useSubidaDeImagenDePerfil,
} from '../hooks/usePerfilPrestador';
import propios from '../paginas/prestador.module.css';
import type { PerfilPrestador } from '../tipos';

/** El archivo que se acaba de elegir, con la URL temporal que lo muestra sin haberlo subido. */
interface Seleccion {
  archivo: File;
  url: string;
  /** El navegador no pudo pintar el archivo: no es una imagen que sepa mostrar. */
  ilegible: boolean;
}

/**
 * La imagen de perfil: previsualizarla, sustituirla y quitarla.
 *
 * La previsualización es de verdad local: el archivo elegido se muestra desde el propio navegador
 * con `URL.createObjectURL`, sin esperar a que el servidor lo devuelva y sin ninguna dependencia
 * nueva. Mientras la carga está en curso se ven las dos —la que sigue guardada y la elegida—
 * porque son cosas distintas: ver el archivo local no significa que se haya guardado, y la imagen
 * anterior sigue siendo la vigente hasta que el backend acepta la nueva.
 *
 * Los formatos y el tamaño los valida el backend, que es la autoridad: aquí solo se anuncian, y lo
 * que se ve elegido nunca se presenta como aceptado hasta que la API lo confirma.
 */
export default function ImagenDePerfil({ perfil }: { perfil: PerfilPrestador }) {
  const subida = useSubidaDeImagenDePerfil();
  const eliminacion = useEliminacionDeImagenDePerfil();
  const entradaDeArchivo = useRef<HTMLInputElement>(null);
  // La URL viva se guarda también en una referencia: la limpieza al desmontar
  // no puede depender de lo que el último render tuviera en el estado.
  const urlLocal = useRef<string | null>(null);
  const [seleccion, setSeleccion] = useState<Seleccion | null>(null);

  const soltarSeleccion = useCallback(() => {
    if (urlLocal.current !== null) {
      URL.revokeObjectURL(urlLocal.current);
      urlLocal.current = null;
    }
    setSeleccion(null);
  }, []);

  // Al desmontar, la última URL temporal se libera aunque nadie la sustituya:
  // si no, el archivo se queda retenido en memoria mientras dure la pestaña.
  useEffect(() => soltarSeleccion, [soltarSeleccion]);

  const subir = (archivo: File) => {
    subida.mutate(archivo, {
      // Con la imagen ya guardada, la que sirve el backend sustituye a la copia
      // local y la URL temporal deja de hacer falta.
      onSuccess: soltarSeleccion,
      // El campo se limpia siempre: si no, elegir dos veces el mismo archivo
      // tras un error no dispararía otro `change`.
      onSettled: () => {
        if (entradaDeArchivo.current !== null) {
          entradaDeArchivo.current.value = '';
        }
      },
    });
  };

  const elegir = (evento: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    if (archivo === undefined) {
      return;
    }

    // La URL anterior se revoca antes de crear la siguiente: en todo momento
    // hay una sola viva, la del archivo que se está viendo.
    if (urlLocal.current !== null) {
      URL.revokeObjectURL(urlLocal.current);
    }
    const url = URL.createObjectURL(archivo);
    urlLocal.current = url;
    setSeleccion({ archivo, url, ilegible: false });

    subir(archivo);
  };

  const enCurso = subida.isPending || eliminacion.isPending;
  const mensajeDeError = mensajeDe(subida.error) ?? mensajeDe(eliminacion.error);

  return (
    <section className={propios.bloqueIdentidad} aria-labelledby="titulo-imagen">
      <h2 className={propios.tituloDeTarjeta} id="titulo-imagen">
        Imagen de perfil
      </h2>

      {mensajeDeError !== null && (
        <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      <div className={propios.imagenDePerfil}>
        <div className={propios.retratos}>
          <figure className={propios.retratoConPie}>
            {perfil.urlImagenPerfil === null ? (
              <p className={propios.retratoVacio}>
                <IconoUsuario className={propios.iconoDeRetrato} />
                Sin imagen todavía
              </p>
            ) : (
              <img
                className={propios.retrato}
                src={perfil.urlImagenPerfil}
                alt={`Imagen de perfil de ${perfil.nombrePublico}`}
                width={88}
                height={88}
              />
            )}
            {seleccion !== null && <figcaption className={propios.pista}>Imagen actual</figcaption>}
          </figure>

          {seleccion !== null && (
            <figure className={propios.retratoConPie}>
              {seleccion.ilegible ? (
                <p className={propios.retratoVacio}>No pudimos mostrar este archivo</p>
              ) : (
                <img
                  className={propios.retrato}
                  src={seleccion.url}
                  alt={`Imagen elegida para el perfil de ${perfil.nombrePublico}, todavía sin guardar`}
                  width={88}
                  height={88}
                  onError={() =>
                    setSeleccion((actual) =>
                      actual === null ? null : { ...actual, ilegible: true }
                    )
                  }
                />
              )}
              <figcaption className={propios.pista}>
                {subida.isPending ? 'Elegida, subiendo…' : 'Elegida, sin guardar'}
              </figcaption>
            </figure>
          )}
        </div>

        <div className={propios.campoDeSubida}>
          <label className={propios.etiquetaDeSubida} htmlFor="archivoDeImagen">
            <IconoCamara />
            {perfil.urlImagenPerfil === null ? 'Subir una imagen' : 'Sustituir la imagen'}
          </label>
          <input
            id="archivoDeImagen"
            className={propios.entradaDeArchivo}
            ref={entradaDeArchivo}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={elegir}
            disabled={enCurso}
            aria-describedby="pista-imagen"
          />
          <p className={propios.pista} id="pista-imagen">
            JPEG, PNG o WebP, hasta 5 MB.
          </p>
        </div>

        {subida.isPending && (
          <p className={propios.estado} role="status">
            Subiendo la imagen… La actual sigue vigente hasta que termine.
          </p>
        )}

        <div className={propios.accionesDeImagen}>
          {subida.isError && seleccion !== null && (
            <Boton
              variante="secundario"
              type="button"
              onClick={() => subir(seleccion.archivo)}
              disabled={enCurso}
            >
              Reintentar la subida
            </Boton>
          )}

          {perfil.urlImagenPerfil !== null && (
            <Boton
              variante="contorno"
              type="button"
              onClick={() => eliminacion.mutate()}
              disabled={enCurso}
            >
              {eliminacion.isPending ? 'Quitando la imagen…' : 'Quitar imagen'}
            </Boton>
          )}
        </div>
      </div>
    </section>
  );
}

function mensajeDe(error: unknown): string | null {
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return error instanceof Error ? error.message : null;
}
