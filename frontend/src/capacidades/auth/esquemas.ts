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

/**
 * Cambio de contraseña.
 *
 * La contraseña actual solo se comprueba que esté escrita: quien decide si es correcta es el
 * backend, y validarla aquí contra la política rechazaría contraseñas antiguas legítimas.
 */
export const esquemaDeCambioDeClave = z
  .object({
    claveActual: z.string().min(1, 'Escribe tu contraseña actual.'),
    claveNueva: esquemaDeClave,
    confirmacionDeClave: z.string().min(1, 'Repite la contraseña nueva.'),
  })
  .refine((datos) => datos.claveNueva === datos.confirmacionDeClave, {
    message: 'Las dos contraseñas deben coincidir.',
    path: ['confirmacionDeClave'],
  });

/**
 * Código del segundo factor.
 *
 * Los espacios se retiran porque las aplicaciones autenticadoras muestran el código partido en dos
 * grupos y es habitual copiarlo tal cual. Aquí no se exige una longitud concreta: el número de
 * dígitos lo decide el backend y anunciarlo en un mensaje describiría la forma del código.
 */
const esquemaDeCodigo = z
  .string()
  .transform((codigo) => codigo.replace(/\s/g, ''))
  .pipe(z.string().min(1, 'Escribe el código de tu aplicación autenticadora.'));

export const esquemaDeCodigoTotp = z.object({
  codigo: esquemaDeCodigo,
});

/** Desactivación del segundo factor: exige los dos factores a la vez. */
export const esquemaDeDesactivacion = z.object({
  claveActual: z.string().min(1, 'Escribe tu contraseña actual.'),
  codigo: esquemaDeCodigo,
});

export type CamposDeRegistro = z.infer<typeof esquemaDeRegistro>;
export type CamposDeInicioSesion = z.infer<typeof esquemaDeInicioSesion>;
export type CamposDeCambioDeClave = z.infer<typeof esquemaDeCambioDeClave>;
export type CamposDeCodigoTotp = z.infer<typeof esquemaDeCodigoTotp>;
export type CamposDeDesactivacion = z.infer<typeof esquemaDeDesactivacion>;
