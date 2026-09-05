import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import { useCambioDeEstadoDeServicio } from '../hooks/useServiciosPropios';
import propios from '../paginas/servicios.module.css';
import { nombreDelEstado } from '../presentacion';
import type { ServicioPropio } from '../tipos';

/**
 * Activa o desactiva un servicio. No hay borrado físico.
 *
 * Activar exige cuenta activa, prestador disponible y al menos verificación básica. El backend
 * responde el motivo; aquí solo se muestra.
 */
export default function EstadoDelServicio({ servicio }: { servicio: ServicioPropio }) {
  const cambio = useCambioDeEstadoDeServicio();
  const activo = servicio.estado === 'ACTIVO';
  const siguiente = activo ? 'INACTIVO' : 'ACTIVO';

  return (
    <section className={propios.tarjetaDeEdicion} aria-labelledby="titulo-estado-del-servicio">
      <h2 className={propios.tituloDeTarjetaEdicion} id="titulo-estado-del-servicio">
        Estado de la publicación
      </h2>
      <div className={propios.explicacionesDeEstado}>
        <p className={activo ? propios.explicacionDestacada : propios.explicacionDeEstado}>
          <strong>Activo.</strong> Cualquier cliente puede encontrarlo en el buscador si tu perfil
          está verificado.
        </p>
        <p className={!activo ? propios.explicacionDestacada : propios.explicacionDeEstado}>
          <strong>Inactivo.</strong> Queda oculto en el buscador. No se elimina: solo deja de
          mostrarse.
        </p>
      </div>

      <div className={propios.filaDePublicacion}>
        <span
          className={unirClases(
            propios.pildoraDeEstado,
            propios.pildoraDeEstadoEstatica,
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
      </div>

      {cambio.isError && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {cambio.error instanceof ErrorDeApi
            ? cambio.error.message
            : 'No pudimos cambiar el estado del servicio.'}
        </p>
      )}

      {cambio.isSuccess && (
        <p className={estilos.aviso} role="status">
          El servicio quedó {nombreDelEstado(cambio.data.estado).toLowerCase()}.
        </p>
      )}
    </section>
  );
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
