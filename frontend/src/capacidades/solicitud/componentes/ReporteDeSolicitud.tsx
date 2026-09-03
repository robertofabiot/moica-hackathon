import { zodResolver } from '@hookform/resolvers/zod';
import { useId, useState } from 'react';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../../../comun/api';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { esquemaDeReporte, type CamposDeReporte } from '../esquemas';
import { useEnvioDeReporte, useReporte } from '../hooks/useReporte';
import { admiteReporte, fechaVisible, nombreDelEstadoDeCaso } from '../presentacion';
import type { DatosDeCasoModeracion, DatosDeSolicitudServicio } from '../tipos';
import propios from './reporte.module.css';

/** Topes de la aplicación, los mismos que valida `ReporteAPresentar` en el backend. */
const MAXIMO_MOTIVO = 120;
const MAXIMO_DESCRIPCION = 3000;

/**
 * El reporte de una solicitud y el caso que abre, dentro de su detalle.
 *
 * No se pinta en una solicitud que nunca llegó a aceptarse: no hay trato del que reportar, y
 * ofrecer el botón sería ofrecer una acción falsa. Después, muestra el formulario, o el resumen del
 * caso propio si esta persona ya reportó. Ocultar el formulario no autoriza nada: el backend vuelve
 * a decidir quién puede reportar en cada petición.
 *
 * Una cuenta restringida conserva el reporte, al contrario que calificar o contratar: es la vía por
 * la que alguien pide ayuda, y quitársela justo a quien ya arrastra una restricción la dejaría sin
 * recurso frente a la contraparte.
 */
export default function ReporteDeSolicitud({ solicitud }: { solicitud: DatosDeSolicitudServicio }) {
  const reportable = admiteReporte(solicitud);
  const estado = useReporte(solicitud.idSolicitudServicio, reportable);

  if (!reportable) {
    return null;
  }

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-reporte">
      <h2 className={secciones.tituloDeSeccion} id="titulo-reporte">
        Reportar un problema
      </h2>

      {estado.isPending ? (
        <p className={secciones.estado} role="status">
          Cargando el estado del reporte…
        </p>
      ) : null}

      {estado.isError ? (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {estado.error instanceof ErrorDeApi
            ? estado.error.message
            : 'No pudimos cargar el estado del reporte.'}{' '}
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
        estado.data.casoAbierto !== null ? (
          <ResumenDelCaso caso={estado.data.casoAbierto} />
        ) : (
          <Reporte
            idSolicitud={solicitud.idSolicitudServicio}
            nombreReportado={estado.data.nombreReportado}
            puedeReportar={estado.data.puedeReportar}
          />
        )
      ) : null}
    </section>
  );
}

/** El caso que esta persona ya abrió. No se edita ni se retira, y no se presenta dos veces. */
function ResumenDelCaso({ caso }: { caso: DatosDeCasoModeracion }) {
  return (
    <div className={propios.resumen}>
      <p className={secciones.explicacion}>
        {`Reportaste a ${caso.nombreReportado}. Tu caso quedó abierto para revisión.`}
      </p>
      <p>
        <span className={secciones.etiquetaDeEstado}>
          {nombreDelEstadoDeCaso(caso.estadoActual)}
        </span>
      </p>
      <dl className={propios.expediente}>
        <dt className={propios.termino}>Motivo</dt>
        <dd className={propios.definicion}>{caso.motivo}</dd>
        <dt className={propios.termino}>Descripción</dt>
        <dd className={`${propios.definicion} ${propios.textoLargo}`}>{caso.descripcion}</dd>
        <dt className={propios.termino}>Fecha de apertura</dt>
        <dd className={propios.definicion}>{fechaVisible(caso.fechaApertura)}</dd>
      </dl>
      <p className={secciones.explicacion}>
        Una persona del equipo revisará lo que enviaste. Un reporte no se edita ni se retira, y solo
        se presenta uno por solicitud.
      </p>
    </div>
  );
}

/** El formulario, o la explicación de por qué la acción ya no está disponible. */
function Reporte({
  idSolicitud,
  nombreReportado,
  puedeReportar,
}: {
  idSolicitud: number;
  nombreReportado: string;
  puedeReportar: boolean;
}) {
  const [abierto, setAbierto] = useState(false);

  if (!puedeReportar) {
    return (
      <p className={secciones.explicacion} role="status">
        Esta solicitud ya no admite tu reporte.
      </p>
    );
  }

  if (!abierto) {
    return (
      <div className={propios.invitacion}>
        <p className={secciones.explicacion}>
          {`Si algo salió mal con ${nombreReportado}, puedes reportarlo.`} Se abrirá un caso para
          que una persona del equipo lo revise. Reportar no sanciona automáticamente a nadie ni
          cambia el estado de esta solicitud.
        </p>
        <button
          className={secciones.botonSecundario}
          type="button"
          onClick={() => setAbierto(true)}
        >
          Reportar un problema
        </button>
      </div>
    );
  }

  return (
    <Formulario
      idSolicitud={idSolicitud}
      nombreReportado={nombreReportado}
      alCancelar={() => setAbierto(false)}
    />
  );
}

