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
 * @param nombreActor nombre de quien originó el evento; nulo cuando el actor fue el sistema
 */
public record DatosDeVersionDeCaso(
    Long idHistorialCaso,
    int numeroVersion,
    TipoEventoHistorial tipoEvento,
    TipoActorHistorial tipoActor,
    Long idActor,
    String nombreActor,
    EstadoCasoModeracion estadoCaso,
    ResultadoCasoModeracion resultadoCaso,
    EstadoCuenta estadoCuenta,
    String resolucion,
    String detalleCambio,
    OffsetDateTime fechaInicioVigencia,
    OffsetDateTime fechaFinVigencia,
    boolean esVersionActual) {

  public static DatosDeVersionDeCaso de(HistorialCaso version, String nombreActor) {
    return new DatosDeVersionDeCaso(
        version.getIdHistorialCaso(),
        version.getNumeroVersion(),
        version.getTipoEvento(),
        version.getTipoActor(),
        version.getIdActor(),
        nombreActor,
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
