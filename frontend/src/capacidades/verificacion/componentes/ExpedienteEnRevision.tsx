import { useState } from 'react';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, IconoDocumento } from '../../../comun/componentes/ui';
import { rutaDeAccesoADocumento } from '../api';
import {
  fechaLegible,
  nombreDelEstado,
  nombreDelNivelSolicitado,
  nombreDelTipoDeDocumento,
  tamanoLegible,
} from '../etiquetas';
import {
  useAprobacionDeExpediente,
  useRechazoDeExpediente,
  useRevocacionDeExpediente,
  useTomaDeExpediente,
} from '../hooks/useRevisionDeVerificaciones';
import type { EstadoDeSolicitud, Expediente } from '../tipos';
import InsigniaDeVerificacion from './InsigniaDeVerificacion';
import ResolucionConMotivo from './ResolucionConMotivo';
import revision from '../paginas/revision.module.css';

/** Qué formulario de motivo está abierto, si hay alguno. */
type Resolucion = 'RECHAZO' | 'REVOCACION' | null;

/**
 * Un expediente abierto para revisarlo: quién lo presenta, qué adjuntó y qué se puede hacer.
 *
 * Las acciones que se ofrecen dependen del estado y de quién tiene la revisión, igual que las
 * reglas del backend. Ocultar un botón no autoriza nada —quien llame a la API sin permiso recibe
 * 403 o 409 igualmente—, pero evita proponer algo que va a fallar.
 *
 * Los documentos se abren por un enlace a Moica, no a una dirección del proveedor: el backend
 * comprueba los permisos en esa misma petición y responde con un acceso temporal que caduca solo.
 */
