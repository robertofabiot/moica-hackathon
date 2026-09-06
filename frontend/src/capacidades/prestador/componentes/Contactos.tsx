import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, Entrada, IconoMensaje } from '../../../comun/componentes/ui';
import { intercambiar } from '../../../comun/listas';
import { esquemaDeContacto, type CamposDeContacto } from '../esquemas';
import {
  useActualizacionDeContacto,
  useContactos,
  useCreacionDeContacto,
  useEliminacionDeContacto,
  useOrdenDeContactos,
} from '../hooks/useContactos';
import propios from '../paginas/prestador.module.css';
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
    <section className={propios.tarjeta} aria-labelledby="titulo-contactos">
      <h2 className={propios.tituloDeTarjeta} id="titulo-contactos">
        Medios de contacto
      </h2>
      <p className={propios.explicacion}>
        Solo tú los ves. Se muestran a un cliente cuando aceptas su solicitud de servicio.
      </p>

      {mensajeDeError !== null && (
        <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
          {mensajeDeError}
        </p>
      )}

      <form className={propios.formulario} onSubmit={agregar} noValidate>
        <div className={propios.campo}>
          <label className={propios.etiqueta} htmlFor="nuevo-contacto">
            Agregar un contacto
          </label>
          <div className={propios.filaDeAlta}>
            <div className={propios.campoFlexible}>
              <Entrada
                id="nuevo-contacto"
                type="text"
                aria-describedby={errors.contenido ? undefined : 'pista-contenido'}
                mensajeDeError={errors.contenido?.message}
                {...register('contenido')}
              />
            </div>
            <Boton type="submit" variante="primario" disabled={creacion.isPending}>
              {creacion.isPending ? 'Agregando…' : 'Agregar contacto'}
            </Boton>
          </div>
          <p className={propios.pista} id="pista-contenido">
            Un teléfono, un correo, un usuario o un enlace. Por ejemplo: WhatsApp 8888-8888.
          </p>
        </div>
      </form>

      {contactos.isPending && (
        <p className={propios.estado} role="status">
          Cargando tus contactos…
        </p>
      )}

      {contactos.isError && (
        <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
          No pudimos cargar tus contactos.{' '}
          <button
            className={propios.enlaceDeTexto}
            type="button"
            onClick={() => void contactos.refetch()}
          >
            Reintentar
          </button>
        </p>
      )}

      {contactos.isSuccess &&
        (lista.length === 0 ? (
          <p className={propios.vacio}>Todavía no agregaste ningún contacto.</p>
        ) : (
          <ul className={propios.listaDeContactos}>
            {lista.map((contacto, posicion) => (
              <li className={propios.tarjetaDeContacto} key={contacto.idMedioContactoPrestador}>
                {editando === contacto.idMedioContactoPrestador ? (
                  <EdicionDeContacto contacto={contacto} alTerminar={() => setEditando(null)} />
                ) : (
                  <>
                    <div className={propios.filaDeContacto}>
                      <span className={propios.iconoDeContacto} aria-hidden="true">
                        <IconoMensaje />
                      </span>
                      <p className={propios.contenidoDeContacto}>{contacto.contenido}</p>
                    </div>
                    <div className={propios.accionesDeFila}>
                      <Boton
                        className={propios.botonCompacto}
                        variante="secundario"
                        type="button"
                        onClick={() => mover(posicion, -1)}
                        disabled={posicion === 0 || orden.isPending}
                        aria-label={`Subir el contacto ${contacto.contenido}`}
                      >
                        Subir
                      </Boton>
                      <Boton
                        className={propios.botonCompacto}
                        variante="secundario"
                        type="button"
                        onClick={() => mover(posicion, 1)}
                        disabled={posicion === lista.length - 1 || orden.isPending}
                        aria-label={`Bajar el contacto ${contacto.contenido}`}
                      >
                        Bajar
                      </Boton>
                      <Boton
                        className={propios.botonCompacto}
                        variante="secundario"
                        type="button"
                        onClick={() => setEditando(contacto.idMedioContactoPrestador)}
                        aria-label={`Editar el contacto ${contacto.contenido}`}
                      >
                        Editar
                      </Boton>
                      <Boton
                        className={propios.botonCompacto}
                        variante="contorno"
                        type="button"
                        onClick={() => eliminacion.mutate(contacto.idMedioContactoPrestador)}
                        disabled={eliminacion.isPending}
                        aria-label={`Quitar el contacto ${contacto.contenido}`}
                      >
                        Quitar
                      </Boton>
                    </div>
                  </>
                )}
              </li>
            ))}
          </ul>
        ))}
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
    <form className={propios.formulario} onSubmit={guardar} noValidate>
      <div className={propios.campo}>
        <label className={propios.etiqueta} htmlFor={identificador}>
          Editar contacto
        </label>
        <Entrada
          id={identificador}
          type="text"
          mensajeDeError={errors.contenido?.message}
          {...register('contenido')}
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

function mensajeDe(error: unknown): string | null {
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return error instanceof Error ? error.message : null;
}
