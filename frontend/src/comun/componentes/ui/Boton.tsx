import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { Link } from 'react-router';

import estilos from './Boton.module.css';

export type VarianteDeBoton = 'primario' | 'secundario' | 'contorno' | 'fantasma';
export type FormaDeBoton = 'normal' | 'pildora';

type PropiedadesComunes = {
  variante?: VarianteDeBoton;
  forma?: FormaDeBoton;
  className?: string;
  children?: ReactNode;
};

type PropiedadesComoBoton = PropiedadesComunes &
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
    to?: undefined;
  };

type PropiedadesComoEnlace = PropiedadesComunes & {
  to: string;
};

export type PropiedadesDeBoton = PropiedadesComoBoton | PropiedadesComoEnlace;

/**
 * Botón reutilizable del sistema de diseño.
 *
 * La variante por omisión es la primaria. `type` arranca en `button` para no
 * enviar un formulario por accidente; las acciones de envío pasan `submit`.
 * `forma="pildora"` redondea el botón para barras de búsqueda y llamadas a la acción.
 * Con `to`, se pinta como enlace con la misma apariencia.
 */
export const Boton = forwardRef<HTMLButtonElement, PropiedadesDeBoton>(function Boton(props, ref) {
  const variante = props.variante ?? 'primario';
  const forma = props.forma ?? 'normal';
  const clases = unirClases(
    estilos.boton,
    claseDeVariante(variante),
    forma === 'pildora' ? estilos.pildora : undefined,
    props.className
  );

  if (props.to !== undefined) {
    return (
      <Link to={props.to} className={clases}>
        {props.children}
      </Link>
    );
  }

  const {
    variante: _variante,
    forma: _forma,
    className: _className,
    type = 'button',
    to: _to,
    ...rest
  } = props;

  return <button ref={ref} type={type} className={clases} {...rest} />;
});

function claseDeVariante(variante: VarianteDeBoton): string | undefined {
  if (variante === 'secundario') {
    return estilos.secundario;
  }
  if (variante === 'contorno') {
    return estilos.contorno;
  }
  if (variante === 'fantasma') {
    return estilos.fantasma;
  }
  return estilos.primario;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
