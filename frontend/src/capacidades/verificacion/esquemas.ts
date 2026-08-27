import { z } from 'zod';

import type { NivelSolicitado, TipoDeDocumento } from './tipos';

/**
 * Reglas de los formularios de verificación.
 *
 * Validar aquí mejora la experiencia, pero la validación del servidor es la que manda: el backend
 * comprueba además la firma binaria real del archivo, que el navegador no mira.
 *
 * Los formularios con campos de texto —el motivo de un rechazo o de una revocación— usan React Hook
 * Form con estos esquemas. La elección de archivos no es un campo de texto: se acumula en estado
 * local y se valida archivo por archivo con {@link esquemaDeArchivo} en cuanto se elige, que es
 * cuando la persona puede corregirlo.
 */

/** Formatos que admite el expediente, según `Docs/Core/prompt.md` §5. */
export const TIPOS_MIME_ADMITIDOS = ['image/jpeg', 'image/png', 'application/pdf'] as const;

/** Máximo por archivo. Coincide con el valor por omisión de `MOICA_DOCUMENTO_TAMANO_MAXIMO`. */
export const TAMANO_MAXIMO_BYTES = 5 * 1024 * 1024;

export const esquemaDeArchivo = z.object({
  tipoMime: z
    .string()
    .refine(
      (valor) => (TIPOS_MIME_ADMITIDOS as readonly string[]).includes(valor),
      'Solo se admiten archivos JPEG, PNG o PDF.'
    ),
  tamanoBytes: z
    .number()
    .positive('El archivo está vacío.')
    .max(TAMANO_MAXIMO_BYTES, 'El archivo supera el máximo de 5 MB.'),
});

export const esquemaDeMotivo = z.object({
  observacion: z
    .string()
    .trim()
    .min(1, 'Escribe el motivo. Quien presentó el expediente necesita saber qué pasó.')
    .max(1000, 'El motivo no puede superar los 1000 caracteres.'),
});

export type CamposDeMotivo = z.infer<typeof esquemaDeMotivo>;

/**
 * Revocar exige, además del motivo, una confirmación explícita.
 *
 * Es la única acción que le quita a un perfil algo que ya tenía concedido, y en el caso de la
 * básica arrastra también la profesional. Una casilla obligatoria obliga a decirlo a propósito y se
 * marca con el teclado igual que con el ratón.
 */
export const esquemaDeRevocacion = esquemaDeMotivo.extend({
  confirmo: z.boolean().refine((marcada) => marcada, {
    error: 'Marca la casilla para confirmar que entiendes lo que implica revocar.',
  }),
});

export type CamposDeRevocacion = z.infer<typeof esquemaDeRevocacion>;

/** Qué le falta al expediente para poder enviarse, o `null` si ya está completo. */
export function queLeFaltaAlExpediente(
  nivel: NivelSolicitado,
  tipos: TipoDeDocumento[]
): string | null {
  if (tipos.length === 0) {
    return 'Adjunta al menos un documento.';
  }
  if (nivel === 'BASICA' && !tipos.includes('IDENTIDAD')) {
    return 'La verificación básica necesita al menos un documento oficial de identidad.';
  }
  if (nivel === 'PROFESIONAL' && !tipos.some((tipo) => tipo !== 'IDENTIDAD')) {
    return 'La verificación profesional necesita al menos un respaldo profesional, técnico o comercial.';
  }
  return null;
}
