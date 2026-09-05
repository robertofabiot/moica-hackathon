import type { ReactNode } from 'react';

import { BarraLateral } from '../../../comun/componentes/ui';
import propios from './solicitudes.module.css';

/**
 * Destinos de la barra. Se escriben como rutas literales para no importar el
 * panel (ni otras capacidades) desde este módulo y evitar ciclos.
 */
const DESTINOS_DE_BARRA = {
  inicio: '/panel',
  mensajes: '/mensajes',
  perfil: '/prestador',
  configuracion: '/seguridad',
};

/**
 * Marco con barra lateral compartido por las pantallas del ciclo de solicitudes.
 * En teléfono la barra queda oculta; desde escritorio empuja el contenido.
 */
export default function MarcoDeSolicitudes({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={propios.paginaAsistente}>
      <div className={propios.barraLateralDeAsistente}>
        <BarraLateral destinos={DESTINOS_DE_BARRA} />
      </div>
      <main className={propios.principalAsistente}>
        <div className={unirClases(propios.contenidoGestion, className)}>{children}</div>
      </main>
    </div>
  );
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
