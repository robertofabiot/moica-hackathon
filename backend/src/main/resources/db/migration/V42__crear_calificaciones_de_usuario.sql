-- Calificaciones que los participantes emiten al completar una solicitud
-- (rango V40-V49).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- No hay tabla `reputacion`: la definicion 10 es explicita en que el promedio y
-- la cantidad se calculan a partir de estas filas, separados por rol. Guardar un
-- agregado ademas de su origen solo abriria la posibilidad de que discrepen.
--
-- Las calificaciones son inmutables en el MVP: no se editan ni se borran. Por
-- eso las tres FK son RESTRICT y la API no expone DELETE, PUT ni PATCH.
--
-- Aqui viven solo las reglas que la base puede sostener por si sola. Que la
-- solicitud este COMPLETADA, que quien califica participe en ella y que el rol
-- registrado sea el que la contraparte desempeño dependen de otras tablas y los
-- comprueba la aplicacion. La unicidad si vive aqui: es lo unico que resuelve
-- dos envios simultaneos del mismo participante.

CREATE TABLE calificacion_usuario (
    id_calificacion_usuario  BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_solicitud_servicio    BIGINT       NOT NULL,
    id_calificador           BIGINT       NOT NULL,
    id_calificado            BIGINT       NOT NULL,
    rol_calificado           VARCHAR(30)  NOT NULL,
    puntuacion               SMALLINT     NOT NULL,
    comentario               TEXT,
    fecha_creacion           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_calificacion_usuario PRIMARY KEY (id_calificacion_usuario),
    CONSTRAINT fk_calificacion_usuario_solicitud FOREIGN KEY (id_solicitud_servicio)
        REFERENCES solicitud_servicio (id_solicitud_servicio) ON DELETE RESTRICT,
    CONSTRAINT fk_calificacion_usuario_calificador FOREIGN KEY (id_calificador)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_calificacion_usuario_calificado FOREIGN KEY (id_calificado)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    -- Cada participante califica una sola vez por solicitud. Es la restriccion
    -- que decide una carrera entre dos envios: la segunda transaccion choca
    -- aqui y la API responde 409 en lugar de duplicar la fila.
    CONSTRAINT uq_calificacion_usuario_solicitud_calificador
        UNIQUE (id_solicitud_servicio, id_calificador),
    -- Y cada participante recibe como maximo una. Con las dos, una solicitud
    -- admite dos calificaciones: una de cada lado.
    CONSTRAINT uq_calificacion_usuario_solicitud_calificado
        UNIQUE (id_solicitud_servicio, id_calificado),
    CONSTRAINT ck_calificacion_usuario_participantes CHECK (
        id_calificador <> id_calificado
    ),
    CONSTRAINT ck_calificacion_usuario_puntuacion CHECK (
        puntuacion BETWEEN 1 AND 5
    ),
    CONSTRAINT ck_calificacion_usuario_rol CHECK (
        rol_calificado IN ('CLIENTE', 'PRESTADOR')
    )
);

COMMENT ON TABLE calificacion_usuario IS
    'Valoracion que un participante realiza sobre el otro despues de completar una solicitud.';

-- La reputacion se calcula siempre por persona y por rol, recorriendo sus
-- puntuaciones. Con la puntuacion en el propio indice, el promedio, la cantidad
-- y el desglose se resuelven sin volver a la tabla.
CREATE INDEX ix_calificacion_usuario_calificado_rol
    ON calificacion_usuario (id_calificado, rol_calificado, puntuacion);