export default function ExpedienteEnRevision({
  expediente,
  idAdministrador,
  alResolver,
}: {
  expediente: Expediente;
  idAdministrador: number | null;
  alResolver: () => void;
}) {
  const toma = useTomaDeExpediente();
  const aprobacion = useAprobacionDeExpediente();
  const rechazo = useRechazoDeExpediente();
  const revocacion = useRevocacionDeExpediente();
  const [resolviendo, setResolviendo] = useState<Resolucion>(null);

  const id = expediente.idSolicitudVerificacion;
  const enCurso =
    toma.isPending || aprobacion.isPending || rechazo.isPending || revocacion.isPending;
  const laRevisoYo =
    idAdministrador !== null && expediente.idAdministradorRevisor === idAdministrador;
  const mensajeDeError =
    mensajeDe(toma.error) ??
    mensajeDe(aprobacion.error) ??
    mensajeDe(rechazo.error) ??
    mensajeDe(revocacion.error);

  const terminar = () => {
    setResolviendo(null);
    alResolver();
  };

  return (
    <article className={revision.expediente} aria-labelledby={`expediente-${id}`}>
      <h3 className={revision.tituloDeExpediente} id={`expediente-${id}`}>
        {expediente.prestador.nombrePublico}
      </h3>

      <p className={revision.encabezadoDelNivel}>
        <span
          className={`${revision.pildoraDeEstado} ${claseDePildora(expediente.estadoSolicitud)}`}
        >
          {nombreDelEstado(expediente.estadoSolicitud)}
        </span>
        <span className={revision.pildoraDeNivel}>
          {nombreDelNivelSolicitado(expediente.nivelSolicitado)}
        </span>
        <InsigniaDeVerificacion nivel={expediente.prestador.nivelVerificacion} />
      </p>

      <dl className={revision.datos}>
        <div>
          <dt className={revision.metadato}>Cuenta</dt>
          <dd className={revision.dato}>
            {expediente.prestador.nombreCompleto} · {expediente.prestador.correoElectronico}
          </dd>
        </div>
        <div>
          <dt className={revision.metadato}>Enviada</dt>
          <dd className={revision.dato}>{fechaLegible(expediente.fechaSolicitud)}</dd>
        </div>
      </dl>

      {expediente.observacionResolucion !== null && (
        <p className={revision.motivoRegistrado}>
          <strong>Motivo registrado:</strong> {expediente.observacionResolucion}
        </p>
      )}

      {mensajeDeError !== null && (
        <p className={revision.avisoDeError} role="alert">
          {mensajeDeError}
        </p>
      )}

      <h4 className={revision.subtitulo}>Expediente</h4>
      <ul className={revision.visorDeDocumentos}>
        {expediente.documentos.map((documento) => (
          <li className={revision.tarjetaDeDocumento} key={documento.idDocumentoVerificacion}>
            <div className={revision.cabeceraDeDocumento}>
              <span className={revision.iconoDeDocumento} aria-hidden="true">
                <IconoDocumento />
              </span>
              <div className={revision.cuerpoDeDocumento}>
                <p className={revision.tituloDeDocumento}>
                  {nombreDelTipoDeDocumento(documento.tipoDocumento)}
                </p>
                <p className={revision.nombreDeDocumento}>{documento.nombreOriginal}</p>
                <p className={revision.tamanoDeDocumento}>{tamanoLegible(documento.tamanoBytes)}</p>
              </div>
            </div>
            <a
              className={revision.enlaceDeDocumento}
              href={rutaDeAccesoADocumento(id, documento.idDocumentoVerificacion)}
              target="_blank"
              rel="noopener noreferrer"
            >
              Abrir {documento.nombreOriginal}
            </a>
          </li>
        ))}
      </ul>

      {resolviendo === 'RECHAZO' && (
        <ResolucionConMotivo
          titulo="Rechazar la solicitud"
          textoDeAccion="Rechazar"
          enCurso={rechazo.isPending}
          alConfirmar={(observacion) =>
            rechazo.mutate({ idSolicitudVerificacion: id, observacion }, { onSuccess: terminar })
          }
          alCancelar={() => setResolviendo(null)}
        />
      )}

      {resolviendo === 'REVOCACION' && (
        <ResolucionConMotivo
          titulo="Revocar la verificación"
          advertencia={advertenciaDeRevocacion(expediente)}
          textoDeAccion="Sí, revocar la verificación"
          exigeConfirmacion
          textoDeConfirmacion="Entiendo que este perfil perderá su insignia."
          enCurso={revocacion.isPending}
          alConfirmar={(observacion) =>
            revocacion.mutate({ idSolicitudVerificacion: id, observacion }, { onSuccess: terminar })
          }
          alCancelar={() => setResolviendo(null)}
        />
      )}

      {resolviendo === null && (
        <div className={revision.accionesDeResolucion}>
          {expediente.estadoSolicitud === 'PENDIENTE' && (
            <Boton
              className={revision.botonConfianza}
              type="button"
              disabled={enCurso}
              onClick={() => toma.mutate(id, { onSuccess: alResolver })}
            >
              {toma.isPending ? 'Tomando…' : 'Tomar para revisar'}
            </Boton>
          )}

          {expediente.estadoSolicitud === 'EN_REVISION' && laRevisoYo && (
            <>
              <Boton
                className={revision.botonConfianza}
                type="button"
                disabled={enCurso}
                onClick={() => aprobacion.mutate(id, { onSuccess: alResolver })}
              >
                {aprobacion.isPending ? 'Aprobando…' : 'Aprobar'}
              </Boton>
              <Boton
                className={revision.botonPeligro}
                type="button"
                disabled={enCurso}
                onClick={() => setResolviendo('RECHAZO')}
              >
                Rechazar con motivo
              </Boton>
            </>
          )}

          {expediente.estadoSolicitud === 'EN_REVISION' && !laRevisoYo && (
            <p className={revision.explicacion} role="status">
              Esta solicitud la está revisando otra persona administradora. Solo quien la tomó puede
              resolverla.
            </p>
          )}

          {expediente.estadoSolicitud === 'APROBADA' && (
            <Boton
              className={revision.botonPeligro}
              type="button"
              disabled={enCurso}
              onClick={() => setResolviendo('REVOCACION')}
            >
              Revocar con motivo
            </Boton>
          )}
        </div>
      )}
    </article>
  );
}

function advertenciaDeRevocacion(expediente: Expediente): string {
  if (expediente.nivelSolicitado === 'BASICA') {
    return (
      'Revocar la verificación básica devuelve el perfil a «Sin verificar»: deja de poder aparecer' +
      ' públicamente y, si tenía la profesional, esa también queda sin efecto en la misma' +
      ' operación. Recuperarlas exigirá solicitudes nuevas.'
    );
  }
  return (
    'Revocar la verificación profesional deja el perfil en «Verificado Básico». Recuperar la' +
    ' insignia profesional exigirá una solicitud nueva.'
  );
}

function mensajeDe(error: unknown): string | null {
  if (error instanceof ErrorDeApi) {
    return error.message;
  }
  return error instanceof Error ? error.message : null;
}

function claseDePildora(estado: EstadoDeSolicitud): string | undefined {
  switch (estado) {
    case 'PENDIENTE':
      return revision.pildoraPendiente;
    case 'EN_REVISION':
      return revision.pildoraEnRevision;
    case 'APROBADA':
      return revision.pildoraAprobada;
    case 'RECHAZADA':
    case 'REVOCADA':
      return revision.pildoraRechazada;
    default:
      return undefined;
  }
}