function Formulario({
  idSolicitud,
  nombreReportado,
  alCancelar,
}: {
  idSolicitud: number;
  nombreReportado: string;
  alCancelar: () => void;
}) {
  const envio = useEnvioDeReporte(idSolicitud);
  const idDelMotivo = useId();
  const idDeLaDescripcion = useId();
  const idDelContador = useId();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeReporte>({
    resolver: zodResolver(esquemaDeReporte),
    defaultValues: { motivo: '', descripcion: '' },
  });

  // El contador vive en su propio estado y no en `watch()`: esa función de React
  // Hook Form no se puede memoizar sin arriesgar una interfaz obsoleta, y aquí
  // basta con contar lo que se escribe en un solo campo.
  const [caracteres, setCaracteres] = useState(0);
  const campoDeDescripcion = register('descripcion');
  const falloAlEnviar = envio.error;
  // Confirmado cuenta tambien como en curso: entre el 201 y el estado nuevo la
  // consulta todavia dice que no hay caso, asi que el formulario seguiria en
  // pantalla con los campos rehabilitados. Rehabilitarlo ahi invita a un
  // segundo envio que solo puede acabar en conflicto.
  const enCurso = envio.isPending || envio.isSuccess;

  return (
    <form
      className={estilos.formulario}
      // Nada se limpia hasta que el backend confirma: si falla la red, el motivo
      // y la descripción siguen ahí para reintentar sin volver a escribirlos.
      onSubmit={(evento) =>
        void handleSubmit((campos) => {
          if (enCurso) {
            return;
          }
          envio.mutate({ motivo: campos.motivo.trim(), descripcion: campos.descripcion.trim() });
        })(evento)
      }
      noValidate
    >
      <p className={secciones.explicacion}>
        {`Reportas a ${nombreReportado}.`} Se abrirá un caso de moderación para que una persona del
        equipo lo revise. Reportar no sanciona automáticamente a nadie, no cambia el estado de la
        solicitud y no se puede deshacer.
      </p>

      {falloAlEnviar !== null ? (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {falloAlEnviar instanceof ErrorDeApi
            ? falloAlEnviar.message
            : 'No pudimos abrir el caso. Lo que escribiste sigue aquí.'}
        </p>
      ) : null}

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={idDelMotivo}>
          Motivo
        </label>
        <input
          className={claseDeEntrada(errors.motivo !== undefined)}
          id={idDelMotivo}
          type="text"
          maxLength={MAXIMO_MOTIVO}
          disabled={enCurso}
          aria-invalid={errors.motivo !== undefined}
          aria-describedby={errors.motivo === undefined ? undefined : `${idDelMotivo}-error`}
          {...register('motivo')}
        />
        {errors.motivo !== undefined && (
          <p className={estilos.error} id={`${idDelMotivo}-error`} role="alert">
            {errors.motivo.message}
          </p>
        )}
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={idDeLaDescripcion}>
          Descripción
        </label>
        <textarea
          className={`${claseDeEntrada(errors.descripcion !== undefined)} ${propios.area}`}
          id={idDeLaDescripcion}
          rows={4}
          maxLength={MAXIMO_DESCRIPCION}
          disabled={enCurso}
          aria-invalid={errors.descripcion !== undefined}
          // El contador va siempre en la descripcion: con dos campos de texto,
          // uno suelto al pie no dice de cual habla.
          aria-describedby={
            errors.descripcion === undefined
              ? idDelContador
              : `${idDeLaDescripcion}-error ${idDelContador}`
          }
          {...campoDeDescripcion}
          onChange={(evento) => {
            setCaracteres(evento.target.value.trim().length);
            void campoDeDescripcion.onChange(evento);
          }}
        />
        {errors.descripcion !== undefined && (
          <p className={estilos.error} id={`${idDeLaDescripcion}-error`} role="alert">
            {errors.descripcion.message}
          </p>
        )}
      </div>

      <div className={propios.pie}>
        <span className={propios.contador} id={idDelContador}>
          {caracteres} de {MAXIMO_DESCRIPCION} caracteres
        </span>
        <div className={propios.acciones}>
          <button className={estilos.boton} type="submit" disabled={enCurso}>
            {enCurso ? 'Enviando…' : 'Enviar reporte'}
          </button>
          <button
            className={secciones.botonSecundario}
            type="button"
            disabled={enCurso}
            onClick={alCancelar}
          >
            Volver
          </button>
        </div>
      </div>
    </form>
  );
}
