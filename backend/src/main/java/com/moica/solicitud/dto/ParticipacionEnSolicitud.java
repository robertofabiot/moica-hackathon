package com.moica.solicitud.dto;

import com.moica.solicitud.entity.EstadoSolicitud;

/**
 * Quiénes participan en una solicitud y en qué condición está su hilo.
 *
 * <p>Existe para que la regla «quién es cliente, quién es prestador, si la solicitud llegó a estar
 * aceptada y si todavía admite mensajes» se resuelva **una sola vez**, en la capacidad que es dueña
 * del estado, y no se reescriba en cada superficie que la necesite.
 *
 * <p>{@code llegoAAceptada} no se deduce del estado vigente: una solicitud {@code CANCELADA} puede
 * venir de {@code PENDIENTE} —y entonces nunca hubo hilo— o de {@code ACEPTADA}, y en ese caso el
 * historial queda visible en solo lectura. La diferencia solo la conoce {@code
 * CambioEstadoSolicitud}.
 *
 * @param idPrestador el prestador destinatario, resuelto desde el servicio publicado
 */
public record ParticipacionEnSolicitud(
    Long idSolicitudServicio,
    Long idCliente,
    Long idPrestador,
    EstadoSolicitud estadoActual,
    boolean llegoAAceptada) {

  public boolean esCliente(Long idUsuario) {
    return idCliente.equals(idUsuario);
  }

  public boolean esPrestador(Long idUsuario) {
    return idPrestador.equals(idUsuario);
  }

  /**
   * Si el hilo admite mensajes nuevos.
   *
   * <p>Solo mientras el compromiso sigue vivo. Cancelar o completar lo deja en solo lectura, y eso
   * es definitivo: ninguno de los dos estados se reabre.
   */
  public boolean admiteMensajesNuevos() {
    return estadoActual == EstadoSolicitud.ACEPTADA;
  }
}
