-- Solicitudes de servicio e historial de transiciones (rango V40-V49).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- Las solicitudes y su historial no se eliminan fisicamente: se conservan como
-- evidencia del ciclo. Por eso las FK son RESTRICT y no hay DELETE en la API.
-- MensajeSolicitud y CalificacionUsuario quedan para incrementos posteriores.

CREATE TABLE solicitud_servicio (
    id_solicitud_servicio  BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_cliente             BIGINT       NOT NULL,
    id_servicio_publicado  BIGINT       NOT NULL,
    id_municipio           INTEGER      NOT NULL,
    descripcion_necesidad  TEXT         NOT NULL,
    indicacion_ubicacion   TEXT         NOT NULL,
    fecha_preferida        DATE,
    estado_actual          VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_solicitud_servicio PRIMARY KEY (id_solicitud_servicio),
    CONSTRAINT fk_solicitud_servicio_cliente FOREIGN KEY (id_cliente)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitud_servicio_servicio FOREIGN KEY (id_servicio_publicado)
        REFERENCES servicio_publicado (id_servicio_publicado) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitud_servicio_municipio FOREIGN KEY (id_municipio)
        REFERENCES municipio (id_municipio) ON DELETE RESTRICT,
    CONSTRAINT ck_solicitud_servicio_estado CHECK (
        estado_actual IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA', 'COMPLETADA')
    )
);

COMMENT ON TABLE solicitud_servicio IS
    'Interes de un cliente en un servicio publicado, con su estado vigente.';

CREATE TABLE cambio_estado_solicitud (
    id_cambio_estado_solicitud  BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_solicitud_servicio       BIGINT       NOT NULL,
    estado_anterior             VARCHAR(30),
    estado_nuevo                VARCHAR(30)  NOT NULL,
    id_actor                    BIGINT       NOT NULL,
    motivo                      TEXT,
    fecha_cambio                TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_cambio_estado_solicitud PRIMARY KEY (id_cambio_estado_solicitud),
    CONSTRAINT fk_cambio_estado_solicitud_solicitud FOREIGN KEY (id_solicitud_servicio)
        REFERENCES solicitud_servicio (id_solicitud_servicio) ON DELETE RESTRICT,
    CONSTRAINT fk_cambio_estado_solicitud_actor FOREIGN KEY (id_actor)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT ck_cambio_estado_solicitud_estado_anterior CHECK (
        estado_anterior IS NULL
        OR estado_anterior IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA', 'COMPLETADA')
    ),
    CONSTRAINT ck_cambio_estado_solicitud_estado_nuevo CHECK (
        estado_nuevo IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'CANCELADA', 'COMPLETADA')
    ),
    CONSTRAINT ck_cambio_estado_solicitud_transicion CHECK (
        estado_nuevo IS DISTINCT FROM estado_anterior
    )
);

COMMENT ON TABLE cambio_estado_solicitud IS
    'Historial de cada transicion de una solicitud de servicio.';

-- Bandeja del cliente: las enviadas se consultan siempre por solicitante.
CREATE INDEX ix_solicitud_servicio_id_cliente
    ON solicitud_servicio (id_cliente);

-- Bandeja del prestador y propiedad: se llega al prestador via el servicio.
CREATE INDEX ix_solicitud_servicio_id_servicio_publicado
    ON solicitud_servicio (id_servicio_publicado);

-- Filtros por estado vigente en ambas bandejas.
CREATE INDEX ix_solicitud_servicio_estado_actual
    ON solicitud_servicio (estado_actual);

-- El historial se lee siempre por solicitud, en orden cronologico.
CREATE INDEX ix_cambio_estado_solicitud_id_solicitud
    ON cambio_estado_solicitud (id_solicitud_servicio, fecha_cambio, id_cambio_estado_solicitud);
