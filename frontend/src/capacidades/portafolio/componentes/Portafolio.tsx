import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, Entrada } from '../../../comun/componentes/ui';
import { intercambiar } from '../../../comun/listas';
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
import propios from './portafolio.module.css';

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
    <section className={propios.tarjeta} aria-labelledby="titulo-portafolio">
      <h2 className={propios.titulo} id="titulo-portafolio">
        Portafolio
      </h2>
      <p className={propios.explicacion}>
        Agrega los trabajos que quieras mostrar. Tú decides cuáles aparecen y en qué orden.
      </p>

      {mensajeDeError !== null && (
        <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      <form className={propios.formulario} onSubmit={agregar} noValidate>
        <h3 className={propios.subtitulo}>Agregar un trabajo</h3>

        <div className={propios.campo}>
          <label className={propios.etiqueta} htmlFor="tituloDelTrabajo">
            Título
          </label>
          <Entrada
            id="tituloDelTrabajo"
            type="text"
            mensajeDeError={errors.titulo?.message}
            {...register('titulo')}
          />
        </div>

        <div className={propios.campo}>
          <label className={propios.etiqueta} htmlFor="descripcionDelTrabajo">
            Descripción
          </label>
          <textarea
            id="descripcionDelTrabajo"
            className={unirClases(
              propios.control,
              errors.descripcion !== undefined ? propios.controlConError : undefined
            )}
            rows={4}
            aria-invalid={errors.descripcion !== undefined}
            aria-describedby={errors.descripcion ? 'error-descripcionDelTrabajo' : undefined}
            {...register('descripcion')}
          />
          {errors.descripcion && (
            <p className={propios.error} id="error-descripcionDelTrabajo">
              {errors.descripcion.message}
            </p>
          )}
        </div>

        <div className={propios.campo}>
          <label className={propios.etiqueta} htmlFor="fechaDelTrabajo">
            Fecha de realización (opcional)
          </label>
          <Entrada
            id="fechaDelTrabajo"
            type="date"
            mensajeDeError={errors.fechaRealizacion?.message}
            {...register('fechaRealizacion')}
          />
        </div>

        <div className={propios.accionesDeFormulario}>
          <Boton type="submit" variante="primario" disabled={creacion.isPending}>
            {creacion.isPending ? 'Agregando…' : 'Agregar trabajo'}
          </Boton>
        </div>
      </form>

      {portafolio.isPending && (
        <p className={propios.estado} role="status">
          Cargando tu portafolio…
        </p>
      )}

      {portafolio.isError && (
        <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
          No pudimos cargar tu portafolio.{' '}
          <button
            className={propios.enlaceDeTexto}
            type="button"
            onClick={() => void portafolio.refetch()}
          >
            Reintentar
          </button>
        </p>
      )}

      {portafolio.isSuccess &&
        (lista.length === 0 ? (
          <p className={propios.vacio}>Todavía no agregaste ningún trabajo.</p>
        ) : (
          <ul className={propios.rejillaDeTrabajos}>
            {lista.map((trabajo, posicion) => (
              <li className={propios.tarjetaDeTrabajo} key={trabajo.idTrabajo}>
                {editando === trabajo.idTrabajo ? (
                  <EdicionDeTrabajo trabajo={trabajo} alTerminar={() => setEditando(null)} />
                ) : (
                  <>
                    <h3 className={propios.tituloDelTrabajo}>{trabajo.titulo}</h3>
                    {trabajo.fechaRealizacion !== null && (
                      <p className={propios.metadato}>
                        Realizado el {formatearFecha(trabajo.fechaRealizacion)}
                      </p>
                    )}
                    <p className={propios.descripcionDelTrabajo}>{trabajo.descripcion}</p>
                    <div className={propios.accionesDeFila}>
                      <Boton
                        className={propios.botonCompacto}
                        variante="secundario"
                        type="button"
                        onClick={() => mover(posicion, -1)}
                        disabled={posicion === 0 || orden.isPending}
                        aria-label={`Subir el trabajo ${trabajo.titulo}`}
                      >
                        Subir
                      </Boton>
                      <Boton
                        className={propios.botonCompacto}
                        variante="secundario"
                        type="button"
                        onClick={() => mover(posicion, 1)}
                        disabled={posicion === lista.length - 1 || orden.isPending}
                        aria-label={`Bajar el trabajo ${trabajo.titulo}`}
                      >
                        Bajar
                      </Boton>
                      <Boton
                        className={propios.botonCompacto}
                        variante="secundario"
                        type="button"
                        onClick={() => setEditando(trabajo.idTrabajo)}
                        aria-label={`Editar el trabajo ${trabajo.titulo}`}
                      >
                        Editar
                      </Boton>
                      <Boton
                        className={propios.botonCompacto}
                        variante="contorno"
                        type="button"
                        onClick={() => eliminacion.mutate(trabajo.idTrabajo)}
                        disabled={eliminacion.isPending}
                        aria-label={`Quitar el trabajo ${trabajo.titulo}`}
                      >
                        Quitar
                      </Boton>
                    </div>
                    <ImagenesDelTrabajo trabajo={trabajo} />
                  </>
                )}
              </li>
            ))}
          </ul>
        ))}
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
    <form className={propios.formulario} onSubmit={guardar} noValidate>
      <div className={propios.campo}>
        <label className={propios.etiqueta} htmlFor={`titulo-${trabajo.idTrabajo}`}>
          Título
        </label>
        <Entrada
          id={`titulo-${trabajo.idTrabajo}`}
          type="text"
          mensajeDeError={errors.titulo?.message}
          {...register('titulo')}
        />
      </div>

      <div className={propios.campo}>
        <label className={propios.etiqueta} htmlFor={`descripcion-${trabajo.idTrabajo}`}>
          Descripción
        </label>
        <textarea
          id={`descripcion-${trabajo.idTrabajo}`}
          className={unirClases(
            propios.control,
            errors.descripcion !== undefined ? propios.controlConError : undefined
          )}
          rows={4}
          aria-invalid={errors.descripcion !== undefined}
          {...register('descripcion')}
        />
        {errors.descripcion && <p className={propios.error}>{errors.descripcion.message}</p>}
      </div>

      <div className={propios.campo}>
        <label className={propios.etiqueta} htmlFor={`fecha-${trabajo.idTrabajo}`}>
          Fecha de realización (opcional)
        </label>
        <Entrada
          id={`fecha-${trabajo.idTrabajo}`}
          type="date"
          mensajeDeError={errors.fechaRealizacion?.message}
          {...register('fechaRealizacion')}
        />
      </div>

      <div className={propios.accionesDeFila}>
        <Boton type="submit" variante="primario" disabled={actualizacion.isPending}>
          {actualizacion.isPending ? 'Guardando…' : 'Guardar'}
        </Boton>
        <Boton variante="secundario" type="button" onClick={alTerminar}>
          Cancelar
        </Boton>
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

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
