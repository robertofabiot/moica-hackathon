package com.moica.prestador.dto;

import com.moica.prestador.entity.MedioContactoPrestador;
import java.time.OffsetDateTime;

/**
 * Vista de un medio de contacto para su propietario.
 *
 * <p>Nadie más lo recibe. Al cliente de una solicitud aceptada se le entrega {@link
 * ContactoRevelado}, que es más corto: la revelación no necesita saber cuándo se creó la entrada.
 */
public record DatosDeMedioContacto(
    Long idMedioContactoPrestador,
    String contenido,
    short ordenVisualizacion,
    OffsetDateTime fechaCreacion) {

  public static DatosDeMedioContacto de(MedioContactoPrestador contacto) {
    return new DatosDeMedioContacto(
        contacto.getIdMedioContactoPrestador(),
        contacto.getContenido(),
        contacto.getOrdenVisualizacion(),
        contacto.getFechaCreacion());
  }
}
