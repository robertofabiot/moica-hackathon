package com.moica.prestador.dto;

import com.moica.prestador.entity.MedioContactoPrestador;

/**
 * Un medio de contacto tal como se revela al cliente de una solicitud aceptada.
 *
 * <p>Es deliberadamente más corto que {@link DatosDeMedioContacto}: quien recibe la revelación no
 * necesita saber cuándo se creó la entrada, solo qué dice y en qué orden va. Y son estas entradas
 * libres —las que el prestador configuró expresamente— lo único que se revela: nunca el correo de
 * la cuenta ni ningún dato tomado de la autenticación.
 */
public record ContactoRevelado(
    Long idMedioContactoPrestador, String contenido, short ordenVisualizacion) {

  public static ContactoRevelado de(MedioContactoPrestador contacto) {
    return new ContactoRevelado(
        contacto.getIdMedioContactoPrestador(),
        contacto.getContenido(),
        contacto.getOrdenVisualizacion());
  }
}
