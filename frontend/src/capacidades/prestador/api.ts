import {
  enviarArchivo,
  enviarJson,
  enviarSinRespuesta,
  ErrorDeApi,
  obtenerJson,
} from '../../comun/api';
import type {
  DatosDePerfil,
  Departamento,
  EstadoDisponibilidad,
  MedioContacto,
  PerfilPrestador,
} from './tipos';

/** Llamadas a la API del perfil de prestador, sus contactos y el catálogo territorial. */

const RUTA_PERFIL = '/api/prestador/perfil';
const RUTA_DISPONIBILIDAD = '/api/prestador/disponibilidad';
const RUTA_IMAGEN = '/api/prestador/perfil/imagen';
const RUTA_CONTACTOS = '/api/prestador/contactos';
const RUTA_CATALOGO = '/api/catalogos/departamentos';

/** Departamentos habilitados con sus municipios, para el selector del formulario. */
export function obtenerCatalogoTerritorial(): Promise<Departamento[]> {
  return obtenerJson(RUTA_CATALOGO);
}

/**
 * El perfil propio, o `null` si la cuenta todavía no lo creó.
 *
 * Solo ese 404 concreto significa «aún no existe»: cualquier otro error sube tal cual.
 */
export async function obtenerPerfil(): Promise<PerfilPrestador | null> {
  try {
    return await obtenerJson<PerfilPrestador>(RUTA_PERFIL);
  } catch (error) {
    if (error instanceof ErrorDeApi && error.codigo === 'PERFIL_NO_ENCONTRADO') {
      return null;
    }
    throw error;
  }
}

export function crearPerfil(datos: DatosDePerfil): Promise<PerfilPrestador> {
  return enviarJson('POST', RUTA_PERFIL, datos);
}

export function actualizarPerfil(datos: DatosDePerfil): Promise<PerfilPrestador> {
  return enviarJson('PUT', RUTA_PERFIL, datos);
}

export function cambiarDisponibilidad(
  disponibilidad: EstadoDisponibilidad
): Promise<PerfilPrestador> {
  return enviarJson('PUT', RUTA_DISPONIBILIDAD, { disponibilidad });
}

/** Sube o sustituye la imagen de perfil. El backend valida formato, firma y tamaño. */
export function subirImagenDePerfil(archivo: File): Promise<PerfilPrestador> {
  const formulario = new FormData();
  formulario.append('archivo', archivo);
  return enviarArchivo('PUT', RUTA_IMAGEN, formulario);
}

export function eliminarImagenDePerfil(): Promise<PerfilPrestador> {
  return enviarJson('DELETE', RUTA_IMAGEN);
}

export function listarContactos(): Promise<MedioContacto[]> {
  return obtenerJson(RUTA_CONTACTOS);
}

export function crearContacto(contenido: string): Promise<MedioContacto> {
  return enviarJson('POST', RUTA_CONTACTOS, { contenido });
}

export function actualizarContacto(id: number, contenido: string): Promise<MedioContacto> {
  return enviarJson('PUT', `${RUTA_CONTACTOS}/${id}`, { contenido });
}

export function eliminarContacto(id: number): Promise<void> {
  return enviarSinRespuesta('DELETE', `${RUTA_CONTACTOS}/${id}`);
}

/** Deja los contactos en el orden pedido; viaja la lista completa de identificadores. */
export function reordenarContactos(idsEnOrden: number[]): Promise<MedioContacto[]> {
  return enviarJson('PUT', `${RUTA_CONTACTOS}/orden`, { idsEnOrden });
}
