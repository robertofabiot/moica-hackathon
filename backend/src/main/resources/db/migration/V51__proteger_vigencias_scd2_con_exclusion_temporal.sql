-- Exclusion temporal de las vigencias SCD2 de `historial_caso` (rango V50-V59).
--
-- `uq_historial_caso_version_actual` impide dos versiones vigentes a la vez,
-- pero no dice nada de los periodos ya cerrados: dos versiones podrian declarar
-- intervalos que se solapan y el historial dejaria de reconstruir un unico
-- estado por instante. Eso es exactamente lo que una restriccion EXCLUDE
-- resuelve y ninguna combinacion de UNIQUE y CHECK puede resolver.
--
-- El periodo se construye como intervalo semiabierto `[inicio, fin)`. Es lo que
-- permite que la version anterior termine en el mismo instante en que empieza
-- la nueva sin que los dos rangos se consideren superpuestos, que es como el
-- SCD2 encadena sus versiones. Con `fecha_fin_vigencia` nula el rango queda
-- abierto por arriba, asi que la version actual excluye cualquier otra que
-- pretenda empezar despues.
--
-- Se declara en su propia migracion y no dentro de `V50` porque necesita la
-- extension `btree_gist`: sin ella un indice GiST no sabe comparar un `bigint`
-- con el operador `=`, y la restriccion combina precisamente esa igualdad con
-- el solapamiento de rangos.

-- Idempotente a proposito: la extension puede existir ya en una base creada
-- para otro entorno, y volver a instalarla no debe abortar la migracion.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE historial_caso
    ADD CONSTRAINT ex_historial_caso_vigencia EXCLUDE USING GIST (
        id_caso_moderacion WITH =,
        tstzrange(fecha_inicio_vigencia, fecha_fin_vigencia, '[)') WITH &&
    );
