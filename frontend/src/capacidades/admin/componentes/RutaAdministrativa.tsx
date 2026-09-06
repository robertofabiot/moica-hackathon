import type { ReactNode } from 'react';

import AccesoNoAutorizado from '../../../paginas/AccesoNoAutorizado';
import { RutaProtegida, useSesionActual } from '../../auth';

/**
 * Deja ver el área administrativa solo a quien cumple sus dos condiciones.
 *
 * Igual que {@link RutaProtegida}, esto **no** es un control de seguridad: quien pida
 * `/api/admin/...` sin rol o sin el segundo factor verificado recibe 403 del backend. Aquí lo que
 * se decide es qué explicación se muestra, en lugar de dejar una pantalla rota.
 *
 * Se apoya en {@code RutaProtegida}, que ya resuelve «sin sesión» y «sesión provisional».
 */
export default function RutaAdministrativa({ children }: { children: ReactNode }) {
  return (
    <RutaProtegida>
      <ConPermisosAdministrativos>{children}</ConPermisosAdministrativos>
    </RutaProtegida>
  );
}

function ConPermisosAdministrativos({ children }: { children: ReactNode }) {
  const sesion = useSesionActual();

  if (!sesion.data) {
    return null;
  }

  if (!sesion.data.usuario.esAdministrador) {
    return <AccesoNoAutorizado tipo="permisos-insuficientes" />;
  }

  if (!sesion.data.sesion.segundoFactorVerificado) {
    return <AccesoNoAutorizado tipo="requiere-segundo-factor" />;
  }

  return <>{children}</>;
}
