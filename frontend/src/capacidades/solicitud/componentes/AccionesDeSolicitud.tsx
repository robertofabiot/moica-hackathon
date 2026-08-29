import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { useSesionActual } from '../../auth';
import { ErrorDeApi } from '../../../comun/api';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { esquemaDeCancelacion, type CamposDeCancelacion } from '../esquemas';
import {
  useAceptacionDeSolicitud,
  useCancelacionDeSolicitud,
  useCompletadoDeSolicitud,
  useRechazoDeSolicitud,
} from '../hooks/useSolicitudes';
import { cuentaEstaActiva } from '../presentacion';
import type { DatosDeSolicitudServicio } from '../tipos';
import propios from '../paginas/solicitudes.module.css';

/**
 * Acciones contextuales según el actor y el estado. El backend vuelve a autorizar cada una.
 */
export default function AccionesDeSolicitud({
  solicitud,
}: {
  solicitud: DatosDeSolicitudServicio;
}) {
  const sesion = useSesionActual();
  const idUsuario = sesion.data?.usuario.idUsuario;
  const cuentaActiva = cuentaEstaActiva(sesion.data?.usuario.estadoCuenta);
  const esCliente = idUsuario === solicitud.idCliente;
  const esPrestador = idUsuario === solicitud.idPrestador;
  const [pendiente, setPendiente] = useState<
    'aceptar' | 'rechazar' | 'cancelar' | 'completar' | null
  >(null);

  const aceptacion = useAceptacionDeSolicitud();
  const rechazo = useRechazoDeSolicitud();
  const cancelacion = useCancelacionDeSolicitud();
  const completado = useCompletadoDeSolicitud();
  const enCurso =
    aceptacion.isPending || rechazo.isPending || cancelacion.isPending || completado.isPending;
  const fallo = aceptacion.error ?? rechazo.error ?? cancelacion.error ?? completado.error;
  const mensaje =
    fallo instanceof ErrorDeApi ? fallo.message : fallo instanceof Error ? fallo.message : null;

  if (solicitud.estadoActual === 'RECHAZADA' || solicitud.estadoActual === 'CANCELADA') {
    return (
      <p className={secciones.explicacion} role="status">
        Esta solicitud ya está cerrada. Un nuevo intento exige otra solicitud.
      </p>
    );
  }

  if (solicitud.estadoActual === 'COMPLETADA') {
    return (
      <p className={secciones.explicacion} role="status">
        El servicio se marcó como completado. Las calificaciones llegan en un próximo incremento.
      </p>
    );
  }

  if (solicitud.estadoActual === 'PENDIENTE') {
    return (
      <div className={propios.confirmacion}>
        {mensaje !== null && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {mensaje}
          </p>
        )}
        {esPrestador && cuentaActiva && pendiente === 'aceptar' ? (
          <Confirmacion
            titulo="¿Aceptar esta solicitud?"
            advertencia="Al aceptar quedará lista para el chat y los contactos, que se habilitan en el siguiente incremento."
            textoDeAccion="Sí, aceptar"
            enCurso={enCurso}
            alConfirmar={() =>
              aceptacion.mutate(solicitud.idSolicitudServicio, {
                onSuccess: () => setPendiente(null),
              })
            }
            alCancelar={() => setPendiente(null)}
          />
        ) : null}
        {esPrestador && cuentaActiva && pendiente === 'rechazar' ? (
          <Confirmacion
            titulo="¿Rechazar esta solicitud?"
            advertencia="Esta acción no se puede deshacer."
            textoDeAccion="Sí, rechazar"
            enCurso={enCurso}
            alConfirmar={() =>
              rechazo.mutate(solicitud.idSolicitudServicio, {
                onSuccess: () => setPendiente(null),
              })
            }
            alCancelar={() => setPendiente(null)}
          />
        ) : null}
        {esCliente && pendiente === 'cancelar' ? (
          <Confirmacion
            titulo="¿Cancelar esta solicitud?"
            advertencia="El prestador dejará de verla como pendiente. Esta acción no se puede deshacer."
            textoDeAccion="Sí, cancelar"
            enCurso={enCurso}
            alConfirmar={() =>
              cancelacion.mutate(
                { idSolicitud: solicitud.idSolicitudServicio },
                { onSuccess: () => setPendiente(null) }
              )
            }
            alCancelar={() => setPendiente(null)}
          />
        ) : null}
        {pendiente === null && esPrestador && !cuentaActiva ? (
          <p className={secciones.explicacion} role="status">
            Tu cuenta está restringida y por ahora no puede aceptar ni rechazar solicitudes.
          </p>
        ) : null}
        {pendiente === null && (
          <div className={propios.acciones}>
            {esPrestador && cuentaActiva ? (
              <>
                <button
                  className={estilos.boton}
                  type="button"
                  disabled={enCurso}
                  onClick={() => setPendiente('aceptar')}
                >
                  Aceptar
                </button>
                <button
                  className={secciones.botonSecundario}
                  type="button"
                  disabled={enCurso}
                  onClick={() => setPendiente('rechazar')}
                >
                  Rechazar
                </button>
              </>
            ) : null}
            {esCliente ? (
              <button
                className={secciones.botonSecundario}
                type="button"
                disabled={enCurso}
                onClick={() => setPendiente('cancelar')}
              >
                Cancelar solicitud
              </button>
            ) : null}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className={propios.confirmacion}>
      {mensaje !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensaje}
        </p>
      )}
      {esPrestador && cuentaActiva && pendiente === 'completar' ? (
        <Confirmacion
          titulo="¿Marcar como completada?"
          advertencia="Indica que el servicio ya se realizó. Esta acción no se puede deshacer."
          textoDeAccion="Sí, completar"
          enCurso={enCurso}
          alConfirmar={() =>
            completado.mutate(solicitud.idSolicitudServicio, {
              onSuccess: () => setPendiente(null),
            })
          }
          alCancelar={() => setPendiente(null)}
        />
      ) : null}
      {pendiente === 'cancelar' ? (
        <CancelacionConMotivo
          enCurso={enCurso}
          alConfirmar={(motivo) =>
            cancelacion.mutate(
              { idSolicitud: solicitud.idSolicitudServicio, motivo },
              { onSuccess: () => setPendiente(null) }
            )
          }
          alCancelar={() => setPendiente(null)}
        />
      ) : null}
      {pendiente === null && esPrestador && !cuentaActiva ? (
        <p className={secciones.explicacion} role="status">
          Tu cuenta está restringida y por ahora no puede completar solicitudes.
        </p>
      ) : null}
      {pendiente === null && (
        <div className={propios.acciones}>
          {esPrestador && cuentaActiva ? (
            <button
              className={estilos.boton}
              type="button"
              disabled={enCurso}
              onClick={() => setPendiente('completar')}
            >
              Marcar como completada
            </button>
          ) : null}
          {esCliente || esPrestador ? (
            <button
              className={secciones.botonSecundario}
              type="button"
              disabled={enCurso}
              onClick={() => setPendiente('cancelar')}
            >
              Cancelar con motivo
            </button>
          ) : null}
        </div>
      )}
    </div>
  );
}

