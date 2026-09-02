import { z } from 'zod';

const textoObligatorio = (maximo: number, vacio: string, largo: string) =>
  z.string().trim().min(1, vacio).max(maximo, largo);

export const esquemaDeContratacion = z.object({
  descripcionNecesidad: textoObligatorio(
    3000,
    'Describe lo que necesitas.',
    'La descripción no puede pasar de 3000 caracteres.'
  ),
  idMunicipio: z
    .string()
    .trim()
    .min(1, 'Elige el municipio.')
    .transform((valor) => Number(valor))
    .refine((valor) => Number.isInteger(valor) && valor > 0, 'Elige el municipio.'),
  indicacionUbicacion: textoObligatorio(
    2000,
    'Indica la dirección, el sector o una referencia.',
    'La ubicación no puede pasar de 2000 caracteres.'
  ),
  fechaPreferida: z
    .string()
    .trim()
    .transform((valor) => (valor === '' ? null : valor))
    .refine(
      (valor) => valor === null || /^\d{4}-\d{2}-\d{2}$/.test(valor),
      'Si indicas una fecha, usa el formato de calendario.'
    ),
});

export type CamposDeContratacion = z.input<typeof esquemaDeContratacion>;
export type DatosValidadosDeContratacion = z.output<typeof esquemaDeContratacion>;

export const esquemaDeCancelacion = z.object({
  motivo: textoObligatorio(
    2000,
    'Indica el motivo de la cancelación.',
    'El motivo no puede pasar de 2000 caracteres.'
  ),
});

export type CamposDeCancelacion = z.input<typeof esquemaDeCancelacion>;

/**
 * El reporte que abre un caso de moderación.
 *
 * Los dos topes son los mismos que valida `ReporteAPresentar` en el backend: el motivo
 * respeta el ancho de la columna `varchar(120)` y la descripción usa el límite de
 * aplicación de 3000 caracteres, el mismo que la descripción de una solicitud.
 */
export const esquemaDeReporte = z.object({
  motivo: textoObligatorio(
    120,
    'Indica el motivo del reporte.',
    'El motivo no puede pasar de 120 caracteres.'
  ),
  descripcion: textoObligatorio(
    3000,
    'Describe lo que ocurrió.',
    'La descripción no puede pasar de 3000 caracteres.'
  ),
});

export type CamposDeReporte = z.input<typeof esquemaDeReporte>;
