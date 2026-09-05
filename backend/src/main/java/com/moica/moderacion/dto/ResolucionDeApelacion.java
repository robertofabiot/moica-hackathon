package com.moica.moderacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * La decisión sobre una apelación ya registrada.
 *
 * <p>Aceptar o rechazar es lo único que se decide aquí. Reabrir el expediente es una decisión
 * distinta y tiene su propio recurso: la definición 11.5 dice que el administrador la aceptará o la
 * rechazará «y, cuando proceda, reabrirá el mismo expediente», así que aceptar no reabre solo.
 * Separarlas deja además un evento por decisión en el historial, que es como versiona el resto del
 * expediente.
 *
 * @param aceptada si la apelación prospera. Aceptarla no levanta la medida por sí sola: revocarla
 *     es otra decisión, con su propio motivo y su propio evento
 */
public record ResolucionDeApelacion(
    @NotNull(message = "Indica si aceptas o rechazas la apelación.") Boolean aceptada,
    @NotBlank(message = "Escribe la resolución de la apelación.") @Size(max = 3000, message = "La resolución no puede pasar de 3000 caracteres.") String resolucion) {

  public ResolucionDeApelacion {
    resolucion = resolucion == null ? null : resolucion.strip();
  }
}
