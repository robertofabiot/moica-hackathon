import type { HTMLAttributes } from 'react';

import estilos from './EstrellasCalificacion.module.css';
import { IconoEstrella } from './iconos';

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
      <IconoEstrella className={estilos.icono} />
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

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
