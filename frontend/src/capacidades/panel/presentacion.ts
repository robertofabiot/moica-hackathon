import { nombreDeContraparte } from '../solicitud/presentacion';
import type { EstadoSolicitud, ResumenDeSolicitudServicio } from '../solicitud/tipos';

export const LIMITE_DE_ACTIVIDAD = 4;

/** El primer nombre, para saludos. Si el texto viene vacío, se devuelve tal cual. */
export function primerNombreDe(nombreCompleto: string): string {
  return nombreCompleto.trim().split(/\s+/)[0] || nombreCompleto;
}

/** Inicial visible en el avatar de la barra superior. */
export function inicialDe(nombreCompleto: string): string {
  const primero = nombreCompleto.trim().split(/\s+/)[0] ?? '';
  return primero.slice(0, 1).toUpperCase();
}

export type ItemDeActividad = {
  idSolicitudServicio: number;
  descripcion: string;
  estado: EstadoSolicitud;
  fechaCreacion: string;
};

/**
 * Une enviadas y recibidas, deja una por identificador y se queda con las más recientes.
 */
export function actividadReciente(
  enviadas: ResumenDeSolicitudServicio[] | undefined,
  recibidas: ResumenDeSolicitudServicio[] | undefined,
  idUsuario: number | undefined,
  limite = LIMITE_DE_ACTIVIDAD
): ItemDeActividad[] {
  const porId = new Map<number, ResumenDeSolicitudServicio>();
  for (const item of [...(enviadas ?? []), ...(recibidas ?? [])]) {
    porId.set(item.idSolicitudServicio, item);
  }
  return [...porId.values()]
    .sort(
      (izquierda, derecha) =>
        new Date(derecha.fechaCreacion).getTime() - new Date(izquierda.fechaCreacion).getTime()
    )
    .slice(0, limite)
    .map((item) => ({
      idSolicitudServicio: item.idSolicitudServicio,
      descripcion: `Solicitud para ${item.nombreServicio} · ${nombreDeContraparte(item, idUsuario)}`,
      estado: item.estadoActual,
      fechaCreacion: item.fechaCreacion,
    }));
}
