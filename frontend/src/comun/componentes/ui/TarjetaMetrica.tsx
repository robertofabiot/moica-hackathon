import type { ReactNode } from 'react';
import { Link } from 'react-router';

import estilos from './TarjetaMetrica.module.css';

type PropiedadesDeTarjetaMetrica = {
  titulo: string;
  valor: ReactNode;
  icono: ReactNode;
  destino?: string;
};

/**
 * Tarjeta de métrica para paneles: título, valor destacado e icono.
 *
 * Si llega `destino`, toda la tarjeta es un enlace. Si no, es un contenedor
 * estático —por ejemplo, una calificación que no navega a ninguna pantalla.
 */
export function TarjetaMetrica({ titulo, valor, icono, destino }: PropiedadesDeTarjetaMetrica) {
  const etiquetaAccesible =
    typeof valor === 'string' || typeof valor === 'number' ? `${titulo} ${valor}` : undefined;

  const contenido = (
    <>
      <span className={estilos.cabecera}>
        <span className={estilos.titulo}>{titulo}</span>
        <span className={estilos.icono} aria-hidden="true">
          {icono}
        </span>
      </span>
      <span className={estilos.valor}>{valor}</span>
    </>
  );

  if (destino !== undefined) {
    return (
      <Link className={estilos.tarjeta} to={destino} aria-label={etiquetaAccesible}>
        {contenido}
      </Link>
    );
  }

  return (
    <div className={estilos.tarjeta} aria-label={etiquetaAccesible}>
      {contenido}
    </div>
  );
}
