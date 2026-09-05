import { Link, useParams } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import AccionesDelCaso from '../componentes/AccionesDelCaso';
import ApelacionDelCaso from '../componentes/ApelacionDelCaso';
import EvidenciasDelCaso from '../componentes/EvidenciasDelCaso';
import HistorialDelCaso from '../componentes/HistorialDelCaso';
import MedidaDelCaso from '../componentes/MedidaDelCaso';
import MensajesDelCaso from '../componentes/MensajesDelCaso';
import { fechaLegible, nombreDelEstado, nombreDelResultado } from '../etiquetas';
import { useExpedienteDeCaso } from '../hooks/useRevisionDeCasos';
import { RUTA_ADMIN_CASOS } from '../rutas';
import propios from './casos.module.css';

/**
 * El expediente de un caso: lo que hay que leer para decidir y las decisiones que caben.
 *
 * Reúne el reporte, la solicitud con su historial de transiciones, las evidencias que ya existían
 * del trato, el hilo de mensajes y las versiones del propio caso. Nada de esto lo compone la
 * pantalla: llega en una sola respuesta del backend, salvo los mensajes, que se piden aparte y solo
 * cuando alguien decide leerlos.
 */
export default function ExpedienteDeCaso() {
  const { idCaso } = useParams();
  const identificador = Number(idCaso);

  if (!Number.isInteger(identificador) || identificador <= 0) {
    return (
      <main className={propios.pantalla}>
        <div className={propios.contenido}>
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            Ese caso no existe.
          </p>
          <p className={propios.pie}>
            <Link to={RUTA_ADMIN_CASOS}>Volver a la bandeja</Link>
          </p>
        </div>
      </main>
    );
  }

  return <Expediente idCaso={identificador} />;
}

function Expediente({ idCaso }: { idCaso: number }) {
  const expediente = useExpedienteDeCaso(idCaso);

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <p className={propios.migaDePan}>
          <Link to={RUTA_ADMIN_CASOS}>Casos de moderación</Link>
        </p>

        {expediente.isPending && (
          <p className={secciones.estado} role="status">
            Cargando el expediente…
          </p>
        )}

        {expediente.isError && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {expediente.error instanceof ErrorDeApi
              ? expediente.error.message
              : 'No pudimos cargar el expediente.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void expediente.refetch()}
            >
              Reintentar
            </button>
          </p>
        )}

        {expediente.data !== undefined && (
          <>
            <header className={propios.encabezado}>
              <h1 className={propios.titulo}>{expediente.data.caso.motivo}</h1>
              <p className={secciones.explicacion}>
                {expediente.data.caso.nombreReportante} reportó a{' '}
                {expediente.data.caso.nombreReportado} · Abierto el{' '}
                {fechaLegible(expediente.data.caso.fechaApertura)}
              </p>
              <p className={propios.estadoDestacado}>
                {nombreDelEstado(expediente.data.caso.estadoActual)}
                {expediente.data.caso.resultadoActual !== null &&
                  ` · ${nombreDelResultado(expediente.data.caso.resultadoActual)}`}
              </p>
            </header>

            <section className={secciones.seccion} aria-labelledby="reporte">
              <h2 className={secciones.tituloDeSeccion} id="reporte">
                Lo que se reportó
              </h2>
              <p className={propios.textoLargo}>{expediente.data.descripcion}</p>
            </section>

            <AccionesDelCaso expediente={expediente.data} />

            {expediente.data.resolucionActual !== null && (
              <section className={secciones.seccion} aria-labelledby="resolucion">
                <h2 className={secciones.tituloDeSeccion} id="resolucion">
                  Resolución vigente
                </h2>
                <p className={propios.textoLargo}>{expediente.data.resolucionActual}</p>
              </section>
            )}

            <MedidaDelCaso expediente={expediente.data} />
            <ApelacionDelCaso expediente={expediente.data} />

            <SolicitudDelCaso expediente={expediente.data} />
            <EvidenciasDelCaso imagenes={expediente.data.imagenesDelServicio} />
            <MensajesDelCaso idCaso={idCaso} />
            <HistorialDelCaso versiones={expediente.data.historial} />
          </>
        )}
      </div>
    </main>
  );
}

/** La solicitud reportada, con lo que la pantalla del participante también muestra. */
function SolicitudDelCaso({
  expediente,
}: {
  expediente: NonNullable<ReturnType<typeof useExpedienteDeCaso>['data']>;
}) {
  const { solicitud } = expediente;

  return (
    <section className={secciones.seccion} aria-labelledby="solicitud">
      <h2 className={secciones.tituloDeSeccion} id="solicitud">
        Solicitud reportada
      </h2>
      <dl className={propios.ficha}>
        <div>
          <dt className={propios.etiqueta}>Servicio</dt>
          <dd className={propios.valor}>{solicitud.nombreServicio}</dd>
        </div>
        <div>
          <dt className={propios.etiqueta}>Cliente</dt>
          <dd className={propios.valor}>{solicitud.nombreCliente}</dd>
        </div>
        <div>
          <dt className={propios.etiqueta}>Prestador</dt>
          <dd className={propios.valor}>{solicitud.nombrePublicoPrestador}</dd>
        </div>
        <div>
          <dt className={propios.etiqueta}>Ubicación</dt>
          <dd className={propios.valor}>
            {solicitud.nombreMunicipio}, {solicitud.nombreDepartamento}
          </dd>
        </div>
        <div>
          <dt className={propios.etiqueta}>Necesidad descrita</dt>
          <dd className={propios.valor}>{solicitud.descripcionNecesidad}</dd>
        </div>
      </dl>

      <h3 className={propios.subtitulo}>Cómo evolucionó</h3>
      <ol className={propios.linea}>
        {solicitud.historial.map((cambio) => (
          <li key={cambio.idCambioEstadoSolicitud} className={propios.hito}>
            <p className={propios.hitoTitulo}>
              {cambio.estadoAnterior === null
                ? `Creada como ${cambio.estadoNuevo.toLowerCase()}`
                : `${cambio.estadoAnterior.toLowerCase()} → ${cambio.estadoNuevo.toLowerCase()}`}
            </p>
            <p className={propios.hitoDetalle}>
              {cambio.nombreActor} · {fechaLegible(cambio.fechaCambio)}
              {cambio.motivo !== null && ` · ${cambio.motivo}`}
            </p>
          </li>
        ))}
      </ol>
    </section>
  );
}
