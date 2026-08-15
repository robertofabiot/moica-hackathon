# MOICA - Hackathon Nicaragua 2026

MOICA es una plataforma digital desarrollada por el equipo Nova Studios para la categoria Avanzado del Hackathon Nicaragua 2026. 

El proyecto resuelve la desconexion estructural entre personas que requieren contratar servicios (mantenimiento, reparacion, cuidado) y prestadores independientes informales. MOICA actua como un puente digital que sustituye la informalidad por perfiles verificados, portafolio visible y calificaciones emitidas despues de completar una solicitud dentro de la plataforma, reduciendo la asimetria de informacion.

### Verificacion de prestadores

El acceso es inmediato y la validacion posterior: cualquier cuenta puede crear y preparar su perfil de prestador desde el primer momento, pero para aparecer publicamente debe superar una verificacion documental. La verificacion se aplica al perfil de prestador, no a las cuentas que solo contratan, y tiene dos niveles progresivos:

*   **Verificado Basico:** una persona administradora reviso y aprobo la documentacion oficial de identidad de la persona responsable del perfil. Es el requisito para aparecer en la busqueda publica, activar servicios y recibir solicitudes.
*   **Profesional Verificado:** nivel opcional y posterior al basico. Una persona administradora reviso y aprobo documentacion profesional, tecnica o comercial que respalda la actividad declarada.

Toda la revision es **manual**: en el MVP no hay OCR, biometria, prueba de vida, consulta a bases externas ni proveedores de verificacion de terceros. Los documentos del expediente se almacenan como recursos privados y solo un administrador con segundo factor verificado puede abrirlos; el resto de las personas unicamente ve la insignia del nivel vigente. El detalle funcional completo esta en `Docs/Core/DefinicionProducto.md` (seccion 5.6).

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
| Node.js | 22 LTS o superior | `frontend/.nvmrc` y `frontend/package.json` |
| Vite | 8 | `frontend/package.json` |
| React | 19 | `frontend/package.json` |
| TypeScript | 6 (modo estricto) | `frontend/tsconfig.app.json` |

Dependencias principales del backend: Spring Web, Spring Data JPA, Spring Boot Actuator, Flyway, el controlador de PostgreSQL, Spring Boot Test y Testcontainers. Calidad: Spotless y SpotBugs.

Dependencias principales del frontend: React Router, TanStack React Query y el soporte PWA de Vite. Calidad: ESLint, Prettier, TypeScript, Vitest y React Testing Library.

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
*   **Node.js 22 LTS o superior.** Node 20 ya no recibe soporte y Vite 8 no lo admite.
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
| `MOICA_DB_PORT` | Puerto de PostgreSQL | Docker Compose y backend |
| `MOICA_PGADMIN_EMAIL` | Usuario de pgAdmin | Docker Compose |
| `MOICA_PGADMIN_CLAVE` | Contrasena de pgAdmin | Docker Compose |
| `MOICA_PGADMIN_PORT` | Puerto web de pgAdmin | Docker Compose |
| `MOICA_BACKEND_PORT` | Puerto de Spring Boot | Backend y proxy de Vite |

Los valores de `.env.example` son de desarrollo local. En produccion cada variable se define en el entorno del servidor; ningun secreto se versiona.

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

La API queda en `http://localhost:8080`. Flyway aplica las migraciones de `src/main/resources/db/migration` al arrancar; hoy ese directorio esta vacio a proposito y Flyway solo crea su tabla de historial.

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

Rutas disponibles hoy: `/` (pantalla base) y cualquier otra direccion, que muestra la pagina de ruta no encontrada.

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

En P1 todas las pruebas del backend son de integracion, porque lo que hay que demostrar es el arranque real contra PostgreSQL. Por eso `./mvnw test` todavia no ejecuta ninguna.

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

El repositorio contiene la base tecnica del monorepo: proyectos backend y frontend inicializados, controles de calidad, pruebas de arranque, integracion continua y entorno local configurable. Todavia no hay registro, inicio de sesion, perfiles, servicios, solicitudes, chat, calificaciones ni area administrativa: cada uno llega con su propio incremento del plan.

## Licencia

Este proyecto se distribuye bajo los terminos de la Licencia MIT. Para mas informacion, consulte el archivo `LICENSE`.
