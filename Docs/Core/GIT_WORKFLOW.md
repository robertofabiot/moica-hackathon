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

El formato es: `<tipo>: <mensaje claro en minúsculas y modo imperativo>`

**Tipos permitidos:**
*   `feat:` -> Para una nueva funcionalidad de código. (Ej: `feat: integrar JWT en el login`).
*   `fix:` -> Para arreglar un bug o error. (Ej: `fix: resolver error de CORS en peticiones del chat`).
*   `docs:` -> Para cambios en la documentación (markdown, diagramas). (Ej: `docs: actualizar diagrama de clases`).
*   `chore:` -> Tareas de mantenimiento, configuraciones, instalación de dependencias, ignore. (Ej: `chore: instalar react-query y zustand`).
*   `refactor:` -> Cambios en el código que no añaden funcionalidades ni arreglan bugs, pero mejoran la estructura. (Ej: `refactor: limpiar importaciones sin uso en perfil`).
*   `style:` -> Cambios de formato visual del código (espacios, punto y coma, indentación).

*Ejemplo de un buen historial de commits:*
> `feat: crear componente de tarjeta de portafolio`
> `fix: centrar imagen rota en dispositivos móviles`
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
*   **Título del PR:** Debe utilizar la convención de commits (Ej: `feat: módulo de autenticación`).
*   **Borrar ramas:** Una vez que el PR es aprobado y se integra a `develop` exitosamente, la rama `feature/` correspondiente debe ser eliminada del repositorio remoto para mantener el orden.
