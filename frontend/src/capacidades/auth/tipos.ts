/**
 * Forma de lo que devuelve la API de autenticación.
 *
 * El frontend declara sus propios tipos en lugar de reutilizar los del backend: si un día la
 * respuesta cambia, el compilador debe avisar aquí.
 *
 * El cuerpo uniforme de error se mudó a `src/comun/api` junto con la infraestructura de red; se
 * reexporta para no romper a quien lo importaba de aquí.
 */

export type { CuerpoDeError, ErrorDeCampo } from '../../comun/api';

/** Estado operativo de una cuenta. Coincide con el dominio `EstadoCuenta` del diccionario. */
export type EstadoCuenta =
  'ACTIVA' | 'RESTRINGIDA_TEMPORAL' | 'SUSPENDIDA_TEMPORAL' | 'SUSPENDIDA_PERMANENTE';

/** Estado del segundo factor de una cuenta. Coincide con el dominio `EstadoSegundoFactor`. */
export type EstadoSegundoFactor = 'PENDIENTE_ACTIVACION' | 'ACTIVO' | 'DESACTIVADO';

/** Datos públicos de una cuenta. Nunca incluye la contraseña ni su hash. */
export interface Usuario {
  idUsuario: number;
  nombreCompleto: string;
  correoElectronico: string;
  estadoCuenta: EstadoCuenta;
  /** Solo sirve para decidir qué ofrecer; quien decide si puede entrar es el backend. */
  esAdministrador: boolean;
  fechaRegistro: string;
}

/** Vigencia de la sesión en curso y estado de su segundo factor. */
export interface VigenciaDeSesion {
  fechaInicio: string;
  fechaExpiracion: string;
  /** La cuenta tiene el segundo factor activo. */
  segundoFactorRequerido: boolean;
  /** Esta sesión ya presentó un código válido. */
  segundoFactorVerificado: boolean;
  /** Las dos cosas a la vez: la sesión es provisional y hay que verificarla. */
  pendienteDeSegundoFactor: boolean;
}

/** Respuesta de la API cuando hay una sesión activa. */
export interface SesionActual {
  usuario: Usuario;
  sesion: VigenciaDeSesion;
}

/** Estado del segundo factor tal como lo describe la API. */
export interface SegundoFactor {
  /** `null` cuando la cuenta nunca registró ninguno. */
  estado: EstadoSegundoFactor | null;
  /** La cuenta no puede desactivarlo; hoy solo ocurre con el rol administrativo. */
  obligatorio: boolean;
  fechaActivacion: string | null;
}

/**
 * Lo que hace falta para configurar la aplicación autenticadora.
 *
 * Solo llega una vez, al iniciar la activación. No se guarda en `localStorage`, ni en la caché de
 * React Query, ni en ningún estado global: vive en el estado del formulario y desaparece al salir
 * de la pantalla.
 */
export interface ActivacionDeSegundoFactor {
  claveManual: string;
  uriDeConfiguracion: string;
  digitos: number;
  periodoEnSegundos: number;
}
