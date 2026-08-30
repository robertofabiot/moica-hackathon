import { forwardRef, useId, useState, type InputHTMLAttributes, type ReactNode } from 'react';

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
 *
 * Cuando `type="password"`, se agrega automáticamente un botón para alternar
 * la visibilidad del texto. El campo arranca oculto y el usuario lo destapa
 * con un clic o con el teclado.
 */
export const Entrada = forwardRef<HTMLInputElement, PropiedadesDeEntrada>(function Entrada(
  {
    mensajeDeError,
    icono,
    variante = 'predeterminada',
    className,
    id,
    type,
    'aria-describedby': descritoPor,
    ...rest
  },
  ref
) {
  const idGenerado = useId();
  const idDelCampo = id ?? idGenerado;
  const idDeError = `${idDelCampo}-error`;
  const tieneError = mensajeDeError !== undefined && mensajeDeError !== '';

  const esContrasena = type === 'password';
  const [visible, setVisible] = useState(false);

  const clases = [estilos.entrada];
  if (variante === 'fusionada') {
    clases.push(estilos.fusionada);
  }
  if (tieneError) {
    clases.push(estilos.entradaConError);
  }
  if (esContrasena) {
    clases.push(estilos.entradaConToggle);
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
          type={esContrasena ? (visible ? 'text' : 'password') : type}
          className={clases.filter((parte) => parte !== undefined && parte !== '').join(' ')}
          aria-invalid={tieneError}
          aria-describedby={unirIds(descritoPor, tieneError ? idDeError : undefined)}
        />
        {esContrasena && (
          <button
            type="button"
            className={estilos.toggleDeContrasena}
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            tabIndex={-1}
          >
            {visible ? <IconoOjoCerrado /> : <IconoOjoAbierto />}
          </button>
        )}
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

/* ─── Iconos de ojo (SVG inline, sin dependencias externas) ─── */

function IconoOjoAbierto() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function IconoOjoCerrado() {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M10.733 5.076a10.744 10.744 0 0 1 11.205 6.575 1 1 0 0 1 0 .696 10.747 10.747 0 0 1-1.444 2.49" />
      <path d="M14.084 14.158a3 3 0 0 1-4.242-4.242" />
      <path d="M17.479 17.499a10.75 10.75 0 0 1-15.417-5.151 1 1 0 0 1 0-.696 10.75 10.75 0 0 1 4.446-5.143" />
      <path d="m2 2 20 20" />
    </svg>
  );
}
