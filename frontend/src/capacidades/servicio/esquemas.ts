import { z } from 'zod';

const textoObligatorio = (maximo: number, vacio: string, largo: string) =>
  z.string().trim().min(1, vacio).max(maximo, largo);

export const esquemaDeServicio = z
  .object({
    nombre: textoObligatorio(
      150,
      'Escribe el nombre del servicio.',
      'El nombre no puede pasar de 150 caracteres.'
    ),
    descripcion: textoObligatorio(
      3000,
      'Describe el servicio.',
      'La descripción no puede pasar de 3000 caracteres.'
    ),
    idSubcategoriaServicio: z
      .string()
      .trim()
      .min(1, 'Elige una subcategoría.')
      .transform((valor) => Number(valor))
      .refine((valor) => Number.isInteger(valor) && valor > 0, 'Elige una subcategoría.'),
    precioReferencia: z
      .string()
      .trim()
      .transform((valor) => (valor === '' ? null : Number(valor)))
      .refine(
        (valor) => valor === null || (Number.isFinite(valor) && valor > 0),
        'Si indicas un precio, debe ser mayor que cero.'
      ),
  })
  .refine(
    (datos) =>
      datos.precioReferencia === null ||
      Number(datos.precioReferencia.toFixed(2)) === datos.precioReferencia,
    { message: 'El precio admite como máximo dos decimales.', path: ['precioReferencia'] }
  );

export type CamposDeServicio = z.input<typeof esquemaDeServicio>;
export type DatosValidadosDeServicio = z.output<typeof esquemaDeServicio>;
