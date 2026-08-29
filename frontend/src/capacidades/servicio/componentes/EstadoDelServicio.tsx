import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useCambioDeEstadoDeServicio } from '../hooks/useServiciosPropios';
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
  const siguiente = servicio.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-estado-del-servicio">
      <h2 className={secciones.tituloDeSeccion} id="titulo-estado-del-servicio">
        Publicación
      </h2>
      <p className={secciones.explicacion}>
        Un servicio inactivo no aparece en el descubrimiento. No se elimina: solo se deja de
        mostrar.
      </p>
      <p className={secciones.estado}>
        Estado actual:{' '}
        <span className={secciones.etiquetaDeEstado}>{nombreDelEstado(servicio.estado)}</span>
      </p>

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

      <button
        className={secciones.botonSecundario}
        type="button"
        disabled={cambio.isPending}
        onClick={() =>
          cambio.mutate({
            idServicio: servicio.idServicioPublicado,
            estado: siguiente,
          })
        }
      >
        {cambio.isPending
          ? 'Actualizando…'
          : siguiente === 'ACTIVO'
            ? 'Activar servicio'
            : 'Desactivar servicio'}
      </button>
    </section>
  );
}
