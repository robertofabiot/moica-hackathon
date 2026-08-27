import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../../../comun/api';
import { intercambiar } from '../../../comun/listas';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import { esquemaDeContacto, type CamposDeContacto } from '../esquemas';
import {
  useActualizacionDeContacto,
  useContactos,
  useCreacionDeContacto,
  useEliminacionDeContacto,
  useOrdenDeContactos,
} from '../hooks/useContactos';
import secciones from '../../../comun/estilos/secciones.module.css';
import type { MedioContacto } from '../tipos';

/**
 * Los medios de contacto del perfil: agregarlos, editarlos, ordenarlos y quitarlos.
 *
 * Son entradas libres —un número, un correo, un usuario o un enlace— porque Moica no las
 * clasifica por plataforma. Para reordenar hay botones de subir y bajar en lugar de arrastrar: se
 * usan con teclado sin depender de ninguna librería.
 */
export default function Contactos() {
  const contactos = useContactos(true);
  const creacion = useCreacionDeContacto();
  const eliminacion = useEliminacionDeContacto();
  const orden = useOrdenDeContactos();
  const [editando, setEditando] = useState<number | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CamposDeContacto>({ resolver: zodResolver(esquemaDeContacto), mode: 'onBlur' });

  const agregar = handleSubmit((campos) => {
    creacion.mutate(campos.contenido, { onSuccess: () => reset({ contenido: '' }) });
  });

  const lista = contactos.data ?? [];

  const mover = (posicion: number, desplazamiento: number) => {
    const reordenada = intercambiar(lista, posicion, posicion + desplazamiento);
    if (reordenada === null) {
      return;
    }
    orden.mutate(reordenada.map((contacto) => contacto.idMedioContactoPrestador));
  };

  const mensajeDeError =
    mensajeDe(creacion.error) ?? mensajeDe(eliminacion.error) ?? mensajeDe(orden.error);

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-contactos">
      <h2 className={secciones.tituloDeSeccion} id="titulo-contactos">
        Medios de contacto
      </h2>
      <p className={secciones.explicacion}>
        Solo tú los ves. Se muestran a un cliente cuando aceptas su solicitud de servicio.
      </p>

      {mensajeDeError !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      {contactos.isPending && (
        <p className={secciones.estado} role="status">
          Cargando tus contactos…
        </p>
      )}

      {contactos.isError && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          No pudimos cargar tus contactos.{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => void contactos.refetch()}
          >
            Reintentar
          </button>
        </p>
      )}

      {contactos.isSuccess &&
        (lista.length === 0 ? (
          <p className={secciones.vacio}>Todavía no agregaste ningún contacto.</p>
        ) : (
          <ul className={secciones.lista}>
            {lista.map((contacto, posicion) => (
              <li className={secciones.elemento} key={contacto.idMedioContactoPrestador}>
                {editando === contacto.idMedioContactoPrestador ? (
                  <EdicionDeContacto contacto={contacto} alTerminar={() => setEditando(null)} />
                ) : (
                  <>
                    <p className={secciones.contenidoDelElemento}>{contacto.contenido}</p>
                    <div className={secciones.accionesDelElemento}>
                      <button
                        className={secciones.botonPequeno}
                        type="button"
                        onClick={() => mover(posicion, -1)}
                        disabled={posicion === 0 || orden.isPending}
                        aria-label={`Subir el contacto ${contacto.contenido}`}
                      >
                        Subir
                      </button>
                      <button
                        className={secciones.botonPequeno}
                        type="button"
                        onClick={() => mover(posicion, 1)}
                        disabled={posicion === lista.length - 1 || orden.isPending}
                        aria-label={`Bajar el contacto ${contacto.contenido}`}
                      >
                        Bajar
                      </button>
                      <button
                        className={secciones.botonPequeno}
                        type="button"
                        onClick={() => setEditando(contacto.idMedioContactoPrestador)}
                        aria-label={`Editar el contacto ${contacto.contenido}`}
                      >
                        Editar
                      </button>
                      <button
                        className={secciones.botonPequeno}
                        type="button"
                        onClick={() => eliminacion.mutate(contacto.idMedioContactoPrestador)}
                        disabled={eliminacion.isPending}
                        aria-label={`Quitar el contacto ${contacto.contenido}`}
                      >
                        Quitar
                      </button>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ul>
        ))}

      <form className={estilos.formulario} onSubmit={agregar} noValidate>
        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="contenidoDeContacto">
            Agregar un contacto
          </label>
          <input
            id="contenidoDeContacto"
            className={claseDeEntrada(errors.contenido !== undefined)}
            type="text"
            aria-invalid={errors.contenido !== undefined}
            aria-describedby={errors.contenido ? 'error-contenido' : 'pista-contenido'}
            {...register('contenido')}
          />
          <p className={estilos.pista} id="pista-contenido">
            Un teléfono, un correo, un usuario o un enlace. Por ejemplo: WhatsApp 8888-8888.
          </p>
          {errors.contenido && (
            <p className={estilos.error} id="error-contenido">
              {errors.contenido.message}
            </p>
          )}
        </div>

        <button className={estilos.boton} type="submit" disabled={creacion.isPending}>
          {creacion.isPending ? 'Agregando…' : 'Agregar contacto'}
        </button>
      </form>
    </section>
  );
}

/** Edición en el sitio de un contacto ya guardado. */
function EdicionDeContacto({
  contacto,
  alTerminar,
}: {
  contacto: MedioContacto;
  alTerminar: () => void;
}) {
  const actualizacion = useActualizacionDeContacto();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeContacto>({
    resolver: zodResolver(esquemaDeContacto),
    mode: 'onBlur',
    defaultValues: { contenido: contacto.contenido },
  });

  const guardar = handleSubmit((campos) => {
    actualizacion.mutate(
      { id: contacto.idMedioContactoPrestador, contenido: campos.contenido },
      { onSuccess: alTerminar }
    );
  });

  const identificador = `contacto-${contacto.idMedioContactoPrestador}`;

  return (
    <form className={estilos.formulario} onSubmit={guardar} noValidate>
      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={identificador}>
          Editar contacto
        </label>
        <input
          id={identificador}
          className={claseDeEntrada(errors.contenido !== undefined)}
          type="text"
          aria-invalid={errors.contenido !== undefined}
          {...register('contenido')}
        />
        {errors.contenido && <p className={estilos.error}>{errors.contenido.message}</p>}
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

function mensajeDe(error: unknown): string | null {
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return error instanceof Error ? error.message : null;
}
