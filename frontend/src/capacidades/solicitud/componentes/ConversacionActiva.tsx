import { useEffect, useId, useRef, useState, type FormEvent, type KeyboardEvent } from 'react';
import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, IconoChevronIzquierda, IconoEnviar } from '../../../comun/componentes/ui';
import estilosDeFormulario from '../../../comun/estilos/formulario.module.css';
import { useSesionActual } from '../../auth';
import { useEnvioDeMensaje, useMensajes } from '../hooks/useChat';
import { useSolicitud } from '../hooks/useSolicitudes';
import {
  admiteMensajesNuevos,
  cuentaEstaActiva,
  estadoVisibleDeConversacion,
  hiloHabilitado,
  horaDeMensaje,
  inicialesDeNombre,
  nombreDeContraparte,
} from '../presentacion';
import { rutaDeSolicitud } from '../rutas';
import type { MensajeSolicitud, ResumenDeSolicitudServicio } from '../tipos';
import BurbujaMensaje from './BurbujaMensaje';
import { AvatarDeChat } from './ItemConversacion';
import estilos from './ConversacionActiva.module.css';

/** Tope de la aplicación, el mismo que valida `MensajeAEnviar` en el backend. */
const MAXIMO_CARACTERES = 2000;

type PropiedadesDeConversacionActiva = {
  idSolicitud: number;
  resumen: ResumenDeSolicitudServicio | undefined;
  idUsuario: number | undefined;
  alVolver: () => void;
  alUltimoMensaje?: (idSolicitud: number, mensaje: MensajeSolicitud) => void;
};

/**
 * Hilo de la conversación seleccionada: encabezado, mensajes con autoscroll y envío.
 *
 * No autoriza nada: el backend vuelve a decidir quién lee y quién escribe. Aquí solo se pinta
 * según el detalle y se deja de ofrecer el formulario cuando la solicitud ya no admite mensajes.
 */
