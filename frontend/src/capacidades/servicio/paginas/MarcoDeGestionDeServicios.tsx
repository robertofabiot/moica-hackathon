import type { ReactNode } from 'react';

import { BarraLateral } from '../../../comun/componentes/ui';
import { RUTA_SEGURIDAD } from '../../auth';
import { RUTA_PANEL } from '../../panel';
import { RUTA_PRESTADOR } from '../../prestador';
import { RUTA_MENSAJES } from '../../solicitud';
import propios from './servicios.module.css';

const DESTINOS_DE_BARRA = {
  inicio: RUTA_PANEL,
  mensajes: RUTA_MENSAJES,
  perfil: RUTA_PRESTADOR,
  configuracion: RUTA_SEGURIDAD,
};

/**
 * Marco con barra lateral del panel, compartido por el listado y la edición
 * de servicios propios. En teléfono la barra queda oculta; desde tableta
 * empuja el contenido como en `NuevoServicio` y `PanelUsuario`.
 */
export default function MarcoDeGestionDeServicios({
  children,
  itemActivo = 'inicio',
}: {
  children: ReactNode;
  itemActivo?: 'inicio' | 'perfil';
}) {
  return (
    <div className={propios.paginaAsistente}>
      <div className={propios.barraLateralDeAsistente}>
        <BarraLateral itemActivo={itemActivo} destinos={DESTINOS_DE_BARRA} />
      </div>
      <main className={propios.principalAsistente}>
        <div className={propios.contenidoGestion}>{children}</div>
      </main>
    </div>
  );
}
