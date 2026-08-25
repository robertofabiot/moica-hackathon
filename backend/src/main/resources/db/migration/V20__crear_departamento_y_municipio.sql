-- Territorio: departamentos y municipios (rango reservado V20-V29).
--
-- La estructura, los tipos, la nulabilidad y las restricciones salen de
-- `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd`,
-- con la misma correspondencia camelCase -> snake_case que establecio `V10`.
--
-- Son catalogos: la aplicacion los lee y los filtra, pero no los crea ni los
-- edita desde ningun endpoint. Sus filas llegan por migraciones versionadas,
-- que es lo que permite habilitar otro departamento con un cambio revisable.

CREATE TABLE departamento (
    id_departamento SMALLINT    GENERATED ALWAYS AS IDENTITY,
    nombre          VARCHAR(80) NOT NULL,
    -- Por omision un departamento existe pero no opera: habilitarlo es una
    -- decision expresa. En el MVP unicamente Managua estara habilitado.
    habilitado      BOOLEAN     NOT NULL DEFAULT FALSE,

    CONSTRAINT pk_departamento PRIMARY KEY (id_departamento),
    CONSTRAINT uq_departamento_nombre UNIQUE (nombre)
);

COMMENT ON TABLE departamento IS
    'Catalogo de departamentos. Permite habilitar territorialmente el servicio; en el MVP se habilita Managua.';

CREATE TABLE municipio (
    id_municipio    INTEGER      GENERATED ALWAYS AS IDENTITY,
    id_departamento SMALLINT     NOT NULL,
    nombre          VARCHAR(100) NOT NULL,

    CONSTRAINT pk_municipio PRIMARY KEY (id_municipio),
    -- RESTRICT a proposito: un departamento con municipios no puede borrarse
    -- por accidente, y ningun flujo del MVP borra territorio.
    CONSTRAINT fk_municipio_departamento FOREIGN KEY (id_departamento)
        REFERENCES departamento (id_departamento) ON DELETE RESTRICT,
    -- El nombre de un municipio puede repetirse entre departamentos, pero no
    -- dentro del mismo.
    CONSTRAINT uq_municipio_departamento_nombre UNIQUE (id_departamento, nombre)
);

COMMENT ON TABLE municipio IS
    'Catalogo de municipios pertenecientes a un departamento y utilizados para ubicar perfiles y solicitudes.';
