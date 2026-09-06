import { enviarArchivo, enviarJson, enviarSinRespuesta, obtenerJson } from '../../comun/api';
import type { DatosDeTrabajo, ImagenDeTrabajo, Trabajo } from './tipos';

/** Llamadas a la API del portafolio propio. */

const RUTA_TRABAJOS = '/api/prestador/portafolio/trabajos';

export function listarTrabajos(): Promise<Trabajo[]> {
  return obtenerJson(RUTA_TRABAJOS);
}

export function crearTrabajo(datos: DatosDeTrabajo): Promise<Trabajo> {
  return enviarJson('POST', RUTA_TRABAJOS, datos);
}

export function actualizarTrabajo(id: number, datos: DatosDeTrabajo): Promise<Trabajo> {
  return enviarJson('PUT', `${RUTA_TRABAJOS}/${id}`, datos);
}

export function eliminarTrabajo(id: number): Promise<void> {
  return enviarSinRespuesta('DELETE', `${RUTA_TRABAJOS}/${id}`);
}

/** Deja los trabajos en el orden pedido; viaja la lista completa de identificadores. */
export function reordenarTrabajos(idsEnOrden: number[]): Promise<Trabajo[]> {
  return enviarJson('PUT', `${RUTA_TRABAJOS}/orden`, { idsEnOrden });
}

/**
 * Sube una imagen a un trabajo.
 *
 * El texto alternativo viaja como campo del mismo formulario, no como JSON aparte: así la imagen
 * y su descripción llegan en una sola operación.
 */
export function subirImagenDeTrabajo(
  idTrabajo: number,
  archivo: File,
  textoAlternativo: string
): Promise<ImagenDeTrabajo> {
  const formulario = new FormData();
  formulario.append('archivo', archivo);
  formulario.append('textoAlternativo', textoAlternativo);
  return enviarArchivo('POST', `${RUTA_TRABAJOS}/${idTrabajo}/imagenes`, formulario);
}

export function actualizarTextoAlternativo(
  idTrabajo: number,
  idImagen: number,
  textoAlternativo: string
): Promise<ImagenDeTrabajo> {
  return enviarJson('PUT', `${RUTA_TRABAJOS}/${idTrabajo}/imagenes/${idImagen}`, {
    textoAlternativo,
  });
}

export function eliminarImagenDeTrabajo(idTrabajo: number, idImagen: number): Promise<void> {
  return enviarSinRespuesta('DELETE', `${RUTA_TRABAJOS}/${idTrabajo}/imagenes/${idImagen}`);
}

export function reordenarImagenes(
  idTrabajo: number,
  idsEnOrden: number[]
): Promise<ImagenDeTrabajo[]> {
  return enviarJson('PUT', `${RUTA_TRABAJOS}/${idTrabajo}/imagenes/orden`, { idsEnOrden });
}
