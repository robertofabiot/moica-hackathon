import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import {
  fechaLegible,
  nombreDelEstado,
  nombreDelNivelSolicitado,
  nombreDelTipoDeDocumento,
  tamanoLegible,
} from '../etiquetas';
import { useSolicitudesPropias } from '../hooks/useVerificacion';
import type { SolicitudDeVerificacion } from '../tipos';
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
      <p className={secciones.estado} role="status">
        Cargando tus solicitudes…
      </p>
    );
  }

  if (historial.isError) {
    return (
      <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
        {historial.error instanceof ErrorDeApi
          ? historial.error.message
          : 'No pudimos cargar tus solicitudes.'}{' '}
        <button
          className={estilos.enlaceDeTexto}
          type="button"
          onClick={() => void historial.refetch()}
        >
          Reintentar
        </button>
      </p>
    );
  }

  if (historial.data.length === 0) {
    return <p className={secciones.vacio}>Todavía no has presentado ninguna solicitud.</p>;
  }

  return (
    <ul className={secciones.lista}>
      {historial.data.map((solicitud) => (
        <SolicitudDelHistorial key={solicitud.idSolicitudVerificacion} solicitud={solicitud} />
      ))}
    </ul>
  );
}

function SolicitudDelHistorial({ solicitud }: { solicitud: SolicitudDeVerificacion }) {
  return (
    <li className={secciones.elemento}>
      <p className={secciones.tituloDelElemento}>
        {nombreDelNivelSolicitado(solicitud.nivelSolicitado)}
      </p>
      <p className={secciones.estado}>
        <span className={secciones.etiquetaDeEstado}>
          {nombreDelEstado(solicitud.estadoSolicitud)}
        </span>
      </p>
      <p className={secciones.metadatoDelElemento}>
        Enviada el {fechaLegible(solicitud.fechaSolicitud)}
        {solicitud.fechaResolucion !== null &&
          ` · Resuelta el ${fechaLegible(solicitud.fechaResolucion)}`}
      </p>

      {solicitud.observacionResolucion !== null && (
        <p className={propios.motivo}>
          <strong>Motivo:</strong> {solicitud.observacionResolucion}
        </p>
      )}

      <ul className={secciones.lista}>
        {solicitud.documentos.map((documento) => (
          <li className={secciones.metadatoDelElemento} key={documento.idDocumentoVerificacion}>
            {nombreDelTipoDeDocumento(documento.tipoDocumento)}:{' '}
            <span className={secciones.contenidoDelElemento}>{documento.nombreOriginal}</span> (
            {tamanoLegible(documento.tamanoBytes)})
          </li>
        ))}
      </ul>
    </li>
  );
}
