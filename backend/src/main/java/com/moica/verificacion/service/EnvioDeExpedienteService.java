package com.moica.verificacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.almacenamiento.AlmacenamientoDeDocumentosPrivados;
import com.moica.comun.almacenamiento.ClavesDeDocumento;
import com.moica.comun.almacenamiento.LecturaDeMultipart;
import com.moica.comun.almacenamiento.NombreDeArchivo;
import com.moica.comun.almacenamiento.TipoDeDocumento;
import com.moica.comun.almacenamiento.ValidacionDeDocumento;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.verificacion.dto.DatosDeSolicitudVerificacion;
import com.moica.verificacion.entity.NivelVerificacionSolicitado;
import com.moica.verificacion.entity.TipoDocumentoVerificacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * El envío de un expediente completo: validar, subir los archivos y registrar la solicitud.
 *
 * <p>No es transaccional a propósito, por el mismo motivo que la subida de imágenes de P4: guardar
 * en el almacenamiento son llamadas de red que no deben ocurrir dentro de una transacción de base
 * de datos. Lo que mantiene la coherencia es el orden y la compensación:
 *
 * <ol>
 *   <li>Primero se comprueba todo lo que se puede comprobar sin tocar la red: el nivel, la
 *       propiedad, que no haya otra solicitud abierta igual, que el expediente traiga el respaldo
 *       exigido y que cada archivo sea admisible por tamaño, tipo declarado y firma real. Un
 *       expediente que no pasa esto no sube ni un byte.
 *   <li>Después se suben todos los archivos.
 *   <li>Por último se registran la solicitud y sus documentos en una sola transacción.
 *   <li>Si falla cualquier subida o la transacción, se retiran **todos** los objetos que este
 *       intento había subido. No queda una solicitud a medias ni un archivo huérfano.
 * </ol>
 *
 * <p>Esa es la razón de que no exista un estado {@code BORRADOR}: no hay un momento intermedio en
 * el que una solicitud exista sin su expediente.
 */
@Service
public class EnvioDeExpedienteService {

  private static final Logger LOG = LoggerFactory.getLogger(EnvioDeExpedienteService.class);

  private final VerificacionDelPrestadorService verificacion;
  private final ValidacionDeDocumento validacion;
  private final AlmacenamientoDeDocumentosPrivados almacenamiento;

  public EnvioDeExpedienteService(
      VerificacionDelPrestadorService verificacion,
      ValidacionDeDocumento validacion,
      AlmacenamientoDeDocumentosPrivados almacenamiento) {
    this.verificacion = verificacion;
    this.validacion = validacion;
    this.almacenamiento = almacenamiento;
  }

  /**
   * Envía el expediente de la cuenta de la sesión y devuelve la solicitud registrada.
   *
   * @param nivelSolicitado {@code BASICA} o {@code PROFESIONAL}, tal como llegó del formulario
   * @param archivos los documentos, en el mismo orden que sus tipos
   * @param tiposDeclarados un tipo por archivo, en el mismo orden
   */
  public DatosDeSolicitudVerificacion enviar(
      UsuarioAutenticado sujeto,
      String nivelSolicitado,
      List<MultipartFile> archivos,
      List<String> tiposDeclarados) {

    NivelVerificacionSolicitado nivel = nivelDe(nivelSolicitado);
    List<MultipartFile> recibidos = (archivos == null) ? List.of() : archivos;
    List<String> tipos = (tiposDeclarados == null) ? List.of() : tiposDeclarados;

    if (recibidos.size() != tipos.size()) {
      throw new ErrorDeAplicacion(
          HttpStatus.BAD_REQUEST,
          "SOLICITUD_INVALIDA",
          "Cada documento debe llegar con su tipo. Vuelve a intentarlo.");
    }

    List<TipoDocumentoVerificacion> tiposDeDocumento =
        tipos.stream().map(EnvioDeExpedienteService::tipoDe).toList();

    verificacion.exigirQuePuedaSolicitar(sujeto, nivel);
    verificacion.exigirExpedienteCompleto(nivel, tiposDeDocumento);

    List<ArchivoValidado> validados = validarTodos(recibidos, tiposDeDocumento);
    List<DocumentoCargado> cargados = new ArrayList<>(validados.size());

    try {
      for (ArchivoValidado archivo : validados) {
        String clave = ClavesDeDocumento.nueva(archivo.formato());
        almacenamiento.guardar(clave, archivo.contenido(), archivo.formato().tipoMime());
        cargados.add(
            new DocumentoCargado(
                archivo.tipoDocumento(),
                clave,
                archivo.nombreOriginal(),
                archivo.formato().tipoMime(),
                archivo.contenido().length));
      }
      return verificacion.registrar(sujeto.idUsuario(), nivel, cargados);
    } catch (RuntimeException fallo) {
      // Compensación: nada de este intento quedó apuntado por una fila, así que
      // todo lo que llegó a subirse se retira. Si además fallara la limpieza, el
      // aviso queda registrado y el objeto se retira a mano.
      compensar(cargados);
      throw fallo;
    }
  }

