-- Casos de moderacion, catalogo de medidas e historial SCD2 (rango V50-V59).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- P9 abre el expediente: un participante reporta a la contraparte y nace un
-- caso con su primera version historica. Nada mas. La revision administrativa,
-- las resoluciones y la aplicacion de medidas llegan en P10A y P10B; aqui solo
-- se levanta la estructura que esas etapas necesitaran encontrar ya construida.
--
-- `medida_administrativa` se crea ahora porque `caso_moderacion` e
-- `historial_caso` la referencian. Queda vacia a proposito: el catalogo, su
-- gestion y su aplicacion son P10B, y sembrar filas aqui adelantaria decisiones
-- que todavia no se han tomado.
--
-- Ninguna de estas tablas se elimina fisicamente: son la evidencia de una
-- investigacion. Por eso todas las FK son RESTRICT y la API no expone DELETE.

CREATE TABLE medida_administrativa (
    id_medida_administrativa  SMALLINT      GENERATED ALWAYS AS IDENTITY,
    codigo                    VARCHAR(50)   NOT NULL,
    nombre                    VARCHAR(100)  NOT NULL,
    descripcion               TEXT,
    nivel_severidad           SMALLINT      NOT NULL,
    estado_cuenta_resultante  VARCHAR(30),
    requiere_fecha_fin        BOOLEAN       NOT NULL DEFAULT FALSE,
    habilitada                BOOLEAN       NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_medida_administrativa PRIMARY KEY (id_medida_administrativa),
    CONSTRAINT uq_medida_administrativa_codigo UNIQUE (codigo),
    CONSTRAINT uq_medida_administrativa_nombre UNIQUE (nombre),
    -- El nivel ordena medidas progresivas para quien decide. Es descriptivo: no
    -- activa ninguna regla automatica, segun la definicion 11.3.
    CONSTRAINT ck_medida_administrativa_nivel_severidad CHECK (
        nivel_severidad > 0
    ),
    -- Nulo cuando la medida no modifica el acceso, como una advertencia.
    CONSTRAINT ck_medida_administrativa_estado_cuenta_resultante CHECK (
        estado_cuenta_resultante IS NULL
        OR estado_cuenta_resultante IN (
            'ACTIVA', 'RESTRINGIDA_TEMPORAL',
            'SUSPENDIDA_TEMPORAL', 'SUSPENDIDA_PERMANENTE')
    )
);

COMMENT ON TABLE medida_administrativa IS
    'Catalogo de sanciones y acciones administrativas que una persona puede aplicar a un caso.';

CREATE TABLE caso_moderacion (
    id_caso_moderacion              BIGINT        GENERATED ALWAYS AS IDENTITY,
    id_solicitud_servicio           BIGINT        NOT NULL,
    id_reportante                   BIGINT        NOT NULL,
    id_reportado                    BIGINT        NOT NULL,
    id_administrador_responsable    BIGINT,
    id_medida_administrativa_actual SMALLINT,
    motivo                          VARCHAR(120)  NOT NULL,
    descripcion                     TEXT          NOT NULL,
    estado_actual                   VARCHAR(30)   NOT NULL DEFAULT 'ABIERTO',
    resultado_actual                VARCHAR(30),
    resolucion_actual               TEXT,
    fecha_fin_medida_actual         TIMESTAMPTZ,
    fecha_apertura                  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_cierre_actual             TIMESTAMPTZ,
    fecha_actualizacion             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_caso_moderacion PRIMARY KEY (id_caso_moderacion),
    CONSTRAINT fk_caso_moderacion_solicitud FOREIGN KEY (id_solicitud_servicio)
        REFERENCES solicitud_servicio (id_solicitud_servicio) ON DELETE RESTRICT,
    CONSTRAINT fk_caso_moderacion_reportante FOREIGN KEY (id_reportante)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_caso_moderacion_reportado FOREIGN KEY (id_reportado)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_caso_moderacion_administrador FOREIGN KEY (id_administrador_responsable)
        REFERENCES administrador (id_administrador) ON DELETE RESTRICT,
    CONSTRAINT fk_caso_moderacion_medida FOREIGN KEY (id_medida_administrativa_actual)
        REFERENCES medida_administrativa (id_medida_administrativa) ON DELETE RESTRICT,
    -- Cada participante origina como maximo un caso por solicitud. Con ella, una
    -- solicitud admite dos: uno por cada lado. Es tambien la restriccion que
    -- decide una carrera entre dos reportes simultaneos del mismo participante:
    -- el perdedor choca aqui y la API responde 409 en lugar de duplicar la fila.
    CONSTRAINT uq_caso_moderacion_solicitud_reportante
        UNIQUE (id_solicitud_servicio, id_reportante),
    CONSTRAINT ck_caso_moderacion_participantes CHECK (
        id_reportante <> id_reportado
    ),
    CONSTRAINT ck_caso_moderacion_estado CHECK (
        estado_actual IN ('ABIERTO', 'EN_REVISION', 'CERRADO', 'REABIERTO')
    ),
    CONSTRAINT ck_caso_moderacion_resultado CHECK (
        resultado_actual IS NULL
        OR resultado_actual IN ('PROCEDENTE', 'DESESTIMADO')
    ),
    -- El cierre es un bloque: exige resultado, resolucion y fecha a la vez.
    -- Cualquier otro estado los deja nulos, de modo que reabrir un caso retire
    -- los tres y no quede una decision vigente que ya no lo es.
    CONSTRAINT ck_caso_moderacion_cierre CHECK (
        (estado_actual = 'CERRADO'
            AND resultado_actual IS NOT NULL
            AND resolucion_actual IS NOT NULL
            AND fecha_cierre_actual IS NOT NULL)
        OR (estado_actual <> 'CERRADO'
            AND resultado_actual IS NULL
            AND resolucion_actual IS NULL
            AND fecha_cierre_actual IS NULL)
    ),
    CONSTRAINT ck_caso_moderacion_fecha_fin_medida CHECK (
        fecha_fin_medida_actual IS NULL
        OR fecha_fin_medida_actual > fecha_apertura
    )
);