export default function ConversacionActiva({
  idSolicitud,
  resumen,
  idUsuario,
  alVolver,
  alUltimoMensaje,
}: PropiedadesDeConversacionActiva) {
  const sesion = useSesionActual();
  const cuentaActiva = cuentaEstaActiva(sesion.data?.usuario.estadoCuenta);
  const detalle = useSolicitud(idSolicitud);
  const solicitud = detalle.data;
  const habilitado = solicitud !== undefined && hiloHabilitado(solicitud);
  const abierto = solicitud !== undefined && admiteMensajesNuevos(solicitud);

  const mensajes = useMensajes(idSolicitud, habilitado);
  const envio = useEnvioDeMensaje(idSolicitud);
  const [borrador, setBorrador] = useState('');
  const idDelCampo = useId();
  const contenedorDelHilo = useRef<HTMLDivElement>(null);
  const totalDeMensajes = mensajes.data?.length ?? 0;

  useEffect(() => {
    const nodo = contenedorDelHilo.current;
    if (nodo !== null) {
      nodo.scrollTop = nodo.scrollHeight;
    }
  }, [totalDeMensajes]);

  useEffect(() => {
    const ultimo = mensajes.data?.at(-1);
    if (ultimo !== undefined && alUltimoMensaje !== undefined) {
      alUltimoMensaje(idSolicitud, ultimo);
    }
  }, [alUltimoMensaje, idSolicitud, mensajes.data]);

  const nombre =
    solicitud !== undefined
      ? nombreDeContraparte(solicitud, idUsuario)
      : resumen !== undefined
        ? nombreDeContraparte(resumen, idUsuario)
        : '';
  const servicio = solicitud?.nombreServicio ?? resumen?.nombreServicio ?? '';
  const estado = solicitud?.estadoActual ?? resumen?.estadoActual;
  const hilo = mensajes.data;
  const falloAlCargar = mensajes.isError || detalle.isError;
  const falloAlEnviar = envio.error;

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const contenido = borrador.trim();
    if (contenido === '' || envio.isPending || !abierto) {
      return;
    }
    envio.mutate(contenido, { onSuccess: () => setBorrador('') });
  }

  function alTecla(evento: KeyboardEvent<HTMLTextAreaElement>) {
    if (evento.key === 'Enter' && !evento.shiftKey) {
      evento.preventDefault();
      evento.currentTarget.form?.requestSubmit();
    }
  }

  return (
    <section className={estilos.panel} aria-labelledby="titulo-conversacion">
      <header className={estilos.encabezado}>
        <button
          type="button"
          className={estilos.volver}
          onClick={alVolver}
          aria-label="Volver a las conversaciones"
        >
          <IconoChevronIzquierda />
        </button>
        <AvatarDeChat nombre={nombre} iniciales={inicialesDeNombre(nombre)} grande />
        <div className={estilos.identidad}>
          <h2 className={estilos.nombre} id="titulo-conversacion">
            {nombre || 'Conversación'}
          </h2>
          <p className={estilos.estado}>
            {estado !== undefined ? estadoVisibleDeConversacion(estado) : servicio}
          </p>
        </div>
        <Link className={estilos.detalle} to={rutaDeSolicitud(idSolicitud)}>
          Ver detalle del servicio
        </Link>
      </header>

      {falloAlCargar ? (
        <p
          className={`${estilosDeFormulario.aviso} ${estilosDeFormulario.avisoDeError} ${estilos.aviso}`}
          role="alert"
        >
          {mensajeDeFallo(detalle.error, mensajes.error)}{' '}
          <button
            className={estilosDeFormulario.enlaceDeTexto}
            type="button"
            onClick={() => {
              void detalle.refetch();
              void mensajes.refetch();
            }}
          >
            Reintentar
          </button>
        </p>
      ) : null}

      <div
        ref={contenedorDelHilo}
        className={estilos.hilo}
        role="log"
        aria-live="polite"
        aria-label="Mensajes de la conversación"
        tabIndex={0}
      >
        {detalle.isPending || (habilitado && mensajes.isPending) ? (
          <p className={estilos.estadoDeHilo} role="status">
            Cargando los mensajes…
          </p>
        ) : null}

        {solicitud !== undefined && !habilitado ? (
          <p className={estilos.estadoDeHilo} role="status">
            Esta solicitud nunca habilitó el chat. El historial no está disponible.
          </p>
        ) : null}

        {hilo !== undefined && hilo.length === 0 ? (
          <p className={estilos.estadoDeHilo} role="status">
            Todavía no hay mensajes. Escribe el primero para coordinar el trabajo.
          </p>
        ) : null}

        {hilo !== undefined
          ? hilo.map((mensaje) => (
              <BurbujaMensaje
                key={mensaje.idMensajeSolicitud}
                contenido={mensaje.contenido}
                instante={horaDeMensaje(mensaje.fechaEnvio)}
                esPropio={mensaje.idRemitente === idUsuario}
              />
            ))
          : null}
      </div>

      {abierto && cuentaActiva ? (
        <form className={estilos.composicion} onSubmit={enviar} noValidate>
          {falloAlEnviar !== null ? (
            <p
              className={`${estilosDeFormulario.aviso} ${estilosDeFormulario.avisoDeError}`}
              role="alert"
            >
              {falloAlEnviar instanceof ErrorDeApi
                ? falloAlEnviar.message
                : 'No pudimos enviar el mensaje. Tu texto sigue aquí.'}
            </p>
          ) : null}
          <div className={estilos.filaDeEnvio}>
            <textarea
              className={estilos.area}
              id={idDelCampo}
              rows={1}
              maxLength={MAXIMO_CARACTERES}
              value={borrador}
              disabled={envio.isPending}
              placeholder="Escribe un mensaje..."
              aria-label="Mensaje"
              onChange={(evento) => setBorrador(evento.target.value)}
              onKeyDown={alTecla}
            />
            <Boton
              className={estilos.enviar}
              type="submit"
              forma="pildora"
              disabled={envio.isPending || borrador.trim() === ''}
              aria-label={envio.isPending ? 'Enviando…' : 'Enviar'}
            >
              <IconoEnviar />
            </Boton>
          </div>
        </form>
      ) : null}

      {abierto && !cuentaActiva ? (
        <p className={estilos.soloLectura} role="status">
          Tu cuenta está restringida: puedes leer el historial, pero por ahora no puedes enviar
          mensajes.
        </p>
      ) : null}

      {!abierto && habilitado ? (
        <p className={estilos.soloLectura} role="status">
          Esta conversación ha finalizado y permanece en solo lectura
        </p>
      ) : null}
    </section>
  );
}

function mensajeDeFallo(detalle: Error | null, mensajes: Error | null): string {
  const error = detalle ?? mensajes;
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return 'No pudimos cargar la conversación.';
}
