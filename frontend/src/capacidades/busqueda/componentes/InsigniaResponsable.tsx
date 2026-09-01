import { useEffect, useId, useRef, useState } from 'react';

import { InsigniaDeVerificacion, nombreDeLaInsignia } from '../../verificacion';
import type { PrestadorPublico } from '../tipos';
import estilos from './insigniaResponsable.module.css';

/**
 * Insignia pública interactiva con popover explicativo.
 *
 * En escritorio muestra la explicación y advertencia al pasar el cursor (hover).
 * En móvil o pantallas táctiles se abre y cierra al tocar la insignia.
 * Incluye cierre automático al hacer clic fuera o presionar Escape.
 */
export default function InsigniaResponsable({ prestador }: { prestador: PrestadorPublico }) {
  const [abierto, setAbierto] = useState(false);
  const contenedorRef = useRef<HTMLDivElement>(null);
  const idPopover = useId();

  // Cerrar al hacer clic fuera del componente o con la tecla Escape
  useEffect(() => {
    if (!abierto) {
      return;
    }

    function manejarClicFuera(evento: Event) {
      if (
        contenedorRef.current !== null &&
        !contenedorRef.current.contains(evento.target as Node)
      ) {
        setAbierto(false);
      }
    }

    function manejarTecla(evento: KeyboardEvent) {
      if (evento.key === 'Escape') {
        setAbierto(false);
      }
    }

    document.addEventListener('pointerdown', manejarClicFuera);
    document.addEventListener('keydown', manejarTecla);

    return () => {
      document.removeEventListener('pointerdown', manejarClicFuera);
      document.removeEventListener('keydown', manejarTecla);
    };
  }, [abierto]);

  const textoInsignia = nombreDeLaInsignia(prestador.nivelVerificacion);

  return (
    <div
      ref={contenedorRef}
      className={estilos.contenedor}
      onMouseEnter={() => setAbierto(true)}
      onMouseLeave={() => setAbierto(false)}
    >
      <button
        type="button"
        className={estilos.gatillo}
        aria-expanded={abierto}
        aria-controls={idPopover}
        aria-haspopup="dialog"
        aria-label={`Insignia ${textoInsignia}`}
        onClick={() => setAbierto((actual) => !actual)}
      >
        <InsigniaDeVerificacion nivel={prestador.nivelVerificacion} />
        <span className={estilos.iconoAyuda} aria-hidden="true" title="Más información">
          ?
        </span>
      </button>

      {abierto && (
        <div
          id={idPopover}
          className={estilos.popover}
          role="tooltip"
          aria-label="Detalles de la verificación del prestador"
        >
          <p className={estilos.titulo}>Verificación Moica</p>
          <p className={estilos.significado}>{prestador.significadoVerificacion}</p>
          <p className={estilos.advertencia}>{prestador.advertenciaDeInsignia}</p>
        </div>
      )}
    </div>
  );
}
