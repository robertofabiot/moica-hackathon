import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { RUTA_ADMIN_VERIFICACIONES } from '../../verificacion';
import { obtenerResumenAdministrativo } from '../api';
import estilos from './admin.module.css';

/**
 * Pantalla mínima del área administrativa.
 *
 * P3 protegió el área y P4V le da su primera función: la cola de verificaciones documentales.
 * Además de ella se muestra con qué cuenta se entró y desde cuándo tiene permisos, que es lo que
 * demuestra que la protección funciona de extremo a extremo.
 */
export default function PanelAdministrativo() {
  const resumen = useQuery({
    queryKey: ['admin', 'resumen'],
    queryFn: obtenerResumenAdministrativo,
    retry: false,
  });

  return (
    <main className={estilos.pantalla}>
      <div className={estilos.contenido}>
        <h1 className={estilos.titulo}>Área administrativa</h1>
        <p className={estilos.explicacion}>
          Entraste con una cuenta administrativa y el segundo factor verificado en esta sesión.
        </p>

        {resumen.isPending && (
          <p className={estilos.explicacion} role="status">
            Cargando el área administrativa…
          </p>
        )}

        {resumen.isError && (
          <div className={estilos.pendiente}>
            <p role="alert">
              {resumen.error instanceof ErrorDeApi
                ? resumen.error.message
                : 'No pudimos abrir el área administrativa.'}
            </p>
            <button type="button" onClick={() => void resumen.refetch()}>
              Reintentar
            </button>
          </div>
        )}

        {resumen.data && (
          <dl className={estilos.ficha}>
            <div>
              <dt className={estilos.etiqueta}>Cuenta</dt>
              <dd className={estilos.valor}>{resumen.data.nombreCompleto}</dd>
            </div>
            <div>
              <dt className={estilos.etiqueta}>Correo</dt>
              <dd className={estilos.valor}>{resumen.data.correoElectronico}</dd>
            </div>
            <div>
              <dt className={estilos.etiqueta}>Permisos administrativos desde</dt>
              <dd className={estilos.valor}>{fechaLegible(resumen.data.fechaAsignacion)}</dd>
            </div>
          </dl>
        )}

        <nav className={estilos.pendiente} aria-label="Funciones administrativas">
          <p>
            <Link to={RUTA_ADMIN_VERIFICACIONES}>Verificaciones documentales</Link>
          </p>
          <p className={estilos.explicacion}>
            Revisa los expedientes de los prestadores, aprueba o rechaza sus solicitudes y revoca
            una verificación ya concedida.
          </p>
        </nav>

        <p className={estilos.pendiente}>La moderación de casos llega con su propio incremento.</p>

        <p className={estilos.pie}>
          <Link to="/">Volver al inicio</Link>
        </p>
      </div>
    </main>
  );
}

function fechaLegible(fechaIso: string): string {
  const fecha = new Date(fechaIso);
  return Number.isNaN(fecha.getTime())
    ? fechaIso
    : fecha.toLocaleDateString('es-NI', { year: 'numeric', month: 'long', day: 'numeric' });
}
