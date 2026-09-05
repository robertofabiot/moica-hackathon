import { ErrorDeApi } from '../../../comun/api';
import { Boton } from '../../../comun/componentes/ui';
import { useDisponibilidad } from '../hooks/usePerfilPrestador';
import propios from '../paginas/prestador.module.css';
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
    <section className={propios.bloqueIdentidad} aria-labelledby="titulo-disponibilidad">
      <h2 className={propios.tituloDeTarjeta} id="titulo-disponibilidad">
        Disponibilidad
      </h2>

      {cambio.error !== null && (
        <p className={`${propios.aviso} ${propios.avisoDeError}`} role="alert">
          {cambio.error instanceof ErrorDeApi
            ? cambio.error.message
            : 'No pudimos cambiar tu disponibilidad. Inténtalo otra vez.'}
        </p>
      )}

      <p className={propios.filaDeDisponibilidad}>
        Estado actual:{' '}
        <span className={estaDisponible ? propios.pildoraDisponible : propios.pildoraNoDisponible}>
          {estaDisponible ? 'Disponible' : 'No disponible'}
        </span>
      </p>

      <p className={propios.explicacion}>
        {estaDisponible
          ? 'Estás aceptando nuevas solicitudes de servicio.'
          : 'No estás aceptando nuevas solicitudes. Tu perfil y tu portafolio siguen guardados.'}
      </p>

      <Boton
        variante="secundario"
        type="button"
        onClick={alternar}
        disabled={cambio.isPending}
        forma="pildora"
      >
        {textoDelBoton(estaDisponible, cambio.isPending)}
      </Boton>
    </section>
  );
}

function textoDelBoton(estaDisponible: boolean, enCurso: boolean): string {
  if (enCurso) {
    return 'Cambiando…';
  }
  return estaDisponible ? 'Marcarme como no disponible' : 'Volver a estar disponible';
}
