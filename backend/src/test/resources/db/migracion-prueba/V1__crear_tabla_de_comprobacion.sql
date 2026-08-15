-- Migracion de prueba. NO pertenece al esquema de Moica.
--
-- Existe solo para demostrar, contra PostgreSQL real, que Flyway aplica
-- migraciones versionadas al arrancar y registra su historial. Vive en el
-- classpath de pruebas y nunca se empaqueta con la aplicacion.
--
-- Las migraciones reales van en src/main/resources/db/migration, siguiendo los
-- rangos reservados que documenta el README de ese directorio.

CREATE TABLE comprobacion_migracion (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    descripcion VARCHAR(60) NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now()
);
