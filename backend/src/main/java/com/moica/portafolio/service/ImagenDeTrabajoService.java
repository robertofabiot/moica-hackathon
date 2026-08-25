package com.moica.portafolio.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.almacenamiento.AlmacenamientoDeImagenesPublicas;
import com.moica.comun.almacenamiento.ClavesDeImagen;
import com.moica.comun.almacenamiento.LecturaDeMultipart;
import com.moica.comun.almacenamiento.TipoDeImagen;
import com.moica.comun.almacenamiento.ValidacionDeImagen;
import com.moica.portafolio.dto.DatosDeImagenDeTrabajo;
import com.moica.portafolio.dto.SolicitudDeTextoAlternativo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ciclo de vida de los objetos de las imágenes del portafolio.
 *
 * <p>Como en la imagen de perfil, no es transaccional a propósito: la red no entra en las
 * transacciones. El objeto nuevo se sube antes de registrar su fila y se retira como compensación
 * si el registro falla; los objetos ya desreferenciados se retiran con mejor esfuerzo cuando la
 * base quedó consistente, y un fallo en esa limpieza se registra en lugar de corromper nada.
 */
@Service
public class ImagenDeTrabajoService {

  private static final Logger LOG = LoggerFactory.getLogger(ImagenDeTrabajoService.class);

  private final TrabajoPortafolioService trabajos;
  private final ValidacionDeImagen validacion;
  private final AlmacenamientoDeImagenesPublicas almacenamiento;

  public ImagenDeTrabajoService(
      TrabajoPortafolioService trabajos,
      ValidacionDeImagen validacion,
      AlmacenamientoDeImagenesPublicas almacenamiento) {
    this.trabajos = trabajos;
    this.validacion = validacion;
    this.almacenamiento = almacenamiento;
  }

  /** Sube una imagen a un trabajo propio y la registra al final de las suyas. */
  public DatosDeImagenDeTrabajo subir(
      UsuarioAutenticado sujeto, Long idTrabajo, MultipartFile archivo, String textoAlternativo) {
    // La propiedad se comprueba antes de subir nada: un objeto que la
    // transacción rechazaría siempre no debe llegar al almacén.
    trabajos.exigirQuePuedaModificarElTrabajo(sujeto, idTrabajo);

    // Reutiliza la normalización y el máximo del DTO, aunque aquí el texto
    // llegue como campo del formulario multipart y no como JSON.
    String texto =
        new SolicitudDeTextoAlternativo(textoAlternativo).exigirValida().textoAlternativo();

    byte[] contenido = LecturaDeMultipart.contenidoDe(archivo);
    TipoDeImagen tipo = validacion.validar(contenido, archivo.getContentType());

    String clave = ClavesDeImagen.nueva(ClavesDeImagen.PREFIJO_TRABAJOS, tipo);
    String url = almacenamiento.guardar(clave, contenido, tipo.tipoMime());

    try {
      return trabajos.registrarImagen(sujeto, idTrabajo, url, texto);
    } catch (RuntimeException fallo) {
      // Compensación: la fila no llegó a existir, así que el objeto recién
      // subido se retira para no dejarlo huérfano.
      eliminarSinPropagar(clave);
      throw fallo;
    }
  }

  /** Elimina una imagen propia: primero su fila y después su objeto, con mejor esfuerzo. */
  public void eliminar(UsuarioAutenticado sujeto, Long idTrabajo, Long idImagen) {
    trabajos.exigirQuePuedaModificarElTrabajo(sujeto, idTrabajo);

    String url = trabajos.eliminarFilaDeImagen(sujeto, idTrabajo, idImagen);
    eliminarObjetoDesreferenciado(url);
  }

  /** Elimina un trabajo propio con sus imágenes y retira sus objetos del almacén. */
  public void eliminarTrabajo(UsuarioAutenticado sujeto, Long idTrabajo) {
    List<String> urls = trabajos.eliminar(sujeto, idTrabajo);
    urls.forEach(this::eliminarObjetoDesreferenciado);
  }

  private void eliminarObjetoDesreferenciado(String url) {
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
