import { forwardRef, type ButtonHTMLAttributes } from 'react';

import estilos from './Boton.module.css';

export type VarianteDeBoton = 'primario' | 'secundario' | 'contorno';
export type FormaDeBoton = 'normal' | 'pildora';

type PropiedadesDeBoton = ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: VarianteDeBoton;
  forma?: FormaDeBoton;
};

/**
 * Botón reutilizable del sistema de diseño.
 *
 * La variante por omisión es la primaria. `type` arranca en `button` para no
 * enviar un formulario por accidente; las acciones de envío pasan `submit`.
 * `forma="pildora"` redondea el botón para barras de búsqueda y llamadas a la acción.
 */
export const Boton = forwardRef<HTMLButtonElement, PropiedadesDeBoton>(function Boton(
  { variante = 'primario', forma = 'normal', type = 'button', className, ...rest },
  ref
) {
  return (
    <button
      ref={ref}
      type={type}
      className={unirClases(
        estilos.boton,
        claseDeVariante(variante),
        forma === 'pildora' ? estilos.pildora : undefined,
        className
      )}
      {...rest}
    />
  );
});

function claseDeVariante(variante: VarianteDeBoton): string | undefined {
  if (variante === 'secundario') {
    return estilos.secundario;
  }
  if (variante === 'contorno') {
    return estilos.contorno;
  }
  return estilos.primario;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
