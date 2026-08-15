# Directrices de Claude para Moica

Reglas operativas para trabajar en este repositorio. Son obligatorias y tienen
prioridad sobre cualquier costumbre general.

## Fuentes de verdad

Cuando dos documentos parezcan discrepar, se aplica este orden:

1. `Docs/Core/DefinicionProducto.md` — alcance y reglas funcionales.
2. `Docs/Dev/Moica - Diccionario de Datos.xlsx` y `Docs/Dev/DiagramaLogico.mmd` —
   estructura, tipos, nulabilidad y restricciones de PostgreSQL.
3. UML (`DiagramaCasosDeUso.tex`, `DiagramaActividades.tex`,
   `DiagramaClasesDominio.mmd`) — actores, recorridos y responsabilidades.
4. `Docs/Core/prompt.md` — restricciones técnicas del MVP.
5. `Docs/Core/post-mvp.md` — exclusiones expresas.
6. Entregables de preclasificación.
7. `Docs/Marketing/` y `Docs/Design/` — lenguaje, identidad y experiencia.

`Docs/Core/DocumentoBase.md` es histórico: no se programa a partir de él cuando
contradiga la definición consolidada.

Reglas de código: `Docs/Dev/ESTANDARES_CODIGO.md`. Reglas de Git:
`Docs/Core/GIT_WORKFLOW.md`. Evidencia: `Docs/Dev/MatrizCumplimiento.md`.

## Arquitectura aprobada

Está cerrada. No se sustituye ni se reabre sin una contradicción documental
concreta.

- Monolito modular. Prohibido microservicios y arquitectura hexagonal.
- Backend: Java 21 + Spring Boot, capas clásicas dentro de cada capacidad.
- Frontend: React + TypeScript, PWA mobile-first.
- PostgreSQL con migraciones versionadas de Flyway.
- CSS Modules. Prohibido Tailwind, MUI y Bootstrap.
- TanStack React Query para estado remoto; Zustand solo cuando exista estado
  global real. Prohibido Redux.
- Chat por short polling. Prohibido WebSockets en el MVP.
- Un solo origen entre frontend y API en producción; en desarrollo, proxy de
  Vite hacia Spring Boot.

## Flujo Git obligatorio

- Toda rama nace de `develop` actualizado: `feature/<descripcion-corta>`.
- Prohibido hacer commits directos, merge local o push directo a `develop` y a
  `main`. Todo entra por Pull Request con revisión de otra persona.
- Prohibido `reset --hard`, force-push, rebase de historial publicado y
  cualquier reescritura de lo ya publicado.
- Commits atómicos con Conventional Commits y los tipos de `GIT_WORKFLOW.md`:
  `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `ci`, `build`, `style`,
  `perf`, `revert`. El scope es opcional.
- No se usa `feat` para configuración que no introduce comportamiento.
- No se cambia la identidad ni las credenciales de Git o GitHub configuradas.

## Inspeccionar antes de modificar

- Leer el estado real del repositorio y los archivos afectados antes de editar.
  No asumir contenido por el nombre de un archivo.
- Comprobar la rama, el `git status` y que el incremento anterior está integrado.
- Si un archivo ya existe con reglas útiles, se actualiza al mínimo necesario;
  no se reemplaza a ciegas.

## Alcance

- Se implementa **un incremento por vez**, el que indique el prompt. Nada más.
- Prohibido ampliar el MVP en silencio: ninguna funcionalidad, entidad,
  endpoint, pantalla ni dependencia que el incremento no pida.
- Prohibido incorporar funciones de `Docs/Core/post-mvp.md`.
- Prohibido modificar archivos ajenos a la tarea para «aprovechar» el PR. Si se
  detecta un problema fuera de alcance, se menciona en el informe y no se toca.
- Prohibido crear carpetas, clases, interfaces o componentes vacíos anticipando
  módulos futuros.

## Cómo se resuelve

- Entre dos soluciones equivalentes gana la más simple, explícita y comprobable.
- Nada de abstracciones prematuras ni patrones sin un problema actual.
- Cada decisión reversible y local se toma y se justifica en una línea; no se
  abre una discusión por ella.

## Terminado significa comprobado

- Antes de afirmar que algo funciona hay que ejecutar sus comandos:
  `./mvnw verify` en el backend; `npm run lint`, `npm run typecheck`,
  `npm run test` y `npm run build` en el frontend.
- Prohibido declarar que una prueba pasó sin haberla ejecutado. Si el entorno lo
  impide, se describe la limitación exacta y cómo reproducir la comprobación.
- Cada PR actualiza la fila que corresponda de `Docs/Dev/MatrizCumplimiento.md`
  con evidencia real.

## Secretos y datos sensibles

- Prohibido escribir en el repositorio contraseñas, tokens, claves de API,
  secretos TOTP, cadenas de conexión reales o archivos `.env`.
- Las credenciales van por variables de entorno y se documentan **sin valor** en
  `.env.example`.
- Prohibido registrar o devolver hashes, secretos, tokens de sesión, documentos
  de identidad, claves de almacenamiento privado y trazas internas.
- Antes de subir cambios se revisa el diff buscando secretos.

## Cuándo detenerse y preguntar

Se consulta al equipo, sin implementar, cuando una ambigüedad obligue a cambiar:

- el alcance del MVP,
- el modelo de datos aprobado,
- una regla funcional,
- una decisión de seguridad,
- la arquitectura consolidada,
- el historial publicado o cambios ajenos del repositorio.

En cualquier otro caso se elige la opción más simple compatible con el plan y se
deja la justificación en el informe.
