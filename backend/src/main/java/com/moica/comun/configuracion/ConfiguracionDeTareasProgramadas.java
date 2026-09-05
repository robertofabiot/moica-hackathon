package com.moica.comun.configuracion;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Habilita las tareas periódicas de la aplicación.
 *
 * <p>Existe por una sola necesidad del MVP: levantar las medidas administrativas temporales cuando
 * llega la fecha que una persona fijó al aplicarlas. Es la parte de la moderación que puede ocurrir
 * sin nadie delante, y solo porque ejecuta un plazo ya decidido; Moica no elige, no recomienda y no
 * escala sanciones por su cuenta.
 *
 * <p>Usa el planificador que trae Spring, sin añadir ninguna dependencia ni infraestructura: para
 * un barrido de un minuto sobre una tabla pequeña no hace falta más.
 */
@Configuration
@EnableScheduling
public class ConfiguracionDeTareasProgramadas {}
