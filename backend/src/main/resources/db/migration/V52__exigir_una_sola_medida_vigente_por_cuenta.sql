-- Una sola medida administrativa vigente por cuenta (rango V50-V59).
--
-- `V50` levanto la estructura completa que P10B necesitaba encontrar: el
-- catalogo `medida_administrativa`, la medida vigente del expediente en
-- `caso_moderacion.id_medida_administrativa_actual` y su fotografia en
-- `historial_caso`. No hace falta ninguna tabla ni columna nueva.
--
-- Lo unico que el esquema todavia no sabe decir es la regla D-MOD-03 del plan:
-- «cada cuenta podra tener como maximo una medida vigente». Hoy nada impide que
-- dos expedientes distintos de la misma persona reporten los dos una medida
-- actual, porque la restriccion no vive dentro de una fila sino entre filas.
--
-- Bloquear la fila de `usuario` antes de aplicar serializa a dos administradores
-- que actuen a la vez, y es lo que hace el servicio. Pero un bloqueo solo vale
-- mientras todo el codigo recuerde tomarlo: una ruta futura que lo olvide
-- dejaria dos sanciones vigentes sobre la misma persona sin que nada protestara.
-- Por eso la garantia se escribe tambien aqui, igual que
-- `uq_caso_moderacion_solicitud_reportante` sostiene «un caso por participante»
-- en lugar de confiarlo a la comprobacion previa del servicio.
--
-- Es un indice parcial y no una restriccion UNIQUE porque la unicidad solo debe
-- valer entre los expedientes que sostienen una medida: una persona acumula
-- tantos casos sin medida como reportes reciba, y todos deben convivir.

CREATE UNIQUE INDEX uq_caso_moderacion_medida_vigente_por_cuenta
    ON caso_moderacion (id_reportado)
    WHERE id_medida_administrativa_actual IS NOT NULL;

COMMENT ON INDEX uq_caso_moderacion_medida_vigente_por_cuenta IS
    'D-MOD-03: una cuenta sostiene como maximo una medida administrativa vigente a la vez.';
