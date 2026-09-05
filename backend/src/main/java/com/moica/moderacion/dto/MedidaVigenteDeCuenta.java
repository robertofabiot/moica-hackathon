package com.moica.moderacion.dto;

import com.moica.moderacion.entity.CasoModeracion;
import com.moica.moderacion.entity.MedidaAdministrativa;
import com.moica.usuario.entity.EstadoCuenta;
import java.time.OffsetDateTime;

/**
 * La única medida que la cuenta reportada sostiene ahora mismo, con el expediente que la impuso.
 *
 * <p>Existe porque la regla D-MOD-03 es de la <b>cuenta</b> y no del caso: una persona puede
 * arrastrar varios expedientes y solo uno de ellos sostiene la sanción vigente. Quien mira un caso
 * necesita saberlo antes de aplicar otra, y por eso la respuesta del expediente la incluye aunque
 * la medida venga de otro caso.
 *
 * @param esDeEsteCaso si la sostiene el expediente que se está mirando. Cuando es {@code false}, la
 *     interfaz debe advertir que aplicar aquí sustituirá una sanción impuesta en otro expediente
 * @param fechaFinMedida cuándo termina; nulo cuando la medida solo se levanta revocándola
 * @param estadoCuentaResultante el estado que esta medida impuso; nulo si no cambia el acceso
 */
public record MedidaVigenteDeCuenta(
    Long idCasoModeracion,
    boolean esDeEsteCaso,
    Short idMedidaAdministrativa,
    String codigo,
    String nombre,
    EstadoCuenta estadoCuentaResultante,
    OffsetDateTime fechaFinMedida) {

  public static MedidaVigenteDeCuenta de(
      CasoModeracion caso, MedidaAdministrativa medida, boolean esDeEsteCaso) {

    return new MedidaVigenteDeCuenta(
        caso.getIdCasoModeracion(),
        esDeEsteCaso,
        medida.getIdMedidaAdministrativa(),
        medida.getCodigo(),
        medida.getNombre(),
        medida.getEstadoCuentaResultante(),
        caso.getFechaFinMedidaActual());
  }
}
