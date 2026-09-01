import { useState } from 'react';
import { Link, useParams } from 'react-router';

import logoIcono from '../../../assets/logos/moica-icono.svg';
import { ErrorDeApi } from '../../../comun/api';
import {
  Boton,
  IconoCasa,
  IconoCheckCirculo,
  IconoEstrella,
  IconoGuardar,
  IconoPin,
  InsigniaVerificado,
  PieDePagina,
} from '../../../comun/componentes/ui';
import { AccionDeSolicitud } from '../../solicitud';
import InsigniaResponsable from '../componentes/InsigniaResponsable';
import { useServicioPublico } from '../hooks/useBusquedaPublica';
import {
  CALIFICACION_DE_MUESTRA,
  DESGLOSE_DE_RESENAS_DE_MUESTRA,
  nombreDelTipoPrestador,
  precioEnTarjeta,
  RESENAS_DE_FICHA_DE_MUESTRA,
} from '../presentacion';
import { RUTA_EXPLORAR, rutaDePrestadorPublico } from '../rutas';
import type { DetallePublicoDeServicio, ImagenPublicaDeServicio, PrestadorPublico } from '../tipos';
import estilos from './detalleServicio.module.css';

const GARANTIAS = [
  'Instalaciones y reparaciones garantizadas',
  'Atención con materiales incluidos o mano de obra',
  'Disponibilidad en el municipio indicado',
] as const;

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
            <div className={estilos.bloqueGaleria}>
              <GaleriaDeServicio nombre={servicio.nombre} imagenes={servicio.imagenes} />
            </div>

            <section
              className={`${estilos.tarjeta} ${estilos.bloqueDescripcion}`}
              aria-labelledby="titulo-descripcion-servicio"
            >
              <h2 className={estilos.tituloDeTarjeta} id="titulo-descripcion-servicio">
                Descripción
              </h2>
              <p className={estilos.descripcion}>{servicio.descripcion}</p>
            </section>

            <div className={estilos.bloquePrestador}>
              <TarjetaDePrestador prestador={prestador} />
            </div>
          </div>

          <div className={estilos.columnaLateral}>
            <div className={estilos.bloqueFicha}>
              <FichaDeContratacion servicio={servicio} />
            </div>
            <div className={estilos.bloqueResenas}>
              <DesgloseDeResenas />
            </div>
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

function FichaDeContratacion({ servicio }: { servicio: DetallePublicoDeServicio }) {
  const [guardado, setGuardado] = useState(false);
  const prestador = servicio.prestador;
  const precio = precioEnTarjeta(servicio.precioReferencia);
  const nota = CALIFICACION_DE_MUESTRA.toFixed(1);

  return (
    <section className={estilos.tarjeta} aria-labelledby="titulo-ficha-servicio">
      <h1 className={estilos.tituloDeServicio} id="titulo-ficha-servicio">
        {servicio.nombre}
      </h1>

      <div className={estilos.metadatos}>
        <p
          className={estilos.calificacion}
          aria-label={`Calificación ${nota} de 5, ${RESENAS_DE_FICHA_DE_MUESTRA} reseñas`}
        >
          <IconoEstrella className={estilos.iconoEstrella} />
          <span className={estilos.nota} aria-hidden="true">
            {nota}
          </span>
          <span className={estilos.conteo} aria-hidden="true">
            ({RESENAS_DE_FICHA_DE_MUESTRA} reseñas)
          </span>
        </p>
        <p className={estilos.ubicacion}>
          <IconoPin className={estilos.iconoPin} />
          {prestador.municipioPrincipal.nombreMunicipio}, NIC
        </p>
      </div>

      <div className={estilos.bloqueDePrecio}>
        {precio.prefijo !== null ? (
          <span className={estilos.etiquetaDesde}>{precio.prefijo}</span>
        ) : null}
        <p className={estilos.monto}>{precio.valor}</p>
      </div>

      <p className={estilos.subtitulo}>{servicio.nombreSubcategoria}</p>

      <ul className={estilos.garantias}>
        {GARANTIAS.map((garantia) => (
          <li key={garantia}>
            <IconoCheckCirculo className={estilos.iconoCheck} />
            <span>{garantia}</span>
          </li>
        ))}
      </ul>

      <div className={estilos.acciones}>
        <div className={estilos.ctaSolicitud}>
          <AccionDeSolicitud
            idServicio={servicio.idServicioPublicado}
            idPrestador={prestador.idPrestador}
            admiteContratacion={servicio.admiteContratacion}
          />
        </div>
        <Boton
          variante="contorno"
          className={estilos.botonGuardar}
          aria-pressed={guardado}
          onClick={() => setGuardado((actual) => !actual)}
        >
          <IconoGuardar />
          {guardado ? 'Guardado' : 'Guardar'}
        </Boton>
      </div>
    </section>
  );
}

