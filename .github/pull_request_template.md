<!--
Plantilla de Pull Request de Moica.

El título del PR sigue Conventional Commits, igual que los commits
(`Docs/Core/GIT_WORKFLOW.md`, sección 3). El scope es opcional.
Ejemplos: `feat(auth): implementar inicio de sesión` o `docs: corregir enlaces`.

Borra las secciones que realmente no apliquen y escribe «No aplica» cuando
la sección aplique pero no haya cambios; no dejes casillas sin responder.
-->

## Objetivo

<!-- Qué resuelve este PR y qué recorrido funcional afecta. Una o dos frases. -->

## Tarjeta o issue

<!-- Enlace a la tarjeta de Trello o al issue. Indica también el incremento del plan (P0, P1, P2...). -->

## Cambios de base de datos

<!--
Migraciones Flyway agregadas (archivo y rango reservado), tablas o columnas
afectadas y procedimiento para aplicarlas. Escribe «No aplica» si no hay
cambios de esquema.
-->

- [ ] No hay cambios de esquema en este PR
- [ ] Hay migraciones y están en el mismo PR que el código que depende de ellas

## Consideraciones de autorización

<!--
Quién puede y quién no puede ejecutar cada acción nueva: rol, propiedad del
recurso, estado de cuenta y segundo factor cuando corresponda. Escribe
«No aplica» si el PR no toca endpoints ni datos protegidos.
-->

| Acción | Quién puede | Quién no puede | Dónde se aplica |
|---|---|---|---|
|  |  |  |  |

## Pruebas ejecutadas

<!-- Comandos reales y su resultado. No declares una prueba que no ejecutaste. -->

```text

```

- [ ] Pruebas positivas y negativas relevantes
- [ ] CI en verde

## Capturas

<!--
Obligatorias cuando cambia la interfaz: teléfono, tableta y escritorio.
Escribe «No aplica» si el PR no toca UI.
-->

## Variables de entorno nuevas

<!--
Nombre y para qué sirve cada variable, sin valores secretos. Confirma que
`.env.example` quedó actualizado.
-->

- [ ] No se agregaron variables
- [ ] Se agregaron y están documentadas en `.env.example` y en el `README.md`

## Riesgos y pendientes

<!-- Lo que queda incompleto, lo que puede romperse y lo que se decidió posponer. -->

## Lista de verificación

- [ ] La rama nace de `develop` actualizado y no incluye cambios ajenos a la tarea
- [ ] Los commits siguen Conventional Commits con los tipos de `GIT_WORKFLOW.md`
- [ ] No hay secretos, `.env` reales, tokens ni credenciales versionados
- [ ] `Docs/Dev/MatrizCumplimiento.md` quedó actualizado con evidencia real
- [ ] La documentación afectada (README, variables, comandos) quedó al día
- [ ] Otro integrante del equipo revisará este PR antes de integrarlo
