package com.moica.moderacion.dto;

import com.moica.moderacion.entity.HistorialCaso;
import com.moica.moderacion.entity.TipoEventoHistorial;
import java.util.List;

/**
 * En qué punto va la apelación de un caso, deducida de su historial.
 *
 * <p>No es una columna ni una entidad. El diccionario de datos no modela la apelación como tabla:
 * la representa con los eventos {@code APELACION_PRESENTADA}, {@code APELACION_ACEPTADA} y {@code
 * APELACION_RECHAZADA} del historial del caso, y eso basta porque una apelación no tiene más estado
 * que el de la última decisión tomada sobre ella. Este enumerado es la lectura de esos eventos, no
 * un dato nuevo que haya que guardar y mantener sincronizado.
 *
 * <p>Se calcula mirando el <b>último</b> de esos tres eventos del expediente. Así un caso reabierto
 * y vuelto a cerrar admite una apelación nueva sin que la anterior estorbe.
 */
public enum EstadoDeApelacion {
  /** Nadie ha registrado ninguna apelación sobre este caso, o la última ya se agotó. */
  SIN_APELACION,
  /** Se registró una apelación recibida por el canal externo y todavía no se ha resuelto. */
  PENDIENTE,
  /** La apelación prosperó. Es lo que habilita reabrir el mismo expediente. */
  ACEPTADA,
  /** La apelación se evaluó y la decisión vigente se mantuvo. */
  RECHAZADA;

  /**
   * Lee el estado de la apelación en el historial del caso.
   *
   * <p>Manda el <b>último</b> evento relevante, no el primero: un expediente puede acumular varias
   * apelaciones a lo largo de su vida y solo la más reciente sigue abierta o resuelta.
   *
   * <p>{@link TipoEventoHistorial#CASO_REABIERTO} cuenta como evento relevante y devuelve {@link
   * #SIN_APELACION}. Es lo que impide reabrir dos veces con una sola apelación aceptada: reabrir
   * <em>consume</em> el derecho que aceptarla concedió, y volver a hacerlo exige que la persona
   * apele otra vez por el canal externo.
   *
   * @param versiones las versiones del caso, en cualquier orden: se recorren todas
   */
  public static EstadoDeApelacion deLasVersiones(List<HistorialCaso> versiones) {
    EstadoDeApelacion estado = SIN_APELACION;
    int ultimaVersionRelevante = 0;

    for (HistorialCaso version : versiones) {
      EstadoDeApelacion resultante = resultanteDe(version.getTipoEvento());

      if (resultante != null && version.getNumeroVersion() > ultimaVersionRelevante) {
        estado = resultante;
        ultimaVersionRelevante = version.getNumeroVersion();
      }
    }
    return estado;
  }

  /** En qué deja la apelación cada evento, o nulo si ese evento no dice nada de ella. */
  private static EstadoDeApelacion resultanteDe(TipoEventoHistorial evento) {
    return switch (evento) {
      case APELACION_PRESENTADA -> PENDIENTE;
      case APELACION_ACEPTADA -> ACEPTADA;
      case APELACION_RECHAZADA -> RECHAZADA;
      case CASO_REABIERTO -> SIN_APELACION;
      default -> null;
    };
  }
}