function TarjetaDePrestador({ prestador }: { prestador: PrestadorPublico }) {
  const estaVerificado = prestador.nivelVerificacion !== 'SIN_VERIFICAR';

  return (
    <section className={estilos.tarjeta} aria-labelledby="titulo-prestador-servicio">
      <h2 className={estilos.tituloDeTarjeta} id="titulo-prestador-servicio">
        Quién lo ofrece
      </h2>
      <div className={estilos.cabeceraPrestador}>
        {prestador.urlImagenPerfil !== null ? (
          <img
            className={estilos.avatarPrestador}
            src={prestador.urlImagenPerfil}
            alt={`Foto de ${prestador.nombrePublico}`}
          />
        ) : (
          <span className={estilos.avatarPrestador} aria-hidden="true">
            {prestador.nombrePublico.trim().slice(0, 1).toUpperCase()}
          </span>
        )}
        <div className={estilos.datosPrestador}>
          <Link
            className={estilos.enlacePrestador}
            to={rutaDePrestadorPublico(prestador.idPrestador)}
          >
            <span className={estilos.nombrePrestador}>{prestador.nombrePublico}</span>
            {estaVerificado ? <InsigniaVerificado /> : null}
          </Link>
          <p className={estilos.subtitulo}>
            {nombreDelTipoPrestador(prestador.tipoPrestador)} ·{' '}
            {prestador.municipioPrincipal.nombreMunicipio},{' '}
            {prestador.municipioPrincipal.nombreDepartamento}
          </p>
        </div>
      </div>
      <div className={estilos.cajaVerificacion}>
        <InsigniaResponsable prestador={prestador} />
      </div>
    </section>
  );
}

function DesgloseDeResenas() {
  const total = DESGLOSE_DE_RESENAS_DE_MUESTRA.reduce((suma, fila) => suma + fila.cantidad, 0);
  const nota = CALIFICACION_DE_MUESTRA.toFixed(1);

  return (
    <section className={estilos.tarjeta} aria-labelledby="titulo-resenas-servicio">
      <h2 className={estilos.tituloDeTarjeta} id="titulo-resenas-servicio">
        Reseñas
      </h2>
      <div className={estilos.resumenDeResenas}>
        <p className={estilos.puntuacionGrande}>{nota}</p>
        <p className={estilos.deCinco}>De 5</p>
      </div>
      <ul className={estilos.barras}>
        {DESGLOSE_DE_RESENAS_DE_MUESTRA.map((fila) => {
          const porcentaje = total === 0 ? 0 : Math.round((fila.cantidad / total) * 100);
          return (
            <li key={fila.estrellas} className={estilos.filaDeBarra}>
              <span className={estilos.etiquetaBarra}>
                {fila.estrellas}
                <IconoEstrella className={estilos.iconoEstrellaChica} />
              </span>
              <div
                className={estilos.riel}
                role="meter"
                aria-label={`${fila.estrellas} estrellas`}
                aria-valuemin={0}
                aria-valuemax={total}
                aria-valuenow={fila.cantidad}
              >
                <span className={estilos.relleno} style={{ width: `${porcentaje}%` }} />
              </div>
              <span className={estilos.contadorBarra}>{fila.cantidad}</span>
            </li>
          );
        })}
      </ul>
    </section>
  );
}
