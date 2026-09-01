import { Link, useParams } from 'react-router';

import { useSesionActual } from '../../auth';
import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import AccionesDeSolicitud from '../componentes/AccionesDeSolicitud';
import CalificacionDeSolicitud from '../componentes/CalificacionDeSolicitud';
import ChatDeSolicitud from '../componentes/ChatDeSolicitud';
import ContactosDelPrestador from '../componentes/ContactosDelPrestador';
import { useSolicitud } from '../hooks/useSolicitudes';
import { diaVisible, fechaVisible, nombreDelEstado } from '../presentacion';
import { RUTA_SOLICITUDES } from '../rutas';
import propios from './solicitudes.module.css';

/** Detalle, historial y acciones de una solicitud de la que la persona es participante. */
export default function DetalleDeSolicitud() {
  const { idSolicitud } = useParams();
  const identificador = Number(idSolicitud);
  const sesion = useSesionActual();
  const detalle = useSolicitud(Number.isInteger(identificador) ? identificador : undefined);

  if (detalle.isPending) {
    return (
      <main className={propios.pantalla}>
        <p className={secciones.estado} role="status">
          Cargando la solicitud…
        </p>
      </main>
    );
  }

  if (detalle.isError || detalle.data === undefined) {
    return (
      <main className={propios.pantalla}>
        <div className={propios.contenido}>
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
            <Link to={RUTA_SOLICITUDES}>Volver a mis solicitudes</Link>
          </p>
        </div>
      </main>
    );
  }

  const solicitud = detalle.data;
  const idUsuario = sesion.data?.usuario.idUsuario;
  const contraparte =
    idUsuario === solicitud.idCliente ? solicitud.nombrePublicoPrestador : solicitud.nombreCliente;

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>{solicitud.nombreServicio}</h1>
          <p>
            <span className={secciones.etiquetaDeEstado}>
              {nombreDelEstado(solicitud.estadoActual)}
            </span>
          </p>
          <p className={secciones.explicacion}>
            {contraparte} · {solicitud.nombreMunicipio}, {solicitud.nombreDepartamento}
          </p>
        </header>

        <section className={secciones.seccion} aria-labelledby="titulo-necesidad">
          <h2 className={secciones.tituloDeSeccion} id="titulo-necesidad">
            Necesidad
          </h2>
          <p>{solicitud.descripcionNecesidad}</p>
          <p>
            <strong>Ubicación: </strong>
            {solicitud.indicacionUbicacion}
          </p>
          {solicitud.fechaPreferida !== null ? (
            <p>
              <strong>Fecha preferida: </strong>
              {diaVisible(solicitud.fechaPreferida)}
            </p>
          ) : (
            <p className={secciones.explicacion}>Sin fecha preferida.</p>
          )}
        </section>

        <section className={secciones.seccion} aria-labelledby="titulo-acciones">
          <h2 className={secciones.tituloDeSeccion} id="titulo-acciones">
            Acciones
          </h2>
          <AccionesDeSolicitud solicitud={solicitud} />
        </section>

        <ChatDeSolicitud solicitud={solicitud} />
        <ContactosDelPrestador solicitud={solicitud} />
        <CalificacionDeSolicitud solicitud={solicitud} />

        <section className={secciones.seccion} aria-labelledby="titulo-historial">
          <h2 className={secciones.tituloDeSeccion} id="titulo-historial">
            Historial
          </h2>
          <ol className={propios.historial}>
            {solicitud.historial.map((cambio) => (
              <li key={cambio.idCambioEstadoSolicitud} className={propios.cambio}>
                <p>
                  <strong>
                    {cambio.estadoAnterior === null
                      ? nombreDelEstado(cambio.estadoNuevo)
                      : `${nombreDelEstado(cambio.estadoAnterior)} → ${nombreDelEstado(cambio.estadoNuevo)}`}
                  </strong>
                </p>
                <p className={secciones.metadatoDelElemento}>
                  {cambio.nombreActor} · {fechaVisible(cambio.fechaCambio)}
                </p>
                {cambio.motivo !== null && cambio.motivo !== '' ? <p>{cambio.motivo}</p> : null}
              </li>
            ))}
          </ol>
        </section>

        <p className={propios.pie}>
          <Link to={RUTA_SOLICITUDES}>Volver a mis solicitudes</Link>
        </p>
      </div>
    </main>
  );
}
