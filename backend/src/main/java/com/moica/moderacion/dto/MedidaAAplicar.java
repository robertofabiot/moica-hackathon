package com.moica.moderacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * La medida que una persona administradora decide aplicar a la cuenta reportada.
 *
 * <p><b>La elige siempre una persona.</b> Moica no propone ninguna, no la deduce de la severidad ni
 * de cuántos casos acumule la cuenta, y no escala nada por reincidencia: la definición 11.3 y la
 * decisión D-MOD-01 lo excluyen del MVP. Por eso {@code idMedidaAdministrativa} es obligatorio y no
 * existe ninguna vía de aplicar «la que corresponda».
 *
 * @param fechaFinMedida cuándo termina, obligatoria cuando la medida lo exige y prohibida cuando
 *     no. Debe estar en el futuro: un plazo ya vencido nacería expirado. Lo comprueba el servicio,
 *     que es quien conoce la medida elegida y el reloj
 * @param justificacion por qué se aplica; queda en el historial del caso, que es donde una
 *     apelación posterior irá a leerlo
 * @param confirmaReemplazo la confirmación explícita que D-MOD-03 exige cuando la cuenta ya
 *     sostiene otra medida vigente. Sin ella, esa segunda aplicación responde 409 y no sustituye
 *     nada: la interfaz debe advertir cuál está vigente y preguntar antes de reenviar con este
 *     campo
 */
public record MedidaAAplicar(
    @NotNull(message = "Elige la medida que vas a aplicar.") Short idMedidaAdministrativa,
    OffsetDateTime fechaFinMedida,
    @NotBlank(message = "Escribe por qué aplicas esta medida.") @Size(max = 2000, message = "La justificación no puede pasar de 2000 caracteres.") String justificacion,
    boolean confirmaReemplazo) {

  public MedidaAAplicar {
    justificacion = justificacion == null ? null : justificacion.strip();
  }
}
