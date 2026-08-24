# Guia de entorno local

Detalle de configuracion para levantar MOICA en desarrollo. La version rapida esta en el [README](../../README.md#instalacion-rapida).

## Variables de entorno

Copia la plantilla:

```bash
cp .env.example .env
```

En Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

| Variable | Para que sirve | Quien la lee |
|---|---|---|
| `MOICA_DB_NOMBRE` | Nombre de la base de datos | Docker Compose y backend |
| `MOICA_DB_USUARIO` | Usuario de PostgreSQL | Docker Compose y backend |
| `MOICA_DB_CLAVE` | Contrasena de PostgreSQL | Docker Compose y backend |
| `MOICA_DB_HOST` | Host por el que el backend alcanza la base | Backend |
| `MOICA_DB_PORT` | Puerto por el que se publica PostgreSQL (cambialo si el 5432 ya esta ocupado) | Docker Compose y backend |
| `MOICA_PGADMIN_EMAIL` | Usuario de pgAdmin | Docker Compose |
| `MOICA_PGADMIN_CLAVE` | Contrasena de pgAdmin | Docker Compose |
| `MOICA_PGADMIN_PORT` | Puerto web de pgAdmin | Docker Compose |
| `MOICA_BACKEND_PORT` | Puerto de Spring Boot | Backend y proxy de Vite |
| `MOICA_JWT_SECRETO` | Clave con la que se firma el JWT de sesion (minimo 32 bytes) | Backend |
| `MOICA_SESION_DURACION` | Cuanto dura una sesion, en formato ISO-8601 (por omision `P7D`) | Backend |
| `MOICA_COOKIE_SEGURA` | Marca `Secure` en las cookies; `false` en desarrollo, `true` en produccion | Backend |

Los valores de `.env.example` son de desarrollo local. En produccion cada variable se define en el entorno del servidor; ningun secreto se versiona.

### El secreto del JWT

El valor que trae la plantilla es publico —esta en el repositorio— y solo sirve para trabajar en la maquina propia. Cualquier entorno compartido necesita uno aleatorio y distinto:

```bash
openssl rand -base64 48
```

```powershell
$b = [byte[]]::new(48); [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Si el secreto tiene menos de 32 bytes, el backend no arranca: HMAC-SHA256 no admite una clave mas corta.

El backend lee estas variables del entorno del sistema y, si no estan definidas, importa el mismo archivo `.env` de la raiz. Las variables de entorno reales tienen prioridad, de modo que CI y produccion no dependen del archivo.

## PostgreSQL y pgAdmin

Desde la raiz del proyecto:

```bash
docker compose up -d
```

Esto inicia PostgreSQL en el puerto de `MOICA_DB_PORT` (5432 por omision) y pgAdmin en el de `MOICA_PGADMIN_PORT` (5050 por omision). Para validar la configuracion sin levantar nada:

```bash
docker compose config
```

**Si el puerto 5432 ya esta ocupado** —es lo que pasa cuando la maquina tiene un PostgreSQL instalado en Windows— el contenedor no podra publicarse y Compose fallara. La solucion es cambiar una sola variable en `.env`:

```dotenv
MOICA_DB_PORT=5433
```

El puerto interno del contenedor sigue siendo 5432; solo cambia por cual se publica hacia la maquina. El backend usa esa misma variable para conectarse, asi que no hay que tocar codigo ni `docker-compose.yml`. Ya se comprobo funcionando de esta forma.

Para detener los servicios:

```bash
docker compose down
```

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

En Windows PowerShell se usa `.\mvnw.cmd` en lugar de `./mvnw`.

La API queda en `http://localhost:8080`. Flyway esta habilitado y aplica al arrancar las migraciones de `src/main/resources/db/migration`. Desde P2 ese directorio contiene `V10__crear_usuario_y_sesion.sql`, que crea las tablas `usuario` y `sesion`.

El arranque lo describe asi:

```text
Migrating schema "public" to version "10 - crear usuario y sesion"
Successfully applied 1 migration to schema "public", now at version v10
```

Hibernate arranca con `ddl-auto=validate`: si el esquema y las entidades dejaran de coincidir, la aplicacion no arrancaria. El esquema lo crea Flyway y solo Flyway.

## Healthcheck

Con el backend en ejecucion:

```bash
curl http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{"status":"UP"}
```

El estado es `UP` solo si la aplicacion arranco y su conexion a PostgreSQL funciona. La respuesta no incluye el detalle por componente: describiria la infraestructura a cualquiera que la consulte.

## Frontend

```bash
cd frontend
npm ci
npm run dev
```

La aplicacion queda en `http://localhost:5173`. El proxy de Vite reenvia `/api` y `/actuator` al backend, de modo que en desarrollo se conserva el mismo contrato de origen unico que habra en produccion.
