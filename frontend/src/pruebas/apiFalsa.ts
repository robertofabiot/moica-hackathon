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
}

export interface RespuestaPreparada {
  estado: number;
  cuerpo?: unknown;
}

export interface ApiFalsa {
  /** Peticiones que ha recibido, en orden. */
  peticiones: PeticionRecibida[];
  /** Prepara la respuesta de una ruta, por ejemplo `responder('GET /api/auth/sesion', {...})`. */
  responder(clave: string, respuesta: RespuestaPreparada): void;
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
  const preparadas = new Map<string, RespuestaPreparada>();

  const fetchFalso = vi.fn(async (entrada: unknown, opciones?: RequestInit) => {
    const ruta = String(entrada);
    const metodo = opciones?.method ?? 'GET';
    const clave = `${metodo} ${ruta}`;

    peticiones.push({
      metodo,
      ruta,
      cuerpo: typeof opciones?.body === 'string' ? JSON.parse(opciones.body) : undefined,
      cabeceras: (opciones?.headers as Record<string, string> | undefined) ?? {},
    });

    const preparada = preparadas.get(clave) ?? SIN_PREPARAR;

    return new Response(preparada.cuerpo === undefined ? null : JSON.stringify(preparada.cuerpo), {
      status: preparada.estado,
      headers: { 'Content-Type': 'application/json' },
    });
  });

  vi.stubGlobal('fetch', fetchFalso);

  return {
    peticiones,
    responder: (clave, respuesta) => preparadas.set(clave, respuesta),
    ultima: (clave) => {
      const [metodo, ruta] = clave.split(' ');
      return peticiones.filter((p) => p.metodo === metodo && p.ruta === ruta).at(-1);
    },
  };
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

/** Sesión de ejemplo, vigente durante los días que se indiquen. */
export function sesionDeEjemplo(diasParaExpirar = 7) {
  const ahora = Date.now();

  return {
    usuario: {
      idUsuario: 1,
      nombreCompleto: 'Erving Miranda',
      correoElectronico: 'erving@moica.test',
      estadoCuenta: 'ACTIVA',
      fechaRegistro: new Date(ahora).toISOString(),
    },
    sesion: {
      fechaInicio: new Date(ahora).toISOString(),
      fechaExpiracion: new Date(ahora + diasParaExpirar * 24 * 60 * 60 * 1000).toISOString(),
      segundoFactorVerificado: false,
    },
  };
}
