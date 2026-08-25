import { vi } from 'vitest';

/**
 * Un backend de mentira para las pruebas del navegador.
 *
 * Sustituye a `fetch` y responde lo que cada prueba haya preparado, guardando las peticiones que
 * recibe para poder comprobar qué se envió y con qué cabeceras.
 */

export interface PeticionRecibida {
  metodo: string;
  ruta: string;
  cuerpo: unknown;
  cabeceras: Record<string, string>;
  /** Partes del formulario cuando la petición fue multipart, como una subida de imagen. */
  formulario?: FormData;
}

export interface RespuestaPreparada {
  estado: number;
  cuerpo?: unknown;
}

type Preparacion =
  | { tipo: 'respuesta'; respuesta: RespuestaPreparada }
  | { tipo: 'rechazo'; error: Error }
  | { tipo: 'colgada' };

export interface ApiFalsa {
  /** Peticiones que ha recibido, en orden. */
  peticiones: PeticionRecibida[];
  /** Prepara la respuesta de una ruta, por ejemplo `responder('GET /api/auth/sesion', {...})`. */
  responder(clave: string, respuesta: RespuestaPreparada): void;
  /** Hace fallar la petición como si no hubiera red. */
  rechazar(clave: string, error?: Error): void;
  /** Deja la petición pendiente hasta que el cliente la aborte por tiempo de espera. */
  colgar(clave: string): void;
  /** Última petición recibida a esa ruta, si hubo alguna. */
  ultima(clave: string): PeticionRecibida | undefined;
}

const SIN_PREPARAR: RespuestaPreparada = {
  estado: 404,
  cuerpo: {
    instante: '2026-08-21T10:00:00-06:00',
    estado: 404,
    codigo: 'RECURSO_NO_ENCONTRADO',
    mensaje: 'El recurso solicitado no existe.',
    ruta: '',
  },
};

/** Instala el backend de mentira. Debe llamarse desde cada prueba o desde un `beforeEach`. */
export function instalarApiFalsa(): ApiFalsa {
  const peticiones: PeticionRecibida[] = [];
  const preparadas = new Map<string, Preparacion>();

  const fetchFalso = vi.fn(async (entrada: unknown, opciones?: RequestInit) => {
    const ruta = String(entrada);
    const metodo = opciones?.method ?? 'GET';
    const clave = `${metodo} ${ruta}`;

    peticiones.push({
      metodo,
      ruta,
      cuerpo: typeof opciones?.body === 'string' ? JSON.parse(opciones.body) : undefined,
      cabeceras: (opciones?.headers as Record<string, string> | undefined) ?? {},
      // Una subida de imagen viaja como multipart: el cuerpo no es JSON y hay
      // que conservarlo tal cual para poder comprobar qué partes se enviaron.
      formulario: opciones?.body instanceof FormData ? opciones.body : undefined,
    });

    const preparada = preparadas.get(clave);

    if (preparada?.tipo === 'colgada') {
      await esperarAborto(opciones?.signal ?? undefined);
    }

    if (preparada?.tipo === 'rechazo') {
      throw preparada.error;
    }

    const respuesta = preparada?.tipo === 'respuesta' ? preparada.respuesta : SIN_PREPARAR;

    return new Response(respuesta.cuerpo === undefined ? null : JSON.stringify(respuesta.cuerpo), {
      status: respuesta.estado,
      headers: { 'Content-Type': 'application/json' },
    });
  });

  vi.stubGlobal('fetch', fetchFalso);

  return {
    peticiones,
    responder: (clave, respuesta) => preparadas.set(clave, { tipo: 'respuesta', respuesta }),
    rechazar: (clave, error = new TypeError('Failed to fetch')) =>
      preparadas.set(clave, { tipo: 'rechazo', error }),
    colgar: (clave) => preparadas.set(clave, { tipo: 'colgada' }),
    ultima: (clave) => {
      const [metodo, ruta] = clave.split(' ');
      return peticiones.filter((p) => p.metodo === metodo && p.ruta === ruta).at(-1);
    },
  };
}

function esperarAborto(senal: AbortSignal | undefined): Promise<never> {
  return new Promise((_, reject) => {
    const abortar = () => {
      reject(
        senal?.reason instanceof Error
          ? senal.reason
          : new DOMException('The operation was aborted.', 'AbortError')
      );
    };

    if (senal === undefined) {
      return;
    }
    if (senal.aborted) {
      abortar();
      return;
    }
    senal.addEventListener('abort', abortar, { once: true });
  });
}

/** Cuerpo de error con la forma uniforme que devuelve la API de Moica. */
export function cuerpoDeError(
  estado: number,
  codigo: string,
  mensaje: string,
  errores?: { campo: string; mensaje: string }[]
) {
  return {
    instante: '2026-08-21T10:00:00-06:00',
    estado,
    codigo,
    mensaje,
    ruta: '/api',
    ...(errores === undefined ? {} : { errores }),
  };
}

