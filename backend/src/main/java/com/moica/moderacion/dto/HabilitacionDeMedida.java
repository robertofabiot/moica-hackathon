package com.moica.moderacion.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Si una medida del catálogo sigue ofreciéndose para aplicaciones nuevas.
 *
 * <p>Es un recurso propio y no un campo de la edición porque deshabilitar es lo que el negocio
 * llama «eliminar»: merece una decisión explícita y no colarse dentro de un formulario que también
 * cambia la descripción.
 *
 * <p>La medida nunca se borra. Deshabilitarla la retira de las aplicaciones futuras y conserva
 * intacta la trazabilidad de las decisiones que la citaron.
 */
public record HabilitacionDeMedida(
    @NotNull(message = "Indica si la medida queda habilitada.") Boolean habilitada) {}
