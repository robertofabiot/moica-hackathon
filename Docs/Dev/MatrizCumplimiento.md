# Matriz de cumplimiento del entregable

Este documento relaciona cada criterio del entregable avanzado con la evidencia
real que lo respalda: el Pull Request donde se implementó, los commits, las
pruebas ejecutadas y el material de apoyo.

## Cómo se usa

- Cada PR actualiza **solo las filas que realmente avanzó**. La matriz no se
  rellena al final de la entrega.
- No se registra evidencia que no exista todavía. Una prueba que no se ejecutó
  no se anota como ejecutada.
- Los enlaces a PR usan la forma `#<número>`; los commits, su SHA corto.
- Las capturas y los resultados de CI se enlazan, no se describen de memoria.

## Estados

| Estado | Significado |
|---|---|
| Pendiente | Todavía no se ha comenzado. |
| En progreso | Hay evidencia parcial; el criterio aún no se cumple por completo. |
| Cumplido | Existe evidencia verificable y el criterio está completo. |

## Criterios del entregable

| # | Criterio | Estado | Incremento | PR | Commits | Pruebas | Evidencia |
|---|---|---|---|---|---|---|---|
| 1 | README técnico completo (requisitos, variables, estructura, scripts, comandos, endpoints) | En progreso | P1 → P11 | #3 | `daaae20` | — | `README.md` cubre requisitos, versiones, variables, estructura del monorepo, arranque de cada pieza, healthcheck, build de la PWA y comandos de validación. Endpoints de negocio y despliegue se completan en P11. |
| 2 | Modelo ER en 3FN y tres diagramas UML completos | Cumplido | P0 | #1, #2 | | | `Docs/Dev/DiagramaLogico.mmd`, `DiagramaConceptual.mmd`, `DiagramaClasesDominio.mmd`, `DiagramaCasosDeUso.tex`, `DiagramaActividades.tex`, `Moica - Diccionario de Datos.xlsx` |
| 3 | Interfaz navegable, validada y responsiva | Pendiente | P2 → P11 | | | | |
| 4 | Ramas, Conventional Commits, Pull Requests y trazabilidad | En progreso | P0 → P11 | #1, #2, #3 | `1192f84`, `363ccc9` | Check «Título y commits convencionales» en verde | `Docs/Core/GIT_WORKFLOW.md` define ramas, tipos y promoción a `main`; P1 agrega `.github/pull_request_template.md` y la validación automática de título y commits del PR. |
| 5 | Matriz de cumplimiento mantenida | En progreso | P1 → P11 | #3 | `1192f84` | — | Este documento, creado en P1 y actualizado por cada PR. |
| 6 | Validación de entradas y manejo uniforme de errores | Pendiente | P2 | | | | |
| 7 | Protección de rutas y datos (rol, propiedad, estado de cuenta) | Pendiente | P3 → P10B | | | | |
| 8 | Verificación documental de prestadores en dos niveles | Pendiente | P4V | | | | |
| 9 | Autenticación de dos factores (TOTP) | Pendiente | P3 | | | | |
| 10 | Expiración y revocación de sesión | Pendiente | P2 → P3 | | | | |
| 11 | Preparación para producción (contenedores, configuración por entorno, migraciones, healthcheck) | En progreso | P1 → P11 | #3 | `d429cf3`, `fc0cc70`, `f3305ce`, `b2a1d15` | `./mvnw verify` en CI | Configuración por variables de entorno, Flyway aplicando migraciones versionadas sobre PostgreSQL real y `GET /actuator/health` respondiendo `UP`. Imágenes de producción, despliegue y proveedor corresponden a P11. |

## Base técnica de P1

Controles que P1 dejó funcionando, con el resultado real de cada comando.
Evidencia de CI: [ejecución 31858690293](https://github.com/robertofabiot/moica-hackathon/actions/runs/31858690293)
del Pull Request #3, con los cuatro checks en verde.

| Control | Dónde vive | Cómo se comprueba | Estado | Evidencia |
|---|---|---|---|---|
| Finales de línea normalizados | `.gitattributes` | `git add --renormalize .` no produce cambios | Cumplido | Ejecutado antes de `8a3211c`: el índice ya estaba en LF y no se reescribió ningún archivo |
| Sin secretos versionados | `.env.example`, `.gitignore` | `docker-compose.yml` lee variables; `.env` está ignorado | Cumplido | `git ls-files` solo devuelve `.env.example`; revisión del diff sin credenciales |
| Formato del backend | `backend/pom.xml` (Spotless) | `./mvnw spotless:check` | Cumplido | Local y CI: «keeping 2 files clean, 0 needs changes» |
| Análisis estático del backend | `backend/pom.xml` (SpotBugs) | `./mvnw spotbugs:check` | Cumplido | Local y CI: «BugInstance size is 0» |
| Arranque del backend contra PostgreSQL real | `backend/src/test` (Testcontainers) | `./mvnw verify` | Cumplido en CI | 4 pruebas de `ArranqueConPostgresIT` en verde; no ejecutable en local por falta de Docker |
| Migraciones versionadas | `backend/src/main/resources/db/migration` | Flyway aplica las migraciones y registra su historial | Cumplido en CI | Registro de CI: «Successfully applied 1 migration to schema public, now at version v1» |
| Healthcheck | Spring Boot Actuator | `GET /actuator/health` responde `UP` | Cumplido en CI | Prueba `elHealthcheckReportaLaBaseDeDatosDisponible` |
| Formato del frontend | `frontend/.prettierrc.json` | `npm run format:check` | Cumplido | Local y CI: «All matched files use Prettier code style» |
| Lint del frontend | `frontend/eslint.config.js` | `npm run lint` | Cumplido | Local y CI en verde; comprobado que un `any` lo hace fallar |
| Tipos del frontend | `frontend/tsconfig.*.json` | `npm run typecheck` | Cumplido | Local y CI en verde |
| Pruebas del frontend | `frontend/src/**/*.test.tsx` | `npm run test` | Cumplido | 3 pruebas de navegación en verde, local y CI |
| Build y PWA del frontend | `frontend/vite.config.ts` | `npm run build` genera `manifest.webmanifest` y service worker | Cumplido | Build local y en CI; manifiesto enlazado e iconos de 192 y 512 servidos. La instalación en un navegador real queda pendiente de comprobar |
| Entorno local | `docker-compose.yml` | `docker compose config -q` | Cumplido en CI | Trabajo «Entorno local» a partir de `.env.example`; no ejecutable en local por falta de Docker |
| Integración continua | `.github/workflows/ci.yml` | Los checks del PR quedan en verde | Cumplido | Backend, Frontend y Entorno local en verde en el PR #3 |
| Convenciones de commits y títulos | `.github/workflows/convenciones.yml` | El check falla ante un mensaje no convencional | Cumplido | Check en verde sobre los commits del PR; comprobado que rechaza tipo en mayúscula, descripción en mayúscula, punto final y encabezado sin tipo |
