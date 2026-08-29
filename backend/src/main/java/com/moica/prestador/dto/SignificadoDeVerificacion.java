package com.moica.prestador.dto;

import com.moica.prestador.entity.NivelVerificacionPrestador;

/**
 * Textos públicos de la insignia de verificación.
 *
 * <p>Una insignia confirma la revisión documental de un momento determinado. No garantiza la
 * calidad futura del trabajo. El texto en primera persona del perfil propio vive en la capacidad
 * {@code verificacion}; este es el que ve un visitante.
 */
public final class SignificadoDeVerificacion {

  public static final String ADVERTENCIA =
      "Una insignia confirma que Moica revisó la documentación presentada en un momento"
          + " determinado. No garantiza la calidad futura del trabajo ni sustituye el criterio de"
          + " quien contrata.";

  private SignificadoDeVerificacion() {}

  public static String de(NivelVerificacionPrestador nivel) {
    return switch (nivel) {
      case SIN_VERIFICAR ->
          "Este perfil no ha superado la verificación documental y no aparece en el descubrimiento"
              + " público.";
      case VERIFICADO_BASICO ->
          "Una persona administradora revisó y aprobó la documentación oficial de identidad de"
              + " quien ofrece este servicio.";
      case PROFESIONAL_VERIFICADO ->
          "Además de la identidad, una persona administradora revisó documentación profesional,"
              + " técnica o comercial que respalda la actividad declarada.";
    };
  }
}
