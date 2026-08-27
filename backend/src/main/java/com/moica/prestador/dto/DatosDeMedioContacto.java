package com.moica.prestador.dto;

import com.moica.prestador.entity.MedioContactoPrestador;
import java.time.OffsetDateTime;

/**
 * Vista de un medio de contacto para su propietario.
 *
 * <p>En P4 nadie más lo recibe: para terceros los contactos permanecen ocultos hasta que exista una
 * solicitud aceptada.
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
