import { useRef, useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, Entrada, IconoCamara } from '../../../comun/componentes/ui';
import { intercambiar } from '../../../comun/listas';
import {
  useEliminacionDeImagen,
  useOrdenDeImagenes,
  useSubidaDeImagenDeTrabajo,
  useTextoAlternativo,
} from '../hooks/usePortafolio';
import type { Trabajo } from '../tipos';
import propios from './portafolio.module.css';

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
    <div className={propios.bloqueDeImagenes}>
      <h4 className={propios.metadato}>Imágenes</h4>

      {mensajeDeError !== null && (
        <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      {trabajo.imagenes.length === 0 ? (
        <p className={propios.vacio}>Este trabajo todavía no tiene imágenes.</p>
      ) : (
        <ul className={propios.galeria}>
          {trabajo.imagenes.map((imagen, posicion) => (
            <li className={propios.miniaturaEnvoltorio} key={imagen.idImagenTrabajoPortafolio}>
              <img
                className={propios.miniatura}
                src={imagen.urlImagen}
                alt={imagen.textoAlternativo ?? `Imagen del trabajo ${trabajo.titulo}`}
                loading="lazy"
              />
              <div className={propios.accionesDeFila}>
                <Boton
                  className={propios.botonCompacto}
                  variante="secundario"
                  type="button"
                  onClick={() => mover(posicion, -1)}
                  disabled={posicion === 0 || orden.isPending}
                  aria-label={`Subir la imagen ${posicion + 1} de ${trabajo.titulo}`}
                >
                  Subir
                </Boton>
                <Boton
                  className={propios.botonCompacto}
                  variante="secundario"
                  type="button"
                  onClick={() => mover(posicion, 1)}
                  disabled={posicion === trabajo.imagenes.length - 1 || orden.isPending}
                  aria-label={`Bajar la imagen ${posicion + 1} de ${trabajo.titulo}`}
                >
                  Bajar
                </Boton>
                <Boton
                  className={propios.botonCompacto}
                  variante="contorno"
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
                </Boton>
              </div>
              <label
                className={propios.metadato}
                htmlFor={`texto-${imagen.idImagenTrabajoPortafolio}`}
              >
                Texto alternativo
              </label>
              <Entrada
                id={`texto-${imagen.idImagenTrabajoPortafolio}`}
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

      <div className={propios.campo}>
        <label className={propios.etiqueta} htmlFor={`${identificador}-texto`}>
          Texto alternativo de la imagen nueva
        </label>
        <Entrada
          id={`${identificador}-texto`}
          type="text"
          maxLength={200}
          value={textoAlternativo}
          onChange={(evento) => setTextoAlternativo(evento.target.value)}
          aria-describedby={`${identificador}-pista`}
        />
        <p className={propios.metadato} id={`${identificador}-pista`}>
          Describe brevemente lo que se ve, para quien no puede verla.
        </p>

        <label className={propios.etiquetaDeSubida} htmlFor={identificador}>
          <IconoCamara />
          Agregar imagen
        </label>
        <input
          id={identificador}
          className={propios.entradaOculta}
          ref={entradaDeArchivo}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={elegir}
          disabled={subida.isPending}
        />
        <p className={propios.metadato}>JPEG, PNG o WebP, hasta 5 MB.</p>
        {subida.isPending && (
          <p className={propios.estado} role="status">
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
