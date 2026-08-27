import { obtenerJson } from '../../comun/api';
import type { ResumenAdministrativo } from './tipos';

/**
 * Llamadas al área administrativa.
 *
 * Solo hay una: P3 protege el área, no la llena. Las bandejas de verificación documental y de
 * moderación llegan con sus propios incrementos.
 */

const RUTA_RESUMEN = '/api/admin/resumen';

/**
 * Describe la sesión administrativa en curso.
 *
 * El backend responde 401 sin sesión y 403 si falta el rol o el segundo factor verificado. El error
 * llega tal cual para que la pantalla pueda distinguir un caso del otro.
 */
export function obtenerResumenAdministrativo(): Promise<ResumenAdministrativo> {
  return obtenerJson<ResumenAdministrativo>(RUTA_RESUMEN);
}
