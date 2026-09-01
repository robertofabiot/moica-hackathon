import { useId, useState, type FormEvent } from 'react';

import { useSesionActual } from '../../auth';
import { ErrorDeApi } from '../../../comun/api';
import { IconoEstrella } from '../../../comun/componentes/ui';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useCalificacion, useEnvioDeCalificacion } from '../hooks/useCalificacion';
import { admiteCalificacion, cuentaEstaActiva, fechaVisible, nombreDelRol } from '../presentacion';
import type { DatosDeCalificacion, DatosDeSolicitudServicio, RolCalificado } from '../tipos';
import propios from './calificacion.module.css';

/** Tope de la aplicación, el mismo que valida `CalificacionAEmitir` en el backend. */
const MAXIMO_CARACTERES = 2000;

const PUNTUACIONES = [1, 2, 3, 4, 5] as const;

/**
 * La calificación de una solicitud completada, dentro de su detalle.
 *
 * No se pinta antes de `COMPLETADA`: hasta entonces no hay nada que calificar. Después, muestra el
 * formulario, o el resumen inmutable si esta persona ya calificó, o la explicación de por qué la
 * acción no está disponible. Ocultar el formulario no autoriza nada: el backend vuelve a decidir
 * quién puede calificar en cada petición.
 *
 * Calificar es opcional y no hacerlo no penaliza; el texto lo dice sin rodeos para que nadie
 * suponga que dejarlo en blanco tiene consecuencias.
 */
export default function CalificacionDeSolicitud({
  solicitud,
}: {
  solicitud: DatosDeSolicitudServicio;
}) {
  const sesion = useSesionActual();
  const cuentaActiva = cuentaEstaActiva(sesion.data?.usuario.estadoCuenta);
  const completada = admiteCalificacion(solicitud);
  const estado = useCalificacion(solicitud.idSolicitudServicio, completada);

  if (!completada) {
    return null;
  }

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-calificacion">
      <h2 className={secciones.tituloDeSeccion} id="titulo-calificacion">
        Calificación
      </h2>

      {estado.isPending ? (
        <p className={secciones.estado} role="status">
          Cargando la calificación…
        </p>
      ) : null}

      {estado.isError ? (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {estado.error instanceof ErrorDeApi
            ? estado.error.message
            : 'No pudimos cargar la calificación.'}{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => void estado.refetch()}
          >
            Reintentar
          </button>
        </p>
      ) : null}

      {estado.data !== undefined ? (
        <Contenido
          idSolicitud={solicitud.idSolicitudServicio}
          nombreCalificado={estado.data.nombreCalificado}
          rolCalificado={estado.data.rolCalificado}
          puedeCalificar={estado.data.puedeCalificar}
          cuentaActiva={cuentaActiva}
          emitida={estado.data.calificacionEmitida}
        />
      ) : null}
    </section>
  );
}

function Contenido({
  idSolicitud,
  nombreCalificado,
  rolCalificado,
  puedeCalificar,
  cuentaActiva,
  emitida,
}: {
  idSolicitud: number;
  nombreCalificado: string;
  rolCalificado: RolCalificado;
  puedeCalificar: boolean;
  cuentaActiva: boolean;
  emitida: DatosDeCalificacion | null;
}) {
  if (emitida !== null) {
    return <Resumen emitida={emitida} nombreCalificado={nombreCalificado} />;
  }

  if (!cuentaActiva) {
    return (
      <p className={secciones.explicacion} role="status">
        Tu cuenta está restringida: por ahora no puedes calificar. Cuando vuelva a estar activa
        podrás hacerlo.
      </p>
    );
  }

  if (!puedeCalificar) {
    return (
      <p className={secciones.explicacion} role="status">
        Esta solicitud ya no admite tu calificación.
      </p>
    );
  }

  return (
    <Formulario
      idSolicitud={idSolicitud}
      nombreCalificado={nombreCalificado}
      rolCalificado={rolCalificado}
    />
  );
}

