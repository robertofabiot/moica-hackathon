-- Identidad, autenticacion y sesiones (rango reservado V10-V19).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`.
-- El diccionario nombra los atributos en camelCase; en PostgreSQL todos los
-- identificadores van en snake_case, con correspondencia directa y sin comillas
-- (`nombreCompleto` -> `nombre_completo`), que es lo que Hibernate espera por
-- omision.
--
-- Los dominios controlados se modelan como VARCHAR con CHECK, nunca como tipos
-- enum nativos de PostgreSQL: el enum de Java es quien los representa en la
-- aplicacion y un CHECK se puede ampliar con una migracion ordinaria.

CREATE TABLE usuario (
    id_usuario              BIGINT       GENERATED ALWAYS AS IDENTITY,
    nombre_completo         VARCHAR(120) NOT NULL,
    -- 254 caracteres es el maximo de una direccion de correo segun RFC 5321.
    -- Se guarda normalizada (sin espacios exteriores y en minusculas) para que
    -- la unicidad no dependa de como se escriba.
    correo_electronico      VARCHAR(254) NOT NULL,
    -- Solo el resultado del algoritmo de hash. La contrasena original no se
    -- almacena, ni aqui ni en ningun registro.
    clave_hash              VARCHAR(255) NOT NULL,
    estado_cuenta           VARCHAR(30)  NOT NULL DEFAULT 'ACTIVA',
    fecha_fin_estado_cuenta TIMESTAMPTZ,
    fecha_registro          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_correo_electronico UNIQUE (correo_electronico),
    CONSTRAINT ck_usuario_estado_cuenta CHECK (
        estado_cuenta IN (
            'ACTIVA',
            'RESTRINGIDA_TEMPORAL',
            'SUSPENDIDA_TEMPORAL',
            'SUSPENDIDA_PERMANENTE'
        )
    )
);

COMMENT ON TABLE usuario IS
    'Cuenta registrada en Moica. Toda cuenta puede actuar como cliente y, opcionalmente, extenderse como prestador o administrador.';

CREATE TABLE sesion (
    id_sesion                 BIGINT      GENERATED ALWAYS AS IDENTITY,
    id_usuario                BIGINT      NOT NULL,
    -- Identificador aleatorio del token emitido. Viaja en el claim `jti` del
    -- JWT y es lo unico que vincula ese token con esta fila: el token completo
    -- nunca se guarda.
    identificador_token       VARCHAR(64) NOT NULL,
    segundo_factor_verificado BOOLEAN     NOT NULL DEFAULT FALSE,
    fecha_inicio              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion          TIMESTAMPTZ NOT NULL,
    fecha_revocacion          TIMESTAMPTZ,
    motivo_revocacion         VARCHAR(30),

    CONSTRAINT pk_sesion PRIMARY KEY (id_sesion),
    CONSTRAINT fk_sesion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT uq_sesion_identificador_token UNIQUE (identificador_token),
    CONSTRAINT ck_sesion_vigencia CHECK (fecha_expiracion > fecha_inicio),
    -- Una sesion revocada conserva el instante y el motivo: o existen los dos
    -- valores, o no existe ninguno.
    CONSTRAINT ck_sesion_revocacion CHECK (
        (fecha_revocacion IS NULL) = (motivo_revocacion IS NULL)
    ),
    CONSTRAINT ck_sesion_motivo_revocacion CHECK (
        motivo_revocacion IS NULL
        OR motivo_revocacion IN (
            'CIERRE_VOLUNTARIO',
            'CAMBIO_CREDENCIALES',
            'MEDIDA_ADMINISTRATIVA'
        )
    )
);

COMMENT ON TABLE sesion IS
    'Sesion abierta por una cuenta al iniciar sesion; permite expirar y revocar el acceso concedido a un token.';
