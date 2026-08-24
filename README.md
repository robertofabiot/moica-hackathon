<div align="center">
  <img src="Docs/Design/logo/Logo.jpg" alt="Logo de MOICA" width="140">

# MOICA

**La confianza se construye entre todos.**

*Hackathon Nicaragua 2026 — Categoria Avanzado · Equipo Nova Studios*

[![CI](https://github.com/robertofabiot/moica-hackathon/actions/workflows/ci.yml/badge.svg)](https://github.com/robertofabiot/moica-hackathon/actions/workflows/ci.yml)
[![Convenciones](https://github.com/robertofabiot/moica-hackathon/actions/workflows/convenciones.yml/badge.svg)](https://github.com/robertofabiot/moica-hackathon/actions/workflows/convenciones.yml)
[![Licencia: MIT](https://img.shields.io/badge/licencia-MIT-blue.svg)](LICENSE)

</div>

## Tabla de contenidos

- [Sobre el proyecto](#sobre-el-proyecto)
- [Diseño e identidad de marca](#diseño-e-identidad-de-marca)
- [Arquitectura y Tecnologias](#arquitectura-y-tecnologias)
- [Estructura del Repositorio (Monorepo)](#estructura-del-repositorio-monorepo)
- [Guia para Desarrolladores](#guia-para-desarrolladores)
- [Entorno de Desarrollo Local](#entorno-de-desarrollo-local)
- [API de acceso](#api-de-acceso)
- [Validaciones y pruebas](#validaciones-y-pruebas)
- [Construir la PWA](#construir-la-pwa)
- [Estado actual](#estado-actual)
- [Licencia](#licencia)

## Sobre el proyecto

MOICA es una plataforma digital desarrollada por el equipo Nova Studios para la categoria Avanzado del Hackathon Nicaragua 2026. 

El proyecto resuelve la desconexion estructural entre personas que requieren contratar servicios (mantenimiento, reparacion, cuidado) y prestadores independientes informales. MOICA actua como un puente digital que sustituye la informalidad por perfiles verificados, portafolio visible y calificaciones emitidas despues de completar una solicitud dentro de la plataforma, reduciendo la asimetria de informacion.

### Verificacion de prestadores

El acceso es inmediato y la validacion posterior: cualquier cuenta puede crear y preparar su perfil de prestador desde el primer momento, pero para aparecer publicamente debe superar una verificacion documental. La verificacion se aplica al perfil de prestador, no a las cuentas que solo contratan, y tiene dos niveles progresivos:

*   **Verificado Basico:** una persona administradora reviso y aprobo la documentacion oficial de identidad de la persona responsable del perfil. Es el requisito para aparecer en la busqueda publica, activar servicios y recibir solicitudes.
*   **Profesional Verificado:** nivel opcional y posterior al basico. Una persona administradora reviso y aprobo documentacion profesional, tecnica o comercial que respalda la actividad declarada.

Toda la revision es **manual**: en el MVP no hay OCR, biometria, prueba de vida, consulta a bases externas ni proveedores de verificacion de terceros. Los documentos del expediente se almacenan como recursos privados y solo un administrador con segundo factor verificado puede abrirlos; el resto de las personas unicamente ve la insignia del nivel vigente. El detalle funcional completo esta en `Docs/Core/DefinicionProducto.md` (seccion 5.6).

## Diseño e identidad de marca

La identidad visual y el modelo de negocio de MOICA viven en `Docs/Design/` y `Docs/Marketing/`.

**Marca**

* [Concepto e identidad de marca](Docs/Design/IdentidadYConceptoDeMarca.md) — naming, valores, direccion visual y paleta de colores (todavia tentativa)
* [Plan de branding](Docs/Marketing/PlanDeBranding.pdf)
* [Moodboard](Docs/Design/MoodboardMoica.jpg)
* [Logo](Docs/Design/logo/)

**Negocio**

* [Propuesta de valor diferenciada y estrategia de canales](Docs/Marketing/PropuestaDeValorDiferenciada.pdf)
* [Business Model Canvas](Docs/Marketing/MetodologiaBussinessModelCanvas.png)
* [Mapas de empatia del buyer persona](Docs/Marketing/BuyerPersonaYAspiraciones/)

**Interfaz**

* [Mockups de escritorio](Docs/Design/UX/MockupEscritorio.png)
* [Pantalla 404](Docs/Design/UX/Pantalla404.jpeg)
* [Video de animacion de marca](Docs/Design/UX/VideoAnimacion.mp4)

## Arquitectura y Tecnologias

El proyecto utiliza una arquitectura de Monolito Modular dividida en las siguientes tecnologias:

*   **Frontend (Cliente/Prestador):** React con TypeScript (Mobile-First PWA).
*   **Backend (API REST):** Java con Spring Boot.
*   **Base de Datos:** PostgreSQL.
*   **Infraestructura:** Contenedores Docker.

### Versiones utilizadas

| Pieza | Version | Donde se fija |
|---|---|---|
| Java | 21 (LTS) | `backend/pom.xml` (`java.version`) |
| Maven | 3.9.16 mediante Maven Wrapper | `backend/.mvn/wrapper/maven-wrapper.properties` |
| Spring Boot | 4.0.7 | `backend/pom.xml` |
| PostgreSQL | 15 (alpine) | `docker-compose.yml` y Testcontainers |
| Flyway | Gestionado por Spring Boot | `backend/pom.xml` |
| Spring Security | 7 (gestionado por Spring Boot) | `backend/pom.xml` |
| JJWT | 0.13.0 | `backend/pom.xml` (`jjwt.version`) |
| Node.js | 22 LTS o superior | `frontend/.nvmrc` y `frontend/package.json` |
| Vite | 8 | `frontend/package.json` |
| React | 19 | `frontend/package.json` |
| TypeScript | 6 (modo estricto) | `frontend/tsconfig.app.json` |

Dependencias principales del backend: Spring Web, Spring Data JPA, Spring Security, Bean Validation, Spring Boot Actuator, Flyway, JJWT, el controlador de PostgreSQL, Spring Boot Test y Testcontainers. Calidad: Spotless y SpotBugs.

Dependencias principales del frontend: React Router, TanStack React Query, React Hook Form, Zod (con `@hookform/resolvers`) y el soporte PWA de Vite. Calidad: ESLint, Prettier, TypeScript, Vitest y React Testing Library.

Zustand todavia no esta instalado: se incorporara cuando exista estado global real, tal como indica el plan.

## Estructura del Repositorio (Monorepo)

```text
moica-hackathon/
├── .github/
│   ├── scripts/            Validador de Conventional Commits
│   ├── workflows/          CI: backend, frontend, docker compose y convenciones
│   └── pull_request_template.md
├── Docs/
│   ├── Core/               Definicion del producto, flujo Git, restricciones y post-MVP
│   ├── Design/             Identidad de marca y logotipos
│   ├── Dev/                Diagramas, diccionario de datos, estandares y matriz
│   └── Marketing/          Propuesta de valor
├── backend/                API REST (Spring Boot)
│   ├── src/main/java/com/moica/          Codigo por capacidades
│   └── src/main/resources/db/migration/  Migraciones de Flyway
├── frontend/               PWA (React + TypeScript)
│   ├── public/             Iconos de la aplicacion instalable
│   └── src/                Codigo por capacidades, paginas y estilos
├── .env.example            Plantilla de variables de entorno
├── docker-compose.yml      PostgreSQL y pgAdmin para desarrollo
└── README.md
```

El backend se organiza por capacidades (`usuario`, `auth`, `prestador`, ...), y cada capacidad contiene sus capas clasicas. Los paquetes se crean cuando el incremento que los necesita los implementa; no hay carpetas vacias.

## Guia para Desarrolladores

Para mantener la calidad y el orden del codigo base durante el Hackathon, es obligatorio revisar los siguientes documentos antes de realizar el primer commit:

1.  **Reglas de Git y Pull Requests:** Revisar `Docs/Core/GIT_WORKFLOW.md`. Se utiliza una version simplificada de GitFlow. Todo codigo debe pasar por Code Review.
2.  **Estandares de codigo:** Revisar `Docs/Dev/ESTANDARES_CODIGO.md`. Traduce Clean Code y SOLID a reglas verificables y describe los controles automaticos.
3.  **Reglas de Arquitectura para el MVP:** Revisar `Docs/Core/prompt.md`. Contiene las convenciones estrictas sobre librerias permitidas (ej. Zustand, React Query) y limites del alcance.
4.  **Roadmap Futuro:** Revisar `Docs/Core/post-mvp.md` para ideas que se han excluido del MVP (ej. WebSockets) con el fin de priorizar la entrega.
5.  **Evidencia:** Cada PR actualiza la fila que le corresponde en `Docs/Dev/MatrizCumplimiento.md`.

## Entorno de Desarrollo Local

### Requisitos previos

*   **Docker y Docker Compose.** Levantan PostgreSQL y pgAdmin, y ademas Testcontainers los necesita para las pruebas de integracion del backend.
*   **JDK 21 o superior.** Maven no hace falta: el repositorio incluye el Maven Wrapper.
*   **Node.js 22 LTS o superior.** Vite 8 todavia admite Node 20.19 o posterior, pero la linea 20 termino su soporte: el proyecto usa Node 22 para trabajar sobre una linea mantenida y coincidir con la version que ejecuta CI.
*   **Git.**

### 1. Configurar las variables de entorno

Copia la plantilla y ajusta los valores. El archivo `.env` esta ignorado por Git y nunca debe subirse.

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

**`MOICA_JWT_SECRETO` merece una advertencia aparte.** El valor que trae la plantilla es publico —esta en el repositorio— y solo sirve para trabajar en la maquina propia. Cualquier entorno compartido necesita uno aleatorio y distinto:

```bash
openssl rand -base64 48
```

```powershell
$b = [byte[]]::new(48); [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
```

Si el secreto tiene menos de 32 bytes, el backend no arranca: HMAC-SHA256 no admite una clave mas corta.

El backend lee estas variables del entorno del sistema y, si no estan definidas, importa el mismo archivo `.env` de la raiz. Las variables de entorno reales tienen prioridad, de modo que CI y produccion no dependen del archivo.

### 2. Levantar PostgreSQL y pgAdmin

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

### 3. Ejecutar el backend

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

### 4. Comprobar el healthcheck

Con el backend en ejecucion:

```bash
curl http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{"status":"UP"}
```

El estado es `UP` solo si la aplicacion arranco y su conexion a PostgreSQL funciona. La respuesta no incluye el detalle por componente: describiria la infraestructura a cualquiera que la consulte.

### 5. Ejecutar el frontend

```bash
cd frontend
npm ci
npm run dev
```

La aplicacion queda en `http://localhost:5173`. El proxy de Vite reenvia `/api` y `/actuator` al backend, de modo que en desarrollo se conserva el mismo contrato de origen unico que habra en produccion.

Rutas disponibles hoy:

| Ruta | Pantalla |
|---|---|
| `/` | Inicio. Muestra si hay sesion iniciada y ofrece entrar, registrarse o cerrar sesion |
| `/registro` | Creacion de cuenta |
| `/iniciar-sesion` | Inicio de sesion. Admite `?motivo=sesion-vencida` y `?motivo=cuenta-creada` |
| cualquier otra | Pagina de ruta no encontrada |

## API de acceso

Todos los endpoints de negocio viven bajo `/api`, que es lo que reenvia el proxy de Vite en desarrollo y lo que comparte origen con el frontend en produccion.

| Metodo y ruta | Que hace | Quien puede |
|---|---|---|
| `POST /api/usuarios` | Registra una cuenta | Cualquiera |
| `POST /api/auth/sesion` | Inicia sesion y entrega la cookie de sesion | Cualquiera |
| `GET /api/auth/sesion` | Describe la sesion en curso | Sesion vigente |
| `DELETE /api/auth/sesion` | Cierra la sesion y la revoca | Sesion vigente |
| `GET /actuator/health` | Estado de la aplicacion | Cualquiera |

### Como se autentica una peticion

1. Al iniciar sesion, Moica crea una fila en `sesion` con un identificador aleatorio y una fecha de expiracion (siete dias por omision, configurable con `MOICA_SESION_DURACION`).
2. Ese identificador viaja como `jti` dentro de un JWT firmado, y el JWT viaja en la cookie `moica_sesion`, que es `HttpOnly`, `SameSite=Lax` y `Secure` en produccion. **El token no se guarda en `localStorage` ni en `sessionStorage`.**
3. En cada peticion autenticada, el backend lee el `jti` y comprueba la fila: debe existir, no haber expirado y no haber sido revocada. Si falla cualquiera de las tres, la respuesta es 401.

Por eso cerrar sesion tiene efecto inmediato aunque el JWT conserve una expiracion futura: la fuente de verdad es la fila, no el token.

### Proteccion CSRF

La proteccion CSRF esta activa para todas las operaciones mutables. El backend emite la cookie `XSRF-TOKEN` —legible por JavaScript a proposito— y espera recibirla de vuelta en la cabecera `X-XSRF-TOKEN`. El frontend lo hace solo; para probar con `curl` hay que repetir el trámite:

```bash
# 1. Cualquier respuesta trae la cookie con el token
curl -s -c galletas.txt -o /dev/null http://localhost:8080/api/auth/sesion

# 2. Se devuelve en la cabecera de la operacion mutable
TOKEN=$(grep XSRF-TOKEN galletas.txt | awk '{print $7}')
curl -s -b galletas.txt -X POST http://localhost:8080/api/usuarios   -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $TOKEN"   -d '{"nombreCompleto":"Persona de prueba","correoElectronico":"persona@moica.test","clave":"Moica2026$segura"}'
```

Sin el paso 2 la respuesta es `403`.

### Politica de contraseña

De 8 a 72 caracteres, con al menos una mayuscula, una minuscula, un numero y un simbolo. No hace falta alternar tipos en cada caracter. El maximo lo impone BCrypt, que solo tiene en cuenta los primeros 72 bytes: una contraseña con acentos o emojis puede alcanzar ese limite antes de los 72 caracteres, y en ese caso se rechaza con una explicacion, no con un error del servidor.

La recuperacion de contraseña queda fuera del MVP.

### Forma de los errores

Todos los errores comparten cuerpo. El detalle por campo solo aparece cuando el fallo es de validacion:

```json
{
  "instante": "2026-08-21T11:18:40.222525-06:00",
  "estado": 400,
  "codigo": "VALIDACION",
  "mensaje": "Revisa los datos enviados.",
  "ruta": "/api/usuarios",
  "errores": [{ "campo": "correoElectronico", "mensaje": "Escribe un correo electronico valido." }]
}
```

Codigos que devuelve hoy la API: `VALIDACION`, `SOLICITUD_INVALIDA`, `CORREO_YA_REGISTRADO`, `CREDENCIALES_INVALIDAS`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, `RECURSO_NO_ENCONTRADO`, `METODO_NO_PERMITIDO`, `TIPO_DE_CONTENIDO_NO_ADMITIDO` y `ERROR_INTERNO`. Ninguna respuesta de error lleva trazas, SQL ni valores internos.

## Validaciones y pruebas

### Backend

```bash
cd backend
./mvnw verify
```

`verify` encadena, en este orden: `spotless:check` (formato), compilacion, pruebas de Surefire, empaquetado, pruebas de integracion de Failsafe contra PostgreSQL real mediante Testcontainers y `spotbugs:check` (analisis estatico). **Necesita Docker en ejecucion.**

Ordenes individuales:

```bash
./mvnw spotless:check     # comprueba el formato
./mvnw spotless:apply     # corrige el formato
./mvnw spotbugs:check     # analisis estatico
./mvnw test               # pruebas que no necesitan infraestructura
./mvnw verify -DskipITs   # todo menos las pruebas que exigen Docker
```

`./mvnw test` ejecuta las pruebas que no necesitan infraestructura —politica de contraseña, vigencia de una sesion, emision y validacion del JWT, configuracion de seguridad— y `./mvnw verify` añade las de integracion, que trabajan sobre PostgreSQL real y recorren la API por HTTP.

### Frontend

```bash
cd frontend
npm ci
npm run format:check
npm run lint
npm run typecheck
npm run test
npm run build
```

`npm run format` y `npm run lint:fix` corrigen automaticamente lo que se pueda. `npm run test:watch` deja Vitest en modo interactivo para el trabajo diario.

### Integracion continua

Cada Pull Request ejecuta los mismos controles en GitHub Actions:

*   **Backend:** `./mvnw verify` con Java 21.
*   **Frontend:** `npm ci`, formato, lint, tipos, pruebas y build con Node 22.
*   **Entorno local:** validacion de `docker-compose.yml` con los valores de `.env.example`.
*   **Convenciones:** el titulo del PR y los commits que aporta deben seguir Conventional Commits.

Un incumplimiento deja el check en rojo. Ningun paso oculta fallos ni depende de secretos.

## Construir la PWA

```bash
cd frontend
npm run build
npm run preview
```

`npm run build` genera en `frontend/dist/` el `manifest.webmanifest`, el service worker (`sw.js`) y su registro. La instalacion se comprueba con `npm run preview`, abriendo la direccion que indica la consola y usando la opcion de instalar de un navegador basado en Chromium; en el modo `dev` el service worker no esta activo.

La identidad visual todavia no esta cerrada: el manifiesto usa colores neutros y el logotipo ya entregado. La paleta y la tipografia definitivas se aplicaran cuando el equipo de diseno las apruebe.

## Estado actual

El repositorio contiene la base tecnica del monorepo y el ciclo de acceso completo: registro, inicio de sesion, sesion persistida con expiracion y revocacion, cierre de sesion y las pantallas correspondientes.

Todavia no hay segundo factor TOTP, cambio de contraseña, area administrativa, perfiles de prestador, verificacion documental, servicios, solicitudes, chat ni calificaciones: cada uno llega con su propio incremento del plan.

## Licencia

Este proyecto se distribuye bajo los terminos de la Licencia MIT. Para mas informacion, consulte el archivo `LICENSE`.
