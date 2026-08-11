# Directrices y Estándares de Código para IA (MOICA MVP)

Este documento contiene las reglas estrictas de arquitectura y desarrollo para la fase MVP de MOICA. Cualquier asistente de IA o desarrollador del equipo debe apegarse a estas convenciones. No se deben introducir librerías no solicitadas ni cambiar la arquitectura sin aprobación.

## 1. Arquitectura General
- **Enfoque:** Monolito Modular (una sola base de datos). **Prohibido sugerir microservicios**.
- **Backend:** Java + Spring Boot (API REST).
- **Frontend:** React + TypeScript (PWA, Mobile-First).
- **Base de Datos:** PostgreSQL.

## 2. Convenciones de Backend (Spring Boot)
- **Patrón Arquitectónico:** Arquitectura por Capas Clásica (Controller, Service, Repository, Entity). No usar Arquitectura Hexagonal en el MVP.
- **Autenticación:** JWT (JSON Web Tokens) asociados a una sesión registrada en la base de datos. El token transporta el identificador de su sesión, de modo que esta pueda expirar y revocarse; la autenticación no es puramente stateless.
- **Tipos de Datos Críticos:** 
  - IDs siempre en `BIGINT` (en Java, usar `Long`).
  - Fechas siempre en `TIMESTAMPTZ` (en Java, usar `OffsetDateTime` o `Instant` para manejar UTC).
  - Todas las entidades principales deben incluir campos de auditoría (`fechaCreacion`, `fechaActualizacion`).

## 3. Convenciones de Frontend (React)
- **Gestión de Estado Global:** `Zustand`. Prohibido usar Redux o Context API abusivamente.
- **Data Fetching y Caché:** `React Query` (@tanstack/react-query). Usarlo para todas las peticiones a la API REST.
- **Estilos y CSS:** **Vanilla CSS (CSS Modules)**. Prohibido usar TailwindCSS, MUI, Bootstrap o similares. El equipo de diseño requiere control total (pixel-perfect) para el *Urban & Trust Design*.
- **Implementación de Chat (MVP):** Se debe implementar mediante **Short Polling** utilizando el `refetchInterval` de React Query apuntando a un endpoint REST normal. **Prohibido configurar WebSockets en la fase MVP**.

## 4. Reglas de Negocio Clave
- **Sin geolocalización:** El municipio principal y descripciones son texto libre. Nada de mapas interactivos o coordenadas.
- **Sin pasarela de pagos:** Todo precio es referencial o "A convenir".
- **Privacidad de Contactos:** Los medios de contacto y el chat solo se habilitan cuando la solicitud pasa a estado `ACEPTADA`.

## 5. Verificación de Prestadores (Expediente Documental)
- **Revisión manual obligatoria:** Toda solicitud de verificación la resuelve una persona con rol administrativo cuya sesión haya verificado el TOTP. **Prohibido** introducir aprobación, rechazo o revocación automáticas.
- **Prohibido en el MVP sin aprobación expresa del equipo:** OCR o extracción automática de datos, reconocimiento facial, prueba de vida, comparación biométrica, consulta a bases gubernamentales o de terceros y cualquier SDK o API de un proveedor externo de verificación. No se incorporan de forma silenciosa "porque simplifican el flujo".
- **Distinto del segundo factor:** El TOTP protege el inicio de sesión; la verificación documental respalda la identidad o trayectoria de un `PerfilPrestador`. Son mecanismos separados y no se sustituyen entre sí.
- **Archivos admitidos:** JPEG, PNG y PDF, con un máximo configurable de 5 MB por archivo. El tipo MIME y el tamaño se validan en el backend, nunca solo en el navegador.
- **Almacenamiento privado:** Los archivos se guardan en un recurso privado detrás de un servicio de almacenamiento configurable. La base de datos persiste una clave opaca y los metadatos del documento; **prohibido** guardar el binario en PostgreSQL o una URL pública permanente.
- **Autorización:** Solo la persona propietaria del perfil envía documentos y consulta los metadatos de su expediente. Solo un administrador con TOTP verificado abre los archivos y resuelve la solicitud. La entrega de un archivo se hace mediante autorización en el backend y acceso temporal; **prohibido** exponer el almacenamiento directamente o publicar enlaces permanentes.
- **Superficie pública:** Lo único público es la insignia o nivel vigente del perfil. Documentos, números de identificación, claves de almacenamiento y observaciones administrativas nunca se devuelven en un endpoint público.