COMMENT ON TABLE caso_moderacion IS
    'Expediente abierto por el reporte de un participante sobre el otro, con su estado vigente.';

CREATE TABLE historial_caso (
    id_historial_caso            BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_caso_moderacion           BIGINT       NOT NULL,
    id_usuario_afectado          BIGINT       NOT NULL,
    id_actor                     BIGINT,
    id_administrador_responsable BIGINT,
    id_medida_administrativa     SMALLINT,
    numero_version               INTEGER      NOT NULL,
    tipo_actor                   VARCHAR(30)  NOT NULL,
    tipo_evento                  VARCHAR(30)  NOT NULL,
    estado_caso                  VARCHAR(30)  NOT NULL,
    resultado_caso               VARCHAR(30),
    estado_cuenta                VARCHAR(30)  NOT NULL,
    resolucion                   TEXT,
    fecha_fin_medida             TIMESTAMPTZ,
    detalle_cambio               TEXT         NOT NULL,
    fecha_inicio_vigencia        TIMESTAMPTZ  NOT NULL,
    fecha_fin_vigencia           TIMESTAMPTZ,
    es_version_actual            BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_registro               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_historial_caso PRIMARY KEY (id_historial_caso),
    CONSTRAINT fk_historial_caso_caso FOREIGN KEY (id_caso_moderacion)
        REFERENCES caso_moderacion (id_caso_moderacion) ON DELETE RESTRICT,
    CONSTRAINT fk_historial_caso_usuario_afectado FOREIGN KEY (id_usuario_afectado)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_historial_caso_actor FOREIGN KEY (id_actor)
        REFERENCES usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_historial_caso_administrador FOREIGN KEY (id_administrador_responsable)
        REFERENCES administrador (id_administrador) ON DELETE RESTRICT,
    CONSTRAINT fk_historial_caso_medida FOREIGN KEY (id_medida_administrativa)
        REFERENCES medida_administrativa (id_medida_administrativa) ON DELETE RESTRICT,
    CONSTRAINT uq_historial_caso_version UNIQUE (id_caso_moderacion, numero_version),
    CONSTRAINT ck_historial_caso_numero_version CHECK (
        numero_version > 0
    ),
    CONSTRAINT ck_historial_caso_tipo_actor CHECK (
        tipo_actor IN ('USUARIO', 'ADMINISTRADOR', 'SISTEMA')
    ),
    CONSTRAINT ck_historial_caso_tipo_evento CHECK (
        tipo_evento IN (
            'CASO_ABIERTO', 'RESPONSABLE_ASIGNADO', 'ESTADO_CASO_CAMBIADO',
            'RESOLUCION_REGISTRADA', 'MEDIDA_APLICADA', 'MEDIDA_REVOCADA',
            'MEDIDA_EXPIRADA', 'ESTADO_CUENTA_CAMBIADO', 'APELACION_PRESENTADA',
            'APELACION_ACEPTADA', 'APELACION_RECHAZADA', 'CASO_REABIERTO')
    ),
    CONSTRAINT ck_historial_caso_estado_caso CHECK (
        estado_caso IN ('ABIERTO', 'EN_REVISION', 'CERRADO', 'REABIERTO')
    ),
    CONSTRAINT ck_historial_caso_resultado_caso CHECK (
        resultado_caso IS NULL
        OR resultado_caso IN ('PROCEDENTE', 'DESESTIMADO')
    ),
    CONSTRAINT ck_historial_caso_estado_cuenta CHECK (
        estado_cuenta IN (
            'ACTIVA', 'RESTRINGIDA_TEMPORAL',
            'SUSPENDIDA_TEMPORAL', 'SUSPENDIDA_PERMANENTE')
    ),
    -- Una version sin justificacion no sirve como auditoria. El diccionario
    -- responsabiliza de esta regla a la base, igual que hizo `V41` con el
    -- contenido de un mensaje.
    CONSTRAINT ck_historial_caso_detalle_cambio CHECK (
        btrim(detalle_cambio) <> ''
    ),
    -- La version actual no tiene fin; una cerrada termina despues de empezar.
    -- El fin es exclusivo, asi que dos versiones consecutivas comparten el
    -- instante de transicion sin superponerse.
    CONSTRAINT ck_historial_caso_vigencia CHECK (
        (es_version_actual AND fecha_fin_vigencia IS NULL)
        OR (NOT es_version_actual
            AND fecha_fin_vigencia IS NOT NULL
            AND fecha_fin_vigencia > fecha_inicio_vigencia)
    ),
    -- Un evento del sistema no tiene persona detras; los otros dos si.
    CONSTRAINT ck_historial_caso_actor CHECK (
        (tipo_actor = 'SISTEMA' AND id_actor IS NULL)
        OR (tipo_actor <> 'SISTEMA' AND id_actor IS NOT NULL)
    )
);

COMMENT ON TABLE historial_caso IS
    'Version SCD2 con la fotografia completa de un caso y del estado de la cuenta afectada.';

-- Solo una version vigente por caso. Es un indice parcial y no una restriccion
-- UNIQUE porque la unicidad debe valerse unicamente entre las filas actuales:
-- un caso acumula tantas versiones cerradas como eventos haya tenido.
CREATE UNIQUE INDEX uq_historial_caso_version_actual
    ON historial_caso (id_caso_moderacion)
    WHERE es_version_actual;
