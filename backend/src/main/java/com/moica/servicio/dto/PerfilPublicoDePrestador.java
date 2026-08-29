package com.moica.servicio.dto;

import com.moica.portafolio.dto.DatosDeTrabajo;
import com.moica.prestador.dto.DatosPublicosDePrestador;
import com.moica.prestador.entity.EstadoDisponibilidad;
import java.util.List;

/**
 * Perfil público de un prestador verificado: presentación, portafolio y servicios activos.
 *
 * <p>Sin contactos. {@code admiteContratacion} es verdadero solo si está disponible; contratar
 * llega en P6.
 */
public record PerfilPublicoDePrestador(
    DatosPublicosDePrestador prestador,
    List<DatosDeTrabajo> portafolio,
    List<ResumenPublicoDeServicio> servicios,
    boolean admiteContratacion) {

  public PerfilPublicoDePrestador {
    portafolio = List.copyOf(portafolio);
    servicios = List.copyOf(servicios);
  }

  public static PerfilPublicoDePrestador de(
      DatosPublicosDePrestador prestador,
      List<DatosDeTrabajo> portafolio,
      List<ResumenPublicoDeServicio> servicios) {
    return new PerfilPublicoDePrestador(
        prestador,
        portafolio,
        servicios,
        prestador.disponibilidad() == EstadoDisponibilidad.DISPONIBLE);
  }
}
