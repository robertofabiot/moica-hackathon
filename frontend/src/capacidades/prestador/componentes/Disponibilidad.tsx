import { ErrorDeApi } from '../../../comun/api';
import estilos from '../../../comun/estilos/formulario.module.css';
import { useDisponibilidad } from '../hooks/usePerfilPrestador';
import secciones from '../../../comun/estilos/secciones.module.css';
import type { PerfilPrestador } from '../tipos';

/**
 * Interruptor de disponibilidad.
 *
 * Estar no disponible no oculta el perfil: el backend rechaza solicitudes nuevas hacia sus
 * servicios. El perfil y el portafolio siguen visibles.
 */
export default function Disponibilidad({ perfil }: { perfil: PerfilPrestador }) {
  const cambio = useDisponibilidad();
  const estaDisponible = perfil.disponibilidad === 'DISPONIBLE';

  const alternar = () => cambio.mutate(estaDisponible ? 'NO_DISPONIBLE' : 'DISPONIBLE');

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-disponibilidad">
      <h2 className={secciones.tituloDeSeccion} id="titulo-disponibilidad">
        Disponibilidad
      </h2>

      {cambio.error !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {cambio.error instanceof ErrorDeApi
            ? cambio.error.message
            : 'No pudimos cambiar tu disponibilidad. Inténtalo otra vez.'}
        </p>
      )}

      <p className={secciones.estado}>
        Estado actual:{' '}
        <span className={secciones.etiquetaDeEstado}>
          {estaDisponible ? 'Disponible' : 'No disponible'}
        </span>
      </p>

      <p className={secciones.explicacion}>
        {estaDisponible
          ? 'Estás aceptando nuevas solicitudes de servicio.'
          : 'No estás aceptando nuevas solicitudes. Tu perfil y tu portafolio siguen guardados.'}
      </p>

      <button
        className={secciones.botonSecundario}
        type="button"
        onClick={alternar}
        disabled={cambio.isPending}
      >
        {textoDelBoton(estaDisponible, cambio.isPending)}
      </button>
    </section>
  );
}

function textoDelBoton(estaDisponible: boolean, enCurso: boolean): string {
  if (enCurso) {
    return 'Cambiando…';
  }
  return estaDisponible ? 'Marcarme como no disponible' : 'Volver a estar disponible';
}
