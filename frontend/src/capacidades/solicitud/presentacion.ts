import type { EstadoCuenta } from '../auth';
import type {
  DatosDeSolicitudServicio,
  EstadoCasoModeracion,
  EstadoSolicitud,
  ResumenDeSolicitudServicio,
  RolCalificado,
} from './tipos';

/** Enviar, aceptar, rechazar y completar exigen cuenta activa. Cancelar no. */
export function cuentaEstaActiva(estadoCuenta: EstadoCuenta | undefined): boolean {
  return estadoCuenta === 'ACTIVA';
}

const ESTADOS: Record<EstadoSolicitud, string> = {
  PENDIENTE: 'Pendiente',
  ACEPTADA: 'Aceptada',
  RECHAZADA: 'Rechazada',
  CANCELADA: 'Cancelada',
  COMPLETADA: 'Completada',
};

export function nombreDelEstado(estado: EstadoSolicitud): string {
  return ESTADOS[estado];
}

export function fechaVisible(valor: string): string {
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    return valor;
  }
  return new Intl.DateTimeFormat('es-NI', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(fecha);
}

export function diaVisible(valor: string): string {
  const [anioTexto, mesTexto, diaTexto] = valor.split('-');
  const anio = Number(anioTexto);
  const mes = Number(mesTexto ?? '1');
  const dia = Number(diaTexto ?? '1');
  const fecha = new Date(anio, mes - 1, dia);
  if (Number.isNaN(fecha.getTime())) {
    return valor;
  }
  return new Intl.DateTimeFormat('es-NI', { dateStyle: 'medium' }).format(fecha);
}

/**
 * Si la solicitud llegó a estar aceptada y, por tanto, tiene hilo.
 *
 * El estado vigente no basta: una `CANCELADA` puede venir de `PENDIENTE` —y entonces nunca hubo
 * chat— o de `ACEPTADA`, y en ese caso el historial queda visible en solo lectura. La diferencia
 * está en el historial, que el detalle ya trae. El backend aplica la misma regla: esto solo decide
 * qué se pinta.
 */
export function hiloHabilitado(solicitud: DatosDeSolicitudServicio): boolean {
  return (
    solicitud.estadoActual === 'ACEPTADA' ||
    solicitud.historial.some((cambio) => cambio.estadoNuevo === 'ACEPTADA')
  );
}

/** Solo se escribe mientras el compromiso sigue vivo. Cancelar o completar lo cierra. */
export function admiteMensajesNuevos(solicitud: DatosDeSolicitudServicio): boolean {
  return solicitud.estadoActual === 'ACEPTADA';
}

/** La revelación de contactos pertenece al cliente participante, nunca al prestador. */
export function puedeVerContactos(
  solicitud: DatosDeSolicitudServicio,
  idUsuario: number | undefined
): boolean {
  return idUsuario === solicitud.idCliente && hiloHabilitado(solicitud);
}

/**
 * Si la solicitud admite calificación.
 *
 * Solo `COMPLETADA`, que es el único estado que cierra el servicio y además es definitivo. Antes de
 * eso no se pinta ni el formulario ni la sección: no hay nada que calificar todavía. Quién puede
 * calificar de verdad lo decide el backend; esto solo evita ofrecer una acción falsa.
 */
export function admiteCalificacion(solicitud: DatosDeSolicitudServicio): boolean {
  return solicitud.estadoActual === 'COMPLETADA';
}

/** Cómo se nombra en pantalla el rol en que quedó calificada la contraparte. */
export function nombreDelRol(rol: RolCalificado): string {
  return rol === 'PRESTADOR' ? 'prestador' : 'cliente';
}

/**
 * Si la solicitud admite un reporte.
 *
 * Es la misma condición que habilita el hilo: haber llegado alguna vez a `ACEPTADA`. Da igual dónde
 * terminara —completada o cancelada después de aceptarse siguen admitiéndolo, porque el trato
 * existió—, y una que nunca se aceptó no lo admite nunca. Quién puede reportar de verdad lo decide
 * el backend; esto solo evita ofrecer una acción falsa.
 */
export function admiteReporte(solicitud: DatosDeSolicitudServicio): boolean {
  return hiloHabilitado(solicitud);
}

const ESTADOS_DE_CASO: Record<EstadoCasoModeracion, string> = {
  ABIERTO: 'Abierto',
  EN_REVISION: 'En revisión',
  CERRADO: 'Cerrado',
  REABIERTO: 'Reabierto',
};

