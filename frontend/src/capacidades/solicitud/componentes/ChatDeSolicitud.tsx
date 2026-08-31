import { useId, useState, type FormEvent } from 'react';

import { useSesionActual } from '../../auth';
import { ErrorDeApi } from '../../../comun/api';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useEnvioDeMensaje, useMensajes } from '../hooks/useChat';
import {
  admiteMensajesNuevos,
  cuentaEstaActiva,
  fechaVisible,
  hiloHabilitado,
} from '../presentacion';
import type { DatosDeSolicitudServicio, MensajeSolicitud } from '../tipos';
import chat from './chat.module.css';

/** Tope de la aplicación, el mismo que valida `MensajeAEnviar` en el backend. */
const MAXIMO_CARACTERES = 2000;

/**
 * El chat de una solicitud, dentro de su detalle.
 *
 * No se pinta hasta que la solicitud fue aceptada alguna vez, y deja de admitir escritura en cuanto
 * se cancela o se completa. Ocultar el formulario no autoriza nada: el backend vuelve a decidir
 * quién lee y quién escribe en cada petición.
 *
 * Vive dentro de la capacidad `solicitud` y no en una capacidad `chat` propia —como sí ocurre en el
 * backend— porque aquí la dependencia va al revés: es el detalle de la solicitud quien monta el
 * chat, y el chat necesita el detalle para saber si el hilo existe. Separarlo dejaría a las dos
 * capacidades importándose entre sí.
 */
export default function ChatDeSolicitud({ solicitud }: { solicitud: DatosDeSolicitudServicio }) {
  const sesion = useSesionActual();
  const idUsuario = sesion.data?.usuario.idUsuario;
  const cuentaActiva = cuentaEstaActiva(sesion.data?.usuario.estadoCuenta);

  const habilitado = hiloHabilitado(solicitud);
  const abierto = admiteMensajesNuevos(solicitud);

  const mensajes = useMensajes(solicitud.idSolicitudServicio, habilitado);
  const envio = useEnvioDeMensaje(solicitud.idSolicitudServicio);
  const [borrador, setBorrador] = useState('');
  const idDelCampo = useId();

  if (!habilitado) {
    return null;
  }

  const hilo = mensajes.data;
  const falloAlCargar = mensajes.isError;
  const falloAlEnviar = envio.error;

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const contenido = borrador.trim();
    if (contenido === '' || envio.isPending) {
      return;
    }
    // El campo se limpia solo cuando el backend confirmó: si falla la red, lo
    // escrito sigue ahí para reintentar sin volver a teclearlo.
    envio.mutate(contenido, { onSuccess: () => setBorrador('') });
  }

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-chat">
      <h2 className={secciones.tituloDeSeccion} id="titulo-chat">
        Mensajes
      </h2>

      {mensajes.isPending ? (
        <p className={secciones.estado} role="status">
          Cargando los mensajes…
        </p>
      ) : null}

      {falloAlCargar ? (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajes.error instanceof ErrorDeApi
            ? mensajes.error.message
            : 'No pudimos cargar los mensajes.'}{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => void mensajes.refetch()}
          >
            Reintentar
          </button>
        </p>
      ) : null}

      {hilo !== undefined && hilo.length === 0 ? (
        <p className={secciones.vacio}>
          Todavía no hay mensajes. Escribe el primero para coordinar el trabajo.
        </p>
      ) : null}

      {hilo !== undefined && hilo.length > 0 ? (
        <div
          className={chat.hilo}
          role="log"
          aria-live="polite"
          aria-label="Mensajes de la solicitud"
          tabIndex={0}
        >
          {hilo.map((mensaje) => (
            <Mensaje
              key={mensaje.idMensajeSolicitud}
              mensaje={mensaje}
              esPropio={mensaje.idRemitente === idUsuario}
            />
          ))}
        </div>
      ) : null}

      {abierto && cuentaActiva ? (
        <form className={chat.formulario} onSubmit={enviar} noValidate>
          {falloAlEnviar !== null ? (
            <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
              {falloAlEnviar instanceof ErrorDeApi
                ? falloAlEnviar.message
                : 'No pudimos enviar el mensaje. Tu texto sigue aquí.'}
            </p>
          ) : null}
          <div className={estilos.campo}>
            <label className={estilos.etiqueta} htmlFor={idDelCampo}>
              Mensaje
            </label>
            <textarea
              className={`${claseDeEntrada(false)} ${chat.area}`}
              id={idDelCampo}
              rows={3}
              maxLength={MAXIMO_CARACTERES}
              value={borrador}
              disabled={envio.isPending}
              onChange={(evento) => setBorrador(evento.target.value)}
            />
          </div>
          <div className={chat.pie}>
            <span className={chat.contador}>
              {borrador.trim().length} de {MAXIMO_CARACTERES} caracteres
            </span>
            <button
              className={estilos.boton}
              type="submit"
              disabled={envio.isPending || borrador.trim() === ''}
            >
              {envio.isPending ? 'Enviando…' : 'Enviar'}
            </button>
          </div>
        </form>
      ) : null}

      {abierto && !cuentaActiva ? (
        <p className={secciones.explicacion} role="status">
          Tu cuenta está restringida: puedes leer el historial, pero por ahora no puedes enviar
          mensajes.
        </p>
      ) : null}

      {!abierto ? (
        <p className={secciones.explicacion} role="status">
          Esta solicitud ya se cerró. El historial queda visible, pero no admite mensajes nuevos.
        </p>
      ) : null}
    </section>
  );
}

function Mensaje({ mensaje, esPropio }: { mensaje: MensajeSolicitud; esPropio: boolean }) {
  return (
    <article className={`${chat.mensaje} ${esPropio ? chat.propio : chat.ajeno}`}>
      <p className={chat.autor}>{esPropio ? 'Tú' : mensaje.nombreRemitente}</p>
      <p className={chat.texto}>{mensaje.contenido}</p>
      <p className={chat.instante}>{fechaVisible(mensaje.fechaEnvio)}</p>
    </article>
  );
}
