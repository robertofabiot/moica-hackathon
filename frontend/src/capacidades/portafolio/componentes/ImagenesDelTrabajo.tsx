import { useRef, useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import { intercambiar } from '../../../comun/listas';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import propios from './portafolio.module.css';
import {
  useEliminacionDeImagen,
  useOrdenDeImagenes,
  useSubidaDeImagenDeTrabajo,
  useTextoAlternativo,
} from '../hooks/usePortafolio';
import type { Trabajo } from '../tipos';

/**
 * Las imágenes de un trabajo: subirlas con su texto alternativo, ordenarlas y quitarlas.
 *
 * El texto alternativo se pide al subir y se puede corregir después: es lo que hace que la galería
 * sea comprensible para quien usa un lector de pantalla.
 */
export default function ImagenesDelTrabajo({ trabajo }: { trabajo: Trabajo }) {
  const subida = useSubidaDeImagenDeTrabajo();
  const eliminacion = useEliminacionDeImagen();
  const orden = useOrdenDeImagenes();
  const texto = useTextoAlternativo();

  const [textoAlternativo, setTextoAlternativo] = useState('');
  const entradaDeArchivo = useRef<HTMLInputElement>(null);

  const elegir = (evento: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    if (archivo === undefined) {
      return;
    }
    subida.mutate(
      { idTrabajo: trabajo.idTrabajo, archivo, textoAlternativo },
      {
        onSuccess: () => setTextoAlternativo(''),
        // El campo se limpia siempre: si no, elegir dos veces el mismo archivo
        // tras un error no dispararía otro `change`.
        onSettled: () => {
          if (entradaDeArchivo.current !== null) {
            entradaDeArchivo.current.value = '';
          }
        },
      }
    );
  };

  const mover = (posicion: number, desplazamiento: number) => {
    const reordenada = intercambiar(trabajo.imagenes, posicion, posicion + desplazamiento);
    if (reordenada === null) {
      return;
    }
    orden.mutate({
      idTrabajo: trabajo.idTrabajo,
      idsEnOrden: reordenada.map((imagen) => imagen.idImagenTrabajoPortafolio),
    });
  };

  const identificador = `imagen-de-trabajo-${trabajo.idTrabajo}`;
  const mensajeDeError =
    mensajeDe(subida.error) ??
    mensajeDe(eliminacion.error) ??
    mensajeDe(orden.error) ??
    mensajeDe(texto.error);

  return (
    <div>
      <h4 className={secciones.metadatoDelElemento}>Imágenes</h4>

      {mensajeDeError !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      {trabajo.imagenes.length === 0 ? (
        <p className={secciones.vacio}>Este trabajo todavía no tiene imágenes.</p>
      ) : (
        <ul className={propios.galeria}>
          {trabajo.imagenes.map((imagen, posicion) => (
            <li key={imagen.idImagenTrabajoPortafolio}>
              <img
                className={propios.miniatura}
                src={imagen.urlImagen}
                alt={imagen.textoAlternativo ?? `Imagen del trabajo ${trabajo.titulo}`}
                loading="lazy"
              />
              <div className={secciones.accionesDelElemento}>
                <button
                  className={secciones.botonPequeno}
                  type="button"
                  onClick={() => mover(posicion, -1)}
                  disabled={posicion === 0 || orden.isPending}
                  aria-label={`Subir la imagen ${posicion + 1} de ${trabajo.titulo}`}
                >
                  Subir
                </button>
                <button
                  className={secciones.botonPequeno}
                  type="button"
                  onClick={() => mover(posicion, 1)}
                  disabled={posicion === trabajo.imagenes.length - 1 || orden.isPending}
                  aria-label={`Bajar la imagen ${posicion + 1} de ${trabajo.titulo}`}
                >
                  Bajar
                </button>
                <button
                  className={secciones.botonPequeno}
                  type="button"
                  onClick={() =>
                    eliminacion.mutate({
                      idTrabajo: trabajo.idTrabajo,
                      idImagen: imagen.idImagenTrabajoPortafolio,
                    })
                  }
                  disabled={eliminacion.isPending}
                  aria-label={`Quitar la imagen ${posicion + 1} de ${trabajo.titulo}`}
                >
                  Quitar
                </button>
              </div>
              <label
                className={estilos.pista}
                htmlFor={`texto-${imagen.idImagenTrabajoPortafolio}`}
              >
                Texto alternativo
              </label>
              <input
                id={`texto-${imagen.idImagenTrabajoPortafolio}`}
                className={claseDeEntrada(false)}
                type="text"
                defaultValue={imagen.textoAlternativo ?? ''}
                maxLength={200}
                onBlur={(evento) => {
                  const valor = evento.target.value.trim();
                  if (valor !== (imagen.textoAlternativo ?? '')) {
                    texto.mutate({
                      idTrabajo: trabajo.idTrabajo,
                      idImagen: imagen.idImagenTrabajoPortafolio,
                      textoAlternativo: valor,
                    });
                  }
                }}
              />
            </li>
          ))}
        </ul>
      )}

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={`${identificador}-texto`}>
          Texto alternativo de la imagen nueva
        </label>
        <input
          id={`${identificador}-texto`}
          className={claseDeEntrada(false)}
          type="text"
          maxLength={200}
          value={textoAlternativo}
          onChange={(evento) => setTextoAlternativo(evento.target.value)}
          aria-describedby={`${identificador}-pista`}
        />
        <p className={estilos.pista} id={`${identificador}-pista`}>
          Describe brevemente lo que se ve, para quien no puede verla.
        </p>

        <label className={estilos.etiqueta} htmlFor={identificador}>
          Agregar imagen
        </label>
        <input
          id={identificador}
          ref={entradaDeArchivo}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={elegir}
          disabled={subida.isPending}
        />
        <p className={estilos.pista}>JPEG, PNG o WebP, hasta 5 MB.</p>
        {subida.isPending && (
          <p className={secciones.estado} role="status">
            Subiendo la imagen…
          </p>
        )}
      </div>
    </div>
  );
}

function mensajeDe(error: unknown): string | null {
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return error instanceof Error ? error.message : null;
}
