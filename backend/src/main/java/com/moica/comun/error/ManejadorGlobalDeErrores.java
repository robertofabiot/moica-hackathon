package com.moica.comun.error;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce cualquier fallo de la API a la respuesta uniforme {@link RespuestaDeError}.
 *
 * <p>Extiende {@link ResponseEntityExceptionHandler} para cubrir también los errores que produce el
 * propio Spring MVC —ruta inexistente, método no permitido, tipo de contenido no admitido— y no
 * solo las excepciones del dominio.
 *
 * <p>Los mensajes se redactan aquí y no se toman de la excepción: el texto de un fallo interno
 * puede contener nombres de clase, SQL o valores de la petición.
 */
@RestControllerAdvice
public class ManejadorGlobalDeErrores extends ResponseEntityExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ManejadorGlobalDeErrores.class);

  /** Error de negocio esperable: la capacidad ya decidió estado, código y mensaje. */
  @ExceptionHandler(ErrorDeAplicacion.class)
  ResponseEntity<RespuestaDeError> manejarErrorDeAplicacion(
      ErrorDeAplicacion error, HttpServletRequest peticion) {

    RespuestaDeError cuerpo =
        RespuestaDeError.de(
            error.getEstado().value(),
            error.getCodigo(),
            error.getMessage(),
            peticion.getRequestURI());

    return ResponseEntity.status(error.getEstado())
        .contentType(MediaType.APPLICATION_JSON)
        .body(cuerpo);
  }

  /**
   * Cualquier otro fallo: se registra completo en el servidor y hacia fuera solo sale un mensaje
   * genérico.
   */
  @ExceptionHandler(Exception.class)
  ResponseEntity<RespuestaDeError> manejarFalloNoPrevisto(
      Exception fallo, HttpServletRequest peticion) {

    LOG.error("Fallo no previsto al atender {}", peticion.getRequestURI(), fallo);

    RespuestaDeError cuerpo =
        RespuestaDeError.de(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "ERROR_INTERNO",
            "No pudimos completar la operación. Inténtalo de nuevo en unos minutos.",
            peticion.getRequestURI());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_JSON)
        .body(cuerpo);
  }

  /** Entrada que no cumple las reglas del DTO: se devuelve el detalle campo por campo. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException excepcion,
      HttpHeaders cabeceras,
      HttpStatusCode estado,
      WebRequest peticion) {

    List<RespuestaDeError.ErrorDeCampo> errores =
        excepcion.getBindingResult().getFieldErrors().stream()
            .map(ManejadorGlobalDeErrores::aErrorDeCampo)
            .toList();

    RespuestaDeError cuerpo =
        new RespuestaDeError(
            OffsetDateTime.now(),
            HttpStatus.BAD_REQUEST.value(),
            "VALIDACION",
            "Revisa los datos enviados.",
            rutaDe(peticion),
            errores);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_JSON)
        .body(cuerpo);
  }

  /** Punto único por el que salen los errores que ya resuelve Spring MVC. */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception excepcion,
      Object cuerpoOriginal,
      HttpHeaders cabeceras,
      HttpStatusCode estado,
      WebRequest peticion) {

    RespuestaDeError cuerpo =
        RespuestaDeError.de(
            estado.value(), codigoPara(estado), mensajePara(estado), rutaDe(peticion));

    return ResponseEntity.status(estado).contentType(MediaType.APPLICATION_JSON).body(cuerpo);
  }

  private static RespuestaDeError.ErrorDeCampo aErrorDeCampo(FieldError error) {
    String mensaje =
        (error.getDefaultMessage() == null) ? "Valor no admitido." : error.getDefaultMessage();
    return new RespuestaDeError.ErrorDeCampo(error.getField(), mensaje);
  }

  private static String rutaDe(WebRequest peticion) {
    if (peticion instanceof ServletWebRequest servlet) {
      return servlet.getRequest().getRequestURI();
    }
    return peticion.getDescription(false);
  }

  private static String codigoPara(HttpStatusCode estado) {
    return switch (estado.value()) {
      case 400 -> "SOLICITUD_INVALIDA";
      case 404 -> "RECURSO_NO_ENCONTRADO";
      case 405 -> "METODO_NO_PERMITIDO";
      // El tope de transporte de una subida multipart. El máximo de negocio de
      // una imagen lo valida la aplicación antes, con su propio código.
      case 413 -> "CONTENIDO_DEMASIADO_GRANDE";
      case 415 -> "TIPO_DE_CONTENIDO_NO_ADMITIDO";
      default -> estado.is5xxServerError() ? "ERROR_INTERNO" : "SOLICITUD_INVALIDA";
    };
  }

  private static String mensajePara(HttpStatusCode estado) {
    return switch (estado.value()) {
      case 400 -> "No pudimos leer la solicitud. Revisa los datos enviados.";
      case 404 -> "El recurso solicitado no existe.";
      case 405 -> "Esa operación no está disponible en esta dirección.";
      case 413 -> "Lo enviado supera el tamaño máximo admitido.";
      case 415 -> "El formato enviado no está admitido.";
      default ->
          estado.is5xxServerError()
              ? "No pudimos completar la operación. Inténtalo de nuevo en unos minutos."
              : "No pudimos procesar la solicitud.";
    };
  }
}
