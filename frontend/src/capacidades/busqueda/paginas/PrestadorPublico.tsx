import { Link, useParams } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { EstrellasCalificacion } from '../../../comun/componentes/ui';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import InsigniaResponsable from '../componentes/InsigniaResponsable';
import TarjetaDeServicio from '../componentes/TarjetaDeServicio';
import { usePrestadorPublico } from '../hooks/useBusquedaPublica';
import {
  conteoDeCalificaciones,
  nombreDeDisponibilidad,
  nombreDelTipoPrestador,
} from '../presentacion';
import { RUTA_EXPLORAR } from '../rutas';
import propios from './explorar.module.css';
import lista from '../componentes/tarjeta.module.css';

/** Perfil público de un prestador verificado: presentación, portafolio y servicios, sin contactos. */
export default function PrestadorPublico() {
  const { idPrestador } = useParams();
  const identificador = Number(idPrestador);
  const perfil = usePrestadorPublico(Number.isInteger(identificador) ? identificador : undefined);

  if (perfil.isPending) {
    return (
      <main className={propios.pantalla}>
        <p className={secciones.estado} role="status">
          Cargando el perfil…
        </p>
      </main>
    );
  }

  if (perfil.isError || perfil.data === undefined) {
    return (
      <main className={propios.pantalla}>
        <div className={propios.contenido}>
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {perfil.error instanceof ErrorDeApi
              ? perfil.error.message
              : 'Ese perfil no está disponible.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void perfil.refetch()}
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

  const { prestador, portafolio, servicios, admiteContratacion, reputacionPrestador } = perfil.data;

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          {prestador.urlImagenPerfil !== null && (
            <img
              className={propios.imagen}
              src={prestador.urlImagenPerfil}
              alt={`Foto de ${prestador.nombrePublico}`}
            />
          )}
          <h1 className={propios.titulo}>{prestador.nombrePublico}</h1>
          <p className={secciones.explicacion}>
            {nombreDelTipoPrestador(prestador.tipoPrestador)} ·{' '}
            {prestador.municipioPrincipal.nombreMunicipio},{' '}
            {prestador.municipioPrincipal.nombreDepartamento}
          </p>
          <p className={secciones.explicacion}>
            {nombreDeDisponibilidad(prestador.disponibilidad)}
          </p>
          <p className={propios.reputacion}>
            <EstrellasCalificacion
              calificacion={reputacionPrestador.promedio}
              totalCalificaciones={
                reputacionPrestador.cantidad === 0 ? undefined : reputacionPrestador.cantidad
              }
            />
            {reputacionPrestador.cantidad > 0 ? (
              <span className={secciones.explicacion}>
                {conteoDeCalificaciones(reputacionPrestador.cantidad)} de solicitudes completadas
              </span>
            ) : null}
          </p>
        </header>

        <section className={secciones.seccion} aria-labelledby="titulo-presentacion-publica">
          <h2 className={secciones.tituloDeSeccion} id="titulo-presentacion-publica">
            Presentación
          </h2>
          <p>{prestador.descripcion}</p>
          <p className={secciones.explicacion}>{prestador.descripcionCobertura}</p>
          <InsigniaResponsable prestador={prestador} />
        </section>

        <section className={secciones.seccion} aria-labelledby="titulo-portafolio-publico">
          <h2 className={secciones.tituloDeSeccion} id="titulo-portafolio-publico">
            Portafolio
          </h2>
          {portafolio.length === 0 ? (
            <p className={secciones.vacio}>Este prestador todavía no publicó trabajos.</p>
          ) : (
            <ul className={propios.portafolio}>
              {portafolio.map((trabajo) => (
                <li key={trabajo.idTrabajo} className={propios.trabajo}>
                  <h3 className={propios.titulo}>{trabajo.titulo}</h3>
                  <p>{trabajo.descripcion}</p>
                  {trabajo.imagenes.length > 0 && (
                    <div className={propios.miniaturas}>
                      {trabajo.imagenes.map((imagen) => (
                        <img
                          key={imagen.idImagenTrabajoPortafolio}
                          className={propios.miniatura}
                          src={imagen.urlImagen}
                          alt={imagen.textoAlternativo ?? trabajo.titulo}
                          loading="lazy"
                        />
                      ))}
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className={secciones.seccion} aria-labelledby="titulo-servicios-publicos">
          <h2 className={secciones.tituloDeSeccion} id="titulo-servicios-publicos">
            Servicios
          </h2>
          {servicios.length === 0 ? (
            <p className={secciones.vacio}>No hay servicios activos en este momento.</p>
          ) : (
            <ul className={lista.lista}>
              {servicios.map((servicio) => (
                <TarjetaDeServicio key={servicio.idServicioPublicado} servicio={servicio} />
              ))}
            </ul>
          )}
        </section>

        <p
          className={`${propios.aviso} ${
            admiteContratacion ? propios.avisoDisponible : propios.avisoNoDisponible
          }`}
          role="status"
        >
          {admiteContratacion
            ? 'Está disponible. Puedes solicitar un servicio desde su detalle. Los contactos siguen ocultos hasta que acepte.'
            : 'No está disponible para contratar ahora. No se muestran contactos.'}
        </p>

        <p className={propios.pie}>
          <Link to={RUTA_EXPLORAR}>Volver a explorar</Link>
        </p>
      </div>
    </main>
  );
}
