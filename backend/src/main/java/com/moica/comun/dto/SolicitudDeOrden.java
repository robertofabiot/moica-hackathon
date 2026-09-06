package com.moica.comun.dto;

import com.moica.comun.error.ErrorDeAplicacion;
import jakarta.validation.constraints.NotEmpty;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * El orden completo que quiere dejar quien reordena una lista propia.
 *
 * <p>Viaja la lista entera de identificadores, no un movimiento: así la petición es idempotente, no
 * depende de en qué orden lleguen dos cambios y el servidor puede exigir que aparezcan exactamente
 * los elementos existentes, ni uno más ni uno menos.
 */
public record SolicitudDeOrden(@NotEmpty List<Long> idsEnOrden) {

  public SolicitudDeOrden {
    idsEnOrden = (idsEnOrden == null) ? null : List.copyOf(idsEnOrden);
  }

  /**
   * Exige que la solicitud traiga exactamente los identificadores existentes, sin repetirlos.
   *
   * @throws ErrorDeAplicacion con {@code ORDEN_INVALIDO} si sobra, falta o se repite alguno
   */
  public void exigirExactamente(Set<Long> existentes) {
    Set<Long> sinRepetidos = new HashSet<>(idsEnOrden);
    if (sinRepetidos.size() != idsEnOrden.size() || !sinRepetidos.equals(existentes)) {
      throw new ErrorDeAplicacion(
          HttpStatus.BAD_REQUEST,
          "ORDEN_INVALIDO",
          "El orden debe incluir exactamente los elementos existentes, sin repetirlos.");
    }
  }
}
