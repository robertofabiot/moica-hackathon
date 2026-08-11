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

## Estructura del Repositorio (Monorepo)

*   `/Docs`: Contiene la documentacion integral del proyecto. Incluye el PRD (Definicion del Producto), Diagramas UML (Casos de Uso, Actividades, Clases), Diagramas de Base de Datos y Guia de Identidad de Marca.
*   `/backend`: Codigo fuente de la API REST (Spring Boot).
*   `/frontend`: Codigo fuente de la aplicacion cliente (React).

## Guia para Desarrolladores

Para mantener la calidad y el orden del codigo base durante el Hackathon, es obligatorio revisar los siguientes documentos antes de realizar el primer commit:

1.  **Reglas de Git y Pull Requests:** Revisar `Docs/Core/GIT_WORKFLOW.md`. Se utiliza una version simplificada de GitFlow. Todo codigo debe pasar por Code Review.
2.  **Reglas de Arquitectura para el MVP:** Revisar `Docs/Core/prompt.md`. Contiene las convenciones estrictas sobre librerias permitidas (ej. Zustand, React Query) y limites del alcance.
3.  **Roadmap Futuro:** Revisar `Docs/Core/post-mvp.md` para ideas que se han excluido del MVP (ej. WebSockets) con el fin de priorizar la entrega.

## Entorno de Desarrollo Local

El repositorio incluye un archivo `docker-compose.yml` en la raiz para levantar rapidamente la infraestructura de datos sin configuraciones complejas.

### Requisitos Previos
*   Docker y Docker Compose instalados.
*   Node.js (para el entorno frontend).
*   Java JDK 17 o superior (para el entorno backend).

### Iniciar la Base de Datos
Desde la raiz del proyecto, ejecutar:

```bash
docker-compose up -d
```
Esto iniciara:
1. Instancia de PostgreSQL (Puerto 5432).
2. Interfaz visual PgAdmin (Puerto 5050).

## Licencia

Este proyecto se distribuye bajo los terminos de la Licencia MIT. Para mas informacion, consulte el archivo `LICENSE`.
