import type { HTMLAttributes } from 'react';

import estilos from './EstrellasCalificacion.module.css';

type PropiedadesDeEstrellasCalificacion = HTMLAttributes<HTMLSpanElement> & {
  calificacion: number;
  totalResenas?: number;
};

/**
 * Puntuación compacta de un servicio o prestador.
 *
 * Muestra la estrella de aviso, la nota en negrita y, si llega, el recuento
 * de reseñas entre paréntesis: `★ 4.8 (120)`.
 */
export function EstrellasCalificacion({
  calificacion,
  totalResenas,
  className,
  ...rest
}: PropiedadesDeEstrellasCalificacion) {
  const nota = calificacion.toFixed(1);
  const etiqueta =
    totalResenas === undefined
      ? `Calificación ${nota} de 5`
      : `Calificación ${nota} de 5, ${totalResenas} reseñas`;

  return (
    <span className={unirClases(estilos.estrellas, className)} aria-label={etiqueta} {...rest}>
      <IconoEstrella />
      <span className={estilos.puntuacion} aria-hidden="true">
        {nota}
      </span>
      {totalResenas !== undefined && (
        <span className={estilos.resenas} aria-hidden="true">
          ({totalResenas})
        </span>
      )}
    </span>
  );
}

function IconoEstrella() {
  return (
    <svg className={estilos.icono} viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M12 2.6 14.7 8.2l6.2.9-4.5 4.4 1.1 6.2L12 16.8 6.5 19.7l1.1-6.2-4.5-4.4 6.2-.9L12 2.6Z" />
    </svg>
  );
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
