-- Taxonomia de demostracion del MVP (rango V90-V99).
--
-- Tres categorias con pocas subcategorias, las que fija D-CAT-01 y la
-- definicion de producto. Prueban la estructura; no son una clasificacion
-- exhaustiva del mercado. Ampliarla sera otra migracion, revisada en su PR.
--
-- Las claves las genera IDENTITY. Ningun codigo asume un id fijo: la
-- aplicacion resuelve categorias y subcategorias por consulta.

INSERT INTO categoria_servicio (nombre, descripcion)
VALUES
    (
        'Hogar y mantenimiento',
        'Reparaciones y oficios del hogar. Taxonomía de demostración, no exhaustiva.'
    ),
    (
        'Belleza y cuidado personal',
        'Servicios de belleza y cuidado personal. Taxonomía de demostración, no exhaustiva.'
    ),
    (
        'Tecnología y servicios digitales',
        'Reparación de equipos y servicios digitales. Taxonomía de demostración, no exhaustiva.'
    );

INSERT INTO subcategoria_servicio (id_categoria_servicio, nombre)
SELECT c.id_categoria_servicio, s.nombre
FROM categoria_servicio c
JOIN (
    VALUES
        ('Hogar y mantenimiento', 'Plomería'),
        ('Hogar y mantenimiento', 'Electricidad'),
        ('Hogar y mantenimiento', 'Carpintería'),
        ('Belleza y cuidado personal', 'Maquillaje'),
        ('Belleza y cuidado personal', 'Barbería/peluquería'),
        ('Belleza y cuidado personal', 'Uñas'),
        ('Tecnología y servicios digitales', 'Reparación de computadoras'),
        ('Tecnología y servicios digitales', 'Diseño gráfico'),
        ('Tecnología y servicios digitales', 'Soporte técnico')
) AS s(nombre_categoria, nombre)
    ON c.nombre = s.nombre_categoria;
