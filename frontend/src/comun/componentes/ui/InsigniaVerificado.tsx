import type { HTMLAttributes } from 'react';

import estilos from './InsigniaVerificado.module.css';

type PropiedadesDeInsigniaVerificado = HTMLAttributes<HTMLSpanElement>;

/**
 * Marca compacta de cuenta o prestador verificado.
 *
 * El check no va solo: el texto «Verificado» acompaña al color teal para que
 * la insignia se entienda también sin percibir el tono.
 */
export function InsigniaVerificado({ className, ...rest }: PropiedadesDeInsigniaVerificado) {
  return (
    <span className={unirClases(estilos.insignia, className)} {...rest}>
      <span className={estilos.sello} aria-hidden="true">
        <IconoCheck />
      </span>
      <span className={estilos.etiqueta}>Verificado</span>
    </span>
  );
}

function IconoCheck() {
  return (
    <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M5 12.5 9.5 17 19 7.5"
        stroke="currentColor"
        strokeWidth={2.5}
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
