package com.moica.comun.error;

import org.springframework.http.HttpStatus;

/**
 * Error esperable de negocio: un conflicto, un permiso denegado, un estado inválido o un recurso
 * que no existe.
 *
 * <p>Cada capacidad la lanza indicando el estado HTTP, un código estable y el mensaje que verá la
 * persona. {@link ManejadorGlobalDeErrores} la traduce a la respuesta uniforme, de modo que ninguna
 * capacidad tenga que conocer el formato del cuerpo.
 *
 * <p>El mensaje se muestra tal cual: no debe contener detalle interno.
 */
public class ErrorDeAplicacion extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus estado;
  private final String codigo;

  public ErrorDeAplicacion(HttpStatus estado, String codigo, String mensaje) {
    super(mensaje);
    this.estado = estado;
    this.codigo = codigo;
  }

  public HttpStatus getEstado() {
    return estado;
  }

  public String getCodigo() {
    return codigo;
  }
}
