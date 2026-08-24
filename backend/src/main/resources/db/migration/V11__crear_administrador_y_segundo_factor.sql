-- Rol administrativo y segundo factor TOTP (rango reservado V10-V19).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- Ambas tablas son especializaciones 0..1 de `usuario`: comparten su clave
-- primaria, de modo que la propia clave impide que una cuenta tenga dos roles
-- administrativos o dos segundos factores. No hace falta una restriccion unica
-- adicional.

CREATE TABLE administrador (
    id_administrador BIGINT      NOT NULL,
    fecha_asignacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_administrador PRIMARY KEY (id_administrador),
    CONSTRAINT fk_administrador_usuario FOREIGN KEY (id_administrador)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE
);

COMMENT ON TABLE administrador IS
    'Extension de Usuario con permisos para resolver verificaciones de prestadores, gestionar casos de moderacion, revisar apelaciones y aplicar o revertir medidas administrativas.';

CREATE TABLE segundo_factor_usuario (
    id_usuario                BIGINT       NOT NULL,
    -- Secreto compartido con la aplicacion autenticadora. Se guarda cifrado
    -- con AES-GCM y una clave que llega por variable de entorno: la columna
    -- nunca contiene el valor en claro. Cabe de sobra en 255 caracteres.
    secreto_totp              VARCHAR(255) NOT NULL,
    estado_segundo_factor     VARCHAR(30)  NOT NULL DEFAULT 'PENDIENTE_ACTIVACION',
    fecha_activacion          TIMESTAMPTZ,
    fecha_ultima_verificacion TIMESTAMPTZ,
    fecha_creacion            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_segundo_factor_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT fk_segundo_factor_usuario_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT ck_segundo_factor_usuario_estado CHECK (
        estado_segundo_factor IN (
            'PENDIENTE_ACTIVACION',
            'ACTIVO',
            'DESACTIVADO'
        )
    ),
    -- El diccionario exige la fecha de activacion cuando el estado es ACTIVO.
    -- Es una implicacion, no una equivalencia: al desactivar el segundo factor
    -- la fecha se conserva porque documenta cuando llego a estar activo.
    CONSTRAINT ck_segundo_factor_usuario_activacion CHECK (
        estado_segundo_factor <> 'ACTIVO' OR fecha_activacion IS NOT NULL
    )
);

COMMENT ON TABLE segundo_factor_usuario IS
    'Segundo factor de autenticacion TOTP registrado por una cuenta; obligatorio para el rol administrativo y opcional para el resto.';

-- Cambiar la contrasena o el segundo factor revoca todas las sesiones de la
-- cuenta a la vez. Sin este indice esa operacion recorreria la tabla entera.
CREATE INDEX ix_sesion_id_usuario ON sesion (id_usuario);
