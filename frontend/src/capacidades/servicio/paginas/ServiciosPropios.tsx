import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useCambioDeEstadoDeServicio, useServiciosPropios } from '../hooks/useServiciosPropios';
import { nombreDelEstado, precioPropio } from '../presentacion';
import { RUTA_NUEVO_SERVICIO, rutaDeEdicionDeServicio } from '../rutas';
import type { ServicioPropio } from '../tipos';
import propios from './servicios.module.css';

/**
 * Listado de servicios del prestador autenticado, incluidos los inactivos.
 */
export default function ServiciosPropios() {
  const servicios = useServiciosPropios();

  if (servicios.isPending) {
    return (
      <main className={propios.pantalla}>
        <p className={secciones.estado} role="status">
          Cargando tus servicios…
        </p>
      </main>
    );
  }

  if (servicios.isError) {
    return (
      <main className={propios.pantalla}>
        <div className={propios.contenido}>
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {servicios.error instanceof ErrorDeApi
              ? servicios.error.message
              : 'No pudimos cargar tus servicios.'}{' '}
            <button
              className={estilos.enlaceDeTexto}
              type="button"
              onClick={() => void servicios.refetch()}
            >
              Reintentar
            </button>
          </p>
          <p className={propios.pie}>
            <Link to="/">Volver al inicio</Link>
          </p>
        </div>
      </main>
    );
  }

  const lista = servicios.data;

  return (
    <main className={propios.pantalla}>
      <div className={propios.contenido}>
        <header className={propios.encabezado}>
          <h1 className={propios.titulo}>Tus servicios</h1>
          <p className={secciones.explicacion}>
            Prepara publicaciones aunque tu perfil aún no esté verificado. Solo se muestran en el
            descubrimiento cuando están activas, tu cuenta está operativa y tu perfil verificado
            está disponible.
          </p>
          <div className={propios.accionesDeEncabezado}>
            <Link className={estilos.boton} to={RUTA_NUEVO_SERVICIO}>
              Publicar un servicio
            </Link>
          </div>
        </header>

        {lista.length === 0 ? (
          <p className={secciones.vacio}>Todavía no tienes servicios. Publica el primero.</p>
        ) : (
          <ul className={propios.lista}>
            {lista.map((servicio) => (
              <TarjetaPropia key={servicio.idServicioPublicado} servicio={servicio} />
            ))}
          </ul>
        )}

        <p className={propios.pie}>
          <Link to="/">Volver al inicio</Link>
        </p>
      </div>
    </main>
  );
}

function TarjetaPropia({ servicio }: { servicio: ServicioPropio }) {
  const cambio = useCambioDeEstadoDeServicio();
  const siguiente = servicio.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';

  return (
    <li className={propios.tarjeta}>
      <h2 className={propios.nombre}>{servicio.nombre}</h2>
      <p className={propios.meta}>
        {servicio.nombreCategoria} · {servicio.nombreSubcategoria}
      </p>
      <p className={propios.meta}>
        {precioPropio(servicio.precioReferencia)} · {nombreDelEstado(servicio.estado)}
      </p>
      {cambio.isError && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {cambio.error instanceof ErrorDeApi
            ? cambio.error.message
            : 'No pudimos cambiar el estado.'}
        </p>
      )}
      <div className={secciones.accionesDelElemento}>
        <Link
          className={secciones.botonPequeno}
          to={rutaDeEdicionDeServicio(servicio.idServicioPublicado)}
        >
          Editar
        </Link>
        <button
          className={secciones.botonPequeno}
          type="button"
          disabled={cambio.isPending}
          onClick={() =>
            cambio.mutate({
              idServicio: servicio.idServicioPublicado,
              estado: siguiente,
            })
          }
        >
          {cambio.isPending ? 'Actualizando…' : siguiente === 'ACTIVO' ? 'Activar' : 'Desactivar'}
        </button>
      </div>
    </li>
  );
}
