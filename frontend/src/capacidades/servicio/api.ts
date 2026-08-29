import { enviarArchivo, enviarJson, enviarSinRespuesta, obtenerJson } from '../../comun/api';
import type {
  CategoriaDeServicio,
  DatosDeServicio,
  ImagenDeServicio,
  ServicioPropio,
} from './tipos';

const RUTA = '/api/prestador/servicios';

export async function listarServiciosPropios(): Promise<ServicioPropio[]> {
  return obtenerJson<ServicioPropio[]>(RUTA);
}

export async function obtenerServicioPropio(idServicio: number): Promise<ServicioPropio> {
  return obtenerJson<ServicioPropio>(`${RUTA}/${idServicio}`);
}

export async function crearServicio(datos: DatosDeServicio): Promise<ServicioPropio> {
  return enviarJson<ServicioPropio>('POST', RUTA, datos);
}

export async function actualizarServicio(
  idServicio: number,
  datos: DatosDeServicio
): Promise<ServicioPropio> {
  return enviarJson<ServicioPropio>('PUT', `${RUTA}/${idServicio}`, datos);
}

export async function cambiarEstadoDeServicio(
  idServicio: number,
  estado: 'ACTIVO' | 'INACTIVO'
): Promise<ServicioPropio> {
  return enviarJson<ServicioPropio>('PUT', `${RUTA}/${idServicio}/estado`, { estado });
}

export async function subirImagenDeServicio(
  idServicio: number,
  archivo: File,
  textoAlternativo: string
): Promise<ImagenDeServicio> {
  const formulario = new FormData();
  formulario.set('archivo', archivo);
  if (textoAlternativo.trim() !== '') {
    formulario.set('textoAlternativo', textoAlternativo.trim());
  }
  return enviarArchivo<ImagenDeServicio>('POST', `${RUTA}/${idServicio}/imagenes`, formulario);
}

export async function actualizarTextoAlternativoDeServicio(
  idServicio: number,
  idImagen: number,
  textoAlternativo: string | null
): Promise<ImagenDeServicio> {
  return enviarJson<ImagenDeServicio>('PUT', `${RUTA}/${idServicio}/imagenes/${idImagen}`, {
    textoAlternativo,
  });
}

export async function reordenarImagenesDeServicio(
  idServicio: number,
  idsEnOrden: number[]
): Promise<ImagenDeServicio[]> {
  return enviarJson<ImagenDeServicio[]>('PUT', `${RUTA}/${idServicio}/imagenes/orden`, {
    idsEnOrden,
  });
}

export async function eliminarImagenDeServicio(
  idServicio: number,
  idImagen: number
): Promise<void> {
  await enviarSinRespuesta('DELETE', `${RUTA}/${idServicio}/imagenes/${idImagen}`);
}

export async function listarCategoriasDeServicio(): Promise<CategoriaDeServicio[]> {
  return obtenerJson<CategoriaDeServicio[]>('/api/catalogos/categorias');
}
