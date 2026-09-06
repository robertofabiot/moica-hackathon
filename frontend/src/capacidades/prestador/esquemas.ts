import { z } from 'zod';

/**
 * Reglas de los formularios del perfil de prestador.
 *
 * Los máximos son los del diccionario (120 el nombre público, 500 cada contacto) y los límites de
 * la aplicación documentados en el contrato de la API (3000 la descripción, 1000 la cobertura).
 * Validar aquí mejora la experiencia, pero la validación del servidor es la que manda.
 */

export const esquemaDePerfil = z.object({
  nombrePublico: z
    .string()
    .trim()
    .min(1, 'Escribe el nombre con el que quieres aparecer.')
    .max(120, 'El nombre público no puede superar los 120 caracteres.'),
  descripcion: z
    .string()
    .trim()
    .min(1, 'Cuenta quién eres y qué ofreces.')
    .max(3000, 'La presentación no puede superar los 3000 caracteres.'),
  tipoPrestador: z.enum(['INDEPENDIENTE', 'EMPRENDIMIENTO', 'PYME'], {
    error: 'Elige cómo trabajas.',
  }),
  // El selector entrega texto y la API espera un número, así que el esquema
  // convierte: por eso el formulario usa el tipo de entrada y lo enviado usa el
  // de salida.
  idMunicipioPrincipal: z
    .string()
    .min(1, 'Elige tu municipio principal.')
    .transform((valor) => Number(valor))
    .refine((valor) => Number.isInteger(valor) && valor > 0, 'Elige tu municipio principal.'),
  descripcionCobertura: z
    .string()
    .trim()
    .min(1, 'Describe las zonas donde atiendes.')
    .max(1000, 'La cobertura no puede superar los 1000 caracteres.'),
});

export const esquemaDeContacto = z.object({
  contenido: z
    .string()
    .trim()
    .min(1, 'Escribe el dato de contacto.')
    .max(500, 'Un contacto no puede superar los 500 caracteres.'),
});

/** Lo que escribe el formulario: el municipio todavía es el texto del selector. */
export type CamposDePerfil = z.input<typeof esquemaDePerfil>;

/** Lo que sale ya validado y viaja a la API, con el municipio como número. */
export type DatosValidadosDePerfil = z.output<typeof esquemaDePerfil>;

export type CamposDeContacto = z.infer<typeof esquemaDeContacto>;