function Confirmacion({
  titulo,
  advertencia,
  textoDeAccion,
  enCurso,
  alConfirmar,
  alCancelar,
}: {
  titulo: string;
  advertencia: string;
  textoDeAccion: string;
  enCurso: boolean;
  alConfirmar: () => void;
  alCancelar: () => void;
}) {
  return (
    <div className={propios.confirmacion} role="group" aria-label={titulo}>
      <p>
        <strong>{titulo}</strong>
      </p>
      <p className={secciones.explicacion}>{advertencia}</p>
      <div className={propios.acciones}>
        <button className={estilos.boton} type="button" disabled={enCurso} onClick={alConfirmar}>
          {enCurso ? 'Guardando…' : textoDeAccion}
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
  );
}

function CancelacionConMotivo({
  enCurso,
  alConfirmar,
  alCancelar,
}: {
  enCurso: boolean;
  alConfirmar: (motivo: string) => void;
  alCancelar: () => void;
}) {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeCancelacion>({
    resolver: zodResolver(esquemaDeCancelacion),
    defaultValues: { motivo: '' },
  });

  return (
    <form
      className={estilos.formulario}
      onSubmit={(evento) => void handleSubmit((campos) => alConfirmar(campos.motivo))(evento)}
      noValidate
    >
      <p>
        <strong>¿Cancelar esta solicitud aceptada?</strong>
      </p>
      <p className={secciones.explicacion}>
        Debes indicar el motivo. Esta acción no se puede deshacer.
      </p>
      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="motivo-cancelacion">
          Motivo
        </label>
        <textarea
          className={claseDeEntrada(errors.motivo !== undefined)}
          id="motivo-cancelacion"
          rows={3}
          disabled={enCurso}
          aria-invalid={errors.motivo !== undefined}
          aria-describedby={errors.motivo === undefined ? undefined : 'error-motivo-cancelacion'}
          {...register('motivo')}
        />
        {errors.motivo !== undefined && (
          <p className={estilos.error} id="error-motivo-cancelacion" role="alert">
            {errors.motivo.message}
          </p>
        )}
      </div>
      <div className={propios.acciones}>
        <button className={estilos.boton} type="submit" disabled={enCurso}>
          {enCurso ? 'Cancelando…' : 'Confirmar cancelación'}
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
    </form>
  );
}