/** Lo que esta persona calificó. Es definitivo: en el MVP no se edita ni se borra. */
function Resumen({
  emitida,
  nombreCalificado,
}: {
  emitida: DatosDeCalificacion;
  nombreCalificado: string;
}) {
  return (
    <div className={propios.resumen}>
      <p className={secciones.explicacion}>
        {`Calificaste a ${nombreCalificado} como ${nombreDelRol(emitida.rolCalificado)}.`}
      </p>
      <p
        className={propios.puntuacionEmitida}
        aria-label={etiquetaDePuntuacion(emitida.puntuacion)}
      >
        {PUNTUACIONES.map((valor) => (
          <IconoEstrella
            key={valor}
            className={valor <= emitida.puntuacion ? propios.estrellaLlena : propios.estrellaVacia}
            aria-hidden="true"
          />
        ))}
      </p>
      {emitida.comentario !== null ? (
        <p className={propios.comentarioEmitido}>{emitida.comentario}</p>
      ) : (
        <p className={secciones.explicacion}>Sin comentario.</p>
      )}
      <p className={secciones.metadatoDelElemento}>{fechaVisible(emitida.fechaCreacion)}</p>
      <p className={secciones.explicacion}>Las calificaciones no se editan ni se borran.</p>
    </div>
  );
}

function Formulario({
  idSolicitud,
  nombreCalificado,
  rolCalificado,
}: {
  idSolicitud: number;
  nombreCalificado: string;
  rolCalificado: RolCalificado;
}) {
  const envio = useEnvioDeCalificacion(idSolicitud);
  const [puntuacion, setPuntuacion] = useState<number | null>(null);
  const [comentario, setComentario] = useState('');
  const grupoDeEstrellas = useId();
  const idDelComentario = useId();

  const falloAlEnviar = envio.error;

  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (puntuacion === null || envio.isPending) {
      return;
    }
    const texto = comentario.trim();
    // Nada se limpia hasta que el backend confirma: si falla la red, la
    // puntuación y el comentario siguen ahí para reintentar sin rehacerlos.
    envio.mutate({ puntuacion, comentario: texto === '' ? null : texto });
  }

  return (
    <form className={propios.formulario} onSubmit={enviar} noValidate>
      <p className={secciones.explicacion}>
        {`Calificas a ${nombreCalificado} como ${nombreDelRol(rolCalificado)}.`} Calificar es
        opcional: si no lo haces, no pasa nada ni se penaliza a nadie.
      </p>

      {falloAlEnviar !== null ? (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {falloAlEnviar instanceof ErrorDeApi
            ? falloAlEnviar.message
            : 'No pudimos guardar la calificación. Lo que escribiste sigue aquí.'}
        </p>
      ) : null}

      {/*
        Un grupo de radios nativo: las flechas del teclado recorren las cinco
        opciones sin código propio y cada una tiene su nombre accesible escrito,
        de modo que la puntuación no depende del color ni de la posición.
      */}
      <fieldset className={propios.grupoDeEstrellas}>
        <legend className={estilos.etiqueta}>Puntuación</legend>
        <div className={propios.estrellas}>
          {PUNTUACIONES.map((valor) => (
            <label key={valor} className={propios.opcion}>
              <input
                className={propios.radio}
                type="radio"
                name={grupoDeEstrellas}
                value={valor}
                checked={puntuacion === valor}
                disabled={envio.isPending}
                onChange={() => setPuntuacion(valor)}
              />
              <IconoEstrella
                className={
                  puntuacion !== null && valor <= puntuacion
                    ? propios.estrellaLlena
                    : propios.estrellaVacia
                }
                aria-hidden="true"
              />
              <span className={propios.textoDeOpcion}>{etiquetaDePuntuacion(valor)}</span>
            </label>
          ))}
        </div>
      </fieldset>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={idDelComentario}>
          Comentario (opcional)
        </label>
        <textarea
          className={`${claseDeEntrada(false)} ${propios.area}`}
          id={idDelComentario}
          rows={3}
          maxLength={MAXIMO_CARACTERES}
          value={comentario}
          disabled={envio.isPending}
          onChange={(evento) => setComentario(evento.target.value)}
        />
      </div>

      <div className={propios.pie}>
        <span className={propios.contador}>
          {comentario.trim().length} de {MAXIMO_CARACTERES} caracteres
        </span>
        <button
          className={estilos.boton}
          type="submit"
          disabled={envio.isPending || puntuacion === null}
        >
          {envio.isPending ? 'Guardando…' : 'Guardar calificación'}
        </button>
      </div>
    </form>
  );
}

function etiquetaDePuntuacion(puntuacion: number): string {
  return puntuacion === 1 ? '1 estrella' : `${puntuacion} estrellas`;
}
