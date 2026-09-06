/**
 * Forma de lo que devuelve la API administrativa.
 *
 * El frontend declara sus propios tipos en lugar de reutilizar los del backend: si un día la
 * respuesta cambia, el compilador debe avisar aquí.
 */

/** Lo que ve una persona administradora al entrar en el área. */
export interface ResumenAdministrativo {
  nombreCompleto: string;
  correoElectronico: string;
  fechaAsignacion: string;
}
