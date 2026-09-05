import type { MouseEvent, ReactNode } from 'react';
import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, IconoPin } from '../../../comun/componentes/ui';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { usePerfilPrestador } from '../../prestador';
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
  const perfil = usePerfilPrestador();

  // Una cuenta es prestador si tiene perfil creado o ya recibió solicitudes en sus servicios.
  const esPrestador =
    Boolean(perfil.data) || (recibidas.data !== undefined && recibidas.data.length > 0);

  return (
    <MarcoDeSolicitudes className={propios.contenidoBandejas}>
      <header className={propios.encabezado}>
        <h1 className={propios.titulo}>Mis solicitudes</h1>
        <p className={propios.explicacion}>
          {esPrestador
            ? 'Gestiona las solicitudes que recibes en tus servicios y las que envías como cliente.'
            : 'Sigue el estado y los mensajes de los servicios que has solicitado.'}
        </p>
      </header>

      {esPrestador ? (
        <>
          <nav className={propios.resumenRapido} aria-label="Resumen de solicitudes">
            <ChipDeBandeja destino="recibidas" etiqueta="Recibidas" cantidad={recibidas.data?.length} />
            <ChipDeBandeja destino="enviadas" etiqueta="Enviadas" cantidad={enviadas.data?.length} />
          </nav>

          <div className={propios.bandejas}>
            <Bandeja
              id="recibidas"
              titulo="Recibidas"
              descripcion="Solicitudes dirigidas a tus servicios."
              consulta={recibidas}
              vacio="Todavía no has recibido solicitudes."
              contraparte={(item) => item.nombreCliente}
            />

            <Bandeja
              id="enviadas"
              titulo="Enviadas"
              descripcion="Solicitudes que enviaste a otros prestadores."
              consulta={enviadas}
              vacio="Todavía no has enviado solicitudes."
              contraparte={(item) => item.nombrePublicoPrestador}
            />
          </div>
        </>
      ) : (
        <div className={propios.contenedorCliente}>
          <Bandeja
            id="enviadas"
            titulo="Solicitudes enviadas"
            descripcion="Servicios que has solicitado a prestadores en Moica."
            consulta={enviadas}
            vacio="Todavía no has enviado solicitudes."
            contraparte={(item) => item.nombrePublicoPrestador}
            accionVacio={
              <div className={propios.accionVacio}>
                <p className={propios.explicacionVacio}>
                  ¿Necesitas una reparación, instalación o mantenimiento? Encuentra profesionales verificados en tu zona.
                </p>
                <Boton to="/explorar" variante="primario">
                  Explorar servicios
                </Boton>
              </div>
            }
          />

          <aside className={propios.tarjetaPromocionPrestador} aria-label="Ofrecer servicios en Moica">
            <div className={propios.cuerpoPromocion}>
              <h2 className={propios.tituloPromocion}>¿Ofreces servicios profesionales o técnicos?</h2>
              <p className={propios.textoPromocion}>
                Crea tu perfil de prestador para recibir solicitudes de clientes, mostrar tu portafolio y gestionar trabajos en Moica.
              </p>
            </div>
            <Boton to="/prestador" variante="contorno">
              Crear perfil de prestador
            </Boton>
          </aside>
        </div>
      )}
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
  accionVacio,
}: {
  id: string;
  titulo: string;
  descripcion: string;
  consulta: ReturnType<typeof useSolicitudesEnviadas>;
  vacio: string;
  contraparte: (item: ResumenDeSolicitudServicio) => string;
  accionVacio?: ReactNode;
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
        <div className={propios.cajaVacia}>
          <p className={propios.vacio}>{vacio}</p>
          {accionVacio}
        </div>
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
