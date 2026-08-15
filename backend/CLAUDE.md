# Directrices de Claude para el backend

Complementan `../CLAUDE.md` y `../Docs/Dev/ESTANDARES_CODIGO.md`. Aplican a todo
el código de `backend/`.

## Organización

- Monolito modular **por capacidades**, no por tipos técnicos. La raíz de una
  capacidad es `com.moica.<capacidad>`.
- Capacidades previstas: `usuario`, `auth`, `prestador`, `portafolio`,
  `verificacion`, `servicio`, `catalogo`, `solicitud`, `chat`, `calificacion`,
  `moderacion`, `admin` y `comun`.
- Una capacidad se crea **cuando su incremento la implementa**. Prohibido crear
  paquetes o clases vacías para módulos futuros.
- Dentro de cada capacidad, capas clásicas: `controller`, `service`,
  `repository`, `entity` y `dto`. Se crea la capa que hace falta, no las cinco
  por costumbre.
- `comun` guarda errores, seguridad, auditoría y utilidades compartidas, y no
  depende de ninguna capacidad concreta.
- Entre capacidades se habla de `service` a `service`, nunca contra el
  `repository` ajeno.

## Frontera de entrada y salida

- Todo lo que entra y sale del `controller` es un **DTO validado** con Bean
  Validation. Prohibido exponer entidades JPA en un endpoint.
- El DTO de salida publica solo lo necesario. Nunca lleva hashes, secretos TOTP,
  tokens, claves de almacenamiento privado, documentos de identidad ni
  observaciones administrativas.
- Las reglas que las anotaciones no expresan se validan en el `service`.

## Cómo se escribe una clase

- **Inyección por constructor**, con campos `private final`. Prohibido
  `@Autowired` en campos.
- **Controladores delgados:** reciben, delegan y devuelven. Sin `if` de negocio,
  sin consultas, sin transacciones.
- **Lógica de negocio en el `service`**, que es donde vive `@Transactional` y la
  autorización de dominio (rol, propiedad, estado de cuenta).
- **Repositorios solo de acceso a datos.** Un método de repositorio no decide.
- Nombres claros y métodos pequeños con una responsabilidad concreta. Si al
  describir un método hace falta un «y», se divide.
- Prohibido crear interfaces con una sola implementación, clases vacías o capas
  «preparadas» sin uso actual.

## Errores

- Excepciones propias para lo esperable: recurso inexistente, permiso denegado,
  estado inválido, conflicto.
- Un manejador global las traduce a una respuesta de forma uniforme.
- El cuerpo del error no lleva trazas, SQL, nombres de clase ni valores
  internos. Nunca se devuelve un `stacktrace`.
- Prohibido capturar una excepción sin registrarla ni transformarla.

## Datos y persistencia

- Tipos que fija `Docs/Core/prompt.md`: identificadores `BIGINT` (`Long`),
  fechas `TIMESTAMPTZ` (`OffsetDateTime`), auditoría `fechaCreacion` y
  `fechaActualizacion` en las entidades principales.
- Dominios controlados: `VARCHAR` con `CHECK` en PostgreSQL y `enum` de Java en
  la aplicación. Prohibido crear tipos enum nativos de PostgreSQL.
- Todo cambio de esquema es una migración de Flyway en
  `src/main/resources/db/migration`, dentro del rango reservado del plan, y
  viaja en el mismo PR que el código que la usa.
- Prohibido `ddl-auto` distinto de `validate`. El esquema lo crea Flyway.
- La persistencia se prueba contra PostgreSQL real con Testcontainers. Prohibido
  usar H2 para simular compatibilidad.

## Pruebas

- Pruebas unitarias en `*Test`; pruebas que necesitan PostgreSQL en `*IT`, que
  ejecuta `./mvnw verify` con Failsafe.
- Cada regla nueva llega con una prueba positiva y una negativa.
- Antes de dar por terminada una tarea: `./mvnw verify` en verde.

## Configuración

- Ningún valor de conexión, credencial o secreto se escribe en el código ni en
  `application.properties`. Se leen de variables de entorno con la forma
  `${VARIABLE}` y se documentan en `../.env.example` y en el `README.md`.
- Actuator expone únicamente lo necesario. Prohibido publicar endpoints de
  administración o detalles de salud sin decisión del equipo.
