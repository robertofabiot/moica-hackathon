import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { Boton } from '../../../comun/componentes/ui';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useCambioDeEstadoDeServicio, useServiciosPropios } from '../hooks/useServiciosPropios';
import { nombreDelEstado, precioPropio } from '../presentacion';
import { RUTA_NUEVO_SERVICIO, rutaDeEdicionDeServicio } from '../rutas';
import type { ServicioPropio } from '../tipos';
import MarcoDeGestionDeServicios from './MarcoDeGestionDeServicios';
import propios from './servicios.module.css';

/**
 * Listado de servicios del prestador autenticado, incluidos los inactivos.
 */
export default function ServiciosPropios() {
  const servicios = useServiciosPropios();

  if (servicios.isPending) {
    return (
      <MarcoDeGestionDeServicios>
        <p className={secciones.estado} role="status">
          Cargando tus servicios…
        </p>
      </MarcoDeGestionDeServicios>
    );
  }

  if (servicios.isError) {
    return (
      <MarcoDeGestionDeServicios>
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
      </MarcoDeGestionDeServicios>
    );
  }

  const lista = servicios.data;

  return (
    <MarcoDeGestionDeServicios>
      <header className={propios.cabeceraDeListado}>
        <div className={propios.filaDeCabecera}>
          <div className={propios.grupoDeTitulo}>
            <h1 className={propios.tituloDeListado}>Mis servicios</h1>
            <p className={propios.explicacionDeListado}>
              Las publicaciones activas aparecen en el directorio cuando tu cuenta está operativa y
              tu perfil verificado está disponible. Puedes preparar servicios aunque todavía no
              estés verificado.
            </p>
          </div>
          <Boton variante="primario" to={RUTA_NUEVO_SERVICIO}>
            + Publicar nuevo servicio
          </Boton>
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
    </MarcoDeGestionDeServicios>
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
