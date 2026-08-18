# Migraciones de Flyway

Este directorio contiene las migraciones versionadas del esquema de PostgreSQL.
Está vacío a propósito: P1 solo integra el mecanismo. Las tablas del diccionario
de datos llegan con el incremento que las necesita.

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

No se crea una migración vacía para ocupar un número. El primer archivo real
será el del incremento que lo necesite.