/** Cómo se nombra en pantalla la etapa vigente de un caso de moderación. */
export function nombreDelEstadoDeCaso(estado: EstadoCasoModeracion): string {
  return ESTADOS_DE_CASO[estado];
}

/**
 * Si la bandeja de mensajes debe mostrar esta solicitud.
 *
 * El listado de enviadas/recibidas no trae historial, así que no se puede distinguir una
 * `CANCELADA` que nunca se aceptó. Se incluyen los tres estados y el detalle decide si el hilo
 * existe de verdad.
 */
export function apareceEnBandejaDeMensajes(estado: EstadoSolicitud): boolean {
  return estado === 'ACEPTADA' || estado === 'COMPLETADA' || estado === 'CANCELADA';
}

/** Une enviadas y recibidas, deja una por identificador y ordena por la más reciente. */
export function conversacionesDeBandeja(
  enviadas: ResumenDeSolicitudServicio[] | undefined,
  recibidas: ResumenDeSolicitudServicio[] | undefined
): ResumenDeSolicitudServicio[] {
  const porId = new Map<number, ResumenDeSolicitudServicio>();
  for (const item of [...(enviadas ?? []), ...(recibidas ?? [])]) {
    if (apareceEnBandejaDeMensajes(item.estadoActual)) {
      porId.set(item.idSolicitudServicio, item);
    }
  }
  return [...porId.values()].sort(
    (izquierda, derecha) =>
      new Date(derecha.fechaCreacion).getTime() - new Date(izquierda.fechaCreacion).getTime()
  );
}

/** El otro participante: si esta persona es cliente, el prestador; si no, el cliente. */
export function nombreDeContraparte(
  solicitud: Pick<
    ResumenDeSolicitudServicio,
    'idCliente' | 'idPrestador' | 'nombreCliente' | 'nombrePublicoPrestador'
  >,
  idUsuario: number | undefined
): string {
  return idUsuario === solicitud.idCliente
    ? solicitud.nombrePublicoPrestador
    : solicitud.nombreCliente;
}

/** Iniciales de un nombre público para el avatar de respaldo. */
export function inicialesDeNombre(nombre: string): string {
  const partes = nombre
    .trim()
    .split(/\s+/)
    .filter((parte) => parte.length > 0);
  if (partes.length === 0) {
    return '';
  }
  if (partes.length === 1) {
    const unica = partes[0] ?? '';
    return unica.slice(0, Math.min(2, unica.length)).toUpperCase();
  }
  const primera = partes[0]?.[0] ?? '';
  const ultima = partes[partes.length - 1]?.[0] ?? '';
  return `${primera}${ultima}`.toUpperCase();
}

export function filtrarConversaciones(
  items: ResumenDeSolicitudServicio[],
  consulta: string,
  idUsuario: number | undefined
): ResumenDeSolicitudServicio[] {
  const texto = consulta.trim().toLocaleLowerCase('es');
  if (texto === '') {
    return items;
  }
  return items.filter((item) => {
    const nombre = nombreDeContraparte(item, idUsuario).toLocaleLowerCase('es');
    const servicio = item.nombreServicio.toLocaleLowerCase('es');
    return nombre.includes(texto) || servicio.includes(texto);
  });
}

/** En una conversación viva se lee como presencia; si ya cerró, el estado de la solicitud. */
export function estadoVisibleDeConversacion(estado: EstadoSolicitud): string {
  return estado === 'ACEPTADA' ? 'En línea' : nombreDelEstado(estado);
}

/** Hora corta si es hoy; si no, la fecha corta. Sirve en la fila de la bandeja. */
export function instanteDeLista(valor: string): string {
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    return valor;
  }
  const hoy = new Date();
  const esHoy =
    fecha.getFullYear() === hoy.getFullYear() &&
    fecha.getMonth() === hoy.getMonth() &&
    fecha.getDate() === hoy.getDate();
  return new Intl.DateTimeFormat(
    'es-NI',
    esHoy ? { timeStyle: 'short' } : { dateStyle: 'short' }
  ).format(fecha);
}

/** Solo la hora, para el pie de cada burbuja. */
export function horaDeMensaje(valor: string): string {
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    return valor;
  }
  return new Intl.DateTimeFormat('es-NI', { timeStyle: 'short' }).format(fecha);
}
