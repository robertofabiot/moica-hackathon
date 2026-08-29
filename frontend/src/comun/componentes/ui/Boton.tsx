import { forwardRef, type ButtonHTMLAttributes } from 'react';

import estilos from './Boton.module.css';

export type VarianteDeBoton = 'primario' | 'secundario' | 'contorno';

type PropiedadesDeBoton = ButtonHTMLAttributes<HTMLButtonElement> & {
  variante?: VarianteDeBoton;
};

/**
 * Botón reutilizable del sistema de diseño.
 *
 * La variante por omisión es la primaria. `type` arranca en `button` para no
 * enviar un formulario por accidente; las acciones de envío pasan `submit`.
 */
export const Boton = forwardRef<HTMLButtonElement, PropiedadesDeBoton>(function Boton(
  { variante = 'primario', type = 'button', className, ...rest },
  ref
) {
  return (
    <button
      ref={ref}
      type={type}
      className={unirClases(estilos.boton, claseDeVariante(variante), className)}
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
