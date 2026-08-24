import { z } from 'zod';

/**
 * Reglas de los formularios de acceso.
 *
 * Son las mismas que aplica el backend en `@ClaveSegura` (decisión D-SEC-02). Validar aquí mejora
 * la experiencia —la persona ve el problema mientras escribe— pero no sustituye a la validación
 * del servidor, que es la que manda.
 */

const MINUSCULA = /\p{Ll}/u;
const MAYUSCULA = /\p{Lu}/u;
const NUMERO = /\p{N}/u;
const SIMBOLO = /[^\p{L}\p{N}]/u;

/** BCrypt solo admite 72 bytes, y en UTF-8 una eñe o un emoji ocupan más de uno. */
const BYTES_MAXIMOS = 72;
const codificador = new TextEncoder();

const esquemaDeClave = z
  .string()
  .min(8, 'La contraseña debe tener entre 8 y 72 caracteres.')
  .max(72, 'La contraseña debe tener entre 8 y 72 caracteres.')
  .refine((clave) => MINUSCULA.test(clave), 'Debe incluir al menos una letra minúscula.')
  .refine((clave) => MAYUSCULA.test(clave), 'Debe incluir al menos una letra mayúscula.')
  .refine((clave) => NUMERO.test(clave), 'Debe incluir al menos un número.')
  .refine((clave) => SIMBOLO.test(clave), 'Debe incluir al menos un símbolo.')
  .refine(
    (clave) => codificador.encode(clave).length <= BYTES_MAXIMOS,
    'La contraseña es demasiado larga. Los acentos, las eñes y los emojis ocupan más de un espacio.'
  );

const esquemaDeCorreo = z
  .string()
  .trim()
  .toLowerCase()
  .min(1, 'Escribe tu correo electrónico.')
  .max(254, 'El correo no puede superar los 254 caracteres.')
  .pipe(z.email('Escribe un correo electrónico válido.'));

export const esquemaDeRegistro = z
  .object({
    nombreCompleto: z
      .string()
      .trim()
      .min(1, 'Escribe tu nombre completo.')
      .max(120, 'El nombre no puede superar los 120 caracteres.'),
    correoElectronico: esquemaDeCorreo,
    clave: esquemaDeClave,
    // Sin recuperación de contraseña en el MVP, una errata al escribirla dejaría
    // la cuenta inaccesible: por eso se confirma.
    confirmacionDeClave: z.string().min(1, 'Repite la contraseña.'),
  })
  .refine((datos) => datos.clave === datos.confirmacionDeClave, {
    message: 'Las dos contraseñas deben coincidir.',
    path: ['confirmacionDeClave'],
  });

export const esquemaDeInicioSesion = z.object({
  correoElectronico: esquemaDeCorreo,
  clave: z.string().min(1, 'Escribe tu contraseña.'),
});

export type CamposDeRegistro = z.infer<typeof esquemaDeRegistro>;
export type CamposDeInicioSesion = z.infer<typeof esquemaDeInicioSesion>;
