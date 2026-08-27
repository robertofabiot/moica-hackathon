-- Solicitudes de verificacion y su expediente documental (rango reservado V30-V39).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- Reparto de responsabilidades del diccionario: aqui viven los dominios, la
-- unicidad de la solicitud abierta, la coherencia de fechas y la exigencia de
-- observacion. Lo que depende de otras filas o de la sesion -las transiciones,
-- el nivel de verificacion previo, la propiedad del perfil y el segundo factor
-- del revisor- lo valida la aplicacion, porque una restriccion de fila no puede
-- mirar el perfil ni la sesion que hace la peticion.

CREATE TABLE solicitud_verificacion_prestador (
    id_solicitud_verificacion BIGINT      GENERATED ALWAYS AS IDENTITY,
    id_prestador              BIGINT      NOT NULL,
    -- Se rellena cuando un administrador toma la solicitud y conserva quien
    -- dejo el estado vigente. Nulo mientras esta PENDIENTE.
    id_administrador_revisor  BIGINT,
    nivel_solicitado          VARCHAR(30) NOT NULL,
    estado_solicitud          VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    -- Motivo que el administrador registra al resolver. Obligatorio cuando la
    -- decision es negativa; opcional en el resto.
    observacion_resolucion    TEXT,
    fecha_solicitud           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_inicio_revision     TIMESTAMPTZ,
    fecha_resolucion          TIMESTAMPTZ,
    fecha_actualizacion       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_solicitud_verificacion_prestador
        PRIMARY KEY (id_solicitud_verificacion),
    CONSTRAINT fk_solicitud_verificacion_perfil FOREIGN KEY (id_prestador)
        REFERENCES perfil_prestador (id_prestador) ON DELETE CASCADE,
    -- RESTRICT: quien reviso un expediente no desaparece del historial mientras
    -- ese historial exista.
    CONSTRAINT fk_solicitud_verificacion_administrador FOREIGN KEY (id_administrador_revisor)
        REFERENCES administrador (id_administrador) ON DELETE RESTRICT,
    CONSTRAINT ck_solicitud_verificacion_nivel CHECK (
        nivel_solicitado IN ('BASICA', 'PROFESIONAL')
    ),
    CONSTRAINT ck_solicitud_verificacion_estado CHECK (
        estado_solicitud IN (
            'PENDIENTE',
            'EN_REVISION',
            'APROBADA',
            'RECHAZADA',
            'REVOCADA'
        )
    ),
    -- Una decision negativa sin motivo dejaria al prestador sin saber que
    -- corregir, asi que la base lo impide y no solo la aplicacion.
    CONSTRAINT ck_solicitud_verificacion_observacion CHECK (
        estado_solicitud NOT IN ('RECHAZADA', 'REVOCADA')
        OR (observacion_resolucion IS NOT NULL AND btrim(observacion_resolucion) <> '')
    ),
    -- Ninguna fecha posterior puede preceder al envio del expediente, y todo
    -- estado final deja constancia de cuando se resolvio.
    CONSTRAINT ck_solicitud_verificacion_fechas CHECK (
        (fecha_inicio_revision IS NULL OR fecha_inicio_revision >= fecha_solicitud)
        AND (fecha_resolucion IS NULL OR fecha_resolucion >= fecha_solicitud)
        AND (
            estado_solicitud NOT IN ('APROBADA', 'RECHAZADA', 'REVOCADA')
            OR fecha_resolucion IS NOT NULL
        )
    )
);

COMMENT ON TABLE solicitud_verificacion_prestador IS
    'Intento registrado de un perfil de prestador para obtener o renovar un nivel de verificacion documental.';

-- Una sola solicitud abierta por perfil y nivel. El indice es parcial a
-- proposito: las solicitudes ya resueltas se conservan como evidencia
-- historica y no deben estorbar a la siguiente.
CREATE UNIQUE INDEX uq_solicitud_verificacion_abierta
    ON solicitud_verificacion_prestador (id_prestador, nivel_solicitado)
    WHERE estado_solicitud IN ('PENDIENTE', 'EN_REVISION');

-- El prestador consulta su historial completo y el administrador trabaja
-- siempre sobre una cola filtrada por estado; sin estos indices ambas lecturas
-- recorrerian la tabla entera.
CREATE INDEX ix_solicitud_verificacion_id_prestador
    ON solicitud_verificacion_prestador (id_prestador);
CREATE INDEX ix_solicitud_verificacion_estado
    ON solicitud_verificacion_prestador (estado_solicitud, fecha_solicitud);

CREATE TABLE documento_verificacion_prestador (
    id_documento_verificacion BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_solicitud_verificacion BIGINT       NOT NULL,
    tipo_documento            VARCHAR(30)  NOT NULL,
    -- Clave opaca con la que el almacenamiento privado localiza el archivo.
    -- No es una direccion publica ni permanente: la base guarda la clave y los
    -- metadatos, nunca el binario ni una URL.
    clave_almacenamiento      VARCHAR(300) NOT NULL,
    -- Nombre que eligio el prestador, ya saneado por la aplicacion.
    nombre_original           VARCHAR(255) NOT NULL,
    tipo_mime                 VARCHAR(100) NOT NULL,
    tamano_bytes              INTEGER      NOT NULL,
    fecha_carga               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_documento_verificacion_prestador
        PRIMARY KEY (id_documento_verificacion),
    CONSTRAINT fk_documento_verificacion_solicitud FOREIGN KEY (id_solicitud_verificacion)
        REFERENCES solicitud_verificacion_prestador (id_solicitud_verificacion) ON DELETE CASCADE,
    CONSTRAINT uq_documento_verificacion_clave UNIQUE (clave_almacenamiento),
    CONSTRAINT ck_documento_verificacion_tipo CHECK (
        tipo_documento IN (
            'IDENTIDAD',
            'CERTIFICACION',
            'CONSTANCIA',
            'REGISTRO_NEGOCIO',
            'OTRO_RESPALDO'
        )
    ),
    CONSTRAINT ck_documento_verificacion_mime CHECK (
        tipo_mime IN ('image/jpeg', 'image/png', 'application/pdf')
    ),
    -- 5242880 son los 5 MB del tope documentado. El limite operativo es
    -- configurable por debajo de ese valor, nunca por encima.
    CONSTRAINT ck_documento_verificacion_tamano CHECK (
        tamano_bytes > 0 AND tamano_bytes <= 5242880
    )
);

COMMENT ON TABLE documento_verificacion_prestador IS
    'Archivo privado que forma parte del expediente documental de una solicitud de verificacion.';

-- El expediente siempre se lee completo por solicitud.
CREATE INDEX ix_documento_verificacion_id_solicitud
    ON documento_verificacion_prestador (id_solicitud_verificacion);
