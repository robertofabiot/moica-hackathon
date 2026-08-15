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
| 1 | README técnico completo (requisitos, variables, estructura, scripts, comandos, endpoints) | Pendiente | P1 → P11 | | | | |
| 2 | Modelo ER en 3FN y tres diagramas UML completos | Cumplido | P0 | #1, #2 | | | `Docs/Dev/DiagramaLogico.mmd`, `DiagramaConceptual.mmd`, `DiagramaClasesDominio.mmd`, `DiagramaCasosDeUso.tex`, `DiagramaActividades.tex`, `Moica - Diccionario de Datos.xlsx` |
| 3 | Interfaz navegable, validada y responsiva | Pendiente | P2 → P11 | | | | |
| 4 | Ramas, Conventional Commits, Pull Requests y trazabilidad | En progreso | P0 → P11 | #1, #2 | | | `Docs/Core/GIT_WORKFLOW.md` define ramas, tipos de commit y promoción a `main`; P1 agrega plantilla de PR y validación automática. |
| 5 | Matriz de cumplimiento mantenida | En progreso | P1 → P11 | | | | Este documento, creado en P1 y actualizado por cada PR. |
| 6 | Validación de entradas y manejo uniforme de errores | Pendiente | P2 | | | | |
| 7 | Protección de rutas y datos (rol, propiedad, estado de cuenta) | Pendiente | P3 → P10B | | | | |
| 8 | Verificación documental de prestadores en dos niveles | Pendiente | P4V | | | | |
| 9 | Autenticación de dos factores (TOTP) | Pendiente | P3 | | | | |
| 10 | Expiración y revocación de sesión | Pendiente | P2 → P3 | | | | |
| 11 | Preparación para producción (contenedores, configuración por entorno, migraciones, healthcheck) | Pendiente | P1 → P11 | | | | |

## Base técnica de P1

Estas filas registran los controles que P1 debe dejar funcionando. El estado y la
evidencia se completan al cerrar el PR, con el resultado real de cada comando.

| Control | Dónde vive | Cómo se comprueba | Estado | Evidencia |
|---|---|---|---|---|
| Finales de línea normalizados | `.gitattributes` | `git add --renormalize .` no produce cambios | Pendiente | |
| Sin secretos versionados | `.env.example`, `.gitignore` | `docker-compose.yml` lee variables; `.env` está ignorado | Pendiente | |
| Formato del backend | `backend/pom.xml` (Spotless) | `./mvnw spotless:check` | Pendiente | |
| Análisis estático del backend | `backend/pom.xml` (SpotBugs) | `./mvnw spotbugs:check` | Pendiente | |
| Arranque del backend contra PostgreSQL real | `backend/src/test` (Testcontainers) | `./mvnw verify` | Pendiente | |
| Migraciones versionadas | `backend/src/main/resources/db/migration` | Flyway crea `flyway_schema_history` al arrancar | Pendiente | |
| Healthcheck | Spring Boot Actuator | `GET /actuator/health` responde `UP` | Pendiente | |
| Formato del frontend | `frontend/.prettierrc.json` | `npm run format:check` | Pendiente | |
| Lint del frontend | `frontend/eslint.config.js` | `npm run lint` | Pendiente | |
| Tipos del frontend | `frontend/tsconfig.*.json` | `npm run typecheck` | Pendiente | |
| Pruebas del frontend | `frontend/src/**/*.test.tsx` | `npm run test` | Pendiente | |
| Build y PWA del frontend | `frontend/vite.config.ts` | `npm run build` genera `manifest.webmanifest` y service worker | Pendiente | |
| Integración continua | `.github/workflows/` | Los checks del PR quedan en verde | Pendiente | |
| Convenciones de commits y títulos | `.github/workflows/convenciones.yml` | El check falla ante un mensaje no convencional | Pendiente | |
