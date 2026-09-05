import type { MouseEvent } from 'react';
import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { IconoPin } from '../../../comun/componentes/ui';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import PildoraDeEstado from '../componentes/PildoraDeEstado';
import { useSolicitudesEnviadas, useSolicitudesRecibidas } from '../hooks/useSolicitudes';
import { fechaVisible } from '../presentacion';
import { rutaDeSolicitud } from '../rutas';
import type { ResumenDeSolicitudServicio } from '../tipos';
import MarcoDeSolicitudes from './MarcoDeSolicitudes';
import propios from './solicitudes.module.css';

/** Bandejas de solicitudes enviadas y recibidas. */
export default function MisSolicitudes() {
  const enviadas = useSolicitudesEnviadas();
  const recibidas = useSolicitudesRecibidas();

  return (
    <MarcoDeSolicitudes className={propios.contenidoBandejas}>
      <header className={propios.encabezado}>
        <h1 className={propios.titulo}>Mis solicitudes</h1>
        <p className={propios.explicacion}>
          Las que enviaste como cliente y las que recibiste en tus servicios.
        </p>
      </header>

      <nav className={propios.resumenRapido} aria-label="Resumen de solicitudes">
        <ChipDeBandeja destino="enviadas" etiqueta="Enviadas" cantidad={enviadas.data?.length} />
        <ChipDeBandeja destino="recibidas" etiqueta="Recibidas" cantidad={recibidas.data?.length} />
      </nav>

      <div className={propios.bandejas}>
        <Bandeja
          id="enviadas"
          titulo="Enviadas"
          descripcion="Solicitudes que enviaste a otros prestadores."
          consulta={enviadas}
          vacio="Todavía no has enviado solicitudes."
          contraparte={(item) => item.nombrePublicoPrestador}
        />

        <Bandeja
          id="recibidas"
          titulo="Recibidas"
          descripcion="Solicitudes dirigidas a tus servicios."
          consulta={recibidas}
          vacio="Todavía no has recibido solicitudes."
          contraparte={(item) => item.nombreCliente}
        />
      </div>
    </MarcoDeSolicitudes>
  );
}

function ChipDeBandeja({
  destino,
  etiqueta,
  cantidad,
}: {
  destino: string;
  etiqueta: string;
  cantidad: number | undefined;
}) {
  return (
    <a className={propios.chipDeResumen} href={`#${destino}`} onClick={desplazarA(destino)}>
      {etiqueta}:{' '}
      <strong className={propios.conteoChip}>{cantidad === undefined ? '…' : cantidad}</strong>
    </a>
  );
}

function desplazarA(id: string) {
  return (evento: MouseEvent<HTMLAnchorElement>) => {
    evento.preventDefault();
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };
}

function Bandeja({
  id,
  titulo,
  descripcion,
  consulta,
  vacio,
  contraparte,
}: {
  id: string;
  titulo: string;
  descripcion: string;
  consulta: ReturnType<typeof useSolicitudesEnviadas>;
  vacio: string;
  contraparte: (item: ResumenDeSolicitudServicio) => string;
}) {
  return (
    <section className={propios.bandeja} id={id} aria-labelledby={`titulo-${id}`}>
      <h2 className={propios.tituloDeBandeja} id={`titulo-${id}`}>
        {titulo}
      </h2>
      <p className={propios.explicacion}>{descripcion}</p>

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
        <p className={propios.vacio}>{vacio}</p>
      ) : null}

      {consulta.data !== undefined && consulta.data.length > 0 ? (
        <ul className={propios.lista}>
          {consulta.data.map((item) => (
            <li key={item.idSolicitudServicio}>
              <Link className={propios.tarjeta} to={rutaDeSolicitud(item.idSolicitudServicio)}>
                <div className={propios.cabeceraDeTarjeta}>
                  <p className={propios.nombre}>{item.nombreServicio}</p>
                  <PildoraDeEstado estado={item.estadoActual} />
                </div>
                <div className={propios.metaDeTarjeta}>
                  <p>{contraparte(item)}</p>
                  <p className={propios.filaDeUbicacion}>
                    <span className={propios.iconoDeMeta} aria-hidden="true">
                      <IconoPin />
                    </span>
                    <span>
                      {item.nombreMunicipio}
                      {' · '}
                      Enviada {fechaVisible(item.fechaCreacion)}
                    </span>
                  </p>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      ) : null}
    </section>
  );
}
