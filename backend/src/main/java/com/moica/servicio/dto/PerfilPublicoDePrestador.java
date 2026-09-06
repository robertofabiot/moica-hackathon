package com.moica.servicio.dto;

import com.moica.calificacion.dto.ReputacionPorRol;
import com.moica.portafolio.dto.DatosDeTrabajo;
import com.moica.prestador.dto.DatosPublicosDePrestador;
import com.moica.prestador.entity.EstadoDisponibilidad;
import java.util.List;

/**
 * Perfil público de un prestador verificado: presentación, portafolio y servicios activos.
 *
 * <p>Sin contactos. {@code admiteContratacion} es verdadero solo si está disponible; contratar
 * llega en P6.
 *
 * <p>{@code reputacionPrestador} es el mismo agregado que llevan sus tarjetas: la reputación es de
 * la persona, no de cada servicio.
 */
public record PerfilPublicoDePrestador(
    DatosPublicosDePrestador prestador,
    List<DatosDeTrabajo> portafolio,
    List<ResumenPublicoDeServicio> servicios,
    boolean admiteContratacion,
    ReputacionPorRol reputacionPrestador) {

  public PerfilPublicoDePrestador {
    portafolio = List.copyOf(portafolio);
    servicios = List.copyOf(servicios);
  }

  public static PerfilPublicoDePrestador de(
      DatosPublicosDePrestador prestador,
      List<DatosDeTrabajo> portafolio,
      List<ResumenPublicoDeServicio> servicios,
      ReputacionPorRol reputacionPrestador) {
    return new PerfilPublicoDePrestador(
        prestador,
        portafolio,
        servicios,
        prestador.disponibilidad() == EstadoDisponibilidad.DISPONIBLE,
        reputacionPrestador);
  }
}