/** Qué clase de sesión debe describir el ejemplo. */
export interface OpcionesDeSesion {
  diasParaExpirar?: number;
  esAdministrador?: boolean;
  /** La cuenta tiene el segundo factor activo. */
  segundoFactorRequerido?: boolean;
  /** Esta sesión ya presentó un código válido. */
  segundoFactorVerificado?: boolean;
}

/**
 * Sesión de ejemplo con la forma exacta que devuelve la API.
 *
 * Por omisión describe el caso más común: una cuenta ordinaria sin segundo factor, con la sesión
 * completa desde el primer momento.
 */
export function sesionDeEjemplo(opciones: OpcionesDeSesion = {}) {
  const {
    diasParaExpirar = 7,
    esAdministrador = false,
    segundoFactorRequerido = false,
    segundoFactorVerificado = false,
  } = opciones;

  const ahora = Date.now();

  return {
    usuario: {
      idUsuario: 1,
      nombreCompleto: 'Erving Miranda',
      correoElectronico: 'erving@moica.test',
      estadoCuenta: 'ACTIVA',
      esAdministrador,
      fechaRegistro: new Date(ahora).toISOString(),
    },
    sesion: {
      fechaInicio: new Date(ahora).toISOString(),
      fechaExpiracion: new Date(ahora + diasParaExpirar * 24 * 60 * 60 * 1000).toISOString(),
      segundoFactorRequerido,
      segundoFactorVerificado,
      pendienteDeSegundoFactor: segundoFactorRequerido && !segundoFactorVerificado,
    },
  };
}

/** Estado del segundo factor tal como lo devuelve la API. */
export function segundoFactorDeEjemplo(
  estado: 'PENDIENTE_ACTIVACION' | 'ACTIVO' | 'DESACTIVADO' | null = null,
  obligatorio = false
) {
  return {
    estado,
    obligatorio,
    fechaActivacion: estado === 'ACTIVO' ? new Date().toISOString() : null,
  };
}

/** Catálogo territorial de ejemplo: Managua con dos de sus municipios. */
export function catalogoDeEjemplo() {
  return [
    {
      idDepartamento: 1,
      nombre: 'Managua',
      municipios: [
        { idMunicipio: 3, nombre: 'Managua' },
        { idMunicipio: 8, nombre: 'Tipitapa' },
      ],
    },
  ];
}

/** Perfil de prestador de ejemplo con la forma exacta que devuelve la API. */
export function perfilDeEjemplo(cambios: Partial<ReturnType<typeof perfilBase>> = {}) {
  return { ...perfilBase(), ...cambios };
}

function perfilBase() {
  return {
    idPrestador: 1,
    nombrePublico: 'Taller La Esperanza',
    urlImagenPerfil: null as string | null,
    descripcion: 'Reparaciones eléctricas a domicilio con diez años de experiencia.',
    tipoPrestador: 'INDEPENDIENTE',
    municipioPrincipal: {
      idMunicipio: 3,
      nombreMunicipio: 'Managua',
      nombreDepartamento: 'Managua',
    },
    descripcionCobertura: 'Distritos I y II de Managua.',
    disponibilidad: 'DISPONIBLE',
    nivelVerificacion: 'SIN_VERIFICAR',
    fechaCreacion: '2026-08-25T10:00:00-06:00',
    fechaActualizacion: '2026-08-25T10:00:00-06:00',
  };
}

/** Un trabajo del portafolio de ejemplo. */
export function trabajoDeEjemplo(
  cambios: Partial<{
    idTrabajo: number;
    titulo: string;
    descripcion: string;
    fechaRealizacion: string | null;
    ordenVisualizacion: number;
    imagenes: ReturnType<typeof imagenDeEjemplo>[];
  }> = {}
) {
  return {
    idTrabajo: 1,
    titulo: 'Instalación eléctrica',
    descripcion: 'Instalación completa de una vivienda.',
    fechaRealizacion: null as string | null,
    ordenVisualizacion: 0,
    imagenes: [] as ReturnType<typeof imagenDeEjemplo>[],
    fechaCreacion: '2026-08-25T10:00:00-06:00',
    fechaActualizacion: '2026-08-25T10:00:00-06:00',
    ...cambios,
  };
}

/** Una imagen de un trabajo del portafolio. */
export function imagenDeEjemplo(
  idImagenTrabajoPortafolio = 1,
  textoAlternativo: string | null = null
) {
  return {
    idImagenTrabajoPortafolio,
    urlImagen: `https://imagenes.moica.test/trabajos/abc${idImagenTrabajoPortafolio}.png`,
    textoAlternativo,
    ordenVisualizacion: idImagenTrabajoPortafolio - 1,
    fechaCreacion: '2026-08-25T10:00:00-06:00',
  };
}

/** Lo que devuelve el inicio de la activación del segundo factor. */
export function activacionDeEjemplo(claveManual = 'JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP') {
  return {
    claveManual,
    uriDeConfiguracion: `otpauth://totp/Moica%3Aerving%40moica.test?secret=${claveManual}&issuer=Moica&algorithm=SHA1&digits=6&period=30`,
    digitos: 6,
    periodoEnSegundos: 30,
  };
}
