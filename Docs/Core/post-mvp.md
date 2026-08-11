# Roadmap Post-MVP (Mejoras Futuras para MOICA)

Este documento recopila las decisiones arquitectónicas, refactorizaciones y nuevas características que **se excluyen intencionalmente del MVP** para ahorrar tiempo, pero que deben implementarse en las siguientes fases del proyecto o si sobra tiempo antes de la entrega final.

## 1. Refactorización Técnica
- **Chat en Tiempo Real:** 
  - *Estado en MVP:* Short Polling vía REST.
  - *Mejora:* Reemplazar por **WebSockets** bidireccionales usando Spring WebSockets y STOMP. Manejo adecuado de desconexiones y presencia en línea.

## 2. Expansión de Negocio (Según definición inicial)
- **Proveedores de Insumos:** Permitir la conexión no solo para contratación de servicios, sino para proveedores de materia prima y equipos.
- **Geolocalización Automática:** Integrar mapas (Google Maps o Mapbox) y coordenadas reales para reemplazar la descripción libre de cobertura.
- **Pasarela de Pagos / Transacciones:** Implementar pagos dentro de MOICA, retenciones o comisiones.
- **Expiración automática de Solicitudes:** Lógica en servidor (ej. un *Cron Job*) que cancele automáticamente solicitudes pendientes que el prestador ignore por mucho tiempo. No debe confundirse con la expiración de sesiones, que sí forma parte del MVP.
- **App Móvil Nativa:** Escalar la experiencia de la PWA actual a una aplicación nativa en tiendas de aplicaciones.
- **Archivos Multimedia en Chat:** Soporte para subir imágenes, audios o documentos en la conversación.

## 3. Seguridad
- Cifrado de extremo a extremo (E2E) para la mensajería del chat.

## 4. Moderación y Cumplimiento Normativo
En el MVP, una persona administradora revisa cada caso y decide manualmente cada medida. Las capacidades siguientes quedan excluidas y **sus reglas todavía no están definidas**: deberán diseñarse y aprobarse antes de implementarlas.

- **Automatización del cumplimiento normativo:** Comprobaciones y reportes automáticos de cumplimiento sobre las cuentas y los casos de moderación.
- **Detección y reglas de reincidencia:** Identificación automática de conductas repetidas y las reglas que se derivarían de ellas.
- **Umbrales de severidad:** Criterios cuantitativos que asocien un nivel de severidad con una consecuencia determinada. En el MVP, el nivel de severidad del catálogo es únicamente descriptivo.
- **Recomendación, selección o escalamiento automático de medidas:** Que Moica proponga, elija o agrave una medida sin la decisión de una persona administradora. No debe confundirse con la expiración automática de una medida temporal ya elegida por una persona, que sí forma parte del MVP.
