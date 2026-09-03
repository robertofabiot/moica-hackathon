package com.moica.moderacion.repository;

import com.moica.moderacion.entity.HistorialCaso;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Las versiones SCD2 de los casos de moderación.
 *
 * <p>P9 solo escribe: la apertura de un caso crea su primera versión en la misma transacción. No
 * declara consultas propias porque todavía no hay ninguna superficie que lea el historial; leerlo
 * es parte de la revisión administrativa de P10A.
 */
public interface HistorialCasoRepository extends JpaRepository<HistorialCaso, Long> {}
