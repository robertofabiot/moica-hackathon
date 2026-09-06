-- Datos territoriales del MVP: Managua habilitado con sus nueve municipios.
--
-- La lista es la division politico-administrativa vigente del departamento de
-- Managua. Es una carga determinista: en una instalacion limpia produce
-- siempre las mismas filas, y las claves generadas no se asumen en ningun
-- codigo (la aplicacion resuelve municipios por consulta, nunca por id fijo).
--
-- Habilitar otro departamento sera otra migracion como esta, revisada en su
-- propio Pull Request.

INSERT INTO departamento (nombre, habilitado)
VALUES ('Managua', TRUE);

INSERT INTO municipio (id_departamento, nombre)
SELECT d.id_departamento, m.nombre
FROM departamento d
CROSS JOIN (
    VALUES
        ('Ciudad Sandino'),
        ('El Crucero'),
        ('Managua'),
        ('Mateare'),
        ('San Francisco Libre'),
        ('San Rafael del Sur'),
        ('Ticuantepe'),
        ('Tipitapa'),
        ('Villa El Carmen')
) AS m(nombre)
WHERE d.nombre = 'Managua';
