package com.moica.moderacion.dto;

import com.moica.moderacion.entity.MedidaAdministrativa;
import com.moica.usuario.entity.EstadoCuenta;

/**
 * Una medida del catálogo, tal como la lee el área administrativa.
 *
 * <p>Publica la ficha entera porque quien elige una sanción necesita ver exactamente qué implica:
 * en qué estado deja la cuenta, si exige indicar cuándo termina y si sigue disponible.
 *
 * <p>{@code nivelSeveridad} viaja como dato descriptivo. Ordena la lista para quien decide y nada
 * más: Moica no recomienda ni escala medidas, según la definición 11.3.
 *
 * @param estadoCuentaResultante nulo cuando la medida no cambia el acceso, como una advertencia
 * @param requiereFechaFin si aplicarla obliga a indicar cuándo termina
 * @param habilitada una deshabilitada sigue describiendo decisiones pasadas pero ya no se ofrece
 */
public record DatosDeMedidaAdministrativa(
    Short idMedidaAdministrativa,
    String codigo,
    String nombre,
    String descripcion,
    short nivelSeveridad,
    EstadoCuenta estadoCuentaResultante,
    boolean requiereFechaFin,
    boolean habilitada) {

  public static DatosDeMedidaAdministrativa de(MedidaAdministrativa medida) {
    return new DatosDeMedidaAdministrativa(
        medida.getIdMedidaAdministrativa(),
        medida.getCodigo(),
        medida.getNombre(),
        medida.getDescripcion(),
        medida.getNivelSeveridad(),
        medida.getEstadoCuentaResultante(),
        medida.isRequiereFechaFin(),
        medida.isHabilitada());
  }
}
