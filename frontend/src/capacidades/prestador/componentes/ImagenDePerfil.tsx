import { useRef } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import {
  useEliminacionDeImagenDePerfil,
  useSubidaDeImagenDePerfil,
} from '../hooks/usePerfilPrestador';
import secciones from '../../../comun/estilos/secciones.module.css';
import propios from '../paginas/prestador.module.css';
import type { PerfilPrestador } from '../tipos';

/**
 * La imagen de perfil: previsualizarla, sustituirla y quitarla.
 *
 * La previsualización es la imagen ya guardada, no el archivo local: así lo que se ve es
 * exactamente lo que el servidor aceptó y sirve. Los formatos y el tamaño los valida el backend;
 * aquí solo se anuncian y se muestra el mensaje que devuelva.
 */
export default function ImagenDePerfil({ perfil }: { perfil: PerfilPrestador }) {
  const subida = useSubidaDeImagenDePerfil();
  const eliminacion = useEliminacionDeImagenDePerfil();
  const entradaDeArchivo = useRef<HTMLInputElement>(null);

  const elegir = (evento: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    if (archivo === undefined) {
      return;
    }
    subida.mutate(archivo, {
      // El campo se limpia siempre: si no, elegir dos veces el mismo archivo
      // tras un error no dispararía otro `change`.
      onSettled: () => {
        if (entradaDeArchivo.current !== null) {
          entradaDeArchivo.current.value = '';
        }
      },
    });
  };

  const enCurso = subida.isPending || eliminacion.isPending;
  const mensajeDeError = mensajeDe(subida.error) ?? mensajeDe(eliminacion.error);

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-imagen">
      <h2 className={secciones.tituloDeSeccion} id="titulo-imagen">
        Imagen de perfil
      </h2>

      {mensajeDeError !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      <div className={propios.imagenDePerfil}>
        {perfil.urlImagenPerfil === null ? (
          <p className={propios.retratoVacio}>Sin imagen todavía</p>
        ) : (
          <img
            className={propios.retrato}
            src={perfil.urlImagenPerfil}
            alt={`Imagen de perfil de ${perfil.nombrePublico}`}
            width={128}
            height={128}
          />
        )}

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="archivoDeImagen">
            {perfil.urlImagenPerfil === null ? 'Subir una imagen' : 'Sustituir la imagen'}
          </label>
          <input
            id="archivoDeImagen"
            ref={entradaDeArchivo}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            onChange={elegir}
            disabled={enCurso}
            aria-describedby="pista-imagen"
          />
          <p className={estilos.pista} id="pista-imagen">
            JPEG, PNG o WebP, hasta 5 MB.
          </p>
        </div>

        {subida.isPending && (
          <p className={secciones.estado} role="status">
            Subiendo la imagen…
          </p>
        )}

        {perfil.urlImagenPerfil !== null && (
          <button
            className={secciones.botonSecundario}
            type="button"
            onClick={() => eliminacion.mutate()}
            disabled={enCurso}
          >
            {eliminacion.isPending ? 'Quitando la imagen…' : 'Quitar imagen'}
          </button>
        )}
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
