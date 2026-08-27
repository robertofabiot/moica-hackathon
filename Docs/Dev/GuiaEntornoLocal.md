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
| `MOICA_TOTP_CLAVE_CIFRADO` | Clave con la que se cifra el secreto TOTP de cada cuenta (Base64 de 16, 24 o 32 bytes) | Backend |
| `MOICA_ADMIN_CORREO` | Correo de una cuenta ya registrada que recibe el rol administrativo al arrancar; vacia por omision | Backend |
| `MOICA_R2_ID_CUENTA` | Identificador de la cuenta de Cloudflare; forma parte del endpoint S3 de R2 | Backend |
| `MOICA_R2_ACCESS_KEY_ID` | Identificador del token de API de R2 | Backend |
| `MOICA_R2_SECRET_ACCESS_KEY` | Secreto del token de API de R2; nunca se versiona | Backend |
| `MOICA_R2_BUCKET_PUBLICO` | Nombre del bucket publico ya aprovisionado | Backend |
| `MOICA_R2_URL_PUBLICA_BASE` | Origen HTTPS desde el que se leen las imagenes, sin barra final | Backend |
| `MOICA_IMAGEN_TAMANO_MAXIMO` | Maximo por imagen (por omision `5MB`) | Backend |
| `MOICA_R2_PRIVADO_ID_CUENTA` | Identificador de la cuenta de Cloudflare del bucket privado | Backend |
| `MOICA_R2_PRIVADO_ACCESS_KEY_ID` | Identificador del token de API del bucket privado | Backend |
| `MOICA_R2_PRIVADO_SECRET_ACCESS_KEY` | Secreto de ese token; nunca se versiona | Backend |
| `MOICA_R2_BUCKET_PRIVADO` | Nombre del bucket privado de expedientes ya aprovisionado | Backend |
| `MOICA_DOCUMENTO_TAMANO_MAXIMO` | Maximo por documento del expediente (por omision `5MB`; no admite mas) | Backend |
| `MOICA_DOCUMENTO_URL_TEMPORAL_DURACION` | Duracion del enlace temporal con el que se abre un documento (por omision `PT5M`; no admite mas de una hora) | Backend |

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

### La clave de cifrado del segundo factor

El secreto TOTP no puede guardarse como hash: el servidor necesita regenerar los codigos. Se guarda
cifrado con AES-GCM, y la clave llega en `MOICA_TOTP_CLAVE_CIFRADO`, en Base64 y de 16, 24 o 32
bytes. Con cualquier otra cosa —ausente, mal codificada o de otra longitud— la aplicacion **no
arranca**: es preferible eso a guardar un secreto sin cifrar.

El valor de la plantilla es publico, igual que el del JWT, y solo sirve para trabajar en la maquina
propia. Para generar uno de verdad:

```bash
openssl rand -base64 32
```

