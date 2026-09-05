package com.moica.moderacion.dto;

import com.moica.moderacion.entity.EstadoCasoModeracion;
import com.moica.moderacion.entity.HistorialCaso;
import com.moica.moderacion.entity.ResultadoCasoModeracion;
import com.moica.moderacion.entity.TipoActorHistorial;
import com.moica.moderacion.entity.TipoEventoHistorial;
import com.moica.usuario.entity.EstadoCuenta;
import java.time.OffsetDateTime;

/**
 * Una versión del historial SCD2, tal como la lee el área administrativa.
 *
 * <p>Publica la fotografía completa —estado, resultado, resolución y estado de la cuenta afectada—
 * porque es exactamente lo que permite reconstruir una decisión anterior tal como se tomó. Es
 * información administrativa y no sale de {@code /api/admin}.
 *
 * <p>El intervalo de vigencia viaja tal cual: {@code fechaFinVigencia} nula es la versión actual.
 * Quien pinta el historial no necesita deducirlo de las fechas de las demás.
 *
 * <p>El actor y el responsable son cosas distintas y viajan por separado: quien ejecuta una
 * reasignación no es quien queda a cargo del caso, y confundirlos haría ilegible el historial justo
 * en el evento que más importa. Las versiones anteriores a la primera asignación no tienen
 * responsable y lo dejan nulo.
 *
 * @param nombreActor nombre de quien originó el evento; nulo cuando el actor fue el sistema
 * @param nombreAdministradorResponsable nombre de quien respondía por el caso en esa versión; nulo
 *     mientras nadie lo tuviera asignado
 */
public record DatosDeVersionDeCaso(
    Long idHistorialCaso,
    int numeroVersion,
    TipoEventoHistorial tipoEvento,
    TipoActorHistorial tipoActor,
    Long idActor,
    String nombreActor,
    Long idAdministradorResponsable,
    String nombreAdministradorResponsable,
    EstadoCasoModeracion estadoCaso,
    ResultadoCasoModeracion resultadoCaso,
    EstadoCuenta estadoCuenta,
    String resolucion,
    String detalleCambio,
    OffsetDateTime fechaInicioVigencia,
    OffsetDateTime fechaFinVigencia,
    boolean esVersionActual) {

  public static DatosDeVersionDeCaso de(
      HistorialCaso version, String nombreActor, String nombreAdministradorResponsable) {
    return new DatosDeVersionDeCaso(
        version.getIdHistorialCaso(),
        version.getNumeroVersion(),
        version.getTipoEvento(),
        version.getTipoActor(),
        version.getIdActor(),
        nombreActor,
        version.getIdAdministradorResponsable(),
        nombreAdministradorResponsable,
        version.getEstadoCaso(),
        version.getResultadoCaso(),
        version.getEstadoCuenta(),
        version.getResolucion(),
        version.getDetalleCambio(),
        version.getFechaInicioVigencia(),
        version.getFechaFinVigencia(),
        version.isEsVersionActual());
  }
}
