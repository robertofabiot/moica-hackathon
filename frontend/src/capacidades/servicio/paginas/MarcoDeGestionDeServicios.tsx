import type { ReactNode } from 'react';

import { BarraLateral } from '../../../comun/componentes/ui';
import propios from './servicios.module.css';

/**
 * Destinos de la barra. Se escriben como rutas literales para no importar el
 * panel (ni otras capacidades) desde este módulo: `PanelUsuario` ya importa el
 * listado de servicios, y un import inverso deja `RUTA_PANEL` en zona muerta.
 */
const DESTINOS_DE_BARRA = {
  inicio: '/panel',
  mensajes: '/mensajes',
  perfil: '/prestador',
  configuracion: '/seguridad',
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
