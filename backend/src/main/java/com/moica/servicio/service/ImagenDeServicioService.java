package com.moica.servicio.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.almacenamiento.AlmacenamientoDeImagenesPublicas;
import com.moica.comun.almacenamiento.ClavesDeImagen;
import com.moica.comun.almacenamiento.LecturaDeMultipart;
import com.moica.comun.almacenamiento.TipoDeImagen;
import com.moica.comun.almacenamiento.ValidacionDeImagen;
import com.moica.portafolio.dto.SolicitudDeTextoAlternativo;
import com.moica.servicio.dto.DatosDeImagenDeServicio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Ciclo de vida de los objetos de las imágenes de un servicio.
 *
 * <p>No es transaccional a propósito: la red no entra en las transacciones. El objeto nuevo se sube
 * antes de registrar su fila y se retira como compensación si el registro falla; los objetos ya
 * desreferenciados se retiran con mejor esfuerzo cuando la base quedó consistente.
 */
@Service
public class ImagenDeServicioService {

  private static final Logger LOG = LoggerFactory.getLogger(ImagenDeServicioService.class);

  private final ServicioPublicadoService servicios;
  private final ValidacionDeImagen validacion;
  private final AlmacenamientoDeImagenesPublicas almacenamiento;

  public ImagenDeServicioService(
      ServicioPublicadoService servicios,
      ValidacionDeImagen validacion,
      AlmacenamientoDeImagenesPublicas almacenamiento) {
    this.servicios = servicios;
    this.validacion = validacion;
    this.almacenamiento = almacenamiento;
  }

  /** Sube una imagen a un servicio propio y la registra al final de las suyas. */
  public DatosDeImagenDeServicio subir(
      UsuarioAutenticado sujeto, Long idServicio, MultipartFile archivo, String textoAlternativo) {
    servicios.exigirQuePuedaModificarElServicio(sujeto, idServicio);

    String texto =
        new SolicitudDeTextoAlternativo(textoAlternativo).exigirValida().textoAlternativo();

    byte[] contenido = LecturaDeMultipart.contenidoDe(archivo);
    TipoDeImagen tipo = validacion.validar(contenido, archivo.getContentType());

    String clave = ClavesDeImagen.nueva(ClavesDeImagen.PREFIJO_SERVICIOS, tipo);
    String url = almacenamiento.guardar(clave, contenido, tipo.tipoMime());

    try {
      return servicios.registrarImagen(sujeto, idServicio, url, texto);
    } catch (RuntimeException fallo) {
      eliminarSinPropagar(clave);
      throw fallo;
    }
  }

  /** Elimina una imagen propia: primero su fila y después su objeto, con mejor esfuerzo. */
  public void eliminar(UsuarioAutenticado sujeto, Long idServicio, Long idImagen) {
    servicios.exigirQuePuedaModificarElServicio(sujeto, idServicio);

    String url = servicios.eliminarFilaDeImagen(sujeto, idServicio, idImagen);
    eliminarObjetoDesreferenciado(url);
  }

  private void eliminarObjetoDesreferenciado(String url) {
    if (url == null) {
      return;
    }
    almacenamiento.claveDe(url).ifPresentOrElse(this::eliminarSinPropagar, this::avisarUrlAjena);
  }

  private void avisarUrlAjena() {
    LOG.warn(
        "Quedó sin retirar un objeto ya desreferenciado de un servicio: su URL no pertenece a la"
            + " base pública configurada. Revisa si moica.almacenamiento.url-publica-base cambió y"
            + " limpia el bucket a mano.");
  }

  private void eliminarSinPropagar(String clave) {
    try {
      almacenamiento.eliminar(clave);
    } catch (RuntimeException fallo) {
      LOG.warn("Quedó pendiente de limpieza el objeto {} del almacenamiento público", clave);
    }
  }
}
