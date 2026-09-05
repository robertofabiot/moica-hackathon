import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import { Boton, IconoBalanza, IconoDocumento, IconoEscudo } from '../../../comun/componentes/ui';
import { RUTA_ADMIN_CASOS } from '../../moderacion';
import { RUTA_ADMIN_VERIFICACIONES } from '../../verificacion';
import { obtenerResumenAdministrativo } from '../api';
import estilos from './admin.module.css';

/**
 * Pantalla del área administrativa.
 *
 * P3 protegió el área, P4V le dio la cola de verificaciones documentales y P10A la bandeja de casos
 * de moderación. Además se muestra con qué cuenta se entró y desde cuándo tiene permisos, que es lo
 * que demuestra que la protección funciona de extremo a extremo.
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
        <header className={estilos.encabezado}>
          <p className={estilos.insignia}>
            <IconoEscudo />
            Área de Administración
          </p>
          <h1 className={estilos.titulo}>Área administrativa</h1>
          <p className={estilos.explicacion}>
            Entraste con una cuenta administrativa y el segundo factor verificado en esta sesión.
          </p>
        </header>

        {resumen.isPending && (
          <p className={estilos.explicacion} role="status">
            Cargando el área administrativa…
          </p>
        )}

        {resumen.isError && (
          <div className={estilos.aviso}>
            <p role="alert">
              {resumen.error instanceof ErrorDeApi
                ? resumen.error.message
                : 'No pudimos abrir el área administrativa.'}
            </p>
            <Boton type="button" onClick={() => void resumen.refetch()}>
              Reintentar
            </Boton>
          </div>
        )}

        {resumen.data && (
          <section className={estilos.credencial} aria-label="Cuenta administradora">
            <span className={estilos.avatar} aria-hidden="true">
              {inicialesDe(resumen.data.nombreCompleto)}
            </span>
            <dl className={estilos.datosDeLaCuenta}>
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
          </section>
        )}

        <nav className={estilos.modulos} aria-label="Funciones administrativas">
          <article className={estilos.modulo}>
            <span className={estilos.iconoDeModulo} aria-hidden="true">
              <IconoDocumento />
            </span>
            <h2 className={estilos.tituloDeModulo}>Verificaciones documentales</h2>
            <p className={estilos.explicacion}>
              Revisa los expedientes de los prestadores, aprueba o rechaza sus solicitudes y revoca
              una verificación ya concedida.
            </p>
            <Boton
              className={estilos.accionDeModulo}
              forma="pildora"
              to={RUTA_ADMIN_VERIFICACIONES}
            >
              Verificaciones documentales
            </Boton>
          </article>

          <article className={estilos.modulo}>
            <span
              className={`${estilos.iconoDeModulo} ${estilos.iconoDeAlerta}`}
              aria-hidden="true"
            >
              <IconoBalanza />
            </span>
            <h2 className={estilos.tituloDeModulo}>Moderación de casos</h2>
            <p className={estilos.explicacion}>
              Revisa los casos que abren los participantes, asigna responsables y registra el
              resultado y la resolución de cada uno.
            </p>
            <Boton
              className={estilos.accionDeModulo}
              forma="pildora"
              variante="secundario"
              to={RUTA_ADMIN_CASOS}
            >
              Casos de moderación
            </Boton>
          </article>
        </nav>

        <p className={estilos.pie}>
          <Link className={estilos.enlaceDePie} to="/">
            Volver al inicio
          </Link>
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

function inicialesDe(nombre: string): string {
  const partes = nombre
    .trim()
    .split(/\s+/)
    .filter((parte) => parte.length > 0);
  if (partes.length === 0) {
    return '?';
  }
  if (partes.length === 1) {
    return (partes[0] ?? '').slice(0, 2).toUpperCase();
  }
  const primera = partes[0]?.[0] ?? '';
  const ultima = partes[partes.length - 1]?.[0] ?? '';
  return `${primera}${ultima}`.toUpperCase();
}
