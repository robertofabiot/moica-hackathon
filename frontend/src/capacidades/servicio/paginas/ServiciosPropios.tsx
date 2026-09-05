import { ErrorDeApi } from '../../../comun/api';
import { Boton, IconoDeCategoria, IconoLapiz, IconoMaletin } from '../../../comun/componentes/ui';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useCambioDeEstadoDeServicio, useServiciosPropios } from '../hooks/useServiciosPropios';
import { precioPropio } from '../presentacion';
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
  const activos = lista.filter((servicio) => servicio.estado === 'ACTIVO').length;
  const inactivos = lista.length - activos;

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
        <div className={propios.estadoVacio}>
          <span className={propios.iconoVacio} aria-hidden="true">
            <IconoMaletin />
          </span>
          <h2 className={propios.tituloVacio}>Aún no has publicado ningún servicio</h2>
          <p className={propios.explicacionVacio}>
            Publica el primero para que los clientes puedan encontrarte en el directorio cuando la
            publicación esté activa y tu perfil verificado.
          </p>
          <Boton variante="primario" to={RUTA_NUEVO_SERVICIO}>
            + Publicar nuevo servicio
          </Boton>
        </div>
      ) : (
        <>
          <div className={propios.resumenRapido} aria-label="Resumen de publicaciones">
            <span className={propios.chipDeResumen}>
              Total publicados <strong className={propios.conteoChip}>{lista.length}</strong>
            </span>
            <span className={propios.chipDeResumen}>
              <span className={unirClases(propios.puntoDeEstado, propios.puntoActivo)} />
              Activos <strong className={propios.conteoChip}>{activos}</strong>
            </span>
            <span className={propios.chipDeResumen}>
              <span className={unirClases(propios.puntoDeEstado, propios.puntoInactivo)} />
              Inactivos <strong className={propios.conteoChip}>{inactivos}</strong>
            </span>
          </div>
          <ul className={propios.lista}>
            {lista.map((servicio) => (
              <TarjetaPropia key={servicio.idServicioPublicado} servicio={servicio} />
            ))}
          </ul>
        </>
      )}
    </MarcoDeGestionDeServicios>
  );
}

function TarjetaPropia({ servicio }: { servicio: ServicioPropio }) {
  const cambio = useCambioDeEstadoDeServicio();
  const siguiente = servicio.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
  const activo = servicio.estado === 'ACTIVO';
  const imagen = servicio.imagenes[0];
  const precio = precioPropio(servicio.precioReferencia);
  const aConvenir = servicio.precioReferencia === null;

  return (
    <li className={propios.tarjeta}>
      <div className={propios.cabeceraDeTarjeta}>
        {imagen === undefined ? (
          <div className={propios.miniaturaVacia}>
            <span className={propios.iconoDeRespaldo}>
              <IconoDeCategoria nombreCategoria={servicio.nombreCategoria} />
            </span>
          </div>
        ) : (
          <img
            className={propios.miniatura}
            src={imagen.urlImagen}
            alt={imagen.textoAlternativo ?? servicio.nombre}
            loading="lazy"
          />
        )}
        <span
          className={unirClases(
            propios.pildoraDeEstado,
            activo ? propios.pildoraActiva : propios.pildoraInactiva
          )}
        >
          <span
            className={unirClases(
              propios.puntoDeEstado,
              activo ? propios.puntoPulsante : propios.puntoInactivo
            )}
            aria-hidden="true"
          />
          {activo ? 'ACTIVO' : 'INACTIVO'}
        </span>
      </div>

      <div className={propios.cuerpoDeTarjeta}>
        <div className={propios.filaDePildoras}>
          <span className={propios.pildoraCategoria}>{servicio.nombreCategoria}</span>
          <span className={propios.pildoraCategoria}>{servicio.nombreSubcategoria}</span>
        </div>
        <h2 className={propios.nombre}>{servicio.nombre}</h2>
        <p className={unirClases(propios.precio, aConvenir ? propios.precioConvenido : undefined)}>
          {precio}
        </p>
      </div>

      {cambio.isError && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {cambio.error instanceof ErrorDeApi
            ? cambio.error.message
            : 'No pudimos cambiar el estado.'}
        </p>
      )}

      <div className={propios.barraDeAcciones}>
        <button
          type="button"
          role="switch"
          className={unirClases(
            propios.interruptor,
            activo ? propios.interruptorEncendido : undefined
          )}
          aria-checked={activo}
          aria-busy={cambio.isPending}
          aria-label={`Publicación de ${servicio.nombre}`}
          disabled={cambio.isPending}
          onClick={() =>
            cambio.mutate({
              idServicio: servicio.idServicioPublicado,
              estado: siguiente,
            })
          }
        >
          <span className={propios.carril} aria-hidden="true">
            <span className={propios.palanca} />
          </span>
          <span className={propios.textoInterruptor}>
            {cambio.isPending ? 'Actualizando…' : activo ? 'Publicado' : 'Oculto'}
          </span>
        </button>
        <Boton variante="secundario" to={rutaDeEdicionDeServicio(servicio.idServicioPublicado)}>
          <IconoLapiz />
          Editar
        </Boton>
      </div>
    </li>
  );
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
