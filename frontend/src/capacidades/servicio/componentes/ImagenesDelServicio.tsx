import { useRef, useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, Entrada, IconoSubida, IconoX } from '../../../comun/componentes/ui';
import { intercambiar } from '../../../comun/listas';
import estilos from '../../../comun/estilos/formulario.module.css';
import {
  useEliminacionDeImagenDeServicio,
  useOrdenDeImagenesDeServicio,
  useSubidaDeImagenDeServicio,
  useTextoAlternativoDeServicio,
} from '../hooks/useServiciosPropios';
import zona from '../paginas/servicios.module.css';
import type { ServicioPropio } from '../tipos';
import propios from './imagenes.module.css';

const FORMATOS_VALIDOS = ['image/jpeg', 'image/png', 'image/webp'];
const TAMANO_MAXIMO = 5 * 1024 * 1024;

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
  const [arrastrando, setArrastrando] = useState(false);
  const [errorDeArchivo, setErrorDeArchivo] = useState<string | undefined>();
  const [idAConfirmar, setIdAConfirmar] = useState<number | null>(null);
  const entradaDeArchivo = useRef<HTMLInputElement>(null);

  const identificador = `imagen-de-servicio-${servicio.idServicioPublicado}`;
  const mensajeDeError =
    errorDeArchivo ??
    mensajeDe(subida.error) ??
    mensajeDe(eliminacion.error) ??
    mensajeDe(orden.error) ??
    mensajeDe(texto.error);

  function mover(posicion: number, desplazamiento: number) {
    const reordenada = intercambiar(servicio.imagenes, posicion, posicion + desplazamiento);
    if (reordenada === null) {
      return;
    }
    orden.mutate({
      idServicio: servicio.idServicioPublicado,
      idsEnOrden: reordenada.map((imagen) => imagen.idImagenServicioPublicado),
    });
  }

  async function subirArchivos(lista: FileList | null) {
    if (lista === null || lista.length === 0) {
      return;
    }
    setErrorDeArchivo(undefined);
    const { validos, error } = validarArchivos(lista);
    if (error !== undefined) {
      setErrorDeArchivo(error);
    }
    if (validos.length === 0) {
      return;
    }

    for (const archivo of validos) {
      setPrevisualizacion(URL.createObjectURL(archivo));
      try {
        await subida.mutateAsync({
          idServicio: servicio.idServicioPublicado,
          archivo,
          textoAlternativo,
        });
      } catch {
        break;
      }
    }
    setTextoAlternativo('');
    setPrevisualizacion(null);
    if (entradaDeArchivo.current !== null) {
      entradaDeArchivo.current.value = '';
    }
  }

  function confirmarEliminacion(idImagen: number) {
    eliminacion.mutate(
      {
        idServicio: servicio.idServicioPublicado,
        idImagen,
      },
      { onSettled: () => setIdAConfirmar(null) }
    );
  }

  return (
    <section className={zona.tarjetaDeEdicion} aria-labelledby="titulo-imagenes-del-servicio">
      <h2 className={zona.tituloDeTarjetaEdicion} id="titulo-imagenes-del-servicio">
        Galería de imágenes
      </h2>
      <p className={zona.explicacionDeEstado}>
        JPEG, PNG o WebP, hasta 5 MB. La primera foto es la que se muestra como principal en el
        directorio. El texto alternativo describe lo que se ve para quien no puede ver la imagen.
      </p>

      {mensajeDeError !== null && mensajeDeError !== undefined && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      {servicio.imagenes.length === 0 ? (
        <p className={propios.vacio}>Este servicio todavía no tiene imágenes.</p>
      ) : (
        <ul className={propios.galeria}>
          {servicio.imagenes.map((imagen, posicion) => {
            const confirmando = idAConfirmar === imagen.idImagenServicioPublicado;
            return (
              <li key={imagen.idImagenServicioPublicado} className={propios.itemDeGaleria}>
                <div className={propios.marcoDeFoto}>
                  <img
                    className={propios.miniatura}
                    src={imagen.urlImagen}
                    alt={imagen.textoAlternativo ?? `Imagen ${posicion + 1} de ${servicio.nombre}`}
                    loading="lazy"
                  />
                  {posicion === 0 && (
                    <span className={propios.insigniaPrincipal}>Foto principal</span>
                  )}
                  {confirmando ? null : (
                    <button
                      type="button"
                      className={propios.botonEliminar}
                      onClick={() => setIdAConfirmar(imagen.idImagenServicioPublicado)}
                      disabled={eliminacion.isPending}
                      aria-label={`Eliminar la foto ${posicion + 1} de ${servicio.nombre}`}
                    >
                      <IconoX />
                    </button>
                  )}
                </div>
                {confirmando ? (
                  <div
                    className={propios.confirmacion}
                    role="group"
                    aria-label={`Confirmar eliminación de la foto ${posicion + 1}`}
                  >
                    <p className={propios.textoConfirmacion}>¿Eliminar esta foto?</p>
                    <div className={propios.accionesDeConfirmacion}>
                      <Boton
                        variante="primario"
                        type="button"
                        disabled={eliminacion.isPending}
                        onClick={() => confirmarEliminacion(imagen.idImagenServicioPublicado)}
                      >
                        {eliminacion.isPending ? 'Eliminando…' : 'Confirmar'}
                      </Boton>
                      <Boton
                        variante="secundario"
                        type="button"
                        onClick={() => setIdAConfirmar(null)}
                      >
                        Cancelar
                      </Boton>
                    </div>
                  </div>
                ) : null}
                <div className={propios.accionesDeFoto}>
                  <Boton
                    variante="fantasma"
                    type="button"
                    onClick={() => mover(posicion, -1)}
                    disabled={posicion === 0 || orden.isPending}
                    aria-label={`Subir la imagen ${posicion + 1} de ${servicio.nombre}`}
                  >
                    Subir
                  </Boton>
                  <Boton
                    variante="fantasma"
                    type="button"
                    onClick={() => mover(posicion, 1)}
                    disabled={posicion === servicio.imagenes.length - 1 || orden.isPending}
                    aria-label={`Bajar la imagen ${posicion + 1} de ${servicio.nombre}`}
                  >
                    Bajar
                  </Boton>
                </div>
                <label
                  className={propios.etiquetaAlternativa}
                  htmlFor={`texto-servicio-${imagen.idImagenServicioPublicado}`}
                >
                  Texto alternativo
                </label>
                <Entrada
                  id={`texto-servicio-${imagen.idImagenServicioPublicado}`}
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
            );
          })}
        </ul>
      )}

      <div className={zona.campoAsistente}>
        <label className={zona.etiquetaAsistente} htmlFor={`${identificador}-texto`}>
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
        <p className={zona.pistaAsistente} id={`${identificador}-pista`}>
          Describe brevemente lo que se ve, para quien no puede verla.
        </p>

        <label className={zona.etiquetaAsistente} htmlFor={identificador}>
          Agregar imagen
        </label>
        <input
          id={identificador}
          ref={entradaDeArchivo}
          type="file"
          multiple
          accept="image/jpeg,image/png,image/webp"
          className={zona.entradaOculta}
          onChange={(evento) => {
            void subirArchivos(evento.target.files);
          }}
          disabled={subida.isPending}
        />
        <div
          className={unirClases(
            zona.zonaDeSubida,
            arrastrando ? zona.zonaDeSubidaArrastrando : undefined
          )}
          role="button"
          tabIndex={0}
          onClick={() => entradaDeArchivo.current?.click()}
          onKeyDown={(evento) => {
            if (evento.key === 'Enter' || evento.key === ' ') {
              evento.preventDefault();
              entradaDeArchivo.current?.click();
            }
          }}
          onDragOver={(evento) => {
            evento.preventDefault();
            setArrastrando(true);
          }}
          onDragLeave={() => setArrastrando(false)}
          onDrop={(evento) => {
            evento.preventDefault();
            setArrastrando(false);
            void subirArchivos(evento.dataTransfer.files);
          }}
          aria-label="Subir fotos: haz clic o arrastra imágenes aquí"
        >
          <IconoSubida className={zona.iconoDeZona} />
          <p className={zona.tituloDeZona}>Añadir fotos</p>
          <p className={zona.pistaDeZona}>
            Arrastra tus imágenes aquí o haz clic para buscarlas (JPG, PNG o WebP, máx. 5 MB)
          </p>
          <Boton
            type="button"
            variante="contorno"
            className={zona.botonExaminar}
            onClick={(evento) => {
              evento.stopPropagation();
              entradaDeArchivo.current?.click();
            }}
          >
            Explorar archivos
          </Boton>
        </div>
        {previsualizacion !== null && (
          <img
            className={propios.previsualizacion}
            src={previsualizacion}
            alt="Previsualización de la imagen que se está subiendo"
          />
        )}
        {subida.isPending && (
          <p className={propios.estadoDeCarga} role="status">
            Subiendo la imagen…
          </p>
        )}
      </div>
    </section>
  );
}

function validarArchivos(lista: FileList): { validos: File[]; error: string | undefined } {
  const validos: File[] = [];
  let error: string | undefined;

  Array.from(lista).forEach((archivo) => {
    if (!FORMATOS_VALIDOS.includes(archivo.type)) {
      error = 'Solo se admiten formatos JPG, PNG o WebP.';
      return;
    }
    if (archivo.size > TAMANO_MAXIMO) {
      error = 'Las imágenes no deben superar los 5 MB.';
      return;
    }
    validos.push(archivo);
  });

  return { validos, error };
}

function mensajeDe(error: unknown): string | null {
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return error instanceof Error ? error.message : null;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
