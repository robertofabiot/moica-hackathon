-- Categorias, subcategorias, servicios publicados e imagenes (rango V30-V39).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- Los servicios no se eliminan fisicamente: se desactivan para conservar el
-- historial. Por eso la FK hacia el perfil es RESTRICT y no hay DELETE de
-- servicio en la API. Las imagenes si se pueden quitar; su FK es CASCADE
-- por si una limpieza administrativa retirara un servicio huérfano.

CREATE TABLE categoria_servicio (
    id_categoria_servicio SMALLINT     GENERATED ALWAYS AS IDENTITY,
    nombre                VARCHAR(100) NOT NULL,
    descripcion           TEXT,

    CONSTRAINT pk_categoria_servicio PRIMARY KEY (id_categoria_servicio),
    CONSTRAINT uq_categoria_servicio_nombre UNIQUE (nombre)
);

COMMENT ON TABLE categoria_servicio IS
    'Clasificacion general empleada para organizar los servicios ofrecidos en Moica.';

CREATE TABLE subcategoria_servicio (
    id_subcategoria_servicio INTEGER      GENERATED ALWAYS AS IDENTITY,
    id_categoria_servicio    SMALLINT     NOT NULL,
    nombre                   VARCHAR(100) NOT NULL,
    descripcion              TEXT,

    CONSTRAINT pk_subcategoria_servicio PRIMARY KEY (id_subcategoria_servicio),
    CONSTRAINT fk_subcategoria_servicio_categoria FOREIGN KEY (id_categoria_servicio)
        REFERENCES categoria_servicio (id_categoria_servicio) ON DELETE RESTRICT,
    CONSTRAINT uq_subcategoria_servicio_categoria_nombre
        UNIQUE (id_categoria_servicio, nombre)
);

COMMENT ON TABLE subcategoria_servicio IS
    'Clasificacion especifica perteneciente a una categoria y usada para clasificar servicios.';

CREATE TABLE servicio_publicado (
    id_servicio_publicado    BIGINT         GENERATED ALWAYS AS IDENTITY,
    id_prestador             BIGINT         NOT NULL,
    id_subcategoria_servicio INTEGER        NOT NULL,
    nombre                   VARCHAR(150)   NOT NULL,
    descripcion              TEXT           NOT NULL,
    -- Precio orientativo. Nulo se presenta como "A convenir"; si existe, > 0.
    precio_referencia        NUMERIC(12, 2),
    -- El diccionario nace en ACTIVO. La aplicacion crea INACTIVO y solo activa
    -- cuando la cuenta, la disponibilidad y la verificacion lo permiten.
    estado                   VARCHAR(30)    NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion           TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion      TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_servicio_publicado PRIMARY KEY (id_servicio_publicado),
    CONSTRAINT fk_servicio_publicado_perfil FOREIGN KEY (id_prestador)
        REFERENCES perfil_prestador (id_prestador) ON DELETE RESTRICT,
    CONSTRAINT fk_servicio_publicado_subcategoria FOREIGN KEY (id_subcategoria_servicio)
        REFERENCES subcategoria_servicio (id_subcategoria_servicio) ON DELETE RESTRICT,
    CONSTRAINT ck_servicio_publicado_precio CHECK (
        precio_referencia IS NULL OR precio_referencia > 0
    ),
    CONSTRAINT ck_servicio_publicado_estado CHECK (
        estado IN ('ACTIVO', 'INACTIVO')
    )
);

COMMENT ON TABLE servicio_publicado IS
    'Oferta concreta que un prestador publica para que se pueda encontrar y solicitar.';

CREATE TABLE imagen_servicio_publicado (
    id_imagen_servicio_publicado BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_servicio_publicado        BIGINT       NOT NULL,
    -- Direccion publica de la imagen. La base guarda solo la URL, nunca el binario.
    url_imagen                   VARCHAR(500) NOT NULL,
    texto_alternativo            VARCHAR(200),
    orden_visualizacion          SMALLINT     NOT NULL DEFAULT 0,
    fecha_creacion               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_imagen_servicio_publicado PRIMARY KEY (id_imagen_servicio_publicado),
    CONSTRAINT fk_imagen_servicio_publicado_servicio FOREIGN KEY (id_servicio_publicado)
        REFERENCES servicio_publicado (id_servicio_publicado) ON DELETE CASCADE
);

COMMENT ON TABLE imagen_servicio_publicado IS
    'Imagen asociada con un servicio publicado.';

-- Propiedad y listado propio: los servicios se consultan siempre por prestador.
CREATE INDEX ix_servicio_publicado_id_prestador
    ON servicio_publicado (id_prestador);

-- Filtro publico por subcategoria (y, via JOIN, por categoria).
CREATE INDEX ix_servicio_publicado_id_subcategoria
    ON servicio_publicado (id_subcategoria_servicio);

-- Listado publico: solo interesan los ACTIVO.
CREATE INDEX ix_servicio_publicado_estado
    ON servicio_publicado (estado);

-- El catalogo anida subcategorias por categoria.
CREATE INDEX ix_subcategoria_servicio_id_categoria
    ON subcategoria_servicio (id_categoria_servicio);

-- Las imagenes se leen siempre por servicio.
CREATE INDEX ix_imagen_servicio_publicado_id_servicio
    ON imagen_servicio_publicado (id_servicio_publicado);
