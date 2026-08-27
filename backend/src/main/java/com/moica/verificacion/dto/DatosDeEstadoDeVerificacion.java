package com.moica.verificacion.dto;

import com.moica.prestador.entity.NivelVerificacionPrestador;

/**
 * Dónde está el perfil propio dentro del flujo de verificación.
 *
 * <p>Responde de una vez las tres preguntas que tiene quien entra en la sección: qué nivel tiene
 * ahora y qué significa, qué puede solicitar y si ya hay algo esperando decisión.
 *
 * <p>Las dos banderas son la misma regla que aplica el servidor al recibir un envío, no una versión
 * relajada para pintar botones: la interfaz las usa para no proponer algo que la API va a rechazar,
 * y la API vuelve a comprobarlo igualmente.
 *
 * @param significado la frase de la definición del producto para el nivel vigente
 * @param puedeSolicitarBasica cierto solo cuando el perfil está sin verificar y no tiene una básica
 *     abierta
 * @param puedeSolicitarProfesional cierto solo con la básica vigente, sin profesional ya concedida
 *     y sin una profesional abierta
 * @param solicitudAbierta la solicitud que espera decisión, o {@code null} si no hay ninguna
 */
public record DatosDeEstadoDeVerificacion(
    NivelVerificacionPrestador nivelVerificacion,
    String significado,
    boolean puedeSolicitarBasica,
    boolean puedeSolicitarProfesional,
    DatosDeSolicitudVerificacion solicitudAbierta) {

  /**
   * Qué significa cada nivel, en la redacción de {@code Docs/Core/DefinicionProducto.md} §5.6.
   *
   * <p>Viaja desde el servidor para que la insignia del perfil y cualquier superficie futura digan
   * exactamente lo mismo sin repetir la frase en cada pantalla.
   */
  public static String significadoDe(NivelVerificacionPrestador nivel) {
    return switch (nivel) {
      case SIN_VERIFICAR ->
          "Tu perfil todavía no superó la verificación documental: es privado, no aparece en el"
              + " descubrimiento y no puede recibir solicitudes.";
      case VERIFICADO_BASICO ->
          "Una persona administradora revisó y aprobó tu documentación oficial de identidad. Tu"
              + " perfil puede hacerse público y recibir solicitudes.";
      case PROFESIONAL_VERIFICADO ->
          "Además de tu identidad, una persona administradora revisó y aprobó documentación"
              + " profesional, técnica o comercial que respalda tu actividad.";
    };
  }
}
