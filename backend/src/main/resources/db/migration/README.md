# Migraciones de Flyway

Este directorio contiene las migraciones versionadas del esquema de PostgreSQL.
Las tablas del diccionario de datos llegan con el incremento que las necesita;
P2 abre el rango de identidad con `V10__crear_usuario_y_sesion.sql`, P3 lo
completa con `V11__crear_administrador_y_segundo_factor.sql`, P4 abre el rango
de territorio, perfiles y portafolio con `V20`–`V23` y P4V abre el de
verificación documental con `V30`.

## Reglas

- Un archivo por migración, con el nombre `V<numero>__<descripcion>.sql`
  (dos guiones bajos), en minúsculas y con palabras separadas por guion bajo.
  Ejemplo: `V10__crear_usuario_y_sesion.sql`.
- Una migración aplicada **nunca se edita**. Si algo debe cambiar, se agrega una
  migración nueva. Flyway guarda el checksum de cada archivo aplicado y falla si
  detecta que uno cambió.
- La migración viaja en el **mismo Pull Request** que el código que depende de
  ella.
- Los dominios controlados se modelan como `VARCHAR` con restricción `CHECK`.
  Prohibido crear tipos enum nativos de PostgreSQL.
- Identificadores en `BIGINT` y fechas en `TIMESTAMPTZ`, según
  `Docs/Core/prompt.md`.
- La estructura debe corresponder con `Docs/Dev/Moica - Diccionario de Datos.xlsx`
  y `Docs/Dev/DiagramaLogico.mmd`, que son las fuentes de verdad del modelo.

## Rangos reservados

Los rangos evitan que dos ramas elijan el mismo número de versión.

| Rango | Contenido |
|---|---|
| `V1`–`V9` | Extensiones, dominios y base común |
| `V10`–`V19` | Identidad, autenticación y sesiones |
| `V20`–`V29` | Territorio, perfiles y portafolio |
| `V30`–`V39` | Verificación de prestadores, archivos privados, servicios y catálogos |
| `V40`–`V49` | Solicitudes, mensajes y calificaciones |
| `V50`–`V59` | Moderación, medidas e historial SCD2 |
| `V90`–`V99` | Catálogos y datos de demostración aprobados |

No se crea una migración vacía para ocupar un número, ni se ocupa por
adelantado un rango que todavía no hace falta.

## Migraciones aplicadas

| Migración | Incremento | Contenido |
|---|---|---|
| `V10__crear_usuario_y_sesion.sql` | P2 | Tablas `usuario` y `sesion` con sus claves, dominios y restricciones |
| `V11__crear_administrador_y_segundo_factor.sql` | P3 | Tablas `administrador` y `segundo_factor_usuario`, especializaciones 0..1 de `usuario`, y el índice `ix_sesion_id_usuario` |
| `V20__crear_departamento_y_municipio.sql` | P4 | Catálogos `departamento` y `municipio` con su unicidad por departamento |
| `V21__crear_perfil_prestador_y_contactos.sql` | P4 | Tablas `perfil_prestador` (especialización 0..1 de `usuario`) y `medio_contacto_prestador`, con sus dominios e índice por prestador |
| `V22__crear_trabajos_de_portafolio.sql` | P4 | Tablas `trabajo_portafolio` e `imagen_trabajo_portafolio` con sus índices por prestador y por trabajo |
| `V23__cargar_managua_y_sus_municipios.sql` | P4 | Departamento de Managua habilitado y sus nueve municipios |
| `V30__crear_solicitudes_y_documentos_de_verificacion.sql` | P4V | Tablas `solicitud_verificacion_prestador` y `documento_verificacion_prestador`, el índice parcial `uq_solicitud_verificacion_abierta` y los índices por prestador, por estado y por solicitud |
