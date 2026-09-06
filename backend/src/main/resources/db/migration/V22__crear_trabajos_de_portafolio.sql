-- Trabajos del portafolio y sus imagenes (rango reservado V20-V29).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- No existe una tabla `portafolio`: cada trabajo cuelga directamente del
-- perfil, porque el portafolio no tiene atributos propios (definicion 5.5).

CREATE TABLE trabajo_portafolio (
    id_trabajo          BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_prestador        BIGINT       NOT NULL,
    titulo              VARCHAR(150) NOT NULL,
    descripcion         TEXT         NOT NULL,
    -- Solo cuando el prestador desea mostrarla.
    fecha_realizacion   DATE,
    -- El minimo de cero y la coherencia del orden los garantiza la aplicacion,
    -- tal como reparte responsabilidades el diccionario.
    orden_visualizacion SMALLINT     NOT NULL DEFAULT 0,
    fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_trabajo_portafolio PRIMARY KEY (id_trabajo),
    CONSTRAINT fk_trabajo_portafolio_perfil FOREIGN KEY (id_prestador)
        REFERENCES perfil_prestador (id_prestador) ON DELETE CASCADE
);

COMMENT ON TABLE trabajo_portafolio IS
    'Trabajo anterior que el prestador muestra como parte de su portafolio dentro del perfil.';

CREATE TABLE imagen_trabajo_portafolio (
    id_imagen_trabajo_portafolio BIGINT       GENERATED ALWAYS AS IDENTITY,
    id_trabajo                   BIGINT       NOT NULL,
    -- Direccion publica de la imagen en el almacenamiento de objetos. La base
    -- de datos guarda solo la URL, nunca el binario.
    url_imagen                   VARCHAR(500) NOT NULL,
    texto_alternativo            VARCHAR(200),
    orden_visualizacion          SMALLINT     NOT NULL DEFAULT 0,
    fecha_creacion               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_imagen_trabajo_portafolio PRIMARY KEY (id_imagen_trabajo_portafolio),
    CONSTRAINT fk_imagen_trabajo_portafolio_trabajo FOREIGN KEY (id_trabajo)
        REFERENCES trabajo_portafolio (id_trabajo) ON DELETE CASCADE
);

COMMENT ON TABLE imagen_trabajo_portafolio IS
    'Imagen asociada con un trabajo del portafolio.';

-- Los trabajos siempre se consultan por prestador y las imagenes por trabajo;
-- sin estos indices cada lectura del portafolio recorreria las tablas enteras.
CREATE INDEX ix_trabajo_portafolio_id_prestador
    ON trabajo_portafolio (id_prestador);
CREATE INDEX ix_imagen_trabajo_portafolio_id_trabajo
    ON imagen_trabajo_portafolio (id_trabajo);
