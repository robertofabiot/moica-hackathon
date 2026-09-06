package com.moica.calificacion.entity;

/**
 * Rol que desempeñó en la solicitud la persona calificada.
 *
 * <p>Los valores son los del dominio {@code RolCalificado} del diccionario de datos y la
 * restricción {@code ck_calificacion_usuario_rol} los repite en PostgreSQL.
 *
 * <p>No es un dato que elija quien califica: se deriva de la solicitud. El cliente solo puede
 * calificar al otro participante como {@link #PRESTADOR} y el prestador solo puede calificarlo como
 * {@link #CLIENTE}. Por eso existe {@link #contrapartaDe}, que es la única forma en que la
 * aplicación obtiene este valor.
 */
public enum RolCalificado {
  /** La persona calificada actuó como cliente en la solicitud. */
  CLIENTE,
  /** La persona calificada actuó como prestador en la solicitud. */
  PRESTADOR;

  /**
   * El rol en que queda calificada la contraparte de quien emite la calificación.
   *
   * <p>Se lee como una frase: si quien califica es el cliente, el calificado es el prestador.
   *
   * @param calificaElCliente si quien emite la calificación es el cliente de la solicitud
   */
  public static RolCalificado contrapartaDe(boolean calificaElCliente) {
    return calificaElCliente ? PRESTADOR : CLIENTE;
  }
}
