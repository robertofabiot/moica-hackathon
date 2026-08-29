package com.moica.prestador.dto;

import com.moica.prestador.entity.EstadoDisponibilidad;
import com.moica.prestador.entity.NivelVerificacionPrestador;

/**
 * Disponibilidad y verificación leídas con el perfil bloqueado.
 *
 * <p>Lo usa la capacidad {@code servicio} al activar una publicación: necesita decidir sobre el
 * estado actual del perfil sin competir con un cambio de disponibilidad o una revocación, y sin
 * importar el repositorio del perfil.
 */
public record CondicionDePublicacion(
    EstadoDisponibilidad disponibilidad, NivelVerificacionPrestador nivelVerificacion) {

  public boolean estaDisponible() {
    return disponibilidad == EstadoDisponibilidad.DISPONIBLE;
  }

  public boolean tieneVerificacionBasica() {
    return nivelVerificacion != NivelVerificacionPrestador.SIN_VERIFICAR;
  }
}
