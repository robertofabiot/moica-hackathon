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
  idUsuario?: number;
  estadoCuenta?:
    'ACTIVA' | 'RESTRINGIDA_TEMPORAL' | 'SUSPENDIDA_TEMPORAL' | 'SUSPENDIDA_PERMANENTE';
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
    idUsuario = 1,
    estadoCuenta = 'ACTIVA',
    segundoFactorRequerido = false,
    segundoFactorVerificado = false,
  } = opciones;

  const ahora = Date.now();

  return {
    usuario: {
      idUsuario,
      nombreCompleto: 'Erving Miranda',
      correoElectronico: 'erving@moica.test',
      estadoCuenta,
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

/** Un documento del expediente, con solo los metadatos que devuelve la API. */
export function documentoDeVerificacionDeEjemplo(
  idDocumentoVerificacion = 1,
  tipoDocumento = 'IDENTIDAD',
  nombreOriginal = 'cedula.png'
) {
  return {
    idDocumentoVerificacion,
    tipoDocumento,
    nombreOriginal,
    tipoMime: 'image/png',
    tamanoBytes: 128_000,
    fechaCarga: '2026-08-26T10:00:00-06:00',
  };
}

/** Una solicitud de verificación tal como la ve su propietario. */
export function solicitudDeVerificacionDeEjemplo(
  cambios: Partial<ReturnType<typeof solicitudBase>> = {}
) {
  return { ...solicitudBase(), ...cambios };
}

function solicitudBase() {
  return {
    idSolicitudVerificacion: 1,
    nivelSolicitado: 'BASICA',
    estadoSolicitud: 'PENDIENTE',
    observacionResolucion: null as string | null,
    fechaSolicitud: '2026-08-26T10:00:00-06:00',
    fechaInicioRevision: null as string | null,
    fechaResolucion: null as string | null,
    documentos: [documentoDeVerificacionDeEjemplo()],
  };
}

/** Lo que devuelve `GET /api/prestador/verificacion`. */
export function estadoDeVerificacionDeEjemplo(
  cambios: Partial<ReturnType<typeof estadoDeVerificacionBase>> = {}
) {
  return { ...estadoDeVerificacionBase(), ...cambios };
}

function estadoDeVerificacionBase() {
  return {
    nivelVerificacion: 'SIN_VERIFICAR',
    significado:
      'Tu perfil todavía no superó la verificación documental: es privado, no aparece en el descubrimiento y no puede recibir solicitudes.',
    puedeSolicitarBasica: true,
    puedeSolicitarProfesional: false,
    solicitudAbierta: null as ReturnType<typeof solicitudBase> | null,
  };
}

/** Una solicitud tal como la ve la persona que revisa. */
export function expedienteDeEjemplo(cambios: Partial<ReturnType<typeof expedienteBase>> = {}) {
  return { ...expedienteBase(), ...cambios };
}

function expedienteBase() {
  return {
    idSolicitudVerificacion: 1,
    prestador: {
      idPrestador: 7,
      nombrePublico: 'Taller La Esperanza',
      tipoPrestador: 'INDEPENDIENTE',
      nivelVerificacion: 'SIN_VERIFICAR',
      nombreCompleto: 'Liz Martínez',
      correoElectronico: 'liz@moica.test',
    },
    nivelSolicitado: 'BASICA',
    estadoSolicitud: 'PENDIENTE',
    observacionResolucion: null as string | null,
    idAdministradorRevisor: null as number | null,
    fechaSolicitud: '2026-08-26T10:00:00-06:00',
    fechaInicioRevision: null as string | null,
    fechaResolucion: null as string | null,
    documentos: [documentoDeVerificacionDeEjemplo()],
  };
}

/** Tres categorías de demostración, como las carga V90. */
export function catalogoDeCategoriasDeEjemplo() {
  return [
    {
      idCategoriaServicio: 1,
      nombre: 'Hogar y mantenimiento',
      descripcion: 'Oficios del hogar.',
      subcategorias: [
        { idSubcategoriaServicio: 1, nombre: 'Plomería', descripcion: null },
        { idSubcategoriaServicio: 2, nombre: 'Electricidad', descripcion: null },
        { idSubcategoriaServicio: 3, nombre: 'Carpintería', descripcion: null },
      ],
    },
    {
      idCategoriaServicio: 2,
      nombre: 'Belleza y cuidado personal',
      descripcion: 'Cuidado personal.',
      subcategorias: [
        { idSubcategoriaServicio: 4, nombre: 'Maquillaje', descripcion: null },
        { idSubcategoriaServicio: 5, nombre: 'Barbería/peluquería', descripcion: null },
        { idSubcategoriaServicio: 6, nombre: 'Uñas', descripcion: null },
      ],
    },
    {
      idCategoriaServicio: 3,
      nombre: 'Tecnología y servicios digitales',
      descripcion: 'Tecnología.',
      subcategorias: [
        { idSubcategoriaServicio: 7, nombre: 'Reparación de computadoras', descripcion: null },
        { idSubcategoriaServicio: 8, nombre: 'Diseño gráfico', descripcion: null },
        { idSubcategoriaServicio: 9, nombre: 'Soporte técnico', descripcion: null },
      ],
    },
  ];
}

/** Superficie pública de un prestador, sin contactos. */
export function prestadorPublicoDeEjemplo(
  cambios: Partial<ReturnType<typeof prestadorPublicoBase>> = {}
) {
  return { ...prestadorPublicoBase(), ...cambios };
}

function prestadorPublicoBase() {
  return {
    idPrestador: 1,
    nombrePublico: 'Taller La Esperanza',
    urlImagenPerfil: null as string | null,
    descripcion: 'Reparaciones eléctricas a domicilio con diez años de experiencia.',
    tipoPrestador: 'INDEPENDIENTE' as const,
    municipioPrincipal: {
      idMunicipio: 3,
      nombreMunicipio: 'Managua',
      nombreDepartamento: 'Managua',
    },
    descripcionCobertura: 'Distritos I y II de Managua.',
    disponibilidad: 'DISPONIBLE' as const,
    nivelVerificacion: 'VERIFICADO_BASICO' as const,
    significadoVerificacion:
      'Una persona administradora revisó y aprobó la documentación oficial de identidad de quien ofrece este servicio.',
    advertenciaDeInsignia:
      'Una insignia confirma que Moica revisó la documentación presentada en un momento determinado. No garantiza la calidad futura del trabajo ni sustituye el criterio de quien contrata.',
  };
}

/** Resumen público de un servicio, como aparece en el listado. */
export function servicioPublicoDeEjemplo(
  cambios: Partial<ReturnType<typeof servicioPublicoBase>> = {}
) {
  return { ...servicioPublicoBase(), ...cambios };
}

function servicioPublicoBase() {
  return {
    idServicioPublicado: 10,
    nombre: 'Reparación de fugas',
    descripcion: 'Reparo tuberías y fugas en el hogar.',
    precioReferencia: null as number | null,
    idCategoriaServicio: 1,
    nombreCategoria: 'Hogar y mantenimiento',
    idSubcategoriaServicio: 1,
    nombreSubcategoria: 'Plomería',
    imagenPrincipal: null as ReturnType<typeof imagenDeServicioDeEjemplo> | null,
    prestador: prestadorPublicoDeEjemplo(),
    reputacionPrestador: reputacionDeEjemplo(),
  };
}

/** Detalle público de un servicio. */
export function detallePublicoDeServicioDeEjemplo(
  cambios: Partial<ReturnType<typeof detallePublicoBase>> = {}
) {
  return { ...detallePublicoBase(), ...cambios };
}

function detallePublicoBase() {
  const resumen = servicioPublicoBase();
  return {
    ...resumen,
    imagenes: [] as ReturnType<typeof imagenDeServicioDeEjemplo>[],
    admiteContratacion: true,
  };
}

/** Un servicio tal como lo ve su propietario. */
export function servicioPropioDeEjemplo(
  cambios: Partial<ReturnType<typeof servicioPropioBase>> = {}
) {
  return { ...servicioPropioBase(), ...cambios };
}

function servicioPropioBase() {
  return {
    idServicioPublicado: 10,
    nombre: 'Reparación de fugas',
    descripcion: 'Reparo tuberías y fugas en el hogar.',
    precioReferencia: null as number | null,
    estado: 'INACTIVO' as 'ACTIVO' | 'INACTIVO',
    idCategoriaServicio: 1,
    nombreCategoria: 'Hogar y mantenimiento',
    idSubcategoriaServicio: 1,
    nombreSubcategoria: 'Plomería',
    imagenes: [] as ReturnType<typeof imagenDeServicioDeEjemplo>[],
    fechaCreacion: '2026-08-28T10:00:00-06:00',
    fechaActualizacion: '2026-08-28T10:00:00-06:00',
  };
}

/** Una solicitud de servicio tal como la ven sus participantes. */
export function solicitudDeServicioDeEjemplo(
  cambios: Partial<ReturnType<typeof solicitudDeServicioBase>> = {}
) {
  return { ...solicitudDeServicioBase(), ...cambios };
}

function solicitudDeServicioBase() {
  return {
    idSolicitudServicio: 21,
    idServicioPublicado: 10,
    nombreServicio: 'Reparación de fugas',
    idCliente: 2,
    nombreCliente: 'Ana Cliente',
    idPrestador: 1,
    nombrePublicoPrestador: 'Taller La Esperanza',
    idMunicipio: 3,
    nombreMunicipio: 'Managua',
    nombreDepartamento: 'Managua',
    descripcionNecesidad: 'Se fugará el lavamanos del baño principal.',
    indicacionUbicacion: 'De la UCA dos cuadras al lago, portón verde.',
    fechaPreferida: '2026-09-15' as string | null,
    estadoActual: 'PENDIENTE' as
      'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA' | 'CANCELADA' | 'COMPLETADA',
    fechaCreacion: '2026-08-29T10:00:00-06:00',
    fechaActualizacion: '2026-08-29T10:00:00-06:00',
    historial: [
      {
        idCambioEstadoSolicitud: 1,
        estadoAnterior: null as string | null,
        estadoNuevo: 'PENDIENTE',
        idActor: 2,
        nombreActor: 'Ana Cliente',
        motivo: null as string | null,
        fechaCambio: '2026-08-29T10:00:00-06:00',
      },
    ],
  };
}

export function resumenDeSolicitudDeEjemplo(
  cambios: Partial<ReturnType<typeof resumenDeSolicitudBase>> = {}
) {
  return { ...resumenDeSolicitudBase(), ...cambios };
}

function resumenDeSolicitudBase() {
  const detalle = solicitudDeServicioBase();
  return {
    idSolicitudServicio: detalle.idSolicitudServicio,
    idServicioPublicado: detalle.idServicioPublicado,
    nombreServicio: detalle.nombreServicio,
    idCliente: detalle.idCliente,
    nombreCliente: detalle.nombreCliente,
    idPrestador: detalle.idPrestador,
    nombrePublicoPrestador: detalle.nombrePublicoPrestador,
    idMunicipio: detalle.idMunicipio,
    nombreMunicipio: detalle.nombreMunicipio,
    estadoActual: detalle.estadoActual,
    fechaPreferida: detalle.fechaPreferida,
    fechaCreacion: detalle.fechaCreacion,
  };
}

/** Una imagen de un servicio publicado. */
export function imagenDeServicioDeEjemplo(
  idImagenServicioPublicado = 1,
  textoAlternativo: string | null = 'Tubería reparada'
) {
  return {
    idImagenServicioPublicado,
    urlImagen: `https://imagenes.moica.test/servicios/abc${idImagenServicioPublicado}.png`,
    textoAlternativo,
    ordenVisualizacion: idImagenServicioPublicado - 1,
    fechaCreacion: '2026-08-28T10:00:00-06:00',
  };
}

/**
 * Historial de una solicitud que llegó a estar aceptada.
 *
 * Es lo que distingue un hilo habilitado de uno que nunca existió: el estado vigente por sí solo no
 * basta, igual que en el backend.
 */
export function historialConAceptacion() {
  return [
    {
      idCambioEstadoSolicitud: 1,
      estadoAnterior: null as string | null,
      estadoNuevo: 'PENDIENTE',
      idActor: 2,
      nombreActor: 'Ana Cliente',
      motivo: null as string | null,
      fechaCambio: '2026-08-29T10:00:00-06:00',
    },
    {
      idCambioEstadoSolicitud: 2,
      estadoAnterior: 'PENDIENTE' as string | null,
      estadoNuevo: 'ACEPTADA',
      idActor: 1,
      nombreActor: 'Erving Miranda',
      motivo: null as string | null,
      fechaCambio: '2026-08-29T11:00:00-06:00',
    },
  ];
}

/** Una solicitud que pasó por `ACEPTADA` y quedó en el estado indicado. */
export function solicitudConHiloDeEjemplo(
  estadoActual: 'ACEPTADA' | 'CANCELADA' | 'COMPLETADA' = 'ACEPTADA'
) {
  return solicitudDeServicioDeEjemplo({ estadoActual, historial: historialConAceptacion() });
}

/** Un mensaje del hilo tal como lo devuelve la API. */
export function mensajeDeEjemplo(cambios: Partial<ReturnType<typeof mensajeBase>> = {}) {
  return { ...mensajeBase(), ...cambios };
}

function mensajeBase() {
  return {
    idMensajeSolicitud: 1,
    idSolicitudServicio: 21,
    idRemitente: 2,
    nombreRemitente: 'Ana Cliente',
    contenido: '¿A qué hora puede llegar?',
    fechaEnvio: '2026-08-29T11:05:00-06:00',
  };
}

/** Un medio de contacto revelado al cliente de una solicitud aceptada. */
export function contactoReveladoDeEjemplo(
  idMedioContactoPrestador = 1,
  contenido = 'WhatsApp 8888-8888',
  ordenVisualizacion = 0
) {
  return { idMedioContactoPrestador, contenido, ordenVisualizacion };
}

/**
 * Reputación de un prestador tal como la publica el backend.
 *
 * Por omisión es la de quien ya tiene calificaciones. Para el estado vacío se usa
 * {@link reputacionVaciaDeEjemplo}, que deja `promedio` en `null` y todas las
 * filas del desglose en cero: así ninguna prueba puede confundir «sin
 * calificaciones» con «calificado con cero».
 */
export function reputacionDeEjemplo(cambios: Partial<ReturnType<typeof reputacionBase>> = {}) {
  return { ...reputacionBase(), ...cambios };
}

function reputacionBase() {
  return {
    rol: 'PRESTADOR' as 'CLIENTE' | 'PRESTADOR',
    promedio: 4.3 as number | null,
    cantidad: 3,
    desglose: [
      { estrellas: 5, cantidad: 1 },
      { estrellas: 4, cantidad: 2 },
      { estrellas: 3, cantidad: 0 },
      { estrellas: 2, cantidad: 0 },
      { estrellas: 1, cantidad: 0 },
    ],
  };
}

/** Quien todavía no recibió ninguna calificación: promedio nulo, nunca cero. */
export function reputacionVaciaDeEjemplo(rol: 'CLIENTE' | 'PRESTADOR' = 'PRESTADOR') {
  return reputacionDeEjemplo({
    rol,
    promedio: null,
    cantidad: 0,
    desglose: [5, 4, 3, 2, 1].map((estrellas) => ({ estrellas, cantidad: 0 })),
  });
}

/** El estado de la calificación de una solicitud, para la sesión que la consulta. */
export function estadoDeCalificacionDeEjemplo(
  cambios: Partial<ReturnType<typeof estadoDeCalificacionBase>> = {}
) {
  return { ...estadoDeCalificacionBase(), ...cambios };
}

function estadoDeCalificacionBase() {
  return {
    idSolicitudServicio: 21,
    solicitudCompletada: true,
    idCalificado: 1,
    nombreCalificado: 'Taller La Esperanza',
    rolCalificado: 'PRESTADOR' as 'CLIENTE' | 'PRESTADOR',
    puedeCalificar: true,
    calificacionEmitida: null as ReturnType<typeof calificacionDeEjemplo> | null,
  };
}

/** Una calificación ya emitida. En el MVP no se edita ni se borra. */
export function calificacionDeEjemplo(cambios: Partial<ReturnType<typeof calificacionBase>> = {}) {
  return { ...calificacionBase(), ...cambios };
}

function calificacionBase() {
  return {
    idCalificacionUsuario: 1,
    idSolicitudServicio: 21,
    idCalificador: 2,
    idCalificado: 1,
    rolCalificado: 'PRESTADOR' as 'CLIENTE' | 'PRESTADOR',
    puntuacion: 4,
    comentario: 'Puntual y ordenado.' as string | null,
    fechaCreacion: '2026-08-30T09:15:00-06:00',
  };
}