```powershell
$b = [byte[]]::new(32); [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Cambiar esta clave deja ilegibles los secretos ya guardados: quien tuviera el segundo factor activo
tendria que volver a configurarlo.

### Almacenamiento de imagenes

Las imagenes de perfil y de portafolio se guardan en un bucket **publico** de
Cloudflare R2. Las cinco variables `MOICA_R2_*` van juntas: o se definen todas o
no se define ninguna.

- **Sin ellas** el backend arranca con normalidad y todo lo demas funciona; solo
  subir o borrar una imagen responde `503 ALMACENAMIENTO_NO_DISPONIBLE`. Es
  suficiente para trabajar en cualquier otra parte de Moica.
- **A medias** el arranque se detiene con un mensaje que dice cuales faltan, sin
  revelar ningun valor: una configuracion incompleta solo puede ser un error de
  despliegue.

Como crear el bucket, que permisos darle al token y como comprobar una carga
real contra R2: [Almacenamiento.md](Almacenamiento.md). No hace falta ningun
servicio local ni un emulador: las pruebas automaticas usan un doble en memoria.

### Almacenamiento de expedientes

Los documentos de verificacion se guardan en un bucket **privado** de Cloudflare
R2, **distinto del publico y con un token distinto**. Las cuatro variables
`MOICA_R2_PRIVADO_*` van juntas igual que las otras cinco: o se definen todas o
no se define ninguna.

- **Sin ellas** el backend arranca con normalidad; enviar un expediente o abrir
  un documento responde `503 ALMACENAMIENTO_NO_DISPONIBLE` y **no crea ninguna
  fila**. Es suficiente para trabajar en cualquier otra parte de Moica, incluida
  la revision de expedientes ya existentes.
- **A medias** el arranque se detiene con un mensaje que dice cuales faltan, sin
  revelar ningun valor.
- **No reutilices las credenciales del bucket publico.** El sentido de tener dos
  superficies es que un fallo en una no pueda exponer la otra.
- El bucket privado **no debe tener** subdominio `r2.dev` ni dominio propio: no
  existe ninguna direccion desde la que se lea sin permiso. Por eso no hay una
  variable de URL publica para el.

`MOICA_DOCUMENTO_TAMANO_MAXIMO` solo puede bajarse de `5MB`: ese es el tope que
impone PostgreSQL con `ck_documento_verificacion_tamano`, y un valor mayor
detiene el arranque. `MOICA_DOCUMENTO_URL_TEMPORAL_DURACION` tampoco admite mas
de una hora: un acceso «temporal» mas largo deja de serlo.

Como aprovisionar el bucket privado y las diez comprobaciones que exige:
[Almacenamiento.md](Almacenamiento.md).

### Rol administrativo

Moica no tiene registro publico de administradores, ni endpoint de promocion, ni contrasena fija.
La unica via es `MOICA_ADMIN_CORREO`, que se aplica al arrancar:

1. Registra la cuenta desde la aplicacion, como cualquier otra.
2. Escribe su correo en `MOICA_ADMIN_CORREO` y reinicia el backend.
3. Entra con esa cuenta, activa su segundo factor en `/seguridad` y ya puedes abrir `/admin`.

Es idempotente: se ejecuta en cada arranque y, si la cuenta ya tiene el rol, no cambia nada. Si la
variable esta vacia no se promueve a nadie, y si apunta a una cuenta que todavia no existe el
arranque continua y deja este aviso:

```text
MOICA_ADMIN_CORREO apunta a una cuenta que todavia no existe. Registrala desde la aplicacion y
vuelve a arrancar para asignarle el rol administrativo.
```

El aviso no incluye el correo: es un dato personal y el arranque suele quedar registrado.

Recuerda que tener el rol no basta para entrar en `/admin`: hace falta ademas que **esa sesion**
haya verificado el segundo factor.

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

La API queda en `http://localhost:8080`. Flyway esta habilitado y aplica al arrancar las migraciones de `src/main/resources/db/migration`: `V10__crear_usuario_y_sesion.sql` crea las tablas `usuario` y `sesion`; `V11__crear_administrador_y_segundo_factor.sql` agrega `administrador`, `segundo_factor_usuario` y el indice que permite revocar de una vez todas las sesiones de una cuenta; y el rango `V20`–`V23` agrega el territorio (`departamento`, `municipio`), el perfil de prestador con sus contactos y el portafolio, y carga Managua con sus nueve municipios.

El arranque lo describe asi:

```text
Migrating schema "public" to version "10 - crear usuario y sesion"
Migrating schema "public" to version "11 - crear administrador y segundo factor"
Migrating schema "public" to version "20 - crear departamento y municipio"
Migrating schema "public" to version "21 - crear perfil prestador y contactos"
Migrating schema "public" to version "22 - crear trabajos de portafolio"
Migrating schema "public" to version "23 - cargar managua y sus municipios"
Successfully applied 6 migrations to schema "public", now at version v23
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
