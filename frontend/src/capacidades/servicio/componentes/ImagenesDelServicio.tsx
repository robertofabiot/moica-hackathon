import { useRef, useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import { intercambiar } from '../../../comun/listas';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import {
  useEliminacionDeImagenDeServicio,
  useOrdenDeImagenesDeServicio,
  useSubidaDeImagenDeServicio,
  useTextoAlternativoDeServicio,
} from '../hooks/useServiciosPropios';
import type { ServicioPropio } from '../tipos';
import propios from './imagenes.module.css';

/**
 * Imágenes de un servicio: subirlas con texto alternativo, ordenarlas y quitarlas.
 *
 * Cada acción tiene su propio estado de carga para que un fallo al reordenar no bloquee una
 * subida. El texto alternativo se pide al elegir el archivo y se puede corregir después.
 */
export default function ImagenesDelServicio({ servicio }: { servicio: ServicioPropio }) {
  const subida = useSubidaDeImagenDeServicio();
  const eliminacion = useEliminacionDeImagenDeServicio();
  const orden = useOrdenDeImagenesDeServicio();
  const texto = useTextoAlternativoDeServicio();

  const [textoAlternativo, setTextoAlternativo] = useState('');
  const [previsualizacion, setPrevisualizacion] = useState<string | null>(null);
  const entradaDeArchivo = useRef<HTMLInputElement>(null);

  const elegir = (evento: React.ChangeEvent<HTMLInputElement>) => {
    const archivo = evento.target.files?.[0];
    if (archivo === undefined) {
      return;
    }
    setPrevisualizacion(URL.createObjectURL(archivo));
    subida.mutate(
      {
        idServicio: servicio.idServicioPublicado,
        archivo,
        textoAlternativo,
      },
      {
        onSuccess: () => {
          setTextoAlternativo('');
          setPrevisualizacion(null);
        },
        onSettled: () => {
          if (entradaDeArchivo.current !== null) {
            entradaDeArchivo.current.value = '';
          }
        },
      }
    );
  };

  const mover = (posicion: number, desplazamiento: number) => {
    const reordenada = intercambiar(servicio.imagenes, posicion, posicion + desplazamiento);
    if (reordenada === null) {
      return;
    }
    orden.mutate({
      idServicio: servicio.idServicioPublicado,
      idsEnOrden: reordenada.map((imagen) => imagen.idImagenServicioPublicado),
    });
  };

  const identificador = `imagen-de-servicio-${servicio.idServicioPublicado}`;
  const mensajeDeError =
    mensajeDe(subida.error) ??
    mensajeDe(eliminacion.error) ??
    mensajeDe(orden.error) ??
    mensajeDe(texto.error);

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-imagenes-del-servicio">
      <h2 className={secciones.tituloDeSeccion} id="titulo-imagenes-del-servicio">
        Imágenes
      </h2>
      <p className={secciones.explicacion}>
        JPEG, PNG o WebP, hasta 5 MB. El texto alternativo describe lo que se ve para quien no puede
        ver la imagen.
      </p>

      {mensajeDeError !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      {servicio.imagenes.length === 0 ? (
        <p className={secciones.vacio}>Este servicio todavía no tiene imágenes.</p>
      ) : (
        <ul className={propios.galeria}>
          {servicio.imagenes.map((imagen, posicion) => (
            <li key={imagen.idImagenServicioPublicado}>
              <img
                className={propios.miniatura}
                src={imagen.urlImagen}
                alt={imagen.textoAlternativo ?? `Imagen ${posicion + 1} de ${servicio.nombre}`}
                loading="lazy"
              />
              <div className={secciones.accionesDelElemento}>
                <button
                  className={secciones.botonPequeno}
                  type="button"
                  onClick={() => mover(posicion, -1)}
                  disabled={posicion === 0 || orden.isPending}
                  aria-label={`Subir la imagen ${posicion + 1} de ${servicio.nombre}`}
                >
                  Subir
                </button>
                <button
                  className={secciones.botonPequeno}
                  type="button"
                  onClick={() => mover(posicion, 1)}
                  disabled={posicion === servicio.imagenes.length - 1 || orden.isPending}
                  aria-label={`Bajar la imagen ${posicion + 1} de ${servicio.nombre}`}
                >
                  Bajar
                </button>
                <button
                  className={secciones.botonPequeno}
                  type="button"
                  onClick={() =>
                    eliminacion.mutate({
                      idServicio: servicio.idServicioPublicado,
                      idImagen: imagen.idImagenServicioPublicado,
                    })
                  }
                  disabled={eliminacion.isPending}
                  aria-label={`Quitar la imagen ${posicion + 1} de ${servicio.nombre}`}
                >
                  Quitar
                </button>
              </div>
              <label
                className={estilos.pista}
                htmlFor={`texto-servicio-${imagen.idImagenServicioPublicado}`}
              >
                Texto alternativo
              </label>
              <input
                id={`texto-servicio-${imagen.idImagenServicioPublicado}`}
                className={claseDeEntrada(false)}
                type="text"
                defaultValue={imagen.textoAlternativo ?? ''}
                maxLength={200}
                onBlur={(evento) => {
                  const valor = evento.target.value.trim();
                  if (valor !== (imagen.textoAlternativo ?? '')) {
                    texto.mutate({
                      idServicio: servicio.idServicioPublicado,
                      idImagen: imagen.idImagenServicioPublicado,
                      textoAlternativo: valor === '' ? null : valor,
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
        {previsualizacion !== null && (
          <img
            className={propios.previsualizacion}
            src={previsualizacion}
            alt="Previsualización de la imagen que se está subiendo"
          />
        )}
        {subida.isPending && (
          <p className={secciones.estado} role="status">
            Subiendo la imagen…
          </p>
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
