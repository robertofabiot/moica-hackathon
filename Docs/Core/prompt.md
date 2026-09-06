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
- **Contraseña (D-SEC-02):** de 8 a 72 caracteres, con al menos una mayúscula, una minúscula, un número y un símbolo. Además, BCrypt impone un tope de implementación de 72 bytes UTF-8: los 8–72 siguen midiendo caracteres; el límite en bytes es adicional y evita que caracteres multibyte (acentos, eñes, emojis) hagan fallar a BCrypt.
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

## 6. Seguridad de la Cuenta y Autorización
- **401 y 403 no son intercambiables:** `401` significa que ya no hay sesión (ausente, expirada o revocada) y `403` que la hay pero no alcanza. Una contraseña o un código de segundo factor equivocados devuelven `403`, nunca `401`: la sesión sigue viva y un `401` haría que la interfaz creyera que acaba de morir.
- **Autorización cerrada por omisión:** en la cadena de seguridad, lo que no se declara exige una sesión plena. Agregar un endpoint sin pensar en sus permisos debe dejarlo protegido, no abierto.
- **Cambio de credenciales:** cambiar la contraseña o desactivar el segundo factor revoca **todas** las sesiones de la cuenta —incluida aquella desde la que se hizo el cambio— en la misma transacción, con motivo `CAMBIO_CREDENCIALES`.
- **Segundo factor TOTP:** se usa una biblioteca mantenida; **prohibido** implementar el algoritmo a mano. Los dígitos, el periodo y la tolerancia se declaran en un solo lugar (`moica.segundo-factor.*`) y son los mismos que anuncia la URI `otpauth://`.
- **Secreto TOTP:** se genera con una fuente criptográficamente segura y se guarda cifrado con AES-GCM y un nonce aleatorio por cifrado, con una clave que llega por variable de entorno. Una configuración ausente o inválida impide el arranque. El secreto solo sale de Moica una vez, al iniciar la activación y hacia la propia persona autenticada; después no se puede recuperar.
- **Sesión provisional:** cuando la cuenta tiene el segundo factor `ACTIVO`, la sesión recién abierta solo puede consultarse, verificar su código y cerrarse. Verificar completa **esa** sesión, no las demás de la cuenta. `segundoFactorVerificado = false` por sí solo no significa «provisional»: hay que mirar también la configuración de la cuenta.
- **Rol administrativo:** **prohibido** el registro público de administradores, un endpoint de promoción, una contraseña fija o un secreto versionado. El rol se asigna al arrancar, de forma idempotente, sobre una cuenta ordinaria ya registrada que indica una variable de entorno.
- **Estado de cuenta:** `Usuario.estadoCuenta` es una proyección operativa que solo cambia la moderación. Las suspensiones bloquean el acceso; quien las lea no las recalcula ni las expira por su cuenta.
- **Ocultar un control en React no es autorización.** Toda regla se aplica en el backend y se prueba allí; la interfaz solo evita proponer algo que el servidor va a rechazar.

## 7. Evidencia visual de Pull Requests
La evidencia visual de un PR no forma parte del árbol fuente. Antes de versionar PNG, JPG, WebP o `medidas*.json` generados al validar la interfaz, consultar `Docs/Core/GIT_WORKFLOW.md` y `.github/pull_request_template.md`. Por defecto se adjuntan al PR; no se hace commit.
