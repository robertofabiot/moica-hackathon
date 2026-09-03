# Migraciones de Flyway

Este directorio contiene las migraciones versionadas del esquema de PostgreSQL.
Las tablas del diccionario de datos llegan con el incremento que las necesita;
P2 abre el rango de identidad con `V10__crear_usuario_y_sesion.sql`, P3 lo
completa con `V11__crear_administrador_y_segundo_factor.sql`, P4 abre el rango
de territorio, perfiles y portafolio con `V20`–`V23` y P4V abre el de
verificación documental con `V30`. P5 abre servicios y catálogos de oficio con
`V31` y carga la taxonomía de demostración con `V90`. P6 abre el ciclo de
solicitudes con `V40`, P7 suma sus mensajes con `V41` y P8 cierra el rango de
contratación con las calificaciones en `V42`. P9 abre el de moderación con
`V50` y `V51`.

## Reglas

- Un archivo por migración, con el nombre `V<numero>__<descripcion>.sql`
  (dos guiones bajos), en minúsculas y con palabras separadas por guion bajo.
  Ejemplo: `V10__crear_usuario_y_sesion.sql`.
- Flyway aplica migraciones fuera de orden (`spring.flyway.out-of-order=true`)
  porque los rangos reservados pueden insertar una versión intermedia —como
  `V40`— después de que `V90` ya esté aplicada en un entorno existente.
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
| `V31__crear_categorias_y_servicios_publicados.sql` | P5 | Tablas `categoria_servicio`, `subcategoria_servicio`, `servicio_publicado` e `imagen_servicio_publicado`, con dominio `ACTIVO`/`INACTIVO`, precio opcional `numeric(12,2)` y los índices de propiedad, listado y filtros públicos |
| `V40__crear_solicitudes_e_historial_de_estados.sql` | P6 | Tablas `solicitud_servicio` y `cambio_estado_solicitud`, dominio `PENDIENTE`/`ACEPTADA`/`RECHAZADA`/`CANCELADA`/`COMPLETADA`, transición inicial con `estado_anterior` nulo e índices de bandeja, propiedad, estado e historial |
| `V41__crear_mensajes_de_solicitud.sql` | P7 | Tabla `mensaje_solicitud`, con FK `RESTRICT` hacia `solicitud_servicio` y `usuario`, la restricción `ck_mensaje_solicitud_contenido` que rechaza un mensaje en blanco y el índice `ix_mensaje_solicitud_id_solicitud` para leer el hilo en orden estable. Sin tabla `conversacion` |
| `V42__crear_calificaciones_de_usuario.sql` | P8 | Tabla `calificacion_usuario`, con FK `RESTRICT` hacia `solicitud_servicio` y dos veces hacia `usuario`, las restricciones `uq_calificacion_usuario_solicitud_calificador` y `uq_calificacion_usuario_solicitud_calificado` —una calificación emitida y una recibida por participante y solicitud—, `ck_calificacion_usuario_participantes`, `ck_calificacion_usuario_puntuacion` (1 a 5), el dominio `CLIENTE`/`PRESTADOR` y el índice `ix_calificacion_usuario_calificado_rol` que resuelve la reputación por persona y rol. Sin tabla `reputacion` |
| `V50__crear_casos_medidas_e_historial_scd2.sql` | P9 | Tablas `medida_administrativa`, `caso_moderacion` e `historial_caso`, todas con FK `RESTRICT`. El catálogo de medidas se crea vacío porque las otras dos lo referencian; gestionarlo y aplicarlo es P10B. Incluye `uq_caso_moderacion_solicitud_reportante` —un caso por participante y solicitud, y la que arbitra dos reportes simultáneos—, `ck_caso_moderacion_participantes`, `ck_caso_moderacion_cierre` —resultado, resolución y fecha van juntos—, `ck_caso_moderacion_fecha_fin_medida`, los dominios `EstadoCasoModeracion`, `ResultadoCasoModeracion`, `TipoActorHistorial`, `TipoEventoHistorial` y `EstadoCuenta` como `CHECK`, `uq_historial_caso_version`, `ck_historial_caso_numero_version`, `ck_historial_caso_vigencia`, `ck_historial_caso_actor`, `ck_historial_caso_detalle_cambio` y el índice único parcial `uq_historial_caso_version_actual` |
| `V51__proteger_vigencias_scd2_con_exclusion_temporal.sql` | P9 | Extensión `btree_gist` idempotente y la restricción `ex_historial_caso_vigencia`, un `EXCLUDE USING GIST` sobre `id_caso_moderacion` y el intervalo semiabierto `[fecha_inicio_vigencia, fecha_fin_vigencia)`: dos versiones del mismo caso no pueden superponerse, y dos consecutivas sí pueden compartir el instante de transición |
| `V90__cargar_taxonomia_de_demostracion.sql` | P5 | Tres categorías de demostración —hogar, belleza y tecnología— con tres subcategorías cada una. No es una taxonomía exhaustiva |
