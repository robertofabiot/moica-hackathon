import type {
  EstadoDeCaso,
  EstadoDeCuenta,
  ResultadoDeCaso,
  TipoDeActor,
  TipoDeEvento,
} from './tipos';

/**
 * Cómo se nombran en pantalla los dominios del caso.
 *
 * El backend envía el valor del diccionario; aquí se traduce a la voz de Moica. La traducción vive
 * en un solo sitio para que la bandeja, el expediente y el historial no se contradigan.
 */

const ESTADOS: Record<EstadoDeCaso, string> = {
  ABIERTO: 'Abierto',
  EN_REVISION: 'En revisión',
  CERRADO: 'Cerrado',
  REABIERTO: 'Reabierto',
};

const RESULTADOS: Record<ResultadoDeCaso, string> = {
  PROCEDENTE: 'Procedente',
  DESESTIMADO: 'Desestimado',
};

const ACTORES: Record<TipoDeActor, string> = {
  USUARIO: 'Usuario',
  ADMINISTRADOR: 'Administración',
  SISTEMA: 'Sistema',
};

const ESTADOS_DE_CUENTA: Record<EstadoDeCuenta, string> = {
  ACTIVA: 'Activa',
  RESTRINGIDA_TEMPORAL: 'Restringida temporalmente',
  SUSPENDIDA_TEMPORAL: 'Suspendida temporalmente',
  SUSPENDIDA_PERMANENTE: 'Suspendida permanentemente',
};

const EVENTOS: Record<TipoDeEvento, string> = {
  CASO_ABIERTO: 'Caso abierto',
  RESPONSABLE_ASIGNADO: 'Responsable asignado',
  ESTADO_CASO_CAMBIADO: 'Cambio de estado',
  RESOLUCION_REGISTRADA: 'Resolución registrada',
  MEDIDA_APLICADA: 'Medida aplicada',
  MEDIDA_REVOCADA: 'Medida revocada',
  MEDIDA_EXPIRADA: 'Medida expirada',
  ESTADO_CUENTA_CAMBIADO: 'Cambio de estado de cuenta',
  APELACION_PRESENTADA: 'Apelación presentada',
  APELACION_ACEPTADA: 'Apelación aceptada',
  APELACION_RECHAZADA: 'Apelación rechazada',
  CASO_REABIERTO: 'Caso reabierto',
};

export function nombreDelEstado(estado: EstadoDeCaso): string {
  return ESTADOS[estado];
}

export function nombreDelResultado(resultado: ResultadoDeCaso): string {
  return RESULTADOS[resultado];
}

export function nombreDelActor(actor: TipoDeActor): string {
  return ACTORES[actor];
}

export function nombreDelEstadoDeCuenta(estado: EstadoDeCuenta): string {
  return ESTADOS_DE_CUENTA[estado];
}

export function nombreDelEvento(evento: TipoDeEvento): string {
  return EVENTOS[evento];
}

/** Fecha y hora legibles. El caso las necesita con hora: dos eventos caben en el mismo día. */
export function fechaLegible(instante: string): string {
  return new Date(instante).toLocaleString('es-NI', {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}
