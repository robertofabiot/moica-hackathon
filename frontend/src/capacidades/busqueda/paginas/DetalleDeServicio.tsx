import { useState } from 'react';
import { Link, useParams } from 'react-router';

import logoIcono from '../../../assets/logos/moica-icono.svg';
import { ErrorDeApi } from '../../../comun/api';
import { IconoCasa, PieDePagina } from '../../../comun/componentes/ui';
import { AccionDeSolicitud } from '../../solicitud';
import InsigniaResponsable from '../componentes/InsigniaResponsable';
import { useServicioPublico } from '../hooks/useBusquedaPublica';
import { nombreDelTipoPrestador, precioVisible } from '../presentacion';
import { RUTA_EXPLORAR, rutaDePrestadorPublico } from '../rutas';
import type { DetallePublicoDeServicio, ImagenPublicaDeServicio } from '../tipos';
import estilos from './detalleServicio.module.css';

/** Detalle público de un servicio visible. La solicitud se envía desde aquí. */
export default function DetalleDeServicio() {
  const { idServicio } = useParams();
  const identificador = Number(idServicio);
  const detalle = useServicioPublico(Number.isInteger(identificador) ? identificador : undefined);

  if (detalle.isPending) {
    return (
      <div className={estilos.pagina}>
        <main className={estilos.principal}>
          <p className={estilos.estado} role="status">
            Cargando el servicio…
          </p>
        </main>
        <PieDePagina />
      </div>
    );
  }

  if (detalle.isError || detalle.data === undefined) {
    return (
      <div className={estilos.pagina}>
        <main className={estilos.principal}>
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {detalle.error instanceof ErrorDeApi
              ? detalle.error.message
              : 'Ese servicio no está disponible.'}{' '}
            <button
              className={estilos.reintentar}
              type="button"
              onClick={() => void detalle.refetch()}
            >
              Reintentar
            </button>
          </p>
          <p className={estilos.pieDeEstado}>
            <Link to={RUTA_EXPLORAR}>Volver a explorar</Link>
          </p>
        </main>
        <PieDePagina />
      </div>
    );
  }

  return <DetalleCargado servicio={detalle.data} />;
}

function DetalleCargado({ servicio }: { servicio: DetallePublicoDeServicio }) {
  const prestador = servicio.prestador;

  return (
    <div className={estilos.pagina}>
      <main className={estilos.principal}>
        <MigasDePan
          nombreCategoria={servicio.nombreCategoria}
          idCategoria={servicio.idCategoriaServicio}
          nombreServicio={servicio.nombre}
        />

        <div className={estilos.columnas}>
          <div className={estilos.columnaPrincipal}>
            <GaleriaDeServicio nombre={servicio.nombre} imagenes={servicio.imagenes} />

            <section className={estilos.tarjeta} aria-labelledby="titulo-descripcion-servicio">
              <h2 className={estilos.tituloDeTarjeta} id="titulo-descripcion-servicio">
                Descripción
              </h2>
              <p className={estilos.descripcion}>{servicio.descripcion}</p>
            </section>
          </div>

          <div className={estilos.columnaLateral}>
            <header className={estilos.tarjeta}>
              <h1 className={estilos.tituloDeServicio}>{servicio.nombre}</h1>
              <p className={estilos.subtitulo}>
                {servicio.nombreCategoria} · {servicio.nombreSubcategoria}
              </p>
              <p>
                <strong>{precioVisible(servicio.precioReferencia)}</strong>
              </p>
            </header>

            <section className={estilos.tarjeta} aria-labelledby="titulo-prestador-servicio">
              <h2 className={estilos.tituloDeTarjeta} id="titulo-prestador-servicio">
                Quién lo ofrece
              </h2>
              <p>
                <Link to={rutaDePrestadorPublico(prestador.idPrestador)}>
                  {prestador.nombrePublico}
                </Link>
              </p>
              <p className={estilos.subtitulo}>
                {nombreDelTipoPrestador(prestador.tipoPrestador)} ·{' '}
                {prestador.municipioPrincipal.nombreMunicipio},{' '}
                {prestador.municipioPrincipal.nombreDepartamento}
              </p>
              <p className={estilos.descripcion}>{prestador.descripcion}</p>
              <InsigniaResponsable prestador={prestador} />
            </section>

            <AccionDeSolicitud
              idServicio={servicio.idServicioPublicado}
              idPrestador={prestador.idPrestador}
              admiteContratacion={servicio.admiteContratacion}
            />
          </div>
        </div>
      </main>
      <PieDePagina />
    </div>
  );
}

function MigasDePan({
  nombreCategoria,
  idCategoria,
  nombreServicio,
}: {
  nombreCategoria: string;
  idCategoria: number;
  nombreServicio: string;
}) {
  return (
    <nav className={estilos.migas} aria-label="Migas de pan">
      <ol className={estilos.listaMigas}>
        <li>
          <Link className={estilos.miga} to="/">
            <IconoCasa />
            Inicio
          </Link>
        </li>
        <li>
          <Link className={estilos.miga} to={`${RUTA_EXPLORAR}?idCategoria=${idCategoria}`}>
            {nombreCategoria}
          </Link>
        </li>
        <li>
          <span className={estilos.migaActual} aria-current="page">
            {nombreServicio}
          </span>
        </li>
      </ol>
    </nav>
  );
}

function GaleriaDeServicio({
  nombre,
  imagenes,
}: {
  nombre: string;
  imagenes: ImagenPublicaDeServicio[];
}) {
  const [imagenSeleccionada, setImagenSeleccionada] = useState(0);
  const actual = imagenes[imagenSeleccionada];

  return (
    <div className={estilos.galeria}>
      {actual === undefined ? (
        <div className={estilos.escenario}>
          <div className={estilos.placeholder} role="img" aria-label={`${nombre}, sin imágenes`}>
            <img className={estilos.isotipo} src={logoIcono} alt="" />
          </div>
        </div>
      ) : (
        <div className={estilos.escenario}>
          <img
            className={estilos.imagenPrincipal}
            src={actual.urlImagen}
            alt={actual.textoAlternativo ?? `Imagen ${imagenSeleccionada + 1} de ${nombre}`}
          />
        </div>
      )}

      {imagenes.length > 0 ? (
        <ul className={estilos.miniaturas} aria-label="Miniaturas del servicio">
          {imagenes.map((imagen, posicion) => (
            <li key={imagen.idImagenServicioPublicado}>
              <button
                type="button"
                className={
                  posicion === imagenSeleccionada
                    ? `${estilos.miniatura} ${estilos.miniaturaActiva}`
                    : estilos.miniatura
                }
                aria-label={`Ver imagen ${posicion + 1} de ${imagenes.length}`}
                aria-pressed={posicion === imagenSeleccionada}
                onClick={() => setImagenSeleccionada(posicion)}
              >
                <img src={imagen.urlImagen} alt="" />
              </button>
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}
