package com.moica.auth.dto;

import com.moica.auth.entity.EstadoSegundoFactor;
import java.time.OffsetDateTime;

/**
 * Estado del segundo factor de una cuenta.
 *
 * <p>Describe en qué punto está y si la cuenta puede prescindir de él. No lleva el secreto ni nada
 * que permita reconstruirlo: una vez activado, el secreto ya no vuelve a salir de Moica.
 *
 * @param estado punto del ciclo en que se encuentra; {@code null} cuando la cuenta nunca registró
 *     ninguno
 * @param obligatorio si la cuenta no puede desactivarlo. Hoy solo ocurre con el rol administrativo
 * @param fechaActivacion cuándo se confirmó por primera vez, si llegó a confirmarse
 */
public record RespuestaDeSegundoFactor(
    EstadoSegundoFactor estado, boolean obligatorio, OffsetDateTime fechaActivacion) {}