  /**
   * Comprueba todos los archivos antes de subir ninguno.
   *
   * <p>Validar sobre la marcha dejaría objetos subidos cuando el tercer archivo resultara
   * inadmisible; se retirarían por compensación, pero habrían viajado hasta el bucket sin motivo.
   */
  private List<ArchivoValidado> validarTodos(
      List<MultipartFile> archivos, List<TipoDocumentoVerificacion> tipos) {

    List<ArchivoValidado> validados = new ArrayList<>(archivos.size());
    for (int posicion = 0; posicion < archivos.size(); posicion++) {
      MultipartFile archivo = archivos.get(posicion);
      byte[] contenido = LecturaDeMultipart.contenidoDe(archivo);
      TipoDeDocumento formato = validacion.validar(contenido, archivo.getContentType());

      validados.add(
          new ArchivoValidado(
              tipos.get(posicion),
              formato,
              NombreDeArchivo.saneado(archivo.getOriginalFilename()),
              contenido));
    }
    return validados;
  }

  private void compensar(List<DocumentoCargado> cargados) {
    for (DocumentoCargado cargado : cargados) {
      try {
        almacenamiento.eliminar(cargado.claveAlmacenamiento());
      } catch (RuntimeException fallo) {
        // Sin la clave: identifica un documento privado concreto y los
        // estándares prohíben registrarla. Quien opere el despliegue localiza
        // el objeto por su prefijo y su antigüedad.
        LOG.warn(
            "Quedó pendiente de limpieza un objeto del almacenamiento privado tras compensar un"
                + " envío de expediente");
      }
    }
  }

  private static NivelVerificacionSolicitado nivelDe(String valor) {
    String normalizado = normalizar(valor);
    for (NivelVerificacionSolicitado nivel : NivelVerificacionSolicitado.values()) {
      if (nivel.name().equals(normalizado)) {
        return nivel;
      }
    }
    throw new ErrorDeAplicacion(
        HttpStatus.BAD_REQUEST,
        "SOLICITUD_INVALIDA",
        "El nivel solicitado debe ser BASICA o PROFESIONAL.");
  }

  private static TipoDocumentoVerificacion tipoDe(String valor) {
    String normalizado = normalizar(valor);
    for (TipoDocumentoVerificacion tipo : TipoDocumentoVerificacion.values()) {
      if (tipo.name().equals(normalizado)) {
        return tipo;
      }
    }
    throw new ErrorDeAplicacion(
        HttpStatus.BAD_REQUEST,
        "SOLICITUD_INVALIDA",
        "El tipo de cada documento debe ser IDENTIDAD, CERTIFICACION, CONSTANCIA,"
            + " REGISTRO_NEGOCIO u OTRO_RESPALDO.");
  }

  // Se recorre el dominio en lugar de llamar a `valueOf`: así un valor que no
  // existe es una rama normal y no una excepción que haya que capturar, que es
  // lo que exigen los estándares para NullPointerException.
  private static String normalizar(String valor) {
    return (valor == null) ? "" : valor.strip().toUpperCase(Locale.ROOT);
  }

  /** Un archivo que ya pasó todas las comprobaciones y todavía no ha salido hacia el bucket. */
  private record ArchivoValidado(
      TipoDocumentoVerificacion tipoDocumento,
      TipoDeDocumento formato,
      String nombreOriginal,
      byte[] contenido) {}
}
