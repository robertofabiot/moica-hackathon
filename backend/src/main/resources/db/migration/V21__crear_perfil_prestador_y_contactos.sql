-- Perfil de prestador y sus medios de contacto (rango reservado V20-V29).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.

CREATE TABLE perfil_prestador (
    -- Especializacion 0..1 de `usuario`: la clave compartida es lo que impide
    -- que una cuenta tenga dos perfiles de prestador, sin restriccion adicional.
    id_prestador           BIGINT       NOT NULL,
    nombre_publico         VARCHAR(120) NOT NULL,
    -- Direccion publica de la imagen en el almacenamiento de objetos. La base
    -- de datos guarda solo la URL, nunca el binario.
    url_imagen_perfil      VARCHAR(500),
    descripcion            TEXT         NOT NULL,
    tipo_prestador         VARCHAR(30)  NOT NULL,
    id_municipio_principal INTEGER      NOT NULL,
    descripcion_cobertura  TEXT         NOT NULL,
    disponibilidad         VARCHAR(30)  NOT NULL DEFAULT 'DISPONIBLE',
    -- Proyeccion que solo actualiza el flujo de verificacion documental (P4V).
    -- Sin VERIFICADO_BASICO el perfil no aparece en ninguna superficie publica.
    nivel_verificacion     VARCHAR(30)  NOT NULL DEFAULT 'SIN_VERIFICAR',
    fecha_creacion         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion    TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_perfil_prestador PRIMARY KEY (id_prestador),
    CONSTRAINT fk_perfil_prestador_usuario FOREIGN KEY (id_prestador)
        REFERENCES usuario (id_usuario) ON DELETE CASCADE,
    -- RESTRICT: el territorio no se borra mientras algun perfil lo referencie.
    CONSTRAINT fk_perfil_prestador_municipio FOREIGN KEY (id_municipio_principal)
        REFERENCES municipio (id_municipio) ON DELETE RESTRICT,
    CONSTRAINT ck_perfil_prestador_tipo CHECK (
        tipo_prestador IN ('INDEPENDIENTE', 'EMPRENDIMIENTO', 'PYME')
    ),
    CONSTRAINT ck_perfil_prestador_disponibilidad CHECK (
        disponibilidad IN ('DISPONIBLE', 'NO_DISPONIBLE')
    ),
    CONSTRAINT ck_perfil_prestador_nivel_verificacion CHECK (
        nivel_verificacion IN (
            'SIN_VERIFICAR',
            'VERIFICADO_BASICO',
            'PROFESIONAL_VERIFICADO'
        )
    )
);

COMMENT ON TABLE perfil_prestador IS
    'Informacion publica y operativa que permite a un Usuario ofrecer servicios en Moica.';

CREATE TABLE medio_contacto_prestador (
    id_medio_contacto_prestador BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_prestador                BIGINT       NOT NULL,
    -- Entrada libre: numero, correo, usuario, enlace o indicacion escrita.
    -- Moica no la clasifica por plataforma.
    contenido                   VARCHAR(500) NOT NULL,
    -- El minimo de cero y la coherencia del orden los garantiza la aplicacion,
    -- tal como reparte responsabilidades el diccionario.
    orden_visualizacion         SMALLINT     NOT NULL DEFAULT 0,
    fecha_creacion              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_medio_contacto_prestador PRIMARY KEY (id_medio_contacto_prestador),
    CONSTRAINT fk_medio_contacto_prestador_perfil FOREIGN KEY (id_prestador)
        REFERENCES perfil_prestador (id_prestador) ON DELETE CASCADE
);

COMMENT ON TABLE medio_contacto_prestador IS
    'Dato de contacto externo publicado por el prestador; oculto para terceros hasta que una solicitud sea aceptada.';

-- Los contactos siempre se consultan por prestador; sin este indice cada
-- lectura del perfil recorreria la tabla entera.
CREATE INDEX ix_medio_contacto_prestador_id_prestador
    ON medio_contacto_prestador (id_prestador);
