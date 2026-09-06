-- Mensajes del hilo de una solicitud de servicio (rango V40-V49).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- No hay tabla `conversacion`: el MVP admite un unico hilo entre los dos
-- participantes y su estado se deriva de la solicitud, asi que cada mensaje
-- cuelga directamente de `solicitud_servicio` (definicion 9).
--
-- Los mensajes son evidencia de la relacion: no se editan ni se borran. Por eso
-- las FK son RESTRICT y la API no expone DELETE, PUT ni PATCH.

CREATE TABLE mensaje_solicitud (
    id_mensaje_solicitud   BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_solicitud_servicio  BIGINT       NOT NULL,
    id_remitente           BIGINT       NOT NULL,
    contenido              TEXT         NOT NULL,
    fecha_envio            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_mensaje_solicitud PRIMARY KEY (id_mensaje_solicitud),
    CONSTRAINT fk_mensaje_solicitud_solicitud FOREIGN KEY (id_solicitud_servicio)
        REFERENCES solicitud_servicio (id_solicitud_servicio) ON DELETE RESTRICT,
    CONSTRAINT fk_mensaje_solicitud_remitente FOREIGN KEY (id_remitente)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    -- El diccionario modela `contenido` como TEXT sin maximo, asi que aqui no se
    -- inventa uno: lo unico que la base impone es que el mensaje diga algo. El
    -- tope de 2000 caracteres es de la aplicacion y vive en el DTO.
    CONSTRAINT ck_mensaje_solicitud_contenido CHECK (
        btrim(contenido, E' \t\n\r') <> ''
    )
);

COMMENT ON TABLE mensaje_solicitud IS
    'Mensaje de texto enviado por un participante dentro de una solicitud aceptada.';

-- El hilo se lee siempre por solicitud y en orden cronologico. El identificador
-- desempata dos mensajes con el mismo instante, de modo que el orden es estable.
CREATE INDEX ix_mensaje_solicitud_id_solicitud
    ON mensaje_solicitud (id_solicitud_servicio, fecha_envio, id_mensaje_solicitud);
