import type { HTMLAttributes } from 'react';

import estilos from './EstrellasCalificacion.module.css';
import { IconoEstrella } from './iconos';

type PropiedadesDeEstrellasCalificacion = HTMLAttributes<HTMLSpanElement> & {
  calificacion: number | null;
  totalCalificaciones?: number;
};

/**
 * Puntuación compacta de un prestador.
 *
 * Muestra la estrella de aviso, la nota en negrita y, si llega, el recuento
 * entre paréntesis: `★ 4.8 (120)`.
 *
 * Con `calificacion` en `null` no dibuja una nota: escribe «Sin calificaciones».
 * Quien todavía no fue calificado no tiene un `0.0`, no tiene nota, y presentar
 * un cero lo castigaría por algo que nadie hizo.
 *
 * La frase completa —incluido el singular o el plural del recuento— vive en el
 * `aria-label` del contenedor, de modo que quien no ve el icono recibe la misma
 * información que quien sí lo ve.
 */
export function EstrellasCalificacion({
  calificacion,
  totalCalificaciones,
  className,
  ...rest
}: PropiedadesDeEstrellasCalificacion) {
  if (calificacion === null) {
    return (
      <span
        className={unirClases(estilos.estrellas, estilos.sinNota, className)}
        aria-label="Sin calificaciones todavía"
        {...rest}
      >
        <IconoEstrella className={`${estilos.icono} ${estilos.iconoApagado}`} />
        <span className={estilos.vacio} aria-hidden="true">
          Sin calificaciones
        </span>
      </span>
    );
  }

  const nota = calificacion.toFixed(1);
  const etiqueta =
    totalCalificaciones === undefined
      ? `Calificación ${nota} de 5`
      : `Calificación ${nota} de 5, ${conteo(totalCalificaciones)}`;

  return (
    <span className={unirClases(estilos.estrellas, className)} aria-label={etiqueta} {...rest}>
      <IconoEstrella className={estilos.icono} />
      <span className={estilos.puntuacion} aria-hidden="true">
        {nota}
      </span>
      {totalCalificaciones !== undefined && (
        <span className={estilos.resenas} aria-hidden="true">
          ({totalCalificaciones})
        </span>
      )}
    </span>
  );
}

function conteo(cantidad: number): string {
  return cantidad === 1 ? '1 calificación' : `${cantidad} calificaciones`;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
