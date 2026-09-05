import { ErrorDeApi } from '../../../comun/api';
import {
  fechaLegible,
  nombreDelEstado,
  nombreDelNivelSolicitado,
  nombreDelTipoDeDocumento,
  tamanoLegible,
} from '../etiquetas';
import { useSolicitudesPropias } from '../hooks/useVerificacion';
import type { EstadoDeSolicitud, SolicitudDeVerificacion } from '../tipos';
import propios from './verificacion.module.css';

/**
 * Todas las solicitudes propias, la más reciente primero.
 *
 * Incluye las resueltas a propósito: una solicitud rechazada o revocada se conserva como evidencia,
 * y es donde se lee el motivo que explica qué corregir antes de volver a presentarla.
 *
 * De cada documento se muestran sus metadatos y nada más. El archivo no se puede abrir desde aquí:
 * el binario solo lo ve quien revisa.
 */
export default function HistorialDeVerificacion() {
  const historial = useSolicitudesPropias();

  if (historial.isPending) {
    return (
      <p className={propios.estado} role="status">
        Cargando tus solicitudes…
      </p>
    );
  }

  if (historial.isError) {
    return (
      <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
        {historial.error instanceof ErrorDeApi
          ? historial.error.message
          : 'No pudimos cargar tus solicitudes.'}{' '}
        <button
          className={propios.enlaceDeTexto}
          type="button"
          onClick={() => void historial.refetch()}
        >
          Reintentar
        </button>
      </p>
    );
  }

  if (historial.data.length === 0) {
    return <p className={propios.vacio}>Todavía no has presentado ninguna solicitud.</p>;
  }

  return (
    <ul className={propios.lista}>
      {historial.data.map((solicitud) => (
        <SolicitudDelHistorial key={solicitud.idSolicitudVerificacion} solicitud={solicitud} />
      ))}
    </ul>
  );
}

function SolicitudDelHistorial({ solicitud }: { solicitud: SolicitudDeVerificacion }) {
  return (
    <li className={propios.elemento}>
      <p className={propios.tituloDelElemento}>
        {nombreDelNivelSolicitado(solicitud.nivelSolicitado)}
      </p>
      <p className={propios.estado}>
        <span className={claseDePildora(solicitud.estadoSolicitud)}>
          {nombreDelEstado(solicitud.estadoSolicitud)}
        </span>
      </p>
      <p className={propios.metadatoDelElemento}>
        Enviada el {fechaLegible(solicitud.fechaSolicitud)}
        {solicitud.fechaResolucion !== null &&
          ` · Resuelta el ${fechaLegible(solicitud.fechaResolucion)}`}
      </p>

      {solicitud.observacionResolucion !== null && (
        <p className={propios.motivo}>
          <strong>Motivo:</strong> {solicitud.observacionResolucion}
        </p>
      )}

      <ul className={propios.lista}>
        {solicitud.documentos.map((documento) => (
          <li className={propios.metadatoDelElemento} key={documento.idDocumentoVerificacion}>
            {nombreDelTipoDeDocumento(documento.tipoDocumento)}:{' '}
            <span className={propios.contenidoDelElemento}>{documento.nombreOriginal}</span> (
            {tamanoLegible(documento.tamanoBytes)})
          </li>
        ))}
      </ul>
    </li>
  );
}

function claseDePildora(estado: EstadoDeSolicitud): string {
  const extra =
    estado === 'PENDIENTE'
      ? propios.pildoraPendiente
      : estado === 'EN_REVISION'
        ? propios.pildoraEnRevision
        : estado === 'APROBADA'
          ? propios.pildoraAprobada
          : estado === 'RECHAZADA'
            ? propios.pildoraRechazada
            : undefined;
  return [propios.pildoraDeEstado, extra]
    .filter((parte) => parte !== undefined && parte !== '')
    .join(' ');
}
