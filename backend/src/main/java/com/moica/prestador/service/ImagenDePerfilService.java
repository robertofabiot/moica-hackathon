package com.moica.prestador.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.almacenamiento.AlmacenamientoDeImagenesPublicas;
import com.moica.comun.almacenamiento.ClavesDeImagen;
import com.moica.comun.almacenamiento.LecturaDeMultipart;
import com.moica.comun.almacenamiento.TipoDeImagen;
import com.moica.comun.almacenamiento.ValidacionDeImagen;
import com.moica.prestador.dto.DatosDePerfilPrestador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ciclo de vida de la imagen de perfil: validar, guardar el objeto y persistir su URL.
 *
 * <p>No es transaccional a propósito: la subida al almacén es una llamada de red que no debe
 * ocurrir dentro de una transacción de base de datos. El orden de las operaciones es lo que
 * mantiene la coherencia:
 *
 * <ul>
 *   <li>Primero se sube el objeto nuevo; si la persistencia posterior falla, se retira ese objeto
 *       como compensación y ningún huérfano queda apuntado por nadie.
 *   <li>El objeto anterior se conserva hasta que la base de datos ya apunta al nuevo; solo entonces
 *       se retira, con mejor esfuerzo.
 *   <li>Un fallo al retirar un objeto ya desreferenciado no corrompe nada: la base sigue siendo
 *       coherente y queda un objeto suelto en el bucket, que se registra para poder limpiarlo.
 * </ul>
 */
@Service
public class ImagenDePerfilService {

  private static final Logger LOG = LoggerFactory.getLogger(ImagenDePerfilService.class);

  private final PerfilPrestadorService perfiles;
  private final ValidacionDeImagen validacion;
  private final AlmacenamientoDeImagenesPublicas almacenamiento;

  public ImagenDePerfilService(
      PerfilPrestadorService perfiles,
      ValidacionDeImagen validacion,
      AlmacenamientoDeImagenesPublicas almacenamiento) {
    this.perfiles = perfiles;
    this.validacion = validacion;
    this.almacenamiento = almacenamiento;
  }

  /** Sube o sustituye la imagen del perfil de la sesión y devuelve el perfil resultante. */
  public DatosDePerfilPrestador subir(UsuarioAutenticado sujeto, MultipartFile archivo) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    byte[] contenido = LecturaDeMultipart.contenidoDe(archivo);
    TipoDeImagen tipo = validacion.validar(contenido, archivo.getContentType());

    String clave = ClavesDeImagen.nueva(ClavesDeImagen.PREFIJO_PERFILES, tipo);
    String url = almacenamiento.guardar(clave, contenido, tipo.tipoMime());

    String urlAnterior;
    try {
      urlAnterior = perfiles.actualizarUrlImagen(sujeto.idUsuario(), url);
    } catch (RuntimeException fallo) {
      // Compensación: la base no llegó a apuntar al objeto nuevo, así que se
      // retira para no dejarlo huérfano. Si tampoco se puede, queda registrado.
      eliminarSinPropagar(clave);
      throw fallo;
    }

    eliminarObjetoDesreferenciado(urlAnterior);
    return perfiles.consultarPropio(sujeto);
  }

  /** Quita la imagen del perfil de la sesión. Quitar una imagen que no existe no es un error. */
  public DatosDePerfilPrestador eliminar(UsuarioAutenticado sujeto) {
    perfiles.exigirQuePuedaModificarSuPerfil(sujeto);

    String urlAnterior = perfiles.actualizarUrlImagen(sujeto.idUsuario(), null);
    eliminarObjetoDesreferenciado(urlAnterior);

    return perfiles.consultarPropio(sujeto);
  }

  /**
   * Retira un objeto al que la base de datos ya no apunta.
   *
   * <p>Con mejor esfuerzo: la operación del usuario ya es consistente, así que un fallo aquí no se
   * convierte en error para quien la pidió ni en corrupción silenciosa; se registra y el objeto
   * queda pendiente de limpieza manual.
   */
  private void eliminarObjetoDesreferenciado(String url) {
    if (url == null) {
      return;
    }
    almacenamiento.claveDe(url).ifPresent(this::eliminarSinPropagar);
  }

  private void eliminarSinPropagar(String clave) {
    try {
      almacenamiento.eliminar(clave);
    } catch (RuntimeException fallo) {
      LOG.warn("Quedó pendiente de limpieza el objeto {} del almacenamiento público", clave);
    }
  }
}
