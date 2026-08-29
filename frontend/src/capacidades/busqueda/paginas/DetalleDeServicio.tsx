import { Link, useParams } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import InsigniaResponsable from '../componentes/InsigniaResponsable';
import { useServicioPublico } from '../hooks/useBusquedaPublica';
import { nombreDelTipoPrestador, precioVisible } from '../presentacion';
import { RUTA_EXPLORAR, rutaDePrestadorPublico } from '../rutas';
import propios from './explorar.module.css';

/** Detalle público de un servicio visible, sin contactos ni solicitudes. */
export default function DetalleDeServicio() {
  const { idServicio } = useParams();
  const identificador = Number(idServicio);
  const detalle = useServicioPublico(Number.isInteger(identificador) ? identificador : undefined);

  if (detalle.isPending) {
    return (
      <main className={propios.pantalla}>
        <p className={secciones.estado} role="status">
          Cargando el servicio…
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
              : 'Ese servicio no está disponible.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void detalle.refetch()}
            >
              Reintentar
            </button>
          </p>
          <p className={propios.pie}>
            <Link to={RUTA_EXPLORAR}>Volver a explorar</Link>
          </p>
        </div>
      </main>
    );
  }

  const servicio = detalle.data;
  const prestador = servicio.prestador;

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>{servicio.nombre}</h1>
          <p className={secciones.explicacion}>
            {servicio.nombreCategoria} · {servicio.nombreSubcategoria}
          </p>
          <p>
            <strong>{precioVisible(servicio.precioReferencia)}</strong>
          </p>
        </header>

        {servicio.imagenes.length === 0 ? (
          <p className={secciones.vacio}>Este servicio no tiene imágenes.</p>
        ) : (
          <ul className={propios.galeria}>
            {servicio.imagenes.map((imagen, posicion) => (
              <li key={imagen.idImagenServicioPublicado}>
                <img
                  className={propios.imagen}
                  src={imagen.urlImagen}
                  alt={imagen.textoAlternativo ?? `Imagen ${posicion + 1} de ${servicio.nombre}`}
                />
              </li>
            ))}
          </ul>
        )}

        <section className={secciones.seccion} aria-labelledby="titulo-descripcion-servicio">
          <h2 className={secciones.tituloDeSeccion} id="titulo-descripcion-servicio">
            Descripción
          </h2>
          <p>{servicio.descripcion}</p>
        </section>

        <section className={secciones.seccion} aria-labelledby="titulo-prestador-servicio">
          <h2 className={secciones.tituloDeSeccion} id="titulo-prestador-servicio">
            Quién lo ofrece
          </h2>
          <p>
            <Link to={rutaDePrestadorPublico(prestador.idPrestador)}>
              {prestador.nombrePublico}
            </Link>
          </p>
          <p className={secciones.explicacion}>
            {nombreDelTipoPrestador(prestador.tipoPrestador)} ·{' '}
            {prestador.municipioPrincipal.nombreMunicipio},{' '}
            {prestador.municipioPrincipal.nombreDepartamento}
          </p>
          <p>{prestador.descripcion}</p>
          <InsigniaResponsable prestador={prestador} />
        </section>

        <p
          className={`${propios.aviso} ${
            servicio.admiteContratacion ? propios.avisoDisponible : propios.avisoNoDisponible
          }`}
          role="status"
        >
          {servicio.admiteContratacion
            ? 'Este prestador está disponible. La solicitud de contratación llega en un próximo incremento; hoy solo puedes ver su perfil público.'
            : 'Este prestador no está disponible para contratar ahora.'}
        </p>

        <p className={propios.pie}>
          <Link to={RUTA_EXPLORAR}>Volver a explorar</Link>
        </p>
      </div>
    </main>
  );
}
