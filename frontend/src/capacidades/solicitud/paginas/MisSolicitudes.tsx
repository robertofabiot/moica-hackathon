import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useSolicitudesEnviadas, useSolicitudesRecibidas } from '../hooks/useSolicitudes';
import { diaVisible, fechaVisible, nombreDelEstado } from '../presentacion';
import { rutaDeSolicitud } from '../rutas';
import type { ResumenDeSolicitudServicio } from '../tipos';
import propios from './solicitudes.module.css';

/** Bandejas de solicitudes enviadas y recibidas. */
export default function MisSolicitudes() {
  const enviadas = useSolicitudesEnviadas();
  const recibidas = useSolicitudesRecibidas();

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Mis solicitudes</h1>
          <p className={secciones.explicacion}>
            Las que enviaste como cliente y las que recibiste en tus servicios.
          </p>
        </header>

        <Bandeja
          titulo="Enviadas"
          descripcion="Solicitudes que enviaste a otros prestadores."
          consulta={enviadas}
          vacio="Todavía no has enviado solicitudes."
          contraparte={(item) => item.nombrePublicoPrestador}
        />

        <Bandeja
          titulo="Recibidas"
          descripcion="Solicitudes dirigidas a tus servicios."
          consulta={recibidas}
          vacio="Todavía no has recibido solicitudes."
          contraparte={(item) => item.nombreCliente}
        />

        <p className={propios.pie}>
          <Link to="/">Volver al inicio</Link>
        </p>
      </div>
    </main>
  );
}

function Bandeja({
  titulo,
  descripcion,
  consulta,
  vacio,
  contraparte,
}: {
  titulo: string;
  descripcion: string;
  consulta: ReturnType<typeof useSolicitudesEnviadas>;
  vacio: string;
  contraparte: (item: ResumenDeSolicitudServicio) => string;
}) {
  return (
    <section className={secciones.seccion} aria-labelledby={`titulo-${titulo.toLowerCase()}`}>
      <h2 className={secciones.tituloDeSeccion} id={`titulo-${titulo.toLowerCase()}`}>
        {titulo}
      </h2>
      <p className={secciones.explicacion}>{descripcion}</p>

      {consulta.isPending ? (
        <p className={secciones.estado} role="status">
          Cargando solicitudes…
        </p>
      ) : null}

      {consulta.isError ? (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {consulta.error instanceof ErrorDeApi
            ? consulta.error.message
            : 'No pudimos cargar estas solicitudes.'}{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => void consulta.refetch()}
          >
            Reintentar
          </button>
        </p>
      ) : null}

      {consulta.data !== undefined && consulta.data.length === 0 ? (
        <p className={secciones.vacio}>{vacio}</p>
      ) : null}

      {consulta.data !== undefined && consulta.data.length > 0 ? (
        <ul className={propios.lista}>
          {consulta.data.map((item) => (
            <li key={item.idSolicitudServicio}>
              <Link className={propios.tarjeta} to={rutaDeSolicitud(item.idSolicitudServicio)}>
                <p className={propios.nombre}>{item.nombreServicio}</p>
                <p className={secciones.metadatoDelElemento}>
                  {contraparte(item)} · {item.nombreMunicipio}
                </p>
                <p>
                  <span className={secciones.etiquetaDeEstado}>
                    {nombreDelEstado(item.estadoActual)}
                  </span>
                </p>
                <p className={secciones.metadatoDelElemento}>
                  Enviada {fechaVisible(item.fechaCreacion)}
                  {item.fechaPreferida !== null
                    ? ` · Fecha preferida ${diaVisible(item.fechaPreferida)}`
                    : ''}
                </p>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
