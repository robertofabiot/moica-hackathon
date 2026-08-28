import { forwardRef, useId, type InputHTMLAttributes } from 'react';

import estilos from './Entrada.module.css';

type PropiedadesDeEntrada = InputHTMLAttributes<HTMLInputElement> & {
  mensajeDeError?: string;
};

/**
 * Campo de texto compatible con `register` de React Hook Form.
 *
 * El `ref` llega hasta el `input` nativo. Si hay `mensajeDeError`, se anuncia
 * debajo y queda enlazado con `aria-describedby` sin pisar una pista que el
 * padre ya hubiera pasado.
 */
export const Entrada = forwardRef<HTMLInputElement, PropiedadesDeEntrada>(function Entrada(
  { mensajeDeError, className, id, 'aria-describedby': descritoPor, ...rest },
  ref
) {
  const idGenerado = useId();
  const idDelCampo = id ?? idGenerado;
  const idDeError = `${idDelCampo}-error`;
  const tieneError = mensajeDeError !== undefined && mensajeDeError !== '';

  const clases = [estilos.entrada];
  if (tieneError) {
    clases.push(estilos.entradaConError);
  }
  if (className !== undefined && className !== '') {
    clases.push(className);
  }

  return (
    <div className={estilos.envoltorio}>
      <input
        {...rest}
        ref={ref}
        id={idDelCampo}
        className={clases.filter((parte) => parte !== undefined && parte !== '').join(' ')}
        aria-invalid={tieneError}
        aria-describedby={unirIds(descritoPor, tieneError ? idDeError : undefined)}
      />
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
