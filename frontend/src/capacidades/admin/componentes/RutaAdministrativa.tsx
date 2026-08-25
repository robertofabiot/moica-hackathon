import type { ReactNode } from 'react';
import { Link } from 'react-router';

import { RutaProtegida, useSesionActual } from '../../auth';
import estilos from '../paginas/admin.module.css';

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
    return <AccesoDenegado explicacion="Esta cuenta no tiene permisos administrativos en Moica." />;
  }

  if (!sesion.data.sesion.segundoFactorVerificado) {
    return (
      <AccesoDenegado explicacion="El área administrativa exige el segundo factor verificado en esta sesión. Actívalo en la seguridad de tu cuenta y vuelve a entrar." />
    );
  }

  return <>{children}</>;
}

function AccesoDenegado({ explicacion }: { explicacion: string }) {
  return (
    <main className={estilos.pantalla}>
      <div className={estilos.contenido}>
        <h1 className={estilos.titulo}>Acceso denegado</h1>
        <p className={estilos.explicacion} role="alert">
          {explicacion}
        </p>
        <p className={estilos.pie}>
          <Link to="/">Volver al inicio</Link>
        </p>
      </div>
    </main>
  );
}
