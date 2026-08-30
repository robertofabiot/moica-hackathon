import estilos from '../paginas/seguridad.module.css';

const PESTANIAS = ['Perfil', 'Cuenta', 'Notificaciones', 'Privacidad', 'Pagos'] as const;

/**
 * Pestañas de la pantalla de configuración.
 *
 * Solo la pestaña Cuenta está activa; el resto es presentacional hasta que
 * existan esas secciones.
 */
export default function PestaniasDeConfiguracion() {
  return (
    <div className={estilos.barraDeTabs} role="tablist" aria-label="Secciones de configuración">
      {PESTANIAS.map((nombre) => {
        const activa = nombre === 'Cuenta';
        return (
          <button
            key={nombre}
            type="button"
            role="tab"
            aria-selected={activa}
            className={activa ? `${estilos.tab} ${estilos.tabActiva}` : estilos.tab}
          >
            {nombre}
          </button>
        );
      })}
    </div>
  );
}
