import type {
  EstadoDeSolicitud,
  NivelSolicitado,
  NivelVerificacion,
  TipoDeDocumento,
} from './tipos';

/**
 * Cómo se lee cada valor del dominio y cómo se muestran fechas y tamaños.
 *
 * Vive aparte de los componentes porque lo comparten las dos experiencias —la del prestador y la de
 * revisión— y porque así el nombre que ve una persona se escribe una sola vez. Los identificadores
 * del dominio no se traducen a la ligera: son los mismos del diccionario de datos y solo cambia su
 * presentación.
 */

const ESTADOS: Record<EstadoDeSolicitud, string> = {
  PENDIENTE: 'Pendiente de revisión',
  EN_REVISION: 'En revisión',
  APROBADA: 'Aprobada',
  RECHAZADA: 'Rechazada',
  REVOCADA: 'Revocada',
};

const NIVELES_SOLICITADOS: Record<NivelSolicitado, string> = {
  BASICA: 'Verificación básica',
  PROFESIONAL: 'Verificación profesional',
};

const INSIGNIAS: Record<NivelVerificacion, string> = {
  SIN_VERIFICAR: 'Sin verificar',
  VERIFICADO_BASICO: 'Verificado Básico',
  PROFESIONAL_VERIFICADO: 'Profesional Verificado',
};

const TIPOS: Record<TipoDeDocumento, string> = {
  IDENTIDAD: 'Documento de identidad',
  CERTIFICACION: 'Certificación técnica o profesional',
  CONSTANCIA: 'Constancia de experiencia',
  REGISTRO_NEGOCIO: 'Registro del negocio',
  OTRO_RESPALDO: 'Otro respaldo',
};

/** Los tipos en el orden en que se ofrecen al elegir, del más común al menos. */
export const TIPOS_DE_DOCUMENTO = Object.keys(TIPOS) as TipoDeDocumento[];

export function nombreDelEstado(estado: EstadoDeSolicitud): string {
  return ESTADOS[estado];
}

export function nombreDelNivelSolicitado(nivel: NivelSolicitado): string {
  return NIVELES_SOLICITADOS[nivel];
}

export function nombreDeLaInsignia(nivel: NivelVerificacion): string {
  return INSIGNIAS[nivel];
}

export function nombreDelTipoDeDocumento(tipo: TipoDeDocumento): string {
  return TIPOS[tipo];
}

/** Una fecha ISO como la escribiría una persona; si no se puede leer, se muestra tal cual. */
export function fechaLegible(fechaIso: string): string {
  const fecha = new Date(fechaIso);
  return Number.isNaN(fecha.getTime())
    ? fechaIso
    : fecha.toLocaleDateString('es-NI', { year: 'numeric', month: 'long', day: 'numeric' });
}

/** Un tamaño en bytes en la unidad que se entiende de un vistazo. */
export function tamanoLegible(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }
  if (bytes < 1024 * 1024) {
    return `${Math.round(bytes / 1024)} KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
