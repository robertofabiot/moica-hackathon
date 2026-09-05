package com.moica.moderacion.dto;

import com.moica.usuario.entity.EstadoCuenta;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Una medida nueva del catálogo.
 *
 * <p>El código llega solo aquí y no en la edición: identifica la medida ante las decisiones que la
 * citaron, y cambiarlo dejaría un historial hablando de algo que ya no existe.
 *
 * <p>El patrón del código lo restringe a mayúsculas, dígitos y guion bajo porque es un
 * identificador y no una etiqueta: lo leen personas y lo comparan máquinas. El nombre y la
 * descripción son los que sí pueden reescribirse.
 *
 * <p>Que el código y el nombre no estén repetidos lo comprueba el servicio, y {@code
 * uq_medida_administrativa_codigo} y {@code uq_medida_administrativa_nombre} arbitran la carrera:
 * una anotación no puede consultar la base.
 *
 * @param estadoCuentaResultante nulo a propósito cuando la medida no cambia el acceso
 */
public record MedidaACrear(
    @NotBlank(message = "Escribe el código de la medida.") @Size(max = 50, message = "El código no puede pasar de 50 caracteres.") @Pattern(
            regexp = "[A-Z0-9_]+",
            message = "El código solo admite mayúsculas, dígitos y guion bajo.")
        String codigo,
    @NotBlank(message = "Escribe el nombre de la medida.") @Size(max = 100, message = "El nombre no puede pasar de 100 caracteres.") String nombre,
    @Size(max = 2000, message = "La descripción no puede pasar de 2000 caracteres.") String descripcion,
    @Min(value = 1, message = "El nivel de severidad empieza en 1.") @Max(value = 100, message = "El nivel de severidad no pasa de 100.") short nivelSeveridad,
    EstadoCuenta estadoCuentaResultante,
    boolean requiereFechaFin) {

  public MedidaACrear {
    codigo = codigo == null ? null : codigo.strip().toUpperCase(java.util.Locale.ROOT);
    nombre = nombre == null ? null : nombre.strip();
    descripcion = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
  }
}
