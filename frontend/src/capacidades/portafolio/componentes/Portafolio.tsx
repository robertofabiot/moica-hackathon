import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../../../comun/api';
import { intercambiar } from '../../../comun/listas';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { esquemaDeTrabajo, type CamposDeTrabajo } from '../esquemas';
import {
  useActualizacionDeTrabajo,
  useCreacionDeTrabajo,
  useEliminacionDeTrabajo,
  useOrdenDeTrabajos,
  usePortafolio,
} from '../hooks/usePortafolio';
import type { DatosDeTrabajo, Trabajo } from '../tipos';
import ImagenesDelTrabajo from './ImagenesDelTrabajo';

/**
 * El portafolio: trabajos que el prestador administra a mano.
 *
 * Nada lo llena solo. Cada trabajo lleva título, descripción, una fecha opcional y sus imágenes,
 * y el orden se cambia con botones de subir y bajar, accesibles con teclado.
 */
export default function Portafolio() {
  const portafolio = usePortafolio(true);
  const creacion = useCreacionDeTrabajo();
  const eliminacion = useEliminacionDeTrabajo();
  const orden = useOrdenDeTrabajos();
  const [editando, setEditando] = useState<number | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CamposDeTrabajo>({
    resolver: zodResolver(esquemaDeTrabajo),
    mode: 'onBlur',
    defaultValues: { fechaRealizacion: '' },
  });

  const agregar = handleSubmit((campos) => {
    creacion.mutate(aDatos(campos), {
      onSuccess: () => reset({ titulo: '', descripcion: '', fechaRealizacion: '' }),
    });
  });

  const lista = portafolio.data ?? [];

  const mover = (posicion: number, desplazamiento: number) => {
    const reordenada = intercambiar(lista, posicion, posicion + desplazamiento);
    if (reordenada === null) {
      return;
    }
    orden.mutate(reordenada.map((trabajo) => trabajo.idTrabajo));
  };

  const mensajeDeError =
    mensajeDe(creacion.error) ?? mensajeDe(eliminacion.error) ?? mensajeDe(orden.error);

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-portafolio">
      <h2 className={secciones.tituloDeSeccion} id="titulo-portafolio">
        Portafolio
      </h2>
      <p className={secciones.explicacion}>
        Agrega los trabajos que quieras mostrar. Tú decides cuáles aparecen y en qué orden.
      </p>

      {mensajeDeError !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      {portafolio.isPending && (
        <p className={secciones.estado} role="status">
          Cargando tu portafolio…
        </p>
      )}

      {portafolio.isError && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          No pudimos cargar tu portafolio.{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => void portafolio.refetch()}
          >
            Reintentar
          </button>
        </p>
      )}

      {portafolio.isSuccess &&
        (lista.length === 0 ? (
          <p className={secciones.vacio}>Todavía no agregaste ningún trabajo.</p>
        ) : (
          <ul className={secciones.lista}>
            {lista.map((trabajo, posicion) => (
              <li className={secciones.elemento} key={trabajo.idTrabajo}>
                {editando === trabajo.idTrabajo ? (
                  <EdicionDeTrabajo trabajo={trabajo} alTerminar={() => setEditando(null)} />
                ) : (
                  <>
                    <h3 className={secciones.tituloDelElemento}>{trabajo.titulo}</h3>
                    {trabajo.fechaRealizacion !== null && (
                      <p className={secciones.metadatoDelElemento}>
                        Realizado el {formatearFecha(trabajo.fechaRealizacion)}
                      </p>
                    )}
                    <p className={secciones.contenidoDelElemento}>{trabajo.descripcion}</p>
                    <div className={secciones.accionesDelElemento}>
                      <button
                        className={secciones.botonPequeno}
                        type="button"
                        onClick={() => mover(posicion, -1)}
                        disabled={posicion === 0 || orden.isPending}
                        aria-label={`Subir el trabajo ${trabajo.titulo}`}
                      >
                        Subir
                      </button>
                      <button
                        className={secciones.botonPequeno}
                        type="button"
                        onClick={() => mover(posicion, 1)}
                        disabled={posicion === lista.length - 1 || orden.isPending}
                        aria-label={`Bajar el trabajo ${trabajo.titulo}`}
                      >
                        Bajar
                      </button>
                      <button
                        className={secciones.botonPequeno}
                        type="button"
                        onClick={() => setEditando(trabajo.idTrabajo)}
                        aria-label={`Editar el trabajo ${trabajo.titulo}`}
                      >
                        Editar
                      </button>
                      <button
                        className={secciones.botonPequeno}
                        type="button"
                        onClick={() => eliminacion.mutate(trabajo.idTrabajo)}
                        disabled={eliminacion.isPending}
                        aria-label={`Quitar el trabajo ${trabajo.titulo}`}
                      >
                        Quitar
                      </button>
                    </div>
                    <ImagenesDelTrabajo trabajo={trabajo} />
                  </>
                )}
              </li>
            ))}
          </ul>
        ))}

      <form className={estilos.formulario} onSubmit={agregar} noValidate>
        <h3 className={secciones.tituloDelElemento}>Agregar un trabajo</h3>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="tituloDelTrabajo">
            Título
          </label>
          <input
            id="tituloDelTrabajo"
            className={claseDeEntrada(errors.titulo !== undefined)}
            type="text"
            aria-invalid={errors.titulo !== undefined}
            aria-describedby={errors.titulo ? 'error-titulo' : undefined}
            {...register('titulo')}
          />
          {errors.titulo && (
            <p className={estilos.error} id="error-titulo">
              {errors.titulo.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="descripcionDelTrabajo">
            Descripción
          </label>
          <textarea
            id="descripcionDelTrabajo"
            className={claseDeEntrada(errors.descripcion !== undefined)}
            rows={4}
            aria-invalid={errors.descripcion !== undefined}
            aria-describedby={errors.descripcion ? 'error-descripcionDelTrabajo' : undefined}
            {...register('descripcion')}
          />
          {errors.descripcion && (
            <p className={estilos.error} id="error-descripcionDelTrabajo">
              {errors.descripcion.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="fechaDelTrabajo">
            Fecha de realización (opcional)
          </label>
          <input
            id="fechaDelTrabajo"
            className={claseDeEntrada(errors.fechaRealizacion !== undefined)}
            type="date"
            aria-invalid={errors.fechaRealizacion !== undefined}
            {...register('fechaRealizacion')}
          />
          {errors.fechaRealizacion && (
            <p className={estilos.error}>{errors.fechaRealizacion.message}</p>
          )}
        </div>

        <button className={estilos.boton} type="submit" disabled={creacion.isPending}>
          {creacion.isPending ? 'Agregando…' : 'Agregar trabajo'}
        </button>
      </form>
    </section>
  );
}

/** Edición en el sitio de un trabajo ya guardado. */
function EdicionDeTrabajo({ trabajo, alTerminar }: { trabajo: Trabajo; alTerminar: () => void }) {
  const actualizacion = useActualizacionDeTrabajo();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeTrabajo>({
    resolver: zodResolver(esquemaDeTrabajo),
    mode: 'onBlur',
    defaultValues: {
      titulo: trabajo.titulo,
      descripcion: trabajo.descripcion,
      fechaRealizacion: trabajo.fechaRealizacion ?? '',
    },
  });

  const guardar = handleSubmit((campos) => {
    actualizacion.mutate(
      { id: trabajo.idTrabajo, datos: aDatos(campos) },
      { onSuccess: alTerminar }
    );
  });

  return (
    <form className={estilos.formulario} onSubmit={guardar} noValidate>
      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={`titulo-${trabajo.idTrabajo}`}>
          Título
        </label>
        <input
          id={`titulo-${trabajo.idTrabajo}`}
          className={claseDeEntrada(errors.titulo !== undefined)}
          type="text"
          aria-invalid={errors.titulo !== undefined}
          {...register('titulo')}
        />
        {errors.titulo && <p className={estilos.error}>{errors.titulo.message}</p>}
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={`descripcion-${trabajo.idTrabajo}`}>
          Descripción
        </label>
        <textarea
          id={`descripcion-${trabajo.idTrabajo}`}
          className={claseDeEntrada(errors.descripcion !== undefined)}
          rows={4}
          aria-invalid={errors.descripcion !== undefined}
          {...register('descripcion')}
        />
        {errors.descripcion && <p className={estilos.error}>{errors.descripcion.message}</p>}
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={`fecha-${trabajo.idTrabajo}`}>
          Fecha de realización (opcional)
        </label>
        <input
          id={`fecha-${trabajo.idTrabajo}`}
          className={claseDeEntrada(errors.fechaRealizacion !== undefined)}
          type="date"
          {...register('fechaRealizacion')}
        />
      </div>

      <div className={secciones.accionesDelElemento}>
        <button className={secciones.botonPequeno} type="submit" disabled={actualizacion.isPending}>
          {actualizacion.isPending ? 'Guardando…' : 'Guardar'}
        </button>
        <button className={secciones.botonPequeno} type="button" onClick={alTerminar}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

/** El campo de fecha vacío viaja como ausente, no como cadena vacía. */
function aDatos(campos: CamposDeTrabajo): DatosDeTrabajo {
  return {
    titulo: campos.titulo,
    descripcion: campos.descripcion,
    fechaRealizacion: campos.fechaRealizacion === '' ? null : campos.fechaRealizacion,
  };
}

function formatearFecha(fecha: string): string {
  // `es-NI` con la fecha en formato ISO: se construye en UTC para que no
  // retroceda un día según la zona horaria del navegador.
  return new Date(`${fecha}T12:00:00Z`).toLocaleDateString('es-NI', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  });
}

function mensajeDe(error: unknown): string | null {
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return error instanceof Error ? error.message : null;
}
