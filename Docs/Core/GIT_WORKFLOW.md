# Guía de Contribución y Flujo de Trabajo en Git (GitFlow)

Este documento define las reglas estrictas de control de versiones para mantener el repositorio limpio, organizado y profesional durante el desarrollo de MOICA. 

Se utilizará un **GitFlow Simplificado**, ideal para equipos ágiles, garantizando la estabilidad sin introducir burocracia excesiva.

---

## 1. Estructura del Repositorio (Monorepo)

El proyecto utiliza una arquitectura de monorepo, lo que significa que el frontend, el backend y la documentación conviven en el mismo repositorio de Git. La estructura en la raíz debe mantenerse de la siguiente manera:

*   **`Docs/`**: Contiene toda la documentación técnica, diagramas UML, identidad de marca y directrices de arquitectura.
*   **`backend/`**: Contiene el código fuente de la API REST (Spring Boot, Java).
*   **`frontend/`**: Contiene el código fuente de la aplicación cliente interactiva (React, TypeScript).
*   **`README.md`**: Documento principal con la descripción del proyecto, instrucciones de ejecución y enlaces a la documentación.

---

## 2. Arquitectura de Ramas (GitFlow)

Nunca se trabajará directamente sobre la rama principal. El repositorio estará estructurado en las siguientes ramas:

*   **`main` (Producción):** Es sagrada. Aquí solo entra código 100% probado, estable y listo para presentar. Ningún desarrollador hace *commits* directos aquí.
*   **`develop` (Desarrollo/Integración):** Es el corazón del proyecto. Aquí se integran las funcionalidades en las que trabaja el equipo. Debe mantenerse funcional en todo momento.
*   **`feature/` (Nuevas Características):** Cada vez que un desarrollador vaya a programar algo nuevo, debe crear una rama que nazca a partir de `develop`.
    *   *Ejemplo:* `feature/login-jwt`, `feature/crear-solicitud`.
*   **`hotfix/` (Parches Críticos):** Si un error crítico ocurre en `main`, se crea esta rama desde `main`, se aplica la solución, y se integra de vuelta tanto a `main` como a `develop`.
    *   *Ejemplo:* `hotfix/caida-base-de-datos`.

---

## 3. Nomenclatura de Commits (Conventional Commits)

Cada *commit* debe explicar claramente **qué se hizo y por qué**. Se usará el estándar de la industria conocido como Conventional Commits. 

El formato es: `<tipo>(<scope opcional>): <descripción en minúsculas y modo imperativo>`

El *scope* indica el área afectada y **es opcional**: se incluye solo cuando ayuda a ubicar el cambio y se omite cuando el commit no pertenece a un área concreta. Ambas formas son válidas:

```text
docs: actualizar diagrama de clases
docs(git): completar convenciones de commits
```

**Tipos permitidos:**
*   `feat:` -> Nueva funcionalidad de código. (Ej: `feat(auth): integrar jwt en el login`).
*   `fix:` -> Corrección de un bug o error. (Ej: `fix(chat): resolver error de cors en las peticiones`).
*   `docs:` -> Cambios en la documentación (markdown, diagramas). (Ej: `docs: actualizar diagrama de clases`).
*   `refactor:` -> Cambios internos que no añaden funcionalidades ni arreglan bugs, pero mejoran la estructura. (Ej: `refactor(perfil): limpiar importaciones sin uso`).
*   `test:` -> Añadir o corregir pruebas automatizadas. (Ej: `test(solicitud): cubrir transiciones inválidas`).
*   `chore:` -> Tareas de mantenimiento, configuraciones, instalación de dependencias, ignore. (Ej: `chore: instalar react-query y zustand`).
*   `ci:` -> Workflows y automatización de integración continua. (Ej: `ci(repo): validar títulos convencionales`).
*   `build:` -> Sistema de construcción, empaquetado o imágenes de despliegue. (Ej: `build(deploy): crear imagen del backend`).
*   `style:` -> Cambios de formato visual del código (espacios, punto y coma, indentación), sin alterar la lógica.
*   `perf:` -> Mejoras de rendimiento. (Ej: `perf(busqueda): reducir consultas repetidas`).
*   `revert:` -> Revertir un commit anterior. (Ej: `revert: deshacer cambio de política de sesiones`).

