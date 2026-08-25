package com.moica.prestador.dto;

import com.moica.prestador.entity.TipoPrestador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos con los que se crea o se actualiza el perfil de prestador propio.
 *
 * <p>El máximo del nombre público es el del diccionario (120 caracteres). La descripción y la
 * cobertura son {@code TEXT} sin tope en el modelo; los máximos de 3000 y 1000 caracteres son
 * límites de la aplicación para mantener las peticiones en un tamaño razonable, y están
 * documentados en el contrato de la API.
 *
 * <p>No existe ningún campo de nivel de verificación a propósito: es una proyección del flujo de
 * verificación documental y el propietario no puede modificarla.
 */
public record SolicitudDePerfilPrestador(
    @NotBlank @Size(max = 120) String nombrePublico,
    @NotBlank @Size(max = 3000) String descripcion,
    @NotNull TipoPrestador tipoPrestador,
    @NotNull Integer idMunicipioPrincipal,
    @NotBlank @Size(max = 1000) String descripcionCobertura) {

  public SolicitudDePerfilPrestador {
    nombrePublico = strip(nombrePublico);
    descripcion = strip(descripcion);
    descripcionCobertura = strip(descripcionCobertura);
  }

  private static String strip(String valor) {
    return (valor == null) ? null : valor.strip();
  }
}
