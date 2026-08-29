import { forwardRef, useId, type InputHTMLAttributes, type ReactNode } from 'react';

import estilos from './Entrada.module.css';

export type VarianteDeEntrada = 'predeterminada' | 'fusionada';

type PropiedadesDeEntrada = InputHTMLAttributes<HTMLInputElement> & {
  mensajeDeError?: string;
  icono?: ReactNode;
  variante?: VarianteDeEntrada;
};

/**
 * Campo de texto compatible con `register` de React Hook Form.
 *
 * El `ref` llega hasta el `input` nativo. Si hay `mensajeDeError`, se anuncia
 * debajo y queda enlazado con `aria-describedby` sin pisar una pista que el
 * padre ya hubiera pasado.
 *
 * `variante="fusionada"` quita el borde propio para meter el campo dentro de
 * una barra de búsqueda. El `icono` se pinta a la izquierda, fuera del mensaje
 * de error, para que un aviso no desplace la lupa.
 */
export const Entrada = forwardRef<HTMLInputElement, PropiedadesDeEntrada>(function Entrada(
  {
    mensajeDeError,
    icono,
    variante = 'predeterminada',
    className,
    id,
    'aria-describedby': descritoPor,
    ...rest
  },
  ref
) {
  const idGenerado = useId();
  const idDelCampo = id ?? idGenerado;
  const idDeError = `${idDelCampo}-error`;
  const tieneError = mensajeDeError !== undefined && mensajeDeError !== '';

  const clases = [estilos.entrada];
  if (variante === 'fusionada') {
    clases.push(estilos.fusionada);
  }
  if (tieneError) {
    clases.push(estilos.entradaConError);
  }
  if (className !== undefined && className !== '') {
    clases.push(className);
  }

  return (
    <div className={estilos.envoltorio}>
      <div className={estilos.fila}>
        {icono !== undefined && (
          <span className={estilos.icono} aria-hidden="true">
            {icono}
          </span>
        )}
        <input
          {...rest}
          ref={ref}
          id={idDelCampo}
          className={clases.filter((parte) => parte !== undefined && parte !== '').join(' ')}
          aria-invalid={tieneError}
          aria-describedby={unirIds(descritoPor, tieneError ? idDeError : undefined)}
        />
      </div>
      {tieneError && (
        <p className={estilos.error} id={idDeError} role="alert">
          {mensajeDeError}
        </p>
      )}
    </div>
  );
});

function unirIds(...ids: Array<string | undefined>): string | undefined {
  const unidos = ids.filter((id) => id !== undefined && id !== '');
  return unidos.length > 0 ? unidos.join(' ') : undefined;
}
