import { Link } from 'react-router';

import { RUTA_PRESTADOR } from '../../prestador';
import estilos from '../paginas/seguridad.module.css';

type PestaniaDeConfiguracion =
  | { nombre: 'Perfil'; destino: typeof RUTA_PRESTADOR }
  | { nombre: 'Cuenta'; activa: true }
  | { nombre: 'Notificaciones' | 'Privacidad' | 'Pagos'; proxima: true };

const PESTANIAS: readonly PestaniaDeConfiguracion[] = [
  { nombre: 'Perfil', destino: RUTA_PRESTADOR },
  { nombre: 'Cuenta', activa: true },
  { nombre: 'Notificaciones', proxima: true },
  { nombre: 'Privacidad', proxima: true },
  { nombre: 'Pagos', proxima: true },
];

/**
 * Pestañas de la pantalla de configuración.
 *
 * Cuenta es la sección actual. Perfil lleva al perfil de prestador. El resto
 * permanece inactivo hasta que existan esas pantallas.
 */
export default function PestaniasDeConfiguracion() {
  return (
    <div className={estilos.barraDeTabs} role="tablist" aria-label="Secciones de configuración">
      {PESTANIAS.map((pestania) => {
        if ('destino' in pestania) {
          return (
            <Link
              key={pestania.nombre}
              to={pestania.destino}
              role="tab"
              aria-selected={false}
              className={estilos.tab}
            >
              {pestania.nombre}
            </Link>
          );
        }

        if ('activa' in pestania) {
          return (
            <button
              key={pestania.nombre}
              type="button"
              role="tab"
              aria-selected={true}
              className={`${estilos.tab} ${estilos.tabActiva}`}
            >
              {pestania.nombre}
            </button>
          );
        }

        return (
          <button
            key={pestania.nombre}
            type="button"
            role="tab"
            aria-selected={false}
            aria-disabled="true"
            title="Próximamente disponible"
            className={`${estilos.tab} ${estilos.tabInactiva}`}
          >
            {pestania.nombre}
          </button>
        );
      })}
    </div>
  );
}
