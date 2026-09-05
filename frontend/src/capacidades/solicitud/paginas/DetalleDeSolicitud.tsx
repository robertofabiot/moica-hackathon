import { Link, useParams } from 'react-router';

import { useSesionActual } from '../../auth';
import { ErrorDeApi } from '../../../comun/api';
import { IconoChevronIzquierda } from '../../../comun/componentes/ui';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import AccionesDeSolicitud from '../componentes/AccionesDeSolicitud';
import CalificacionDeSolicitud from '../componentes/CalificacionDeSolicitud';
import ChatDeSolicitud from '../componentes/ChatDeSolicitud';
import ContactosDelPrestador from '../componentes/ContactosDelPrestador';
import PildoraDeEstado from '../componentes/PildoraDeEstado';
import ReporteDeSolicitud from '../componentes/ReporteDeSolicitud';
import { useSolicitud } from '../hooks/useSolicitudes';
import { diaVisible, fechaVisible, nombreDelEstado } from '../presentacion';
import type { EstadoSolicitud } from '../tipos';
import { RUTA_SOLICITUDES } from '../rutas';
import MarcoDeSolicitudes from './MarcoDeSolicitudes';
import propios from './solicitudes.module.css';

/** Detalle, historial y acciones de una solicitud de la que la persona es participante. */
export default function DetalleDeSolicitud() {
  const { idSolicitud } = useParams();
  const identificador = Number(idSolicitud);
  const sesion = useSesionActual();
  const detalle = useSolicitud(Number.isInteger(identificador) ? identificador : undefined);

  if (detalle.isPending) {
    return (
      <MarcoDeSolicitudes className={propios.contenidoDetalle}>
        <p className={secciones.estado} role="status">
          Cargando la solicitud…
        </p>
      </MarcoDeSolicitudes>
    );
  }

  if (detalle.isError || detalle.data === undefined) {
    return (
      <MarcoDeSolicitudes className={propios.contenidoDetalle}>
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {detalle.error instanceof ErrorDeApi
            ? detalle.error.message
            : 'No pudimos cargar esta solicitud.'}{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => void detalle.refetch()}
          >
            Reintentar
          </button>
        </p>
        <p className={propios.pie}>
          <Link className={propios.enlaceVolver} to={RUTA_SOLICITUDES}>
            <IconoChevronIzquierda />
            Volver a mis solicitudes
          </Link>
        </p>
      </MarcoDeSolicitudes>
    );
  }

  const solicitud = detalle.data;
  const idUsuario = sesion.data?.usuario.idUsuario;
  const contraparte =
    idUsuario === solicitud.idCliente ? solicitud.nombrePublicoPrestador : solicitud.nombreCliente;

  return (
    <MarcoDeSolicitudes className={propios.contenidoDetalle}>
      <header className={propios.encabezado}>
        <Link className={propios.enlaceVolver} to={RUTA_SOLICITUDES}>
          <IconoChevronIzquierda />
          Volver a mis solicitudes
        </Link>
        <div className={propios.cabeceraDeDetalle}>
          <h1 className={`${propios.titulo} ${propios.tituloDeDetalle}`}>
            {solicitud.nombreServicio}
          </h1>
          <PildoraDeEstado estado={solicitud.estadoActual} grande />
        </div>
        <p className={propios.explicacion}>
          {contraparte} · {solicitud.nombreMunicipio}, {solicitud.nombreDepartamento}
        </p>
      </header>

      <div className={propios.cuerpoDetalle}>
        <div className={propios.columnaPrincipal}>
          <section className={propios.tarjetaInfo} aria-labelledby="titulo-necesidad">
            <h2 className={propios.tituloDeSeccion} id="titulo-necesidad">
              Necesidad
            </h2>
            <p className={propios.necesidad}>{solicitud.descripcionNecesidad}</p>
            <div className={propios.datoDeInfo}>
              <span className={propios.etiquetaDeDato}>Ubicación</span>
              <p className={propios.valorDeDato}>
                {solicitud.indicacionUbicacion}
                {' · '}
                {solicitud.nombreMunicipio}, {solicitud.nombreDepartamento}
              </p>
            </div>
            <div className={propios.datoDeInfo}>
              <span className={propios.etiquetaDeDato}>Fecha preferida</span>
              {solicitud.fechaPreferida !== null ? (
                <p className={propios.valorDeDato}>{diaVisible(solicitud.fechaPreferida)}</p>
              ) : (
                <p className={propios.explicacion}>Sin fecha preferida.</p>
              )}
            </div>
          </section>

          <div className={propios.bloqueContactos}>
            <ContactosDelPrestador solicitud={solicitud} />
          </div>

          <div className={propios.bloqueChat}>
            <ChatDeSolicitud solicitud={solicitud} />
          </div>

          <div className={propios.bloqueCalificacion}>
            <CalificacionDeSolicitud solicitud={solicitud} />
          </div>

          <div className={propios.bloqueReporte}>
            <ReporteDeSolicitud solicitud={solicitud} />
          </div>
        </div>

        <div className={propios.columnaLateral}>
          <section className={propios.panelAcciones} aria-labelledby="titulo-acciones">
            <h2 className={propios.tituloDeSeccion} id="titulo-acciones">
              Acciones
            </h2>
            <AccionesDeSolicitud solicitud={solicitud} />
          </section>

          <section className={propios.bloqueHistorial} aria-labelledby="titulo-historial">
            <h2 className={propios.tituloDeSeccion} id="titulo-historial">
              Historial
            </h2>
            <ol className={propios.historial} aria-label="Historial">
              {solicitud.historial.map((cambio) => (
                <li key={cambio.idCambioEstadoSolicitud} className={propios.cambio}>
                  <span
                    className={unirClases(propios.nodoDeHistorial, claseDeNodo(cambio.estadoNuevo))}
                    aria-hidden="true"
                  />
                  <p className={propios.tituloDeCambio}>
                    {cambio.estadoAnterior === null
                      ? nombreDelEstado(cambio.estadoNuevo)
                      : `${nombreDelEstado(cambio.estadoAnterior)} → ${nombreDelEstado(cambio.estadoNuevo)}`}
                  </p>
                  <p className={propios.metaDeCambio}>
                    {cambio.nombreActor} · {fechaVisible(cambio.fechaCambio)}
                  </p>
                  {cambio.motivo !== null && cambio.motivo !== '' ? (
                    <p className={propios.motivoDeCambio}>{cambio.motivo}</p>
                  ) : null}
                </li>
              ))}
            </ol>
          </section>
        </div>
      </div>
    </MarcoDeSolicitudes>
  );
}

function claseDeNodo(estado: EstadoSolicitud): string | undefined {
  if (estado === 'ACEPTADA') {
    return propios.nodoAceptada;
  }
  if (estado === 'COMPLETADA') {
    return propios.nodoCompletada;
  }
  if (estado === 'RECHAZADA' || estado === 'CANCELADA') {
    return propios.nodoCerrado;
  }
  return undefined;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
