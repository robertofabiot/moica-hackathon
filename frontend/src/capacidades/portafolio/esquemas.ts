import { z } from 'zod';

/**
 * Reglas de los formularios del portafolio.
 *
 * Los máximos son los del diccionario (150 el título, 200 el texto alternativo) y el límite de la
 * aplicación documentado en el contrato de la API (3000 la descripción). La fecha es opcional: el
 * campo vacío viaja como ausente, no como cadena vacía.
 */

export const esquemaDeTrabajo = z.object({
  titulo: z
    .string()
    .trim()
    .min(1, 'Escribe un título para el trabajo.')
    .max(150, 'El título no puede superar los 150 caracteres.'),
  descripcion: z
    .string()
    .trim()
    .min(1, 'Describe el trabajo que realizaste.')
    .max(3000, 'La descripción no puede superar los 3000 caracteres.'),
  fechaRealizacion: z
    .string()
    .trim()
    .refine(
      (fecha) => fecha === '' || !Number.isNaN(Date.parse(fecha)),
      'Escribe una fecha válida o deja el campo vacío.'
    ),
});

export const esquemaDeImagen = z.object({
  textoAlternativo: z
    .string()
    .trim()
    .max(200, 'El texto alternativo no puede superar los 200 caracteres.'),
});

export type CamposDeTrabajo = z.infer<typeof esquemaDeTrabajo>;
export type CamposDeImagen = z.infer<typeof esquemaDeImagen>;
