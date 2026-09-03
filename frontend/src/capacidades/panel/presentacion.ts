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

export type TareaPendiente = {
  id: string;
  texto: string;
  destino: string;
};

/**
 * Acciones que conviene atender ahora, según perfil, verificación y solicitudes.
 *
 * `perfil` en `undefined` significa que todavía se está consultando: no se ofrece
 * crear el perfil hasta saber si ya existe.
 */
export function tareasProximas(entrada: {
  pendientesRecibidas: number;
  solicitudesEnviadas?: number;
  solicitudesAceptadas?: number;
  perfil: { nivelVerificacion: string } | null | undefined;
  cantidadDeServicios: number;
  serviciosConsultados: boolean;
  destinoSolicitudes: string;
  destinoPerfil: string;
  destinoNuevoServicio: string;
  destinoMensajes?: string;
  destinoExplorar?: string;
}): TareaPendiente[] {
  const tareas: TareaPendiente[] = [];

  // Si es un usuario que solo es cliente (no tiene perfil de prestador)
  if (entrada.perfil === null) {
    if ((entrada.solicitudesAceptadas ?? 0) > 0) {
      const n = entrada.solicitudesAceptadas ?? 0;
      tareas.push({
        id: 'aceptadas-cliente',
        texto:
          n === 1
            ? 'Tienes 1 solicitud aceptada en curso. Coordina con el prestador'
            : `Tienes ${n} solicitudes aceptadas en curso`,
        destino: entrada.destinoMensajes ?? entrada.destinoSolicitudes,
      });
    } else if ((entrada.solicitudesEnviadas ?? 0) > 0) {
      const n = entrada.solicitudesEnviadas ?? 0;
      tareas.push({
        id: 'enviadas-cliente',
        texto:
          n === 1
            ? 'Tienes 1 solicitud esperando respuesta del prestador'
            : `Tienes ${n} solicitudes en espera de confirmación`,
        destino: entrada.destinoSolicitudes,
      });
    } else {
      tareas.push({
        id: 'explorar-cliente',
        texto: 'Encuentra y contrata profesionales de confianza cerca de ti',
        destino: entrada.destinoExplorar ?? '/explorar',
      });
    }
    return tareas;
  }

  // Tareas para prestador
  if (entrada.pendientesRecibidas > 0) {
    const n = entrada.pendientesRecibidas;
    tareas.push({
      id: 'pendientes',
      texto:
        n === 1
          ? 'Tienes 1 solicitud pendiente de respuesta'
          : `Tienes ${n} solicitudes pendientes de respuesta`,
      destino: entrada.destinoSolicitudes,
    });
  }

  if (entrada.perfil !== undefined) {
    if (entrada.perfil.nivelVerificacion === 'SIN_VERIFICAR') {
      tareas.push({
        id: 'verificar',
        texto: 'Envía tu documentación para verificar tu perfil',
        destino: entrada.destinoPerfil,
      });
    } else if (entrada.serviciosConsultados && entrada.cantidadDeServicios === 0) {
      tareas.push({
        id: 'publicar',
        texto: 'Publica tu primer servicio para recibir clientes',
        destino: entrada.destinoNuevoServicio,
      });
    }
  }

  return tareas;
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