**Reglas de redacción:**
*   El tipo se escribe siempre en minúscula (`docs:`, nunca `Docs:`).
*   La descripción va en minúscula, en modo imperativo y sin punto final.
*   El mensaje describe el resultado obtenido, no la herramienta utilizada.

**Alcances orientativos:** `repo`, `db`, `backend`, `frontend`, `auth`, `session`, `2fa`, `usuario`, `prestador`, `portafolio`, `servicio`, `busqueda`, `solicitud`, `chat`, `calificacion`, `moderacion`, `admin`, `pwa`, `deploy` y `docs`. Esta lista es una referencia abierta, no un catálogo cerrado: si ninguno describe bien el cambio, es preferible omitir el scope antes que forzar un término incorrecto.

*Ejemplo de un buen historial de commits:*
> `feat(portafolio): crear componente de tarjeta de portafolio`
> `fix(frontend): centrar imagen rota en dispositivos móviles`
> `docs: agregar guía de contribución a los docs`

---

## 4. El Flujo de Trabajo (Paso a Paso)

Cuando un desarrollador inicie una nueva tarea, debe seguir este proceso exacto:

1. **Actualizar el entorno local:**
   ```bash
   git checkout develop
   git pull origin develop
   ```
2. **Crear la rama de trabajo:**
   ```bash
   git checkout -b feature/nombre-de-la-tarea
   ```
3. **Escribir código y hacer commits atómicos (pequeños y constantes):**
   ```bash
   git add .
   git commit -m "feat: agregar endpoint de login"
   ```
4. **Subir la rama al repositorio remoto:**
   ```bash
   git push origin feature/nombre-de-la-tarea
   ```
5. **Crear el Pull Request (PR):**
   * Acceder al repositorio en GitHub y crear un Pull Request apuntando hacia la rama `develop`.

---

## 5. Reglas de Pull Requests (Code Review)

*   **Bloqueo de Ramas:** Está estrictamente **prohibido** hacer *merge* directo a `develop` o a `main`. Todo código debe pasar obligatoriamente por un Pull Request (PR).
*   **Aprobación Cruzada (Code Review):** Cuando un desarrollador abre un PR, **otro miembro del equipo debe revisarlo y aprobarlo**. Esto asegura conocimiento compartido de la base de código y ayuda a prevenir bugs evidentes.
*   **Título del PR:** Debe utilizar la misma convención de commits, con el scope igualmente opcional (Ej: `feat: módulo de autenticación` o `feat(auth): implementar módulo de autenticación`).
*   **Borrar ramas:** Una vez que el PR es aprobado y se integra a `develop` exitosamente, la rama `feature/` correspondiente debe ser eliminada del repositorio remoto para mantener el orden.

---

## 6. Promoción de hitos a `main`

`develop` es la rama de integración del trabajo diario. `main` solo recibe **hitos estables y presentables**, y la única forma de llevar contenido hasta ella es un Pull Request desde `develop`.

**Cuándo se abre ese PR:**

*   Al cerrar un hito completo: una línea base documental, una versión demostrable o la entrega final.
*   **No** se abre después de cada funcionalidad ni después de cada PR integrado en `develop`. Si cada rama `feature/` terminara en `main`, la rama dejaría de representar lo que el equipo puede presentar.

**Cómo se hace:**

1.  Actualizar `develop` local y comprobar que contiene exactamente lo aprobado.
2.  Abrir el PR `develop` → `main` con título convencional (Ej: `docs: publicar linea base documental del mvp`).
3.  Describir todo lo que `main` todavía no contiene, no solo el último PR integrado.
4.  Esperar la revisión y la aprobación cruzada, igual que en cualquier otro PR.

**Prohibiciones que no admiten excepción:**

*   Nunca se hacen *commits* directos sobre `main`.
*   Nunca se hace *merge* local ni `git push` directo hacia `main`.
*   Nunca se reescribe su historial (`--force`, *rebase* o *amend* sobre lo ya publicado).

La etiqueta de versión se crea únicamente cuando el hito corresponde a una versión del producto (Ej: `v0.1.0-mvp`). Una línea base documental previa al código no lleva etiqueta de versión del MVP.
