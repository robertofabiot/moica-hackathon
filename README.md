<div align="center">
  <img src="Docs/Design/logo/Logo.jpg" alt="Logo de MOICA" width="140">

# MOICA

**La confianza se construye entre todos.**

*Hackathon Nicaragua 2026 — Categoria Avanzado · Universidad Americana (UAM) · Equipo Nova Studios*

[![Licencia: MIT](https://img.shields.io/badge/licencia-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](backend/pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-6DB33F?logo=springboot&logoColor=white)](backend/pom.xml)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react&logoColor=white)](frontend/package.json)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)](docker-compose.yml)

</div>

## Tabla de contenidos

- [Que es MOICA](#que-es-moica)
- [Por que MOICA](#por-que-moica)
- [Diseño e identidad de marca](#diseño-e-identidad-de-marca)
- [Arquitectura y tecnologias](#arquitectura-y-tecnologias)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Guia para desarrolladores](#guia-para-desarrolladores)
- [Instalacion rapida](#instalacion-rapida)
- [API de acceso](#api-de-acceso)
- [Validaciones y pruebas](#validaciones-y-pruebas)
- [Construir la PWA](#construir-la-pwa)
- [Estado actual](#estado-actual)
- [Licencia](#licencia)

## Que es MOICA

MOICA conecta a personas que necesitan contratar un servicio (mantenimiento, reparacion, cuidado) con prestadores independientes que hoy operan de forma informal. En vez de depender del boca a boca o de un contacto en Facebook, cada prestador tiene un perfil verificado, un portafolio de trabajos reales y calificaciones que se acumulan despues de cada solicitud completada, reduciendo la asimetria de informacion entre ambas partes.

### Verificacion de prestadores

El acceso es inmediato y la verificacion, posterior: un perfil puede armarse desde el primer momento, pero solo aparece publicamente tras pasar una verificacion documental manual, en dos niveles (**Basico**, obligatorio, y **Profesional**, opcional). Sin OCR, biometria ni proveedores externos de verificacion en el MVP: toda revision la hace una persona administradora, y los documentos quedan como recursos privados.

## Por que MOICA

| Diferenciador | Para el cliente | Para el prestador |
|---|---|---|
| Verificacion en dos etapas | "Sabes quien es antes de que llegue" | "Entras rapido y despues subis de nivel" |
| Perfil-portafolio dinamico | "Mira fotos de trabajos que ya hizo" | "Tu historial se arma solo con cada trabajo" |
| Calificaciones reales | "Otros clientes ya lo calificaron" | "Tu buen trabajo queda escrito, no se olvida" |

Cero cobros iniciales: sin membresia ni pago por contacto, se cobra una comision solo cuando el prestador ya cobro.

## Diseño e identidad de marca

* **Marca:** [concepto e identidad](Docs/Design/IdentidadYConceptoDeMarca.md), [plan de branding](Docs/Marketing/PlanDeBranding.pdf), [moodboard](Docs/Design/MoodboardMoica.jpg), [logo](Docs/Design/logo/)
* **Negocio:** [propuesta de valor y canales](Docs/Marketing/PropuestaDeValorDiferenciada.pdf), [Business Model Canvas](Docs/Marketing/MetodologiaBussinessModelCanvas.png), [mapas de empatia del buyer persona](Docs/Marketing/BuyerPersonaYAspiraciones/)
* **Interfaz:** [mockups de escritorio](Docs/Design/UX/MockupEscritorio.png), [pantalla 404](Docs/Design/UX/Pantalla404.jpeg), [video de animacion](Docs/Design/UX/VideoAnimacion.mp4)

## Arquitectura y tecnologias

Monolito modular: **Java + Spring Boot** (API REST) · **React + TypeScript** (PWA mobile-first) · **PostgreSQL** · **Docker**.

**Versiones:** Java 21 (LTS) · Spring Boot 4.0.7 · PostgreSQL 15 · Node.js 22 LTS+ · React 19 · TypeScript 6 (modo estricto).

## Estructura del repositorio

```text
moica-hackathon/
├── .github/
│   ├── scripts/            Validador de Conventional Commits
│   ├── workflows/          CI: backend, frontend, docker compose y convenciones
│   └── pull_request_template.md
├── Docs/
│   ├── Core/               Definicion del producto, flujo Git, restricciones y post-MVP
│   ├── Design/             Identidad de marca, logotipos y mockups de interfaz (UX/)
│   ├── Dev/                Diagramas, diccionario de datos, estandares y matriz
│   └── Marketing/          Propuesta de valor, canales, business model canvas y buyer persona
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

## Guia para desarrolladores

Antes de tu primer commit, lee [`Docs/Core/GIT_WORKFLOW.md`](Docs/Core/GIT_WORKFLOW.md): ramas, Conventional Commits y Pull Requests (GitFlow simplificado, todo pasa por Code Review).

## Instalacion rapida

**Requisitos:** Docker y Docker Compose, JDK 21 o superior (Maven no hace falta, se usa el wrapper incluido), Node.js 22 LTS o superior, Git.

```bash
git clone https://github.com/robertofabiot/moica-hackathon
cd moica-hackathon
cp .env.example .env                 # Windows PowerShell: Copy-Item .env.example .env
docker compose up -d                 # PostgreSQL + pgAdmin

cd backend
./mvnw spring-boot:run               # API en http://localhost:8080
```

```bash
cd frontend
npm ci
npm run dev                          # App en http://localhost:5173
```

Variables de entorno principales (plantilla completa en `.env.example`):

| Variable | Para que sirve |
|---|---|
| `MOICA_DB_NOMBRE` / `MOICA_DB_USUARIO` / `MOICA_DB_CLAVE` | Credenciales de PostgreSQL |
| `MOICA_DB_HOST` / `MOICA_DB_PORT` | Conexion del backend a la base (cambia el puerto si el 5432 ya esta ocupado) |
| `MOICA_PGADMIN_EMAIL` / `MOICA_PGADMIN_CLAVE` / `MOICA_PGADMIN_PORT` | Acceso a pgAdmin |
| `MOICA_BACKEND_PORT` | Puerto de Spring Boot |
| `MOICA_JWT_SECRETO` | Clave de firma del JWT de sesion (minimo 32 bytes; genera una propia, no uses la de la plantilla fuera de tu maquina) |
| `MOICA_SESION_DURACION` | Duracion de una sesion, en ISO-8601 (`P7D` por omision) |
| `MOICA_COOKIE_SEGURA` | Marca `Secure` en la cookie de sesion; `false` en desarrollo, `true` en produccion |

Guia completa de configuracion (generacion del secreto JWT, conflicto de puertos, arranque de Flyway y healthcheck) en [`Docs/Dev/GuiaEntornoLocal.md`](Docs/Dev/GuiaEntornoLocal.md).

## API de acceso

Todos los endpoints de negocio viven bajo `/api`, que es lo que reenvia el proxy de Vite en desarrollo y lo que comparte origen con el frontend en produccion.

| Metodo y ruta | Que hace | Quien puede |
|---|---|---|
| `POST /api/usuarios` | Registra una cuenta | Cualquiera |
| `POST /api/auth/sesion` | Inicia sesion y entrega la cookie de sesion | Cualquiera |
| `GET /api/auth/sesion` | Describe la sesion en curso | Sesion vigente |
| `DELETE /api/auth/sesion` | Cierra la sesion y la revoca | Sesion vigente |
| `GET /actuator/health` | Estado de la aplicacion | Cualquiera |

La sesion se identifica con un JWT firmado que solo viaja en una cookie `HttpOnly`, respaldado por una fila revocable en base de datos; las operaciones mutables estan protegidas con CSRF de doble cookie. El flujo completo de autenticacion, el walkthrough de CSRF con `curl`, la politica de contraseña y la forma exacta de los errores estan en [`Docs/Dev/ContratoDeApi.md`](Docs/Dev/ContratoDeApi.md).

## Validaciones y pruebas

```bash
cd backend
./mvnw verify          # formato, compilacion, pruebas, integracion (Testcontainers) y analisis estatico — necesita Docker
```

```bash
cd frontend
npm run format:check && npm run lint && npm run typecheck && npm run test && npm run build
```

Cada Pull Request corre los mismos controles en GitHub Actions (backend, frontend, entorno local y convenciones de commits). Detalle de cada herramienta y las reglas de supresion en [`Docs/Dev/ESTANDARES_CODIGO.md`](Docs/Dev/ESTANDARES_CODIGO.md#controles-automáticos).

## Construir la PWA

```bash
cd frontend
npm run build      # genera manifest.webmanifest, sw.js y su registro en frontend/dist/
npm run preview    # sirve el build para probar la instalacion (Chrome/Chromium)
```

La identidad visual del manifiesto todavia usa colores neutros: la paleta definitiva esta pendiente.

## Estado actual

Ciclo de acceso completo: registro, inicio de sesion, sesion persistida con expiracion y revocacion, y cierre de sesion, con sus pantallas correspondientes.

Todavia no hay segundo factor TOTP, cambio de contraseña, area administrativa, perfiles de prestador, verificacion documental, servicios, solicitudes, chat ni calificaciones: cada uno llega con su propio incremento del plan.

## Licencia

Este proyecto se distribuye bajo los terminos de la Licencia MIT. Para mas informacion, consulte el archivo [`LICENSE`](LICENSE).
