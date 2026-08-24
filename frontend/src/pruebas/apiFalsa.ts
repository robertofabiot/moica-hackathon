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
