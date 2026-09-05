import secciones from '../../../comun/estilos/secciones.module.css';
import {
  fechaLegible,
  nombreDelActor,
  nombreDelEstado,
  nombreDelEstadoDeCuenta,
  nombreDelEvento,
  nombreDelResultado,
} from '../etiquetas';
import type { VersionDeCaso } from '../tipos';
import propios from './acciones.module.css';

/**
 * El historial de versiones del caso, de la más antigua a la más reciente.
 *
 * Cada fila es la fotografía completa de un momento, no solo el campo que cambió: por eso puede
 * leerse una decisión anterior tal como se tomó, con el estado de cuenta que la persona reportada
 * tenía entonces.
 *
 * La versión vigente se marca como tal. Solo puede haber una, y su periodo no tiene fin.
 *
 * Quien originó el evento y quien respondía por el caso son personas distintas en una reasignación,
 * así que se nombran por separado. Las versiones anteriores a la primera asignación no tienen
 * responsable y no se les inventa uno.
 */
export default function HistorialDelCaso({ versiones }: { versiones: VersionDeCaso[] }) {
  return (
    <section className={secciones.seccion} aria-labelledby="historial">
      <h2 className={secciones.tituloDeSeccion} id="historial">
        Historial del caso
      </h2>
      <p className={secciones.explicacion}>
        Cada cambio cierra la versión anterior y crea una nueva, en la misma operación.
      </p>

      <ol className={propios.versiones}>
        {versiones.map((version) => (
          <li key={version.idHistorialCaso} className={propios.version}>
            <p className={propios.versionTitulo}>
              {nombreDelEvento(version.tipoEvento)}
              {version.esVersionActual && (
                <span className={propios.insigniaVigente}>Versión vigente</span>
              )}
            </p>
            <p className={propios.versionDetalle}>
              Versión {version.numeroVersion} · {nombreDelActor(version.tipoActor)}
              {version.nombreActor !== null && `: ${version.nombreActor}`} ·{' '}
              {fechaLegible(version.fechaInicioVigencia)}
            </p>
            {version.nombreAdministradorResponsable !== null && (
              <p className={propios.versionDetalle}>
                Responsable entonces: {version.nombreAdministradorResponsable}
              </p>
            )}
            <p className={propios.versionDetalle}>
              Caso {nombreDelEstado(version.estadoCaso).toLowerCase()}
              {version.resultadoCaso !== null &&
                ` · ${nombreDelResultado(version.resultadoCaso)}`}{' '}
              · Cuenta reportada: {nombreDelEstadoDeCuenta(version.estadoCuenta).toLowerCase()}
            </p>
            <p className={propios.versionCambio}>{version.detalleCambio}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}
