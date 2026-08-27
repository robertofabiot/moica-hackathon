/**
 * Rutas de la capacidad de verificación.
 *
 * La cola cuelga de `/admin` porque es una función administrativa: la definición del producto sitúa
 * ahí la revisión de expedientes. La verificación del propio perfil no tiene ruta propia: es una
 * sección dentro de `/prestador`, porque no es un trámite aparte sino lo que decide si ese perfil
 * puede salir al público.
 */

export const RUTA_ADMIN_VERIFICACIONES = '/admin/verificaciones';
