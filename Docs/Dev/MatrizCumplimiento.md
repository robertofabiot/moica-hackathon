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
- Los resultados de CI se enlazan, no se describen de memoria.
- Se distingue siempre **dónde** se comprobó cada cosa. Una verificación local
  que no deja artefacto enlazable —la instalación de una PWA, por ejemplo— se
  anota describiendo qué se ejecutó y qué se observó, y se marca como local.

## Estados

| Estado | Significado |
|---|---|
| Pendiente | Todavía no se ha comenzado. |
| En progreso | Hay evidencia parcial; el criterio aún no se cumple por completo. |
| Cumplido | Existe evidencia verificable y el criterio está completo. |

## Criterios del entregable

| # | Criterio | Estado | Incremento | PR | Commits | Pruebas | Evidencia |
|---|---|---|---|---|---|---|---|
| 1 | README técnico completo (requisitos, variables, estructura, scripts, comandos, endpoints) | En progreso | P1 → P11 | #3, #5, #9, #16, #17, #20, #21, #28, #32, #33 | `70467f6`, `61b4af6`, `19bfaff`, `e594fc0`, `26cec56`, `aec58f9`, `7b5abb1`, `ee75593`, `ed7b0d3`, `29defa0`, `44490f8` | Sus instrucciones se siguieron de principio a fin en una máquina real | `README.md` cubre requisitos, versiones, arquitectura (con diagrama), instalación rápida, comandos de validación y estructura del monorepo. El detalle profundo —variables de entorno, secreto JWT y conflicto de puertos en `Docs/Dev/GuiaEntornoLocal.md`; endpoints, modelo de sesión, CSRF, política de contraseña y forma de los errores en `Docs/Dev/ContratoDeApi.md`— vive en `Docs/Dev/`, enlazado desde la sección «Documentación» del README. El despliegue se completa en P11. P4 corrige la descripción del portafolio —lo administra el prestador, no se arma solo con servicios completados—, agrega el estado del perfil de prestador y suma `Docs/Dev/Almacenamiento.md` con la decisión de Cloudflare R2, la configuración del bucket y las seis variables nuevas. P4V documenta la segunda superficie de almacenamiento: el bucket privado, su token propio, sus seis variables y las diez comprobaciones que exige, además del flujo completo de verificación en el contrato de la API y la precisión sobre revocación en la definición funcional. P5 documenta las publicaciones propias, el descubrimiento público, `V31`/`V90` y el prefijo `servicios/` del almacén público, sin variables nuevas. Tras integrar el #15, el README y el contrato precisan que un prestador `NO_DISPONIBLE` conserva perfil y portafolio públicos, pero sin servicios listados ni contratación. P6 documenta el ciclo de solicitudes, `V40`, las rutas `/solicitudes` y las acciones explícitas del contrato, sin variables nuevas. El #20 agregó `Docs/Dev/PlanImplementacionMvp.md` y lo enlazó desde «Documentación». P7 documenta el chat y la revelación de contactos: `V41`, las tres rutas nuevas, la tabla de estados, los códigos `CHAT_NO_HABILITADO`, `CHAT_SOLO_LECTURA` y `CONTACTOS_NO_REVELADOS`, y el short polling. Sin variables ni dependencias nuevas; la licencia de inspección no se toca y pagos y mapas siguen fuera. README principal: parrafo de reportes y casos de moderacion, que dice tambien lo que reportar no hace. README de migraciones: `V50` y `V51` con sus restricciones. El #32 sintetiza la sección «Estado actual» del README en viñetas directas y legibles para evaluadores y jueces de hackathon, eliminando texto redundante y destacando las capacidades funcionales completadas de punta a punta hasta P9. El #33 actualiza la línea de moderación del «Estado actual» del README: la bandeja administrativa, la asignación de responsables y la resolución ya existen, no solo la apertura de reportes. |
| 2 | Modelo ER en 3FN y tres diagramas UML completos | Cumplido | P0 | #1, #2 | | | `Docs/Dev/DiagramaLogico.mmd`, `DiagramaConceptual.mmd`, `DiagramaClasesDominio.mmd`, `DiagramaCasosDeUso.tex`, `DiagramaActividades.tex`, `Moica - Diccionario de Datos.xlsx` |
| 3 | Interfaz navegable, validada y responsiva | En progreso | P1 → P11 | #3, #5, #7, #9, #10, #14, #15, #16, #17, #18, #21, #22, #23, #24, #25, #26, #27, #28, #29, #30, #31, #33, #34 | `1c49502`, `d280578`, `4ace0e2`, `6f9c4d1`, `87b9340`, `baee29d`, `e592d6f`, `887583a`, `a435cf3`, `329f3e9`, `fcde0fc`, `830a044`, `714471d`, `117af69`, `9c80210`, `e3ba201`, `feff7ef`, `615cba3`, `7045359`, `5bdbd43`, `8cf5957`, `70b9063`, `83597a3`, `034f629`, `5786c35`, `5c13f37`, `e2c5d6d`, `cd68f8f`, `27ae045`, `d096558`, `f906a00`, `57dee03`, `b8bd22e`, `dfd9b50`, `afb6810`, `fea95bf`, `9e0fa07`, `c97fe7f`, `f26ae79`, `2281c02`, `fda8ab8`, `c8b2572`, `e11e6d6`, `dcbe7c8`, `50f4b91`, `29b07d1`, `eeb3176`, `db3f513`, `2556d33`, `93f4f23`, `79d6fd7`, `7f7424c`, `82c01b8`, `b517f8e`, `50a2139`, `6a7666f`, `e7c26e3`, `9c83e19`, `f523017`, `f22fe66`, `e594fc0`, `26cec56`, `aec58f9`, `7b5abb1`, `ee75593`, `ed7b0d3`, `dddf090`, `ccc0f67`, `a74c0cd`, `4a513a4`, `803a6f1`, `766a1b7`, `84b4693`, `4abdf6c`, `2b41e8d`, `6ae10b2`, `172ac65`, `a77e8be`, `45af5c8`, `b0fc4d5`, `0e8caea`, `cbe6770`, `f16e6b0`, `e9c1237`, `a014b5d`, `549be7f`, `1044ae7` | 332 pruebas de Vitest en 38 archivos sobre el #34: las mismas 332 del #31; `PerfilPrestador.test.tsx` conserva sus 12 casos, `ImagenDePerfil.test.tsx` 6, `Portafolio.test.tsx` 11 y `Verificacion.test.tsx` 16. Capturas a 375x812, 768x1024 y 1280x800 adjuntas al PR. Antes: 332 pruebas de Vitest en 38 archivos sobre el #31: las 324 del #30 más 8 de este PR —7 en `PanelUsuario.test.tsx` y 1 en `Boton.test.tsx`—. Capturas a 375x812, 768x1024 y 1280x800 adjuntas al PR. Antes: 324 pruebas de Vitest en 37 archivos sobre el #30: las mismas 324 del #29; `SeguridadCuenta.test.tsx` conserva sus 18 casos. Capturas a 375x812, 768x1024 y 1280x800 adjuntas al PR. Antes: 324 pruebas de Vitest en 37 archivos sobre el #29: las 298 del #28 más 26 de este PR —13 en `Mensajes.test.tsx`, 7 en `presentacion.test.ts`, 4 en `ItemConversacion.test.tsx` y 2 en `BurbujaMensaje.test.tsx`—. Cubren ruta protegida, vacío y carga de la bandeja, filtro de estados y búsqueda, hilo propio/ajeno, envío, fallo que conserva el texto, mensaje en blanco, solo lectura al completar, cuenta restringida y error con reintento. Capturas a 375x812, 768x1024 y 1280x800 adjuntas al PR. Antes: 298 pruebas de Vitest en 33 archivos sobre el #28: las 279 del #27 más 19 de este PR en `ReporteDeSolicitud.test.tsx` —visibilidad según el historial, validación accesible, envío, resumen, error con reintento, conflicto 409, doble envío, ventana entre confirmación y refresco, y cuenta restringida—. Antes: 279 pruebas de Vitest en 32 archivos sobre el #27: las 273 del #26 más 6 de este PR —`NuevoServicio.test.tsx` cubre 8 casos (indicador, validación del paso 1, cancelar al listado, dropzone, fotos al publicar, precio inválido, alta con precio nulo y conservar datos al volver)—; `ServiciosPropios.test.tsx` conserva listado, activar/desactivar, edición e imágenes y deja de cubrir el alta en un solo formulario. Capturas a 375x812, 768x1024 y 1280x800 adjuntas al PR. Antes: 273 pruebas de Vitest en 31 archivos sobre el #26: las 265 del #25 más 8 de este PR —`PrestadorPublico.test.tsx` pasa de 3 a 11 casos (carga, error, reputación real, vacío sin cero, reseñas individuales, cabecera sin cifras de maqueta, servicios, portafolio, marco, seguir y contactar)—; `DetalleDeServicio.test.tsx` y `ExplorarServicios.test.tsx` abren el popover de `InsigniaResponsable` con hover. Recorrido visual contra la API local en `/explorar/prestadores/43`. Capturas a 375x812, 768x1024 y 1280x800 adjuntas al PR. Antes: 265 pruebas de Vitest en 31 archivos sobre el #25 (P8): las 241 del #24 mas 24 de calificaciones y reputacion real —13 en `CalificacionDeSolicitud.test.tsx`, 3 en `PrestadorPublico.test.tsx`, 4 en `ExplorarServicios.test.tsx`, 2 en `DetalleDeServicio.test.tsx` y 2 en `EstrellasCalificacion.test.tsx`—; recorrido manual integrado y 15 capturas a 375x812, 768x1024 y 1280x800, con `scrollWidth == clientWidth` en las quince. Antes: 241 pruebas de Vitest en 29 archivos sobre el #24: las 230 del #23 más 11 en `DetalleDeServicio.test.tsx` (carga, error con reintentar, migas, ficha, precio «Desde», placeholder, galería, prestador, desglose, contratación y Guardar). Las capturas a 375x812, 768x1024 y 1280x800 quedan pendientes y no se anotan como hechas. Antes: 230 pruebas de Vitest en 28 archivos sobre el #23: las mismas 230 del #22; `ExplorarServicios.test.tsx` conserva sus 8 casos y adapta los selectores a la maqueta (botón «Hogar», «Ubicación», «Verificado», `C$450`). Las capturas a 375x812, 768x1024 y 1280x800 quedan pendientes y no se anotan como hechas. Antes: 230 pruebas de Vitest en 28 archivos sobre el #22: las 224 anteriores más 6 de los componentes nuevos —3 en `PieDePagina.test.tsx`, 2 en `EstrellasCalificacion.test.tsx` y 1 en `InsigniaVerificado.test.tsx`—; `Inicio.test.tsx` comprueba el landmark `contentinfo` y el copyright. Las capturas a 375x812, 768x1024 y 1280x800 del #22 quedan pendientes y no se anotan como hechas. Antes: 224 pruebas de Vitest en 25 archivos sobre el #21: las 202 anteriores más 22 de P7 —14 en `ChatDeSolicitud.test.tsx` y 8 en `ContactosDelPrestador.test.tsx`—; recorrido manual integrado y 15 capturas a 375x812, 768x1024 y 1280x800, con `scrollWidth == clientWidth` en las quince. Antes: 202 pruebas de Vitest en 23 archivos sobre el #18, sin casos nuevos: las 179 anteriores más 23 de P6 —5 en `MisSolicitudes.test.tsx`, 7 en `NuevaSolicitud.test.tsx` y 11 en `DetalleDeSolicitud.test.tsx`—. Antes: 179 pruebas de Vitest en 20 archivos sobre el código combinado con la portada del #15: las 167 de P5 más las de sesión y hero que aportó Roberto y 7 de la integración —5 en `Inicio.test.tsx` (Explorar, hero, menú y segundo factor) y 2 en `ExplorarServicios.test.tsx` (texto inicial y limpiar filtros)—; recorrido visual del código combinado y 18 capturas a 375x812, 768x1024 y 1280x800, con `scrollWidth == clientWidth` en las dieciocho. Antes: 167 pruebas de Vitest en 20 archivos: las 154 anteriores más 13 de P5 —6 en `ExplorarServicios.test.tsx`, 6 en `ServiciosPropios.test.tsx` y 1 en `App.test.tsx`—. Antes: 154 pruebas de Vitest en 18 archivos: las 145 anteriores más 9 de este PR —3 en `Boton.test.tsx`, 2 en `Entrada.test.tsx` y 4 en `BarraLateral.test.tsx`—. Antes: 145 pruebas de Vitest en 15 archivos: las 116 anteriores más 29 de P4V —16 en `Verificacion.test.tsx` y 13 en `ColaDeVerificaciones.test.tsx`—; recorrido manual y 15 capturas a 375x812, 768x1024 y 1280x800 sobre `d096558`, con `scrollWidth == clientWidth` medido en las quince. Antes: 116 pruebas de Vitest en 13 archivos: las 82 anteriores más 34 de P4 —12 en `PerfilPrestador.test.tsx`, 11 en `Portafolio.test.tsx`, 6 en `ImagenDePerfil.test.tsx` y 5 en `comun/api.test.ts`—; recorrido manual y capturas a 375x812, 768x1024 y 1280x800 sobre `71adc75`, con `scrollWidth == clientWidth` medido en cada una. Antes: 82 pruebas de Vitest: las 39 de P2 más 43 de P3 —18 en `SeguridadCuenta.test.tsx`, 10 en `VerificacionSegundoFactor.test.tsx`, 10 en `PanelAdministrativo.test.tsx` y 5 en `useVigilanciaDeSesion.test.tsx`—; recorrido manual en Chrome a 375x812, 768x1024 y 1280x800 sobre `7045359` y de nuevo sobre `5bdbd43` | El #34 alinea `/prestador` con el sistema de diseño: `BarraLateral` (ítem perfil activo), tarjetas blancas, `Boton`/`Entrada`, aviso de privacidad, identidad agrupada, contactos, portafolio y verificación con acento teal. El contrato de la API no cambia. Las capturas a tres tamaños quedaron adjuntas al PR. El #31 añade `/panel` como dashboard exclusivo del prestador: métricas reales, actividad reciente (máximo 4), próximas tareas y banner de visibilidad. Sin perfil de prestador redirige a `/explorar`. Las capturas a tres tamaños quedaron adjuntas al PR. El #30 reconecta `/seguridad` con el correo de la sesión, deja visible la barra lateral en escritorio (ítem configuración activo) y hace navegar la pestaña Perfil a `/prestador`. `/configuracion` redirige a `/seguridad`. Correo y teléfono no se editan: el MVP no tiene esos endpoints. Las capturas a tres tamaños quedaron adjuntas al PR. El #29 añade `/mensajes`: bandeja y hilo en dos columnas con `BarraLateral` (ítem mensajes activo), envío naranja y solo lectura al cerrar. Las capturas a tres tamaños quedaron adjuntas al PR. El #27 sustituye el formulario plano de `/prestador/servicios/nuevo` por un asistente de cuatro pasos (Información, Detalles, Precio, Publicar) con barra lateral, indicador, validación por paso y dropzone de fotos que se suben tras el POST. El contrato de `POST /api/prestador/servicios` no cambia. La edición de un servicio ya publicado conserva el formulario plano. Las capturas a tres tamaños quedaron adjuntas al PR. El #26 sustituye el scaffolding de `/explorar/prestadores/:id` por el layout del sistema de diseño: barra lateral, cabecera con avatar circular, métricas reales, filas de servicios, reseñas individuales provisionales, portafolio y `PieDePagina`. `InsigniaResponsable` explica la verificación en un popover (hover o tap). El encabezado conserva `reputacionPrestador` real; el listado de reseñas todavía no sale de la API. Las capturas a tres tamaños quedaron adjuntas al PR. Registro, inicio de sesión, cierre de sesión y aviso de sesión vencida, con validación en el formulario y mensajes del backend por campo. Un fallo al cerrar (red, 403, 500 o tiempo agotado) conserva la sesión y permite reintentar. P3 añade `/seguridad` —cambio de contraseña y ciclo completo del segundo factor, con la clave manual y el QR entregados una sola vez—, `/verificar-segundo-factor` para la sesión provisional y `/admin` para el área administrativa; ninguna se queda colgada cuando falla la red. `RutaProtegida` y `RutaAdministrativa` llevan a cada sesión donde le corresponde según lo que le falte: sin sesión, a iniciarla; provisional, a verificar; sin rol, al acceso denegado. La vigilancia de la sesión vive en `App` y no en una pantalla, así que la sesión termina igual en cualquier ruta, y terminarla descarta la caché privada para que la cuenta siguiente no vea nada de la anterior. P4 añade `/prestador`: creación y edición del perfil, imagen, disponibilidad, contactos y portafolio con trabajos e imágenes, todo con sus estados de carga, éxito, error y sin conexión. La imagen de perfil previsualiza el **archivo local** con `URL.createObjectURL` mientras se sube, junto a la que sigue guardada, y no da por aceptado lo que el backend todavía no confirmó; la URL temporal se revoca al sustituir el archivo y al desmontar. La carga de archivos tiene su propia espera de 90 s, por encima del máximo del backend, en lugar de los 10 s de una petición corriente. El orden se cambia con botones de subir y bajar, accesibles con teclado y sin dependencias nuevas, y un aviso deja claro que el perfil sigue privado mientras esté `SIN_VERIFICAR`. P4V añade la sección de verificación dentro de `/prestador` —insignia, qué significa y qué no, aviso de privacidad, elección de varios documentos con su tipo, retirada antes de enviar, confirmación explícita, estado de la solicitud abierta e historial con el motivo de cada decisión— y la cola `/admin/verificaciones` con sus filtros, el detalle del expediente, la toma, la aprobación, el rechazo con motivo y la revocación con motivo y casilla de confirmación. Un conflicto de concurrencia se explica y vuelve a pedir la cola en lugar de dejar el estado viejo en pantalla. El #14 sustituye las tokens provisionales por la paleta naranja de marca (`--color-primary-500` = `#F57C00`), añade `Boton`, `Entrada` y `BarraLateral` en `src/comun/componentes/ui/` y maqueta `/iniciar-sesion` y `/registro` como tarjetas del diseño, sin cambiar el contrato de acceso. Los botones de redes y «olvidé la contraseña» se pintan deshabilitados: el MVP no tiene OAuth ni recuperación de clave. La barra lateral todavía no está enganchada a las pantallas autenticadas. P5 añade `/explorar`, `/explorar/servicios/:id` y `/explorar/prestadores/:id` sin sesión, y `/prestador/servicios` protegida: listado, alta, edición, activar/desactivar e imágenes. Un precio nulo se lee como «A convenir». La insignia pública lleva la advertencia de que no garantiza la calidad futura. El #15 aportó la portada, el encabezado y el 404; este PR los conserva y cablea «Explorar» a `/explorar`, el hero al filtro `texto` y «Mis servicios» en el menú de una sesión plena. Las categorías y la ubicación de la maqueta siguen siendo presentacionales. P6 añade «Mis solicitudes», el formulario desde el detalle público, las bandejas, el historial y las acciones contextuales. Los contactos siguen ocultos. El #18 maqueta `/seguridad` como Configuración: pestañas (solo Cuenta activa), filas de correo/teléfono/idioma/zona horaria de maqueta, contraseña detrás de «Cambiar», `<Entrada>` y `<Boton>` en cambio de clave y 2FA, y el segundo factor en tarjeta aparte. `Entrada` gana un toggle para ver la contraseña. La barra lateral se monta en esa ruta y queda oculta en escritorio. Las seis capturas de esta maqueta —dos por cada tamaño— quedaron adjuntas al #18; su medición de desbordamiento no se registró. P7 añade al detalle de la solicitud el hilo de mensajes y la sección de contactos: lista cronológica con los mensajes propios distinguidos por lado, borde, fondo **y** la etiqueta «Tú», estados de carga, vacío, error con reintento, formulario con etiqueta real y contador, indicador «Enviando…» que impide el doble envío, texto conservado si falla la red, aviso de solo lectura al cerrar la solicitud y aviso propio para una cuenta restringida. El hilo se abre en el último mensaje y se refresca por short polling. La sección de contactos solo se monta para el cliente autorizado, con su estado vacío honesto, y cada entrada se pinta como **texto**, nunca como enlace automático. El #22 añade `PieDePagina`, `EstrellasCalificacion` e `InsigniaVerificado` a `src/comun/componentes/ui/` y monta el pie en la portada: lockup horizontal blanco, lema, cuatro columnas (acordeón nativo en teléfono, cuadrícula desde 48 rem) y copyright 2026. Las estrellas y la insignia se exportan; todavía no se cablean a las tarjetas públicas —P8 no entra aquí—. El #23 sustituye el scaffolding de `/explorar` por la maqueta del sistema de diseño: barra superior, sidebar de categorías con iconos SVG, filtros de ubicación y precio, cuadrícula de tarjetas con `EstrellasCalificacion` (maqueta 4.8/102; el listado no publica reputación) e `InsigniaVerificado`, y `PieDePagina`. Sigue sin sesión; «A convenir» y la ausencia de contactos se conservan. Las capturas a tres tamaños quedan pendientes. El #24 sustituye el scaffolding de `/explorar/servicios/:id` por la maqueta: migas, galería interactiva, ficha de contratación, prestador, desglose de reseñas de maqueta y `PieDePagina`. Conserva `useServicioPublico`, `admiteContratacion` y la advertencia de la insignia. Las capturas a tres tamaños quedan pendientes. El reporte se integra en el detalle de la solicitud reutilizando secciones, campos y avisos compartidos. 15 capturas de pagina completa a 375x812, 768x1024 y 1280x800 sobre cinco escenarios, todas con `scrollWidth == clientWidth`. El #33 abre `/admin/casos` y `/admin/casos/:idCaso` dentro de `RutaAdministrativa`, con el mismo lenguaje visual que la cola de verificaciones. 15 pruebas nuevas de Vitest —6 en `BandejaDeCasos.test.tsx` y 9 en `ExpedienteDeCaso.test.tsx`— sobre un total de **347 en 40 archivos**. 9 capturas a 375x812, 768x1024 y 1280x800 tomadas por CDP contra la aplicación real, las nueve con `scrollWidth == clientWidth`, adjuntas al PR. Este incremento está detallado en «Revisión y resolución administrativa de casos de P10A». |
| 4 | Ramas, Conventional Commits, Pull Requests y trazabilidad | En progreso | P0 → P11 | #1, #2, #3, #5, #7, #9, #10, #14, #15, #16, #17, #18, #19, #20, #21, #22, #23, #24, #26, #27, #28, #29, #30, #31, #32, #33, #34 | `eb77733`, `d1cba29`, `e594fc0`, `26cec56`, `aec58f9`, `7b5abb1`, `ee75593`, `ed7b0d3`, `dddf090`, `ccc0f67`, `a74c0cd`, `4a513a4`, `803a6f1`, `29defa0`, `44490f8`, `82973e1`, `a24c110`, `7555b98`, `73ca819`, `beff40a`, `e9c1237`, `a014b5d`, `549be7f`, `1044ae7` | Check «Título y commits convencionales» en verde; en el #7, sobre su commit final `44f2c11`, [ejecución 32800192187](https://github.com/robertofabiot/moica-hackathon/actions/runs/32800192187); `./mvnw verify` ejecutado además sobre cada commit del backend de P2 por separado | `Docs/Core/GIT_WORKFLOW.md` define ramas, tipos y promoción a `main`; P1 agrega `.github/pull_request_template.md` y la validación automática de título y commits del PR. P2 aporta siete commits atómicos que se pueden leer en orden: esquema, errores, registro, autenticación, ciclo de sesión y las dos entregas de interfaz. P3 aporta dieciséis en `feature/seguridad-permisos-2fa`: siete de implementación, cuatro documentales y cinco de la revisión correctiva —`f290d6b`, `8cf5957`, `70b9063`, `83597a3` y `17531b9`—, que entraron por el mismo PR en lugar de por un arreglo aparte. El #7 se abrió contra `develop`, `robertofabiot` lo aprobó una vez cerrada esa revisión y entró como el merge `21ef17c`; su rama quedó eliminada en el remoto. P4 aporta veintiún commits atómicos en `feature/perfil-portafolio`, legibles en orden: la decisión de almacenamiento, las cuatro migraciones, la integración con R2, cada capacidad del backend, sus pruebas, el traslado de la infraestructura compartida del frontend a `src/comun/`, la interfaz, sus pruebas, la corrección encontrada en el recorrido manual y cinco documentales. Los tres últimos son la ronda correctiva de la revisión de `robertofabiot` sobre `d3b85bb` —`e2c5d6d`, `71adc75` y este cambio documental—, que entró por el mismo PR en lugar de por un arreglo aparte. P4V aporta nueve commits atómicos en `feature/verificacion-prestadores`, legibles en orden: la migración, el almacenamiento privado, el expediente propio, la revisión administrativa, las pruebas, las dos entregas de interfaz, la corrección responsiva encontrada en el recorrido manual y la documentación. El #11 corrigió la concurrencia sobre el nivel del perfil y se fusionó el 28 de agosto de 2026 **sin revisión registrada**; no se anota aquí una aprobación que no existió. Roberto revisó a posteriori el rango `9790215..f88edff` sin hallazgos; el comentario público en el PR queda pendiente y no bloqueó P5. El #14 integró el sistema de diseño y la maqueta de acceso. El #15 fusionó en `develop` (`1a994ca`) la portada y el 404. P5 nace de `develop` en `f195360` como `feature/servicios-busqueda-publica` y entra por el #16 a `develop` mediante `6cd875f`. Roberto aprobó el PR el 29 de agosto de 2026. La CI posterior al merge quedó verde en backend, frontend y Docker Compose: [ejecución 33264757393](https://github.com/robertofabiot/moica-hackathon/actions/runs/33264757393). P6 nace de ese `develop` como `feature/solicitudes-servicio` y se abre en el #17; `robertofabiot` lo aprobó y entró en `develop` mediante el merge `a9884a9`, con `fa43303` como HEAD final de la rama. El #18 nace de `develop` (`a9884a9`) como `feat/ui-configuracion-cuenta` y se abre contra esa rama, no contra `main`; `ErvingMiranda` lo aprobó el 30 de agosto de 2026 y entró como el merge `cfd8cd0`. El #19 sustituyó la licencia MIT por la de solo inspección y entró como `645fd2e`. El #20 incorporó `Docs/Dev/PlanImplementacionMvp.md` y actualizó el README y esta matriz; Roberto lo aprobó y entró como el merge `2dd8e9c`, tras el cual se eliminó su rama remota. P7 nace de ese `develop` (`2dd8e9c`) como `feature/chat-contactos`. El #21 entró en `develop` mediante el merge `0b17f46`. El #22 nace de ese `develop` como `feature/ui-pie-de-pagina-y-componentes-base` y se abre contra esa rama, no contra `main`. Aporta cinco commits de implementación más este cambio documental: pie institucional, estrellas e insignia, integración en la portada y dos correcciones del lockup. El #23 nace de `develop` (`4b512fc`) como `feature/ui-explorar-servicios` y se abre contra esa rama, no contra `main`. Aporta diez commits de implementación más este cambio documental: iconos de categoría, tarjeta, sidebar, pantalla completa, pruebas adaptadas, filtrado inmediato, precio máximo en el cliente, tildes, buscador compacto en móvil y menú del avatar. El #24 nace de `develop` (`8084acb`) como `feature/ui-detalle-servicio` y se abre contra esa rama, no contra `main`. Aporta cinco commits de implementación más este cambio documental: iconos de estrella, check y marcador; galería y descripción; ficha, reseñas y prestador; pruebas del detalle; y el equilibrio de columnas en escritorio. El #26 nace de `develop` (`763a4ba`) como `feature/ui-perfil-prestador-publico` y se abre contra esa rama, no contra `main`. Aporta nueve commits de implementación más este cambio documental: iconos de métricas, cabecera y estadísticas, tarjetas laterales, layout con barra lateral, pruebas del perfil, recorte del avatar, reseñas individuales provisionales, popover de la insignia responsable y la corrección de CI. El #27 nace de `develop` (`adad65e`) como `feature/ui-publicar-servicio-asistente` y se abre contra esa rama, no contra `main`. Aporta seis commits de implementación, el formateo de Prettier y este cambio documental: icono de subida, stepper, maquetación de los cuatro pasos, navegación del asistente, pruebas y adjunto de fotos al publicar. Rama `feature/casos-moderacion` desde `origin/develop` en `763a4ba`; siete commits de implementación más el merge de `develop` (`b1cc6c0`, que integra el #27) y cinco de la revisión —`dddf090`, `ccc0f67`, `a74c0cd`, `4a513a4` y `803a6f1`— y PR hacia `develop`. El #29 nace de `develop` (`6f7fd7c`) como `feature/pantalla-mensajes-chat` y se abre contra esa rama, no contra `main`. Aporta la pantalla dedicada de mensajes, las pruebas, el formateo de Prettier y este cambio documental. El #30 nace de `develop` (`6b0f8d9`) como `feature/pantalla-configuracion-nueva` y se abre contra esa rama, no contra `main`. Aporta tres commits de implementación más este cambio documental: layout con barra lateral, correo de la sesión y pestañas. El #31 nace de `develop` (`5ec54b9`) como `feature/dashboard-usuario` y se abre contra esa rama, no contra `main`. Aporta ocho commits de implementación más este cambio documental: TarjetaMetrica, página de panel, tareas y banner, ruta `/panel`, pruebas, formato, especialización por rol y restricción a prestadores. El #32 nace de `develop` (`4bacea4`) como `feature/actualizar-readme` y se abre contra esa rama, no contra `main`. Aporta la síntesis ejecutiva del estado actual en el README y la actualización de esta matriz. El #33 (P10A) nace de `develop` en `4d6c2d6` como `feature/admin-casos-moderacion`, con ocho commits convencionales y sin merge locales. Este incremento está detallado en «Revisión y resolución administrativa de casos de P10A». El #34 nace de `develop` (`769218b`) como `feature/ui-perfil-prestador-privado` y se abre contra esa rama, no contra `main`. Aporta el rediseño del perfil privado y este cambio documental. |
| 5 | Matriz de cumplimiento mantenida | En progreso | P1 → P11 | #3, #5, #7, #9, #10, #12, #14, #16, #17, #18, #20, #21, #22, #23, #24, #26, #27, #28, #29, #30, #31, #32, #33, #34 | `eb77733`, `61b4af6`, `d74d2da`, `10ff485`, `b42a542`, `44f2c11`, `3ea4ef2`, `6ac22de`, `d3b85bb`, `29b07d1`, `e594fc0`, `26cec56`, `aec58f9`, `7b5abb1`, `ee75593`, `ed7b0d3`, `dddf090`, `ccc0f67`, `a74c0cd`, `4a513a4`, `803a6f1`, `44490f8`, `82973e1` | — | Este documento, creado en P1 y actualizado por cada PR. El #7 lo mantuvo con cuatro commits documentales: `d74d2da` abrió «Seguridad de la cuenta de P3» y puso las filas 7, 9 y 10 en su estado real; `10ff485` cambió el marcador del PR por `#7` y anotó su CI; `b42a542` registró la revisión correctiva y dejó por escrito los dos pendientes del segundo factor; `44f2c11` anotó el CI de esa revisión. Sobre ese commit final, [ejecución 32800192184](https://github.com/robertofabiot/moica-hackathon/actions/runs/32800192184) y [ejecución 32800192187](https://github.com/robertofabiot/moica-hackathon/actions/runs/32800192187) quedaron en verde. Las filas 3, 4 y 5 no se actualizaron dentro del #7: se completaron después de integrarlo, en un cambio documental aparte. El #9 abre «Perfil y portafolio de P4» con sus controles y su recorrido manual. La carga real contra R2 se declaró primero como **no comprobada**, sin darla por hecha; la revisión de `robertofabiot` la ejecutó contra el bucket `moica-publico-dev` y la fila se actualizó con lo que esa revisión documenta, sin extenderla a lo que no cubrió. El #10 abre «Verificación documental de P4V» con el mismo criterio: el bucket privado se declaró entonces **no comprobado contra R2 real** y se documentó el comportamiento sin esas variables. El 28 de agosto de 2026 se ejecutaron en local los pasos 3 y 4 del bucket público contra `moica-publico-dev`, y el #12 ejecutó los diez pasos del bucket privado contra `moica-privado-dev`. El #14 abre «Sistema de diseño y maqueta de acceso» y actualiza las filas 3, 4 y 5. El #16 abre «Servicios publicados y descubrimiento de P5». El #17 abre «Ciclo e historial de solicitudes de P6» y actualiza las filas 1, 3, 4, 5, 6, 7 y 11. El #18 abre «Maqueta de configuración de la cuenta» y actualiza las filas 3, 4 y 5. Sus seis capturas de `/seguridad` —dos por cada tamaño— quedaron adjuntas al PR; la medición de desbordamiento no se registró y no se anota como hecha. El #20 incorporó el plan del MVP y corrigió esa evidencia caducada. El #21 abre «Chat y contactos de P7» y actualiza las filas 1, 4, 5, 6, 7 y 11. El #22 abre «Pie de página institucional y componentes base» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedan pendientes y no se anotan como hechas. El #23 abre «Exploración pública de servicios» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedan pendientes y no se anotan como hechas. El #24 abre «Detalle público de servicio» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedan pendientes y no se anotan como hechas. El #26 abre «Perfil público del prestador» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedaron adjuntas al PR. El #27 abre «Asistente de publicación de servicio» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedaron adjuntas al PR. Esta seccion y las filas de P9. El #29 abre «Pantalla dedicada de mensajes» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedaron adjuntas al PR. El #30 abre «Configuración de la cuenta con sesión real» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedaron adjuntas al PR. El #31 abre «Panel de actividad del prestador» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedaron adjuntas al PR. El #32 abre «Síntesis del estado actual en el README» y actualiza las filas 1, 4 y 5. El #33 abre «Revisión y resolución administrativa de casos de P10A» y actualiza las filas 1, 3, 4, 5, 6 y 7. Registra además el cierre de las tres omisiones del catálogo de errores que P9 había dejado anotadas. El #34 abre «Perfil privado del prestador» y actualiza las filas 3, 4 y 5. Las capturas a tres tamaños quedaron adjuntas al PR. |
| 6 | Validación de entradas y manejo uniforme de errores | Cumplido | P2 → P7 | #5, #9, #10, #16, #17, #21, #28, #33 | `fe1ab99`, `e2be568`, `525695c`, `b951af8`, `91a4117`, `b48f9de`, `b6fd613`, `e594fc0`, `26cec56`, `aec58f9`, `7b5abb1`, `ee75593`, `ed7b0d3`, `73ca819`, `82973e1` | 17 pruebas de la política de contraseña sobre el DTO; 13 pruebas de integración de registro con casos negativos; 39 pruebas del frontend | Bean Validation en los DTO más un manejador global que traduce cualquier fallo —incluidos los de Spring MVC— a un cuerpo único (`instante`, `estado`, `codigo`, `mensaje`, `ruta` y, en validación, `errores` por campo). Los rechazos de la cadena de seguridad usan ese mismo cuerpo. Ninguna respuesta lleva trazas, SQL ni valores internos. P4 extiende ese formato a las cargas de archivo: tamaño, tipo declarado y firma binaria real se validan en el backend con sus propios códigos, y un fallo del proveedor de almacenamiento sale como un 503 uniforme que no revela endpoint, credenciales ni bucket. P4V hace lo mismo con los documentos del expediente —`DOCUMENTO_NO_ADMITIDO`, `DOCUMENTO_DEMASIADO_GRANDE`, `EXPEDIENTE_INCOMPLETO`— y añade los conflictos del flujo con su propio código: `SOLICITUD_ABIERTA_DUPLICADA`, `SOLICITUD_YA_TOMADA`, `NIVEL_YA_VIGENTE`, `VERIFICACION_BASICA_REQUERIDA` y `TRANSICION_NO_PERMITIDA`. Ninguna respuesta lleva claves de almacenamiento ni URL prefirmadas. P5 añade `SUBCATEGORIA_NO_DISPONIBLE` (400) y `PRESTADOR_NO_DISPONIBLE` (409), reutiliza `VERIFICACION_BASICA_REQUERIDA` al activar y conserva `precioReferencia: null` en la API: «A convenir» es solo presentación. P6 añade `SERVICIO_PROPIO`, `SERVICIO_INACTIVO` y `MOTIVO_OBLIGATORIO`, reutiliza `VERIFICACION_BASICA_REQUERIDA`, `PRESTADOR_NO_DISPONIBLE`, `TRANSICION_NO_PERMITIDA` y `CUENTA_RESTRINGIDA`, y valida descripción, municipio, ubicación y motivo en la frontera. P7 añade `CHAT_NO_HABILITADO`, `CHAT_SOLO_LECTURA` y `CONTACTOS_NO_REVELADOS`, reutiliza `CUENTA_RESTRINGIDA` y `RECURSO_NO_ENCONTRADO`, y valida el contenido del mensaje en la frontera: se recorta antes de validarlo, `@NotBlank` rechaza el mensaje en blanco y `@Size` el que pasa de 2000 caracteres. La base lo respalda con `ck_mensaje_solicitud_contenido`. `ReporteAPresentar` valida motivo (120) y descripcion (3000) con Bean Validation y recorta los dos antes de validarlos; los rechazos salen por el formato uniforme con `VALIDACION`, `SOLICITUD_NO_REPORTABLE` y `REPORTE_DUPLICADO`. P10A añade `CASO_NO_ENCONTRADO` (404), `ADMINISTRADOR_NO_VALIDO` (400), `CASO_SIN_RESPONSABLE` (409) y `CASO_DE_OTRO_ADMINISTRADOR` (403), y reutiliza `TRANSICION_NO_PERMITIDA` y `ACCESO_DENEGADO`. `ResolucionDeCaso` valida el resultado y la resolución en la frontera y recorta el texto antes de validarlo, igual que hace el reporte. El mismo PR completa el catálogo final del contrato con `SUBCATEGORIA_NO_DISPONIBLE`, `SOLICITUD_NO_COMPLETADA` y `CALIFICACION_DUPLICADA`, que faltaban desde el #17 y el #25. Este incremento está detallado en «Revisión y resolución administrativa de casos de P10A». |
| 7 | Protección de rutas y datos (rol, propiedad, estado de cuenta) | En progreso | P3 → P10B | #7, #9, #10, #16, #17, #21, #28, #33 | `14a2d1a`, `ce9cfcc`, `bc4bfeb`, `1b1cc1f`, `2b7bf91`, `4b4e3f0`, `5bdbd43`, `70b9063`, `83597a3`, `17531b9`, `b951af8`, `d15c888`, `dc0df7a`, `214d734`, `e594fc0`, `26cec56`, `aec58f9`, `7b5abb1`, `ee75593`, `ed7b0d3`, `73ca819`, `beff40a` | 47 pruebas de integración de P4 sobre propiedad y estado de cuenta —13 de `PerfilPrestadorIT`, 8 de `MediosDeContactoIT`, 14 de `PortafolioIT` y 12 de `ImagenDePerfilIT`—, más 11 de `AreaAdministrativaIT`, 14 de `SesionProvisionalIT`, `CambioDeClaveIT.separaSinSesionDeSesionQueNoAlcanzaYDeContrasenaEquivocada`, 10 del frontend en `PanelAdministrativo.test.tsx` y 5 en `useVigilanciaDeSesion.test.tsx` | La cadena de seguridad cierra por omisión: lo que no se declara exige una sesión plena. `UsuarioAutenticado` relee en cada petición el rol, el estado de la cuenta y el segundo factor, así que retirar un permiso surte efecto en la petición siguiente. `/api/admin/**` exige rol administrativo **y** segundo factor verificado en esa sesión; una suspensión bloquea todo salvo consultar y cerrar la sesión. La propiedad del recurso se resuelve sin parámetros: cada endpoint de P3 opera sobre la cuenta de la sesión. En el navegador, terminar una sesión descarta toda la caché remota salvo la propia sesión, así que la cuenta que entra después no puede ver nada de la anterior. P4 aplica esa misma regla a todo lo del prestador: ninguna ruta lleva identificador de cuenta, así que el propietario siempre sale de la sesión; un recurso ajeno responde 404 y no 403 para no permitir enumerar identificadores; y una cuenta `RESTRINGIDA_TEMPORAL` conserva la lectura de lo suyo pero no puede modificarlo. Los contactos siguen ocultos para terceros y no se abrió ninguna superficie pública. P4V añade el caso más delicado: los documentos de identidad. El propietario envía y consulta metadatos, pero **no puede descargar sus propios archivos**; solo un administrador con segundo factor verificado en esa sesión abre uno, y con un acceso temporal que caduca y se autoriza en cada petición. Una solicitud ajena responde 404. Dentro del área administrativa hay una segunda capa: solo quien tomó una revisión puede aprobarla o rechazarla, y una toma concurrente choca con un 409 porque la fila se bloquea. P5 abre únicamente `GET` públicos de catálogos, listado, detalle y perfil; un recurso ajeno o no visible responde 404. Las escrituras de servicios exigen cuenta `ACTIVA` y propiedad. Un prestador `NO_DISPONIBLE` con verificación básica y cuenta operativa conserva el perfil y el portafolio públicos, pero `servicios` sale vacío y `admiteContratacion` es falso; el listado y el detalle público tampoco muestran esos servicios. P6 aplica la misma propiedad a las solicitudes: un tercero recibe 404; una cuenta restringida consulta y cancela, pero no crea, acepta, rechaza ni completa —esas cuatro acciones exigen cuenta `ACTIVA`—; una suspendida queda en 403 `ACCESO_DENEGADO`. Las transiciones se autorizan en el backend. P7 añade la superficie más delicada hasta ahora: el hilo y los contactos. Un tercero recibe 404 en las tres rutas y no puede confirmar que existan. El hilo solo existe si la solicitud **llegó a estar `ACEPTADA`**, cosa que se resuelve mirando el historial y no el estado vigente: una cancelación desde `PENDIENTE` nunca abre chat. Escribir exige cuenta `ACTIVA` y estado `ACEPTADA`; una restringida lee y no escribe; una suspendida queda en 403 `ACCESO_DENEGADO`. En `/contactos` el prestador también recibe 404: la revelación pertenece al cliente, y esa ruta responde 200 a una sola persona. El remitente sale siempre de la sesión y un `idRemitente` enviado en el cuerpo se ignora. La moderación llega en P10B. El reportante sale de la sesion y el reportado de la solicitud; un tercero recibe 404 y no puede enumerar; una cuenta `RESTRINGIDA_TEMPORAL` conserva el reporte y una suspendida no llega al controlador; nadie ve el caso de la contraparte. P10A protege toda la superficie administrativa de casos con rol **y** segundo factor verificado en esa sesión, en lectura y en escritura, y limita el acceso al chat al contexto de un caso: no existe ruta administrativa colgada de la solicitud. Iniciar la revisión y cerrar exigen además ser el responsable asignado. Este incremento está detallado en «Revisión y resolución administrativa de casos de P10A». |
| 8 | Verificación documental de prestadores en dos niveles | Cumplido | P4V | #10 | `8f82231`, `33cd1f7`, `91a4117`, `b48f9de`, `669875f`, `cd68f8f`, `27ae045`, `d096558` | 109 pruebas nuevas del backend —40 unitarias del almacenamiento privado y 69 de integración: 13 de `EsquemaDeVerificacionIT`, 27 de `EnvioDeExpedienteIT`, 20 de `RevisionDeVerificacionIT` y 9 de `RevocacionDeVerificacionIT`— y 29 del frontend: 16 en `Verificacion.test.tsx` y 13 en `ColaDeVerificaciones.test.tsx`. Recorrido manual completo contra el backend local con PostgreSQL real | Los dos niveles y sus cinco estados, con revisión **siempre manual**: ninguna transición ocurre sin una petición de una cuenta administrativa con segundo factor verificado en esa sesión. La básica exige un documento de identidad; la profesional, una básica vigente y un respaldo que no sea identidad. El expediente se envía completo en una sola operación —no existe `BORRADOR`— y un fallo a mitad no deja ni solicitud ni archivos huérfanos. Revocar la básica deja `SIN_VERIFICAR` y anula la profesional en la misma transacción, con el mismo motivo, administrador e instante; esa profesional no revive al obtener otra básica. Las solicitudes y sus documentos resueltos se conservan como evidencia. Los archivos viven en un bucket privado con su propio token; PostgreSQL guarda clave opaca y metadatos, nunca el binario ni una URL, y el archivo solo se abre con un acceso temporal autorizado en cada petición. Detalle en «Verificación documental de P4V» |
| 9 | Autenticación de dos factores (TOTP) | Cumplido | P3 | #7 | `14a2d1a`, `ce9cfcc`, `bc4bfeb`, `1b1cc1f`, `2b7bf91`, `4b4e3f0`, `5bdbd43`, `f290d6b`, `8cf5957` | 22 pruebas de integración de `SegundoFactorIT`, 14 de `SesionProvisionalIT`, 9 unitarias de `AlgoritmoTotpTest` con reloj fijo, 7 de `CifradoDeSecretosTest`, 10 de `PropiedadesDeSegundoFactorTest`, 5 de `SegundoFactorUsuarioTest`, 5 de `RepresentacionSinSecretosTest` y 28 del frontend entre `SeguridadCuenta.test.tsx` y `VerificacionSegundoFactor.test.tsx` | Ciclo completo `PENDIENTE_ACTIVACION` → `ACTIVO` → `DESACTIVADO`, uno por cuenta (lo garantiza la clave primaria compartida). El algoritmo es RFC 6238 mediante `java-otp`; los dígitos, el periodo y la tolerancia viven solo en `moica.segundo-factor.*`. El secreto se genera con `SecureRandom`, se guarda cifrado con AES-GCM y nonce aleatorio, y se entrega una única vez al iniciar la activación; la respuesta que lo lleva pide `no-store` y el navegador lo descarta al dejar la pantalla. Obligatorio para el rol administrativo, opcional para el resto. **Pendiente de decisión del equipo:** un código aceptado admite reutilización dentro de su ventana y los intentos fallidos no están limitados (ver «Segundo factor: reutilización de código e intentos»). |
| 10 | Expiración y revocación de sesión | Cumplido | P2 → P3 | #5, #7 | `6f09fdd`, `b3bcfcc`, `feff7ef`, `14a2d1a`, `1b1cc1f`, `2b7bf91`, `83597a3` | 10 pruebas de integración de `CicloDeSesionIT`, 13 de `CambioDeClaveIT`, 14 de `SesionProvisionalIT`, 6 unitarias de `TokenDeSesionServiceTest` y 5 del frontend en `useVigilanciaDeSesion.test.tsx`; recorrido manual con la base de datos a la vista | Cada login crea una fila `sesion` con expiración de siete días configurable; el JWT solo la señala con su `jti` y su `exp` nunca la supera. Cada petición comprueba la fila: expirada o revocada responde 401 aunque el token siga vigente. Cerrar sesión registra `CIERRE_VOLUNTARIO`. P3 añade la revocación por `CAMBIO_CREDENCIALES`: cambiar la contraseña o desactivar el segundo factor revoca en una sola operación todas las sesiones de la cuenta, incluida la actual, apoyándose en el índice `ix_sesion_id_usuario`. En el navegador, la vigilancia de la sesión vive en `App` y no en una pantalla: vence, se revoca o se pierde igual en `/`, en `/seguridad` y en `/admin`, y un 401 de cualquier consulta autenticada la da por terminada. La revocación por medida administrativa llega en P10B. |
| 11 | Preparación para producción (contenedores, configuración por entorno, migraciones, healthcheck) | En progreso | P1 → P11 | #3, #9, #10, #12, #16, #17, #21, #28 | `78518ff`, `286ca5f`, `715fd3d`, `0f464d2`, `525695c`, `33cd1f7`, `e594fc0`, `26cec56`, `aec58f9`, `7b5abb1`, `ee75593`, `ed7b0d3` | `./mvnw verify` en CI; arranque local con Docker Compose | Configuración por variables de entorno comprobada en local incluso con el puerto 5432 ocupado, Flyway aplicando migraciones versionadas sobre PostgreSQL real y `GET /actuator/health` respondiendo `UP`. P4 agrega la configuración del almacenamiento de objetos por entorno: sin las variables `MOICA_R2_*` la aplicación arranca igual y solo las imágenes responden 503, mientras que una configuración a medias detiene el arranque con un mensaje que no revela ningún valor. La conexión con un bucket R2 real quedó comprobada en la revisión del #9 sobre `moica-publico-dev`: configuración, carga de imagen de perfil y de portafolio y persistencia de la URL. El 28 de agosto de 2026 se ejecutaron además los pasos 3 y 4 contra ese mismo bucket: sustituir cambia la URL y deja 404 el objeto anterior; eliminar deja `urlImagenPerfil` en `null` y 404 las dos URLs de la prueba. P4V agrega la segunda superficie con la misma política y dos límites propios: `MOICA_DOCUMENTO_TAMANO_MAXIMO` no admite más de 5 MB —es el tope de `ck_documento_verificacion_tamano`— y `MOICA_DOCUMENTO_URL_TEMPORAL_DURACION` no admite más de una hora; cualquiera de los dos por encima detiene el arranque. El 28 de agosto de 2026 el #12 ejecutó los diez pasos de `Almacenamiento.md` contra `moica-privado-dev`: sin lectura anónima, carga de los tres formatos, clave opaca en PostgreSQL, 403 al propietario y 404 a un ajeno, 302 con acceso temporal de cinco minutos que R2 acepta y luego rechaza, y compensación `503 ALMACENAMIENTO_NO_DISPONIBLE` sin filtrar el proveedor. P5 agrega `V31` y `V90` en el rango reservado, sin variables de entorno nuevas: las imágenes de servicio reutilizan el bucket público y el prefijo `servicios/`. P6 agrega `V40` en el rango `V40`–`V49`, también sin variables nuevas. P7 agrega `V41` en ese mismo rango, igualmente sin variables ni dependencias nuevas. Imágenes de producción, despliegue y proveedor corresponden a P11. Migraciones `V50` y `V51`, aplicadas por Flyway *out of order* sobre una base que estaba en `v90`. |

## Base técnica de P1

Controles que P1 dejó funcionando, con el resultado real de cada comprobación.

- **Local**: ejecutado en la máquina de desarrollo (Windows 11, Docker Desktop,
  Node 22, JDK compilando con `release 21`).
- **CI**: ejecutado por GitHub Actions en el Pull Request #3,
  [ejecución 31866681479](https://github.com/robertofabiot/moica-hackathon/actions/runs/31866681479),
  con los cuatro checks en verde.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Finales de línea normalizados | `git add --renormalize .` no produce cambios | Sí | | Ejecutado antes de `9600ace`: el índice ya estaba en LF y no se reescribió ningún archivo |
| Sin secretos versionados | `git ls-files` y revisión del diff | Sí | | El único archivo de entorno versionado es `.env.example`; `.env` está ignorado |
| Formato del backend | `./mvnw spotless:check` | Sí | Sí | «keeping 2 files clean, 0 needs changes» |
| Análisis estático del backend | `./mvnw spotbugs:check` | Sí | Sí | «BugInstance size is 0» |
| Entorno local con Docker | `docker compose config` y `docker compose up -d` | Sí | Sí | Local: PostgreSQL y pgAdmin levantan saludables. CI: trabajo «Entorno local» valida el archivo con `docker compose config -q` a partir de `.env.example` |
| Arranque del backend contra PostgreSQL real | Local: `./mvnw spring-boot:run` contra el contenedor. CI: `./mvnw verify` con Testcontainers | Sí | Sí | Local: el backend inicia y se conecta a PostgreSQL. CI: 4 pruebas de `ArranqueConPostgresIT` en verde |
| Healthcheck | `GET http://localhost:8080/actuator/health` | Sí | Sí | Local: responde estado `UP`. CI: prueba `elHealthcheckReportaLaBaseDeDatosDisponible` |
| Flyway habilitado | Arranque de la aplicación | Sí | Sí | Local: crea `public.flyway_schema_history`, que en P1 quedaba con 0 filas porque `db/migration` todavía no tenía migraciones de aplicación. CI: «Successfully applied 1 migration to schema public, now at version v1», aplicando la migración aislada del classpath de pruebas. P2 sustituye ese montaje por la migración real `V10` |
| Formato del frontend | `npm run format:check` | Sí | Sí | «All matched files use Prettier code style» |
| Lint del frontend | `npm run lint` | Sí | Sí | En verde; comprobado además que un `any` lo hace fallar con código de salida 1 |
| Tipos del frontend | `npm run typecheck` | Sí | Sí | En verde |
| Pruebas del frontend | `npm run test` | Sí | Sí | 3 de 3 pruebas de navegación en verde |
| Instalación de dependencias reproducible | `npm ci` | Sí | Sí | Instala desde el lockfile; 0 vulnerabilidades |
| Build y PWA del frontend | `npm run build` | Sí | Sí | Genera el manifiesto, el service worker y los archivos de Workbox |
| Navegación e interfaz responsiva | Local: Chrome a 375x812 sobre el build de producción. CI: Vitest | Sí | Sí | Local: la pantalla base se ve correctamente, la ruta inexistente muestra la pantalla 404 y su enlace devuelve al inicio; consola e informe de Issues de Chrome sin errores. CI: las 3 pruebas de navegación |
| Instalación de la PWA | Chrome, sobre `npm run preview` | Sí | | Service worker activado y en ejecución; manifiesto con nombre y descripción de Moica, modo `standalone`, iconos de 192 y 512 píxeles y ninguna orientación forzada; la aplicación se instaló y abrió como aplicación independiente |
| Integración continua | Los checks del PR quedan en verde | | Sí | Backend, Frontend, Entorno local y Convenciones en verde en el PR #3 |
| Convenciones de commits y títulos | `.github/scripts/validar-convenciones.sh` | Sí | Sí | Local: rechaza tipo en mayúscula, descripción en mayúscula, punto final y encabezado sin tipo. CI: check en verde sobre los commits del PR |

### Nota del entorno local

En la máquina donde se validó, el PostgreSQL nativo de Windows ya ocupaba el
puerto 5432, así que el contenedor publicó la base en `localhost:5433`
conservando el 5432 interno. Solo hizo falta cambiar `MOICA_DB_PORT` en `.env`:
ni el código ni `docker-compose.yml` se tocaron. Es la comprobación de que la
externalización de variables cumple su función.

## Ciclo de acceso de P2

Comprobaciones del incremento P2, con el resultado real de cada una.

- **Local**: máquina de desarrollo (Windows 11, Docker Desktop, Node 22, JDK
  compilando con `release 21`), con PostgreSQL publicado en `localhost:5433`.
- **CI**: ejecutado por GitHub Actions en el Pull Request #5 sobre el commit
  final `7045359`,
  [ejecución 32683435097](https://github.com/robertofabiot/moica-hackathon/actions/runs/32683435097),
  con Backend, Frontend y Entorno local en verde, y
  [ejecución 32683435102](https://github.com/robertofabiot/moica-hackathon/actions/runs/32683435102)
  con el check de convenciones también en verde.

| Control | Cómo se comprueba | Local | Evidencia |
|---|---|---|---|
| Migración del esquema de identidad | `./mvnw verify` | Sí | «Migrating schema "public" to version "10 - crear usuario y sesion"» y «Successfully applied 1 migration». `EsquemaDeIdentidadIT` comprueba contra PostgreSQL real las restricciones del diccionario: dominio de `estadoCuenta`, unicidad del correo, `fechaExpiracion > fechaInicio`, revocación con fecha y motivo a la vez, dominio de `motivoRevocacion`, clave foránea y borrado en cascada |
| Contraseña guardada solo como hash | Consulta directa a la base tras registrarse desde el navegador | Sí | `SELECT left(clave_hash, 7), length(clave_hash)` devuelve `$2a$10$` y 60: es un hash de BCrypt, no la contraseña |
| Correo normalizado | Registro desde el navegador escribiendo « Erving@Moica.TEST » | Sí | La fila persiste `erving@moica.test`; repetir el correo con otras mayúsculas o espacios responde 409 `CORREO_YA_REGISTRADO` |
| Política de contraseña | `./mvnw test` y formulario del navegador | Sí | 17 pruebas sobre el DTO cubren la falta de mayúscula, minúscula, número o símbolo, los dos extremos de longitud y el límite real de BCrypt en bytes. En el navegador, «moica2026» se rechaza con «Debe incluir al menos una letra mayúscula» sin llegar a la API |
| Sesión de siete días | Consulta directa tras iniciar sesión | Sí | `fecha_expiracion - fecha_inicio` devuelve `7 days`; el identificador del token mide 43 caracteres y `segundo_factor_verificado` nace en `false` |
| Cookie de sesión inaccesible para JavaScript | `document.cookie` en el navegador con la sesión iniciada | Sí | Solo aparece `XSRF-TOKEN`; `moica_sesion` no es visible. La cabecera es `HttpOnly; SameSite=Lax; Path=/; Max-Age=604800` |
| El JWT no sobrevive a su sesión | `./mvnw verify` | Sí | `elJwtNoValeMasTiempoQueLaSesionPersistida` y `elJwtSenalaLaFilaDeSesionMedianteSuJti` |
| Rechazo de sesión expirada y revocada | `./mvnw verify` | Sí | `CicloDeSesionIT` responde 401 con la fila expirada y con la fila revocada, en ambos casos con el JWT todavía vigente en el navegador |
| Cierre de sesión | Recorrido en Chrome y consulta a la base | Sí | Con red, la fila queda con `motivo_revocacion = CIERRE_VOLUNTARIO` y la siguiente petición autenticada responde 401. Un 204 o un 401 limpian el estado local; un fallo de red, un 403, un 500 o un tiempo de espera agotado conservan la sesión, cortan «Cerrando sesión…» y permiten reintentar. El fallo de `615cba3` en Chrome Offline (mutación pendiente indefinida) quedó corregido en `7045359` y comprobado sobre ese commit: sin conexión no se dispara ningún `DELETE` ni siquiera pasados 12 s —más que el tiempo de espera de 10 s—, la fila sigue sin revocar y el botón vuelve a «Cerrar sesión» habilitado; al volver Online el reintento ejecuta exactamente **un** `DELETE` |
| Credenciales incorrectas | `./mvnw verify` y recorrido en el navegador | Sí | Un correo inexistente y una contraseña incorrecta devuelven exactamente el mismo cuerpo, y ninguno crea sesión |
| Protección CSRF | `curl` a través del proxy de Vite | Sí | `POST /api/usuarios` sin la cabecera `X-XSRF-TOKEN` responde 403 y no crea la cuenta; con el token responde 201. Lo mismo al iniciar y cerrar sesión |
| Errores uniformes | `curl` y pruebas de integración | Sí | Un cuerpo inválido devuelve `instante`, `estado`, `codigo`, `mensaje`, `ruta` y `errores` por campo; un 401 devuelve el mismo cuerpo sin `errores`. Ninguna respuesta lleva trazas ni SQL |
| Pruebas del backend | `./mvnw verify` | Sí | 32 pruebas unitarias y 45 de integración en verde, con Spotless y SpotBugs limpios. Además, `./mvnw verify` ejecutado por separado sobre cada uno de los cinco commits del backend |
| Pruebas del frontend | `npm run test` | Sí | 39 pruebas en verde: API (incluido `fetch` colgado sin aborto), formularios, navegación, aviso de sesión vencida y los casos de cierre (204, 401, sin conexión sin `mutate`, 403, 500, tiempo agotado, `navigator.onLine` y reintento) |
| Cadena completa del frontend | `format:check`, `lint`, `typecheck`, `test` y `build` | Sí | Todo en verde; el build vuelve a generar el manifiesto y el service worker |
| Interfaz responsiva | Chrome a 375x812, 768x1024 y 1280x800 sobre `7045359` | Sí | El formulario se centra con un máximo de 26 rem y los accesos de la pantalla de inicio se apilan en teléfono y se ponen en fila a partir de 48 rem. En las nueve combinaciones de tamaño y pantalla, `scrollWidth` es igual a `clientWidth`: no hay desbordamiento horizontal |
| Sin secretos versionados | Revisión del diff antes de subir | Sí | El único archivo de entorno versionado sigue siendo `.env.example`. `MOICA_JWT_SECRETO` se documenta allí con un valor de desarrollo marcado como público y con instrucciones para generar uno real |

### Recorrido manual comprobado

Con el backend en `localhost:8080`, el frontend servido por Vite (en
`localhost:5173`, o en el puerto libre siguiente si ya hay un servidor de
desarrollo en marcha) y PostgreSQL en el contenedor:

1. Registro escribiendo el correo con mayúsculas y espacios sobrantes: la cuenta
   se crea y la aplicación lleva a iniciar sesión avisando de que quedó creada.
2. Inicio de sesión con la contraseña equivocada: «El correo o la contraseña no
   son correctos», sin crear sesión.
3. Inicio de sesión correcto: la pantalla de inicio saluda por el nombre y
   ofrece cerrar sesión.
4. Cierre de sesión: la fila queda revocada como `CIERRE_VOLUNTARIO` y la
   aplicación vuelve a la pantalla de acceso.
5. Cierre sin conexión, con Chrome en Offline: no queda ninguna mutación
   pendiente. El botón vuelve de inmediato a «Cerrar sesión» habilitado, la
   pantalla conserva «Sesión iniciada como …», la cookie `moica_sesion` sigue
   presente y aparece «No pudimos comunicarnos con Moica. Revisa tu conexión e
   inténtalo otra vez». No se navega a iniciar sesión. En el panel de red no se
   registra ningún `DELETE`, ni siquiera pasados 12 s —más que el tiempo de
   espera de 10 s del cliente—, y la fila de sesión sigue sin revocar.
6. Volver a Online: antes de reintentar, `GET /api/auth/sesion` responde 200,
   así que la persona sigue autenticada. Al pulsar «Cerrar sesión» se ejecuta
   exactamente un `DELETE /api/auth/sesion`, la aplicación navega a
   `/iniciar-sesion` sin aviso de sesión vencida, la cookie `moica_sesion`
   desaparece y la fila queda revocada con `CIERRE_VOLUNTARIO`; la siguiente
   petición autenticada responde 401.

   El fallo que `615cba3` mostraba en Chrome Offline —«Cerrando sesión…»
   indefinido pese a `navigator.onLine === false`— venía de que `mutate()`
   activaba la mutación antes de poder cortar sin red y de que el tiempo de
   espera dependía del evento `abort` mientras `fetch` a `localhost` quedaba
   colgado. `7045359` lo corrige con `solicitarCierre()` (sin `mutate` offline),
   rechazando la carrera desde el temporizador y acotando la lectura del cuerpo
   de error.

La comprobación responsiva se realizó en Chrome a 375x812, 768x1024 y 1280x800
sobre el commit `7045359`. Las nueve capturas de `/registro`, `/iniciar-sesion`
e `/` con sesión están fuera del repositorio, en
`C:\Users\ervin\Desktop\moica-pr5-capturas-p2`, cada una con el tamaño exacto
que indica su nombre. La evidencia del cierre sin conexión va aparte, en
`C:\Users\ervin\Desktop\moica-pr5-capturas-p2\evidencia-offline`, para no
mezclarla con el recorrido responsivo normal. Ninguna imagen se versiona.

## Seguridad de la cuenta de P3

Comprobaciones del incremento P3, con el resultado real de cada una.

- **Local**: máquina de desarrollo (Windows 11, Docker Desktop, Node 22, JDK
  compilando con `release 21`), con PostgreSQL publicado en `localhost:5433`.
- **CI**: ejecutado por GitHub Actions en el Pull Request #7. Sobre el commit
  `d74d2da`,
  [ejecución 32792478119](https://github.com/robertofabiot/moica-hackathon/actions/runs/32792478119),
  con Backend, Frontend y Entorno local en verde, y
  [ejecución 32792478085](https://github.com/robertofabiot/moica-hackathon/actions/runs/32792478085)
  con el check de convenciones también en verde. Sobre `b42a542`, que cierra la
  revisión correctiva,
  [ejecución 32799768721](https://github.com/robertofabiot/moica-hackathon/actions/runs/32799768721)
  y
  [ejecución 32799768722](https://github.com/robertofabiot/moica-hackathon/actions/runs/32799768722),
  las dos en verde.

| Control | Cómo se comprueba | Local | Evidencia |
|---|---|---|---|
| Migración del esquema de seguridad | `./mvnw verify` y arranque local | Sí | «Migrating schema "public" to version "11 - crear administrador y segundo factor"» sobre una base que ya tenía `V10`. `EsquemaDeSeguridadIT` comprueba contra PostgreSQL real las 13 restricciones del diccionario: clave primaria compartida que impide dos roles o dos segundos factores por cuenta, claves foráneas, borrado en cascada, dominio de `estadoSegundoFactor`, la regla «`ACTIVO` exige fecha de activación» y la existencia de `ix_sesion_id_usuario` |
| Secreto TOTP cifrado en la base | Consulta directa tras activar el segundo factor desde el navegador | Sí | `SELECT left(secreto_totp,24), length(secreto_totp)` devuelve 80 caracteres de Base64 que no contienen el secreto; `secreto_totp ~ '^[A-Z2-7]{32}$'` da `false`, así que no es Base32 en claro. `SegundoFactorIT.guardaElSecretoCifradoYNuncaEnClaro` lo descifra con la clave del entorno y recupera exactamente la clave manual entregada |
| Códigos TOTP con reloj controlable | `./mvnw test` | Sí | `AlgoritmoTotpTest` usa `Clock.fixed`: acepta el periodo en curso y los de tolerancia, rechaza dos periodos atrás y adelante, y con tolerancia 0 solo vale el actual. Ninguna prueba espera 30 segundos reales |
| Bootstrap administrativo idempotente | Reinicio real del backend con `MOICA_ADMIN_CORREO` definida | Sí | Antes: sin filas en `administrador`. Al reiniciar: «Rol administrativo asignado a la cuenta indicada en MOICA_ADMIN_CORREO. Para entrar en /admin debe activar su segundo factor TOTP.», sin el correo en el mensaje, y la fila aparece con su `fecha_asignacion`. `BootstrapDeAdministradorIT` cubre además repetirlo, la variable vacía y el correo sin cuenta; `ArranqueConAdministradorInexistenteIT` levanta un contexto con la variable apuntando a una cuenta inexistente y demuestra que la aplicación arranca igual |
| Cambio de contraseña | Recorrido con `curl` a través del backend y consulta a la base | Sí | Con la sesión provisional responde 403; tras verificar el segundo factor responde 204. Las tres sesiones vigentes de la cuenta quedan revocadas **en el mismo instante** (`2026-08-24 23:48:55.238719+00`) con motivo `CAMBIO_CREDENCIALES`; la sesión de otra cuenta no se toca. La petición siguiente con la misma cookie responde 401, la contraseña antigua responde 401 y la nueva 201 |
| Sesión provisional | Recorrido en el navegador y `SesionProvisionalIT` | Sí | Con el segundo factor activo, el login responde 201 con `pendienteDeSegundoFactor: true`. Esa sesión solo consulta, verifica y cierra; el resto de rutas protegidas responden 403 `ACCESO_DENEGADO`. Una cuenta **sin** segundo factor abre una sesión completa aunque `segundoFactorVerificado` sea `false` |
| Verificación por sesión | `./mvnw verify` y consulta a la base | Sí | Verificar en un navegador deja `segundo_factor_verificado = true` solo en esa fila; la otra sesión vigente sigue en 403. `verificarUnaSesionNoCompletaLasDemas` lo comprueba contando las sesiones vigentes verificadas |
| Área administrativa | Recorrido real con la cuenta promovida | Sí | Con el rol asignado pero la sesión sin verificar, `GET /api/admin/resumen` responde 403; tras presentar el código, 200 con el nombre, el correo y la fecha de asignación. `AreaAdministrativaIT` recorre la tabla completa: sin sesión 401, cuenta ordinaria 403, ordinaria con TOTP verificado 403, administrador sin TOTP 403, administrador con sesión provisional 403, administrador verificado 200, y retirar el rol vuelve a cerrar el área en la petición siguiente |
| Administrador atado a su segundo factor | `./mvnw verify` | Sí | `unaCuentaAdministradoraNoPuedeDesactivarSuSegundoFactor` responde 403 `SEGUNDO_FACTOR_OBLIGATORIO` y deja el estado en `ACTIVO` |
| Secretos fuera de las representaciones de texto | `./mvnw verify` | Sí | `RepresentacionSinSecretosTest` construye cada objeto sensible con un centinela reconocible y comprueba que su `toString()` no lo contiene: contraseñas de registro, de inicio de sesión, de cambio y de desactivación, códigos TOTP, la clave manual y la URI `otpauth://`, el JWT de `SesionIniciada`, el secreto del JWT y la clave de cifrado TOTP. Lo que no es secreto —correo, nombre, dígitos, periodo— sí se describe, para que la representación siga sirviendo. Es defensa en profundidad: Moica no registra estos objetos, y esto evita que baste con interpolar uno para que un secreto acabe en un archivo |
| El secreto de activación no sobrevive a su pantalla | `npm run test` y recorrido en el navegador | Sí | El resultado de la mutación se retira de `MutationCache` al desmontar (`gcTime: 0`) y la pantalla llama además a `reset()`. Dos pruebas con el secreto de ejemplo como centinela recorren `MutationCache` y fallan si sigue ahí: una al salir a otra ruta y volver, otra al quedar el segundo factor activo. En el navegador, salir de `/seguridad` y volver muestra «Activación sin terminar» y el botón de activar, sin la clave anterior. `SegundoFactorIT.laRespuestaQueLlevaElSecretoPideQueNoSeGuardeEnNingunaCache` fija el `no-store` de la única respuesta que lleva el secreto |
| Nada de una cuenta queda en memoria para la siguiente | `npm run test` y recorrido en el navegador | Sí | Terminar una sesión descarta **toda** la caché de consultas salvo la propia sesión, y entrar vuelve a descartarla. Dos pruebas recorren A → cierre → B sin recargar, con la consulta de B todavía en curso, y comprueban que no se pinta ni el resumen administrativo ni el estado del segundo factor de A. En el navegador, una cuenta con el segundo factor `ACTIVO` cierra sesión y entra otra sin configurarlo: muestreando `/seguridad` cada 10 ms, la secuencia va del estado en blanco a «Sin configurar», sin pasar por «Activo» |
| El fin de la sesión se resuelve en cualquier ruta | `npm run test` y recorrido en el navegador | Sí | `useVigilanciaDeSesion` se monta en `App` y es el único punto que lleva a iniciar sesión cuando la sesión termina; quien la termina a propósito solo anota el motivo. Cinco pruebas: vencimiento en `/seguridad`, vencimiento en `/admin`, revocación descubierta por la consulta del segundo factor, revocación descubierta por la del área administrativa y un 401 del inicio de sesión público que **no** se confunde con una sesión perdida. Ninguna termina en «Cerrando tu sesión…». En el navegador: con la sesión revocada en la base, volver a `/admin` lleva a `/iniciar-sesion?motivo=sesion-vencida`; con `fecha_expiracion` a 25 segundos, `/seguridad` sale sola a los 12 que le quedaban; cerrar sesión a propósito llega sin motivo, que es lo correcto |
| 401 y 403 no se confunden entre sí ni por dentro | `./mvnw verify` | Sí | `CambioDeClaveIT.separaSinSesionDeSesionQueNoAlcanzaYDeContrasenaEquivocada` recorre los tres casos sobre `PUT /api/auth/clave`: sin sesión 401 `NO_AUTENTICADO`, sesión provisional 403 `ACCESO_DENEGADO`, sesión plena con la contraseña equivocada 403 `CREDENCIALES_INVALIDAS` y la sesión sigue respondiendo 200. El estado separa «ya no hay sesión» de «la hay pero no alcanza»; dentro del 403, el código dice qué falta |
| Respuestas sin secretos ni trazas | `./mvnw verify` y revisión de las respuestas del recorrido | Sí | Ninguna respuesta del ciclo contiene el secreto, ni el valor cifrado, ni hashes, ni `com.moica`, ni SQL, ni `Exception`. Consultar el segundo factor una vez activo devuelve estado, obligatoriedad y fecha, sin `claveManual` ni `otpauth` |
| 401 y 403 con el formato de siempre | `./mvnw verify` | Sí | El cuerpo uniforme se conserva: `instante`, `estado`, `codigo`, `mensaje`, `ruta` y sin `errores` fuera de validación. Códigos nuevos: `CODIGO_INVALIDO`, `SEGUNDO_FACTOR_NO_ACTIVO`, `SEGUNDO_FACTOR_YA_ACTIVO`, `SEGUNDO_FACTOR_SIN_ACTIVACION_PENDIENTE`, `SEGUNDO_FACTOR_OBLIGATORIO` y `CUENTA_SUSPENDIDA` |
| Pruebas del backend | `./mvnw verify` | Sí | 69 pruebas unitarias y 127 de integración en verde, con Spotless y SpotBugs limpios («BugInstance size is 0») |
| Pruebas del frontend | `npm run test` | Sí | 82 pruebas en verde (39 de P2 más 43 de P3): cambio de contraseña, activación y desactivación del segundo factor, verificación de la sesión provisional, accesos denegados, `/admin`, los fallos de red de cada pantalla, la vida del secreto de activación, la caché privada entre cuentas y el fin de la sesión en cada ruta |
| Cadena completa del frontend | `format:check`, `lint`, `typecheck`, `test` y `build` | Sí | Todo en verde; el build vuelve a generar el manifiesto y el service worker |
| Interfaz responsiva | Chrome sobre el commit `5bdbd43`, a 375x812, 768x1024 y 1280x800 | Sí | En las 18 capturas, `scrollWidth` es igual a `clientWidth`: no hay desbordamiento horizontal en ninguna de las pantallas nuevas. La clave manual del segundo factor se parte en lugar de desbordar y el QR se limita a 12 rem. La revisión correctiva no cambió ningún elemento visible, así que las capturas siguen valiendo; aun así se repitió la medida en `/seguridad` con la activación abierta a los tres tamaños, y `scrollWidth` volvió a coincidir con `clientWidth` (375/375, 753/753 y 1265/1265) |
| Sin secretos versionados | Revisión del diff antes de subir | Sí | El único archivo de entorno versionado sigue siendo `.env.example`. `MOICA_TOTP_CLAVE_CIFRADO` se documenta allí con un valor de desarrollo marcado como público y con instrucciones para generar uno real; `MOICA_ADMIN_CORREO` viaja vacía |

### Segundo factor: reutilización de código e intentos

Dos huecos de endurecimiento que **no** se corrigen en P3 porque cerrarlos bien
exige decisiones que no son de una revisión correctiva. Se dejan escritos aquí
para que el equipo los resuelva a propósito y no por omisión.

**Qué se comprobó.** Con una prueba de integración desechable sobre el escenario
real:

- Un código aceptado en una sesión **vuelve a servir** en otra sesión distinta
  mientras dure su ventana: las dos verificaciones respondieron 200 con el mismo
  código. La RFC 6238, en su apartado 5.2, pide lo contrario: el verificador no
  debe aceptar un segundo uso del mismo código.
- Doce intentos fallidos seguidos responden los doce 403 `CODIGO_INVALIDO` y un
  código correcto sigue funcionando después: **no hay límite de intentos**. Con
  seis dígitos y tres periodos admitidos, cada intento acierta con probabilidad
  3 entre 10⁶, así que sin límite la fuerza bruta es cuestión de tiempo.

**Qué tan expuesto está hoy.** La ventana es de 90 segundos
(`periodo` 30 s y `pasos-de-tolerancia` 1). Reutilizar un código exige además
conocer la contraseña, porque sin ella no se abre la sesión provisional que lo
presenta; el escenario real es alguien que capturó un código —suplantación de
pantalla, malware, phishing— y lo aprovecha dentro de esa ventana. `java-otp`
solo calcula códigos: no guarda estado ni sabe cuál se usó, así que impedirlo es
responsabilidad de Moica.

**Solución mínima recomendada.**

1. *Reutilización.* Que `AlgoritmoTotp` devuelva **qué periodo** encajó en lugar
   de un booleano, y que la verificación rechace todo periodo menor o igual al
   del último código aceptado. Hace falta leer la fila con bloqueo
   (`SELECT ... FOR UPDATE`) para que dos peticiones simultáneas no la acepten a
   la vez.
2. *Intentos.* Un contador de fallos consecutivos y el instante del último, con
   un bloqueo temporal al superar el umbral, y su reinicio ante un código
   correcto.

**Qué cambiaría del esquema y del contrato.**

- La reutilización cabe en la columna `fecha_ultima_verificacion` que ya existe,
  pero cambiándole el significado: pasaría a guardar el instante del **periodo
  aceptado** y no el de la verificación, hasta 30 segundos de diferencia. Eso es
  un cambio en el diccionario de datos, no un detalle de implementación.
- El límite de intentos necesita estado nuevo y persistente (contador y ventana
  de bloqueo), es decir una migración `V12` y filas nuevas en el diccionario.
- Hay que decidir el número de intentos, la duración del bloqueo y qué responde
  la API mientras dure: reutilizar `CODIGO_INVALIDO` para no revelar nada, o un
  código y un estado propios.

**Por qué no vale una solución solo en memoria.** Un contador o una lista de
códigos usados en memoria se pierde en cada reinicio y no se comparte entre
instancias: bastaría con esperar un despliegue, o con que el balanceador mandara
la petición a otra instancia, para recuperar los intentos. Serviría como
mitigación declarada, nunca como la protección definitiva.

**Si bloquea o no la integración de P3.** No la bloquea. P3 no empeora nada de lo
que ya había: antes de este incremento no existía segundo factor. Las dos cosas
son endurecimientos de una capacidad que ya cumple su función —exigir un segundo
factor para completar una sesión— y encajan mejor como un incremento propio, con
su migración y sus valores acordados, que como un añadido improvisado dentro de
una revisión.

### Recorrido manual comprobado

Con el backend en `localhost:8080`, el frontend servido por Vite en
`localhost:5173` y PostgreSQL en el contenedor:

1. Registro e inicio de sesión de una cuenta ordinaria: la pantalla de inicio
   ofrece «Seguridad de la cuenta» y **no** ofrece el área administrativa.
2. En `/seguridad`, el segundo factor aparece «Sin configurar». «Activar el
   segundo factor» entrega el QR y la clave manual, con el aviso de que después
   no se podrá volver a mostrar.
3. Confirmado el primer código, el estado pasa a «Activo» y esa misma sesión
   queda verificada, sin pedir el código otra vez.
4. Cerrar sesión y volver a entrar: la respuesta trae
   `pendienteDeSegundoFactor: true` y la aplicación lleva a
   `/verificar-segundo-factor`. Pedir `/seguridad` desde ahí devuelve a la
   verificación, y `GET /api/auth/segundo-factor` responde 403.
5. Un código correcto completa esa sesión; otra sesión abierta a la vez sigue
   pendiente hasta presentar el suyo.
6. Cambio de contraseña: 204, cookie caducada y las tres sesiones de la cuenta
   revocadas como `CAMBIO_CREDENCIALES` en el mismo instante. La contraseña
   antigua ya no entra.
7. Se define `MOICA_ADMIN_CORREO` con esa cuenta y se reinicia el backend: el
   registro anuncia la asignación sin nombrar el correo y la fila aparece en
   `administrador`.
8. Al volver a entrar, `esAdministrador: true` pero la sesión nace provisional:
   `/api/admin/resumen` responde 403. Tras verificar el código, responde 200 y
   `/admin` muestra la cuenta, el correo y la fecha de asignación.

La revisión correctiva repitió, sobre el mismo montaje, los recorridos que toca:

9. Empezar una activación en `/seguridad`, salir al inicio y volver: la clave
   manual anterior no reaparece y la sección vuelve a ofrecer «Activar el segundo
   factor» con el estado «Activación sin terminar». Confirmar la activación deja
   el estado en «Activo» y la clave desaparece en ese mismo instante.
10. Con la sesión revocada directamente en la base, volver a `/admin` dentro de
    la aplicación: la consulta del resumen recibe 401 y la aplicación lleva a
    `/iniciar-sesion?motivo=sesion-vencida`, sin quedarse en «Cerrando tu
    sesión…» y sin dejar a la vista ningún dato de la cuenta.
11. Con `fecha_expiracion` puesta a 25 segundos y `/seguridad` abierta, la
    aplicación sale sola a los 12 segundos que le quedaban, con el aviso de
    sesión vencida. Antes de la corrección, en esa ruta no ocurría nada.
12. Cerrar sesión a propósito lleva a `/iniciar-sesion` **sin** motivo: la
    vigilancia no pisa la explicación de quien termina la sesión.
13. Una cuenta con el segundo factor activo cierra sesión y entra otra sin
    configurarlo, sin recargar: muestreando `/seguridad` cada 10 ms, la secuencia
    observada es «(sin sección de estado)» y luego «Sin configurar». En ningún
    momento aparece «Activo».

Las 18 capturas de `/seguridad`, de la sección del segundo factor (desactivado
y en activación), de `/verificar-segundo-factor`, de `/admin` y del
inicio con la cuenta administradora están fuera del repositorio, en
`C:\Users\ervin\Desktop\moica-pr7-capturas-p3`, cada una con el tamaño exacto
que indica su nombre. Ninguna imagen se versiona.

Chrome impone un ancho mínimo de ventana en Windows, así que las capturas se
tomaron fijando el viewport con `Emulation.setDeviceMetricsOverride` del
protocolo de DevTools. Cada archivo se acompaña de la medida de `scrollWidth` y
`clientWidth` tomada en esa misma página.

## Perfil y portafolio de P4

Controles que P4 dejó funcionando, con el resultado real de cada comprobación.

- **Local**: ejecutado en la máquina de desarrollo (Windows 11, Docker Desktop,
  Node 22, JDK compilando con `release 21`). La primera vuelta, sobre `19bfaff`;
  la ronda correctiva de la revisión, sobre `71adc75`.
- **Revisión**: `robertofabiot` aprobó el PR sobre `d3b85bb` y ejecutó ahí las
  comprobaciones que exigen credenciales reales de Cloudflare R2.
- **CI**: ejecutado por GitHub Actions en el Pull Request #9. Sobre `237b4e4`,
  el último commit de código de la primera vuelta,
  [ejecución 32896632730](https://github.com/robertofabiot/moica-hackathon/actions/runs/32896632730)
  y [ejecución 32896665532](https://github.com/robertofabiot/moica-hackathon/actions/runs/32896665532)
  dejaron los cuatro checks en verde. Sobre `71adc75`, el último commit de
  código de la ronda correctiva,
  [ejecución 33039238600](https://github.com/robertofabiot/moica-hackathon/actions/runs/33039238600)
  y [ejecución 33039238601](https://github.com/robertofabiot/moica-hackathon/actions/runs/33039238601)
  volvieron a dejarlos en verde: «Backend (Java 21)» en 1m26s, «Frontend
  (Node 22)» en 40s, «Entorno local (docker compose)» y «Título y commits
  convencionales». Este cambio es documental y no toca código; el CI de su
  propio commit queda anotado en el cuerpo del PR.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Migraciones sobre base limpia | `./mvnw verify` con Testcontainers | Sí | Sí | «Successfully applied 6 migrations to schema public, now at version v23». `EsquemaDeTerritorioYPerfilIT` comprueba claves, dominios, valores por omisión, unicidad, cascadas y `RESTRICT` sobre PostgreSQL real |
| Catálogo de Managua | `EsquemaDeTerritorioYPerfilIT` y `CatalogoTerritorialIT` | Sí | Sí | Los nueve municipios en orden alfabético: Ciudad Sandino, El Crucero, Managua, Mateare, San Francisco Libre, San Rafael del Sur, Ticuantepe, Tipitapa y Villa El Carmen. Un departamento deshabilitado no se publica |
| Un solo perfil por cuenta | `PerfilPrestadorIT` y `EsquemaDeTerritorioYPerfilIT` | Sí | Sí | Lo garantiza la clave primaria compartida con `usuario`, no una comprobación de la aplicación. El segundo intento responde 409 `PERFIL_YA_EXISTE` |
| Municipio de un departamento habilitado | `PerfilPrestadorIT` | Sí | Sí | Municipio inexistente y municipio de departamento deshabilitado responden 400 `MUNICIPIO_NO_DISPONIBLE`, con el mismo mensaje en ambos casos |
| Nivel de verificación fuera del alcance del propietario | `PerfilPrestadorIT.elPropietarioNoPuedeTocarSuNivelDeVerificacion` | Sí | Sí | Ningún DTO de P4 acepta el campo; enviarlo no tiene efecto y el perfil sigue `SIN_VERIFICAR` en la base |
| Propiedad de perfil, contactos, trabajos e imágenes | `MediosDeContactoIT` y `PortafolioIT`, con dos cuentas reales | Sí | Sí | Lo ajeno no aparece en las listas y responde 404, nunca 403: distinguirlos permitiría enumerar identificadores |
| Estado de cuenta | `PerfilPrestadorIT`, `MediosDeContactoIT`, `PortafolioIT` e `ImagenDePerfilIT` | Sí | Sí | `RESTRINGIDA_TEMPORAL` conserva la lectura y recibe 403 `CUENTA_RESTRINGIDA` en toda mutación; una suspensión no llega ni a la lectura |
| Orden de contactos, trabajos e imágenes | `MediosDeContactoIT` y `PortafolioIT` | Sí | Sí | Se envía la lista completa; si sobra, falta o se repite algún identificador, 400 `ORDEN_INVALIDO` |
| Validación de imágenes | `ValidacionDeImagenTest`, `TipoDeImagenTest`, `ImagenDePerfilIT` y `PortafolioIT` | Sí | Sí | Tamaño contra el máximo configurable (413), tipo declarado (400) y **firma binaria real** frente a lo declarado (400). Una cabecera `image/png` con contenido JPEG se rechaza; SVG y PDF no entran |
| Solo la URL en PostgreSQL | `EsquemaDeTerritorioYPerfilIT` e `ImagenDePerfilIT` | Sí | Sí | Las columnas de imagen son `character varying`; la fila guarda la dirección pública y el binario vive en el almacén de objetos |
| Claves opacas | `ClavesDeImagenTest` e `ImagenDePerfilIT` | Sí | Sí | `perfiles/<32 hex>.<ext>` y `trabajos/<32 hex>.<ext>`; mil claves generadas sin colisión y sin rastro del nombre original |
| Invocaciones al almacenamiento y compensación | `AlmacenamientoR2Test`, `ImagenDePerfilIT` y `PortafolioIT` | Sí | Sí | El cliente S3 recibe el bucket, la clave y el tipo correctos sin tocar la red. Si la persistencia falla tras subir, el objeto se retira; sustituir conserva el anterior hasta persistir el nuevo; borrar un trabajo retira sus objetos |
| Respuesta uniforme con el almacenamiento caído | `ImagenDePerfilIT.conElAlmacenamientoCaidoRespondeElErrorUniformeSinTocarLaBase` | Sí | Sí | 503 `ALMACENAMIENTO_NO_DISPONIBLE`; el cuerpo no menciona R2, Cloudflare ni S3, y la base no queda apuntando a nada |
| Sin fugas en errores ni registros | `RepresentacionSinSecretosTest`, `PropiedadesDeAlmacenamientoTest` y revisión del registro del servidor | Sí | Sí | La configuración no revela **ninguna de las dos mitades de la credencial** al convertirse en texto: ni el secreto del token ni su identificador, cada uno con su propio centinela en la prueba. Cuenta, bucket y base pública sí salen, porque no son credencial y hacen útil la representación. Una configuración a medias detiene el arranque sin nombrar ningún valor, y en el recorrido manual el único aviso del registro nombra las variables ausentes |
| Limpieza de un objeto cuya URL no se resuelve | `ImagenDePerfilIT.unaUrlQueNoPerteneceALaBasePublicaNoSeIntentaBorrar` y su equivalente en `PortafolioIT` | Sí | Sí | Con la fila apuntando a otro dominio —lo que queda tras cambiar `url-publica-base`— la operación responde 200/204, la fila se limpia y **no se pide borrar ninguna clave**: nunca se deduce una clave de un texto arbitrario. El objeto queda suelto y se registra un aviso que nombra la causa sin escribir la URL |
| Espera propia de las cargas de imagen | `comun/api.test.ts`, con temporizadores simulados | Sí | Sí | Una petición corriente sigue expirando a los 10 s; una carga de archivo no expira ahí y sí a los 90 s, por encima del máximo de 60 s que `AlmacenamientoR2` concede a cada llamada. Sin conexión se corta al instante, sin armar temporizador, y un rechazo tardío de `fetch` queda absorbido |
| Previsualización local del archivo elegido | `ImagenDePerfil.test.tsx` y recorrido en el navegador real | Sí | Sí | La imagen que se ve durante la subida es el archivo local (`URL.createObjectURL`), no lo ya guardado. La URL se revoca al sustituir el archivo, al desmontar y al guardarse; la imagen anterior sigue a la vista como «Imagen actual» y la elegida se rotula «Elegida, subiendo…» o «Elegida, sin guardar», nunca como aceptada |
| Interfaz responsiva sin desbordamiento | Capturas con el viewport fijado por CDP, midiendo `scrollWidth` y `clientWidth` | Sí | | Inicio y perfil a 375x812, 768x1024 y 1280x800: `scrollWidth == clientWidth` en los seis casos. Las de perfil se rehicieron sobre `71adc75` con un archivo ya elegido, que es el estado donde conviven los dos retratos: a 375 px caben en fila (bordes derechos en 180 y 324) y siguen sin desbordar |
| Teclado y mensajes accesibles en la imagen de perfil | Recorrido en Chrome sobre la aplicación real | Sí | | Desde el campo de archivo, un `Tab` llega a «Reintentar la subida» con el foco visible (`outline: 2px`, `:focus-visible`), y `Enter` reintenta con el mismo archivo sin volver a leerlo. El error usa `role="alert"`, el progreso `role="status"` y cada retrato lleva su texto alternativo y su pie |
| Caché limpia entre dos cuentas | `PerfilPrestador.test.tsx` | Sí | Sí | Al entrar la segunda cuenta, la consulta del perfil está vacía y no se ve ningún dato de la anterior |
| Cadena del frontend | `format:check`, `lint`, `typecheck`, `test` y `build` | Sí | Sí | 116 pruebas de Vitest en 13 archivos, sin hallazgos de lint ni errores de tipos, `npm audit` sin vulnerabilidades y build con PWA |
| **Carga real contra un bucket R2** | Procedimiento manual de `Almacenamiento.md` | **Sí** | **No** | Comprobado por `robertofabiot` sobre `d3b85bb` contra el bucket **`moica-publico-dev`**: conexión y configuración del cliente S3, carga de la imagen de perfil con su URL pública, persistencia de esa URL en PostgreSQL y carga de imágenes de portafolio. El 28 de agosto de 2026 se ejecutaron en local los pasos 3 y 4 contra el mismo bucket: sustituir un PNG por un JPEG cambió la URL y la anterior respondió 404; eliminar dejó `urlImagenPerfil` en `null` y las dos URLs de la prueba en 404; las claves de esa prueba no quedaron en el bucket. La CI no cubre R2 a propósito |

### Almacenamiento de imágenes: lo comprobado contra R2 y lo que sigue abierto

Cloudflare R2 quedó adoptado como proveedor y el código está completo y aislado
tras la interfaz `AlmacenamientoDeImagenesPublicas`, con su implementación real
`AlmacenamientoR2`.

**Comprobado contra un bucket real.** En la revisión del PR #9, `robertofabiot`
configuró las variables `MOICA_R2_*` y ejecutó el flujo sobre el bucket
`moica-publico-dev`, según deja por escrito su aprobación:

- el cliente S3 se inicializó y conectó con el bucket público;
- la imagen de perfil se subió, se generó su URL pública y esa URL quedó
  persistida en PostgreSQL;
- las imágenes de un trabajo del portafolio también se subieron.

Eso cierra lo que el doble en memoria no podía demostrar: que las credenciales
valen y que R2 acepta la firma que este cliente produce.

**Comprobado después contra el mismo bucket.** El 28 de agosto de 2026, con las
variables `MOICA_R2_*` del entorno local, se ejecutaron los pasos 3 y 4 de
`Docs/Dev/Almacenamiento.md`:

- sustituir un PNG por un JPEG cambió la URL; la lectura anónima de la nueva
  respondió 200 y la de la anterior, 404;
- eliminar dejó `urlImagenPerfil` en `null`; las dos URLs de esa prueba
  respondieron 404;
- un listado S3 confirmó que las claves de esa prueba ya no estaban.

Quedaron dos objetos previos, ajenos a esa prueba y sin fila en PostgreSQL
(`perfiles/bd70b56d8bce4ea780419bba44695d47.png` y
`trabajos/a98a3766de57423da9e4de7fa4424335.png`). No se borraron.

Lo que **sigue sin comprobarse contra R2 real** es el bucket **privado**: el
entorno no tiene `MOICA_R2_PRIVADO_ID_CUENTA`, `MOICA_R2_PRIVADO_ACCESS_KEY_ID`,
`MOICA_R2_PRIVADO_SECRET_ACCESS_KEY` ni `MOICA_R2_BUCKET_PRIVADO`. Los diez
pasos de esa superficie no se dan por hechos.

Sin variables, subir o borrar una imagen responde
`503 ALMACENAMIENTO_NO_DISPONIBLE` y el resto de Moica funciona con normalidad.

La CI **no** cubre R2 a propósito: montar LocalStack solo para estas pruebas
añadiría infraestructura pesada sin demostrar lo que importa, que es que R2
acepte la firma y sirva el objeto.

### Recorrido manual comprobado

Los puntos 1 a 17, sobre `19bfaff`, con PostgreSQL en Docker y el backend en
marcha. Los puntos 18 a 21, sobre `71adc75`, en el mismo montaje y con Chrome
gobernado por el protocolo de DevTools.

1. Registro e inicio de sesión: 201 y 201.
2. `GET /api/catalogos/departamentos` devuelve Managua con sus nueve municipios
   en orden alfabético.
3. Consultar el perfil antes de crearlo responde 404 `PERFIL_NO_ENCONTRADO`, que
   es el código con el que la interfaz distingue «todavía no existe» de un fallo.
4. Crear el perfil devuelve «Taller La Esperanza · Managua · DISPONIBLE ·
   SIN_VERIFICAR».
5. Un segundo perfil para la misma cuenta responde 409 `PERFIL_YA_EXISTE`.
6. La disponibilidad alterna en ambos sentidos y queda persistida.
7. Tres contactos creados y reordenados con la lista completa; el orden nuevo se
   lee de vuelta.
8. Un orden con la lista incompleta responde 400 `ORDEN_INVALIDO`.
9. Dos trabajos creados, uno con fecha y otro sin ella; el segundo la conserva
   nula.
10. Subir un PNG válido **sin credenciales R2** responde 503
    `ALMACENAMIENTO_NO_DISPONIBLE`, que es el comportamiento documentado.
11. Una cabecera `image/png` con contenido JPEG responde 400
    `IMAGEN_NO_ADMITIDA`; un SVG, lo mismo.
12. Un PNG de 5,3 MB responde 413 `IMAGEN_DEMASIADO_GRANDE`.
13. Tras esos fallos, `imagen_trabajo_portafolio` tiene cero filas y
    `url_imagen_perfil` sigue nula: ninguna fila quedó apuntando a un objeto que
    no existe.
14. Una segunda cuenta no ve los contactos ni los trabajos de la primera, y
    borrar un trabajo ajeno responde 404 dejando el trabajo intacto.
15. Con la cuenta en `RESTRINGIDA_TEMPORAL`, la lectura del perfil responde 200 y
    la mutación 403 `CUENTA_RESTRINGIDA`.
16. En el registro del servidor, la única mención al almacenamiento es un aviso
    con el **nombre** de las variables ausentes. No aparece ninguna clave, ningún
    binario ni ningún dato de la cuenta.
17. En el navegador, el selector de municipio mostraba «Ciudad Sandino» en un
    perfil guardado con Managua: las opciones del catálogo llegan después que el
    formulario y, al sustituirlas, el navegador descartaba el valor que ya no
    encontraba entre sus hijos. Corregido en `5c13f37` con un campo controlado y
    verificado de nuevo en el navegador real.
18. Al elegir un PNG en el perfil, la imagen que aparece es el archivo local
    servido por una URL `blob:`, con el pie «Elegida, sin guardar», al lado de
    «Imagen actual». Sin credenciales R2 la subida responde 503, y la pantalla
    lo dice con `role="alert"` sin presentar el archivo como guardado.
19. Elegir un segundo archivo crea una URL `blob:` nueva y **revoca la
    anterior**, comprobado envolviendo `URL.revokeObjectURL` en la propia
    página: la lista de revocadas contiene exactamente la primera URL.
20. «Reintentar la subida» se alcanza con un `Tab` desde el campo de archivo,
    con el foco visible, y `Enter` reenvía **el mismo archivo**: la URL `blob:`
    no cambia y no se revoca nada.
21. Inicio y perfil medidos otra vez a 375x812, 768x1024 y 1280x800 con el
    archivo ya elegido: `scrollWidth == clientWidth` en los seis casos.

Las 6 capturas de inicio y perfil a 375x812, 768x1024 y 1280x800 están fuera del
repositorio, junto con `medidas.json`, que registra `scrollWidth` y
`clientWidth` de cada una. Ninguna imagen se versiona; se adjuntan al Pull
Request, que es donde quedan accesibles para quien revisa sin depender de una
ruta local. Como en P3, el viewport se fijó con
`Emulation.setDeviceMetricsOverride` del protocolo de DevTools, porque Chrome
impone un ancho mínimo de ventana en Windows. Las seis se rehicieron sobre
`71adc75`, y las de perfil muestran ahora el estado con un archivo ya elegido,
que es el que estrena la corrección.


## Verificación documental de P4V

Controles que P4V dejó funcionando, con el resultado real de cada comprobación.

- **Local**: ejecutado en la máquina de desarrollo (Windows 11, Docker Desktop,
  Node 22, JDK compilando con `release 21`), con PostgreSQL publicado en
  `localhost:5433`. Sobre `d096558`, el último commit de código. El 28 de
  agosto de 2026 el #12 ejecutó los diez pasos de `Almacenamiento.md` contra
  `moica-privado-dev`; el detalle está en la fila «Bucket privado real».
- **CI**: ejecutado por GitHub Actions en el Pull Request #10. Sobre `2d6c8c7`,
  el commit que ya contiene todo el código y toda la documentación,
  [ejecución 33094908156](https://github.com/robertofabiot/moica-hackathon/actions/runs/33094908156)
  dejó en verde «Backend (Java 21)», «Frontend (Node 22)» y «Entorno local
  (docker compose)», y
  [ejecución 33094908157](https://github.com/robertofabiot/moica-hackathon/actions/runs/33094908157)
  dejó en verde «Título y commits convencionales». Este cambio solo anota esos
  enlaces; el CI de su propio commit queda en un comentario del PR.
- **Corrección de concurrencia**: la fila «Concurrencia sobre el nivel del
  perfil» llega con el Pull Request #11, posterior a P4V. En local, `./mvnw
  verify` sobre `5bca790` dejó 138 unitarias y 262 de integración en verde, y
  `ConcurrenciaDeVerificacionIT` se ejecutó seis veces seguidas sin un solo
  fallo para descartar inestabilidad. En CI, sobre ese mismo commit,
  [ejecución 33124855278](https://github.com/robertofabiot/moica-hackathon/actions/runs/33124855278)
  dejó en verde «Backend (Java 21)», «Frontend (Node 22)» y «Entorno local
  (docker compose)», y
  [ejecución 33124855452](https://github.com/robertofabiot/moica-hackathon/actions/runs/33124855452)
  dejó en verde «Título y commits convencionales». Que el backend pase también
  en la máquina del CI, con otros tiempos, es parte de la evidencia: la
  coordinación de esas pruebas no depende de pausas. El #11 se fusionó **sin
  revisión registrada**: no hay aprobación de `robertofabiot` en ese PR. Antes
  de iniciar P5, Roberto revisó a posteriori el rango `9790215..f88edff` sin
  hallazgos. El comentario público queda pendiente y no bloqueó este incremento.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Migración sobre base limpia | `./mvnw verify` con Testcontainers | Sí | Sí | «Migrating schema "public" to version "30 - crear solicitudes y documentos de verificacion"» y «Successfully applied 7 migrations to schema "public", now at version v30». Sobre la base local, ya en v23, aplicó solo la nueva |
| Índice parcial de solicitud abierta | `EsquemaDeVerificacionIT` | Sí | Sí | Dos abiertas del mismo nivel chocan; una de cada nivel conviven; resolver la primera deja sitio a la siguiente y la resuelta se conserva; el índice no alcanza a otro perfil. Se prueba contra PostgreSQL real porque un índice parcial con `WHERE` sobre un conjunto de estados no existe en H2 |
| Restricciones de la tabla | `EsquemaDeVerificacionIT` | Sí | Sí | Dominios de nivel, estado, tipo documental y MIME; `tamanoBytes > 0 AND <= 5242880` con sus dos extremos; observación obligatoria y no vacía en `RECHAZADA` y `REVOCADA`; fecha de resolución obligatoria en todo estado final; revisión y resolución nunca anteriores al envío; unicidad de la clave de almacenamiento; cascada desde el perfil y `RESTRICT` hacia el administrador |
| Creación atómica del expediente | `EnvioDeExpedienteIT` | Sí | Sí | Una sola petición crea la solicitud y sus documentos. Sin documentos, sin identidad en una básica o sin respaldo en una profesional no se crea nada y **no se sube ni un byte** |
| Compensación | `EnvioDeExpedienteIT` | Sí | Sí | Con la carga fallando en el segundo archivo, el primero se retira y no queda solicitud. Con la persistencia rota —una restricción `CHECK (false) NOT VALID` añadida para la prueba— la transacción entera se deshace y los dos objetos se retiran |
| Formatos y firma real | `TipoDeDocumentoTest`, `ValidacionDeDocumentoTest`, `EnvioDeExpedienteIT` | Sí | Sí | JPEG, PNG y PDF con su firma real se admiten; WebP no; una cabecera PNG con contenido PDF se rechaza; archivo vacío y archivo de 5 MB + 1 byte se rechazan con sus códigos |
| Clave y metadatos, nunca binario ni URL | `EnvioDeExpedienteIT` | Sí | Sí | La fila guarda `expedientes/<32 hex>.png`, el nombre saneado, el MIME y el tamaño. La respuesta no contiene la clave, ni el texto `claveAlmacenamiento`, ni `expedientes/`, ni ninguna URL |
| Saneamiento del nombre | `NombreDeArchivoTest`, `EnvioDeExpedienteIT` | Sí | Sí | Se quitan ruta, caracteres de control y los que Windows no admite; `..` y un nombre vacío caen a `documento`; se recorta a 255 caracteres |
| Propiedad y ocultación | `EnvioDeExpedienteIT` | Sí | Sí | La solicitud de otro prestador responde 404 `SOLICITUD_NO_ENCONTRADA` y su historial sale vacío. El propietario no tiene ninguna ruta que le entregue su binario |
| Estado de cuenta | `EnvioDeExpedienteIT` | Sí | Sí | Una cuenta `RESTRINGIDA_TEMPORAL` recibe 403 `CUENTA_RESTRINGIDA` al enviar y conserva la lectura de su estado y su historial |
| Área administrativa | `RevisionDeVerificacionIT` | Sí | Sí | Sin sesión 401; cuenta ordinaria con TOTP verificado 403; administrador sin verificar el TOTP en esa sesión 403. Solo entra el administrador con la sesión verificada |
| Toma concurrente | `RevisionDeVerificacionIT` | Sí | Sí | Dos administradores que la toman **a la vez**, desde dos hilos con una barrera común: uno recibe 200 y el otro 409 `SOLICITUD_YA_TOMADA`, nunca los dos 200. La fila queda asignada a uno solo. Cubre la misma solicitud; las carreras sobre solicitudes **distintas** del mismo perfil las cubre la fila siguiente |
| Concurrencia sobre el nivel del perfil | `ConcurrenciaDeVerificacionIT` | Sí | Sí | Seis pruebas con el orden fijado por la prueba, no por el azar: una conexión ajena retiene `perfil_prestador` con `SELECT … FOR UPDATE`, cada petición se lanza y se espera a que `pg_stat_activity` confirme que está detenida, y al soltar la retención PostgreSQL las despierta en el orden en que llegaron. Aprobación profesional **contra** revocación básica en los dos sentidos: si aprueba primero, la revocación arrastra la profesional recién aprobada y el perfil queda `SIN_VERIFICAR`; si revoca primero, la aprobación responde 409 `VERIFICACION_BASICA_REQUERIDA` y no deja nada a medias. Edición del perfil **contra** aprobación y **contra** revocación, en ambos órdenes: se conservan a la vez el dato editado y el nivel decidido. Y la comprobación estructural de que actualizar el perfil, cambiar la disponibilidad y sustituir la imagen esperan **leyendo** la fila, no escribiéndola. Contra el código anterior a esta corrección las seis fallan, cada una con su incoherencia concreta: la profesional queda `APROBADA` con el perfil `SIN_VERIFICAR`, la aprobación responde 200 sobre una básica revocada, la edición devuelve el perfil a `SIN_VERIFICAR`, la aprobación restaura el nombre anterior y la edición revive una insignia revocada |
| Transiciones | `RevisionDeVerificacionIT`, `RevocacionDeVerificacionIT` | Sí | Sí | Aprobar sin tomar, revocar algo que no está aprobado y resolver una revisión ajena responden 409 o 403 y no cambian nada. Solo quien tomó la revisión la resuelve |
| Proyección de niveles | `RevisionDeVerificacionIT` | Sí | Sí | Aprobar una básica deja `VERIFICADO_BASICO`; aprobar la profesional sobre ella deja `PROFESIONAL_VERIFICADO`; rechazar una profesional conserva la básica |
| Revocación dependiente | `RevocacionDeVerificacionIT` | Sí | Sí | Revocar la profesional degrada a `VERIFICADO_BASICO`. Revocar la básica deja el perfil `SIN_VERIFICAR` y la profesional `REVOCADA` con el **mismo motivo, administrador e instante** —comprobado comparando las tres columnas de las dos filas—. Una básica nueva no revive la profesional. Los documentos no se borran |
| Motivo obligatorio | `RevisionDeVerificacionIT`, `RevocacionDeVerificacionIT`, `EsquemaDeVerificacionIT` | Sí | Sí | Rechazar o revocar con el motivo en blanco responde 400 `VALIDACION` y no cambia el estado; PostgreSQL lo vuelve a rechazar si se intenta por SQL |
| Acceso temporal | `RevisionDeVerificacionIT`, `AlmacenamientoPrivadoR2Test` | Sí | Sí | El endpoint responde 302 con `Cache-Control: no-store`; el enlace vale ahora y no diez minutos después. La firma real, generada con un `S3Presigner` de verdad, lleva `X-Amz-Expires=300` con `PT5M` y `900` con `PT15M`, apunta al bucket privado por estilo de ruta y no contiene el secreto |
| Configuración ausente y a medias | `PropiedadesDeAlmacenamientoPrivadoTest`, `AlmacenamientoPrivadoR2Test`, `PropiedadesDeDocumentosTest` | Sí | Sí | Sin las cuatro variables la aplicación arranca y las operaciones documentales responden el 503 uniforme; con tres de cuatro el arranque se detiene con un mensaje que **no revela ningún valor**; el máximo por documento no admite más de 5 MB y el acceso temporal no admite más de una hora |
| Errores sin detalle interno | `RevisionDeVerificacionIT`, `EnvioDeExpedienteIT`, `AlmacenamientoPrivadoR2Test` | Sí | Sí | Ninguna respuesta contiene `R2`, `cloudflare`, `S3`, `expedientes/` ni `X-Amz`. La representación de `PropiedadesDeAlmacenamientoPrivado` oculta el secreto **y** el identificador del token |
| Pruebas del backend | `./mvnw verify` | Sí | Sí | 138 unitarias y 262 de integración en verde —las 256 de P4V más 6 de `ConcurrenciaDeVerificacionIT`—, con Spotless limpio («179 files clean») y SpotBugs en «BugInstance size is 0» |
| Pruebas del frontend | `npm run test` | Sí | Sí | 145 en verde, 29 de ellas de P4V |
| Cadena completa del frontend | `format:check`, `lint`, `typecheck`, `test`, `build` y `npm audit` | Sí | Sí | Todo en verde; `npm audit` reporta 0 vulnerabilidades y el build vuelve a generar el manifiesto y el service worker |
| Entorno local | `docker compose config -q` y `git diff --check` | Sí | Sí | Ambos sin salida |
| Interfaz responsiva | Chrome con `Emulation.setDeviceMetricsOverride` a 375x812, 768x1024 y 1280x800 | Sí | | 15 capturas: verificación del prestador, envío del expediente, confirmación, cola administrativa y expediente abierto, en los tres tamaños. `scrollWidth == clientWidth` en las quince, registrado en `medidas.json` |
| Bucket privado real | Los diez pasos de `Almacenamiento.md` | Sí | | El 28 de agosto de 2026, contra `moica-privado-dev`, con archivos ficticios. GET anónimo al API S3: 400; la misma clave por la URL pública: 404. POST del expediente: 201, tres documentos PNG/JPEG/PDF, sin clave ni URL en la respuesta. PostgreSQL: tres claves `expedientes/<32 hex>.(png\|jpg\|pdf)`, MIME y tamaño, sin URL. Propietario: 403. Ajeno: 404. Administrador sin TOTP verificado en esa sesión: 403. Con TOTP: 302 `Cache-Control: no-store`; el `Location` es HTTPS de R2, estilo de ruta, prefijo `expedientes/` y `X-Amz-Expires=300` (la URL no se registra). GET al `Location`: 200; a los 330 s: 403. Compensación con un bucket inexistente solo en el proceso: 503 `ALMACENAMIENTO_NO_DISPONIBLE`, sin filtrar el proveedor, mismas filas y mismos objetos |

### Recorrido manual comprobado

Con PostgreSQL en `localhost:5433`, el backend en `localhost:8080` y el frontend
servido por Vite. Los archivos usados son **ficticios**, preparados para la
prueba: ningún documento de identidad real intervino ni aparece en ninguna
captura.

1. Enviar un expediente con el bucket privado **sin configurar** responde
   `503 ALMACENAMIENTO_NO_DISPONIBLE`, **no crea ninguna fila** —solicitudes y
   documentos siguen en el mismo número antes y después— y el cuerpo del error
   no contiene `R2`, `cloudflare`, `S3` ni `expedientes/`.
2. El prestador ve su nivel vigente con su insignia, la frase que lo explica, el
   aviso de que una insignia no garantiza calidad futura, la solicitud abierta
   con cuántos documentos lleva y el historial con el motivo de cada decisión.
   La respuesta de la API no contiene `claveAlmacenamiento`.
3. Un administrador con el segundo factor verificado ve la cola; el mismo
   administrador en una sesión **sin** verificar el código recibe 403, y el
   prestador también.
4. Tomar la solicitud la deja `EN_REVISION` asignada a quien la tomó. Un segundo
   administrador que intenta tomarla recibe 409 `SOLICITUD_YA_TOMADA`, y si
   intenta aprobarla, 403 `REVISION_DE_OTRO_ADMINISTRADOR`. Aprobarla desde la
   sesión que la tomó deja el perfil en `VERIFICADO_BASICO`.
5. Abrir un documento con el bucket sin configurar responde el mismo 503; el
   propietario que pide esa misma ruta recibe 403.
6. Con la básica vigente, la profesional se solicita, se toma y se aprueba: el
   perfil queda `PROFESIONAL_VERIFICADO`.
7. Revocar con el motivo en blanco responde 400 `VALIDACION` y no cambia nada.
   Con motivo, revocar la **básica** deja las dos solicitudes `REVOCADA` y el
   perfil `SIN_VERIFICAR`; las dos filas comparten motivo, administrador e
   instante exactos —una sola combinación distinta en la consulta— y los dos
   documentos siguen en la base.
8. Aprobar después una básica nueva devuelve `VERIFICADO_BASICO` y **no** revive
   la profesional, que sigue `REVOCADA`. La API vuelve a ofrecer solicitar la
   profesional.
9. Rechazar una profesional con motivo no toca la básica: el perfil sigue
   `VERIFICADO_BASICO` y el prestador lee el motivo en su historial.
10. En el navegador, elegir dos archivos los muestra con su nombre, su tamaño y
    un selector de tipo por documento; «Quitar» retira uno sin tocar el otro;
    un archivo de formato no admitido o de más de 5 MB se rechaza antes de
    salir del navegador y no entra en la lista; «Revisar y enviar» abre la
    confirmación que advierte de que después no se podrá editar.
11. Las cinco pantallas medidas a 375x812, 768x1024 y 1280x800:
    `scrollWidth == clientWidth` en las quince.

El 28 de agosto de 2026 se ejecutaron además los diez pasos de
`Almacenamiento.md` contra el bucket privado real. El detalle está en esa
guía; no se copian aquí claves ni URLs prefirmadas.

La corrección `d096558` salió justamente de este recorrido: a 375 px la cola
administrativa de cinco columnas obligaba a desplazarse dentro de la tabla y
partía las palabras a la mitad. El nivel y la fecha pasaron a acompañar al
nombre del prestador, el botón de cada fila muestra «Abrir» con su nombre
accesible completo, y la tabla cabe entera en un teléfono.

Las 15 capturas y `medidas.json` están fuera del repositorio, como en P2, P3 y
P4; se adjuntan al Pull Request. El viewport se fijó otra vez con
`Emulation.setDeviceMetricsOverride` del protocolo de DevTools, porque Chrome
impone un ancho mínimo de ventana en Windows.

## Sistema de diseño y maqueta de acceso

Controles de la maqueta de interfaz (tokens, componentes reutilizables y
tarjetas de iniciar sesión / registro), con el resultado real de cada
comprobación.

- **Local**: `npm run test` en el frontend sobre `6af9ed3`. Recorrido en el
  navegador de `/iniciar-sesion` (tarjeta, botón primario naranja y errores
  bajo los campos al enviar vacío).
- **CI**: este PR se abre contra `develop`. El merge-base con
  `origin/develop` es `f88edff`, el HEAD actual de esa rama. Los enlaces de CI
  se anotan cuando existan; no se describen de memoria.
- **Rama**: `feature/pantallas-acceso` nace de `develop` actualizado. Los
  commits de `feature/tokens-de-diseno` viajan en el mismo PR porque esa rama
  todavía no estaba integrada.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Tokens de marca | Revisión de `frontend/src/estilos/global.css` | Sí | | `--color-primary-500` es `#F57C00`; el fondo de la app es `--color-neutral-50`; los alias `--moica-*` apuntan a estas tokens para no romper las pantallas que aún no se migran |
| Componentes reutilizables | `npm run test` | Sí | | `Boton` (primario, secundario, contorno), `Entrada` (ref para React Hook Form y mensaje de error) y `BarraLateral` (siete destinos, aviso de mensajes). La barra **no** está enganchada todavía a las pantallas autenticadas |
| Maqueta de acceso | Recorrido en `/iniciar-sesion` y `/registro` | Sí | | Tarjeta blanca, `radius-xl`, `shadow-sm`, copy del diseño. `Boton` y `Entrada` enlazados a `useInicioSesion` y `useRegistro`. Los avisos de sesión vencida, cuenta creada y credenciales cambiadas se conservan |
| Sin OAuth ni recuperación de clave | Revisión del diff y de `DefinicionProducto.md` | Sí | | Google, Facebook y Apple se pintan deshabilitados. «¿Olvidaste tu contraseña?» no navega. «Recordarme» no se envía: la sesión ya persiste en cookie `HttpOnly` |
| Pruebas del frontend | `npm run test` | Sí | | 154 en verde, 18 archivos: las 145 anteriores más 9 de los componentes nuevos. Las pruebas de registro e inicio de sesión siguen cubriendo validación, 409, errores por campo y el ciclo que ya existía |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `f88edff`, el HEAD de `develop`. El PR se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | **No** | | **Pendiente.** Se comprobó la tarjeta de inicio de sesión en el navegador; no hay todavía el juego de capturas con `scrollWidth == clientWidth` |

## Servicios publicados y descubrimiento de P5

Controles que P5 deja funcionando, con el resultado real de cada comprobación.

- **Local**: ejecutado en la máquina de desarrollo (Windows 11, Docker Desktop,
  Node 22, JDK compilando con `release 21`), con PostgreSQL publicado en
  `localhost:5433`. Tras el merge de `origin/develop` (`1a994ca`, PR #15), el
  último código de integración es `dc43e32`. Este cambio documental viaja en el
  mismo PR.
- **CI**: los cuatro checks del #16 quedaron en verde sobre `fe26f77`, antes de
  integrar el #15; no validan el resultado combinado. Sobre `dbea837`,
  [CI](https://github.com/robertofabiot/moica-hackathon/actions/runs/33263780265)
  —backend, frontend y `docker compose`— y
  [Convenciones](https://github.com/robertofabiot/moica-hackathon/actions/runs/33263780167)
  quedaron en verde. Roberto aprobó el PR #16 el 29 de agosto de 2026. Se
  integró en `develop` mediante `6cd875f`. La CI posterior al merge,
  [ejecución 33264757393](https://github.com/robertofabiot/moica-hackathon/actions/runs/33264757393)
  —backend, frontend y `docker compose`—, quedó en verde.
- **Rama**: `feature/servicios-busqueda-publica` nace de `develop` en
  `f195360` e incorpora la portada del #15 mediante merge, no rebase.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Migraciones `V31` y `V90` | `EsquemaDeServiciosIT` y `CatalogoDeServiciosIT` contra PostgreSQL real | Sí | | `V31` crea categorías, subcategorías, servicios e imágenes; `V90` carga las tres categorías de demostración con tres subcategorías cada una. Un nombre duplicado en la misma categoría choca; el mismo nombre en otra categoría convive. El precio nulo se admite y uno menor o igual a cero no |
| Preparación inactiva sin verificar | `ServicioPublicadoIT` | Sí | | Un perfil `SIN_VERIFICAR` crea el servicio en `INACTIVO`. Activarlo responde 409 `VERIFICACION_BASICA_REQUERIDA` y la fila no cambia |
| Activación válida | `ServicioPublicadoIT` | Sí | | Con cuenta `ACTIVA`, prestador `DISPONIBLE` y al menos `VERIFICADO_BASICO`, el servicio pasa a `ACTIVO` |
| Rechazo al activar | `ServicioPublicadoIT` | Sí | | Sin disponibilidad: 409 `PRESTADOR_NO_DISPONIBLE`. Con cuenta no activa: no muta. Recurso ajeno: 404, no 403 |
| Imágenes y compensación | `ImagenDeServicioIT` y `ClavesDeImagenTest` | Sí | | JPEG, PNG y WebP con el tope de 5 MB. Prefijo `servicios/`. Si falla la persistencia se limpia el objeto. El cuerpo no revela `R2`, `cloudflare` ni `S3`. Sin variables nuevas |
| Descubrimiento público | `DescubrimientoIT` y `CatalogoTerritorialIT` | Sí | | `GET` de catálogos, listado y detalle sin sesión. Solo `ACTIVO` de cuenta `ACTIVA`, prestador `DISPONIBLE` y perfil con al menos básica. Texto, categoría, subcategoría y municipio se combinan. Orden `nombre`, `id`. Sin contactos, correos, documentos ni claves. `precioReferencia` viaja nulo. Un perfil `NO_DISPONIBLE` verificado responde 200 con portafolio, `servicios` vacío y `admiteContratacion: false` —`unPerfilNoDisponibleSigueVisibleSinServiciosNiContratacion`— |
| Frontend de gestión y exploración | `npm run test` | Sí | | 179 pruebas en 20 archivos sobre el código combinado: las 8 de `ExplorarServicios.test.tsx` (carga, error, vacío, filtros, URL inicial, limpiar, «A convenir», insignia), 6 de `ServiciosPropios.test.tsx`, 1 de rutas en `App.test.tsx` y las de portada/sesión/hero del #15 más 5 nuevas en `Inicio.test.tsx` |
| Verificación local completa | Comandos del criterio de salida | Sí | | Backend: `./mvnw -B -ntp verify` — 138 unitarias, 308 de integración, Spotless limpio, SpotBugs 0. Frontend: `npm ci`, `format:check`, `lint`, `typecheck`, `test` (179) y `build` en verde. Raíz: `git diff --check` sin salida y `docker compose --env-file .env.example config -q` válido |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | Sí | | Recorrido de la portada integrada, exploración, vacío, detalle, insignia, perfil público y administración propia. `scrollWidth == clientWidth` en las 18. Carpeta `C:\Users\ervin\Desktop\moica-pr16-capturas-p5`; se adjunta al PR |
| Bucket público real de una imagen de servicio | Carga, lectura anónima y eliminación contra el bucket público local | Sí | | El 28 de agosto de 2026 se subió un PNG de 1×1 al servicio de demostración: `201`, lectura pública `200`, eliminación `204` y `404` al volver a pedir el objeto. La URL persistida usa el prefijo `servicios/` y no se copian aquí claves ni el origen. Sin variables nuevas |

El hero de la portada envía el texto a `/explorar?texto=`. Las categorías y la
ubicación de la maqueta del #15 siguen siendo presentacionales: no se documentan
como filtros funcionales.

Las 18 capturas y `medidas.json` están fuera del repositorio, como en P2, P3,
P4 y P4V; se adjuntan al Pull Request. El viewport se fijó otra vez con
`Emulation.setDeviceMetricsOverride` del protocolo de DevTools, porque Chrome
impone un ancho mínimo de ventana en Windows. En tableta y escritorio
`scrollWidth` y `clientWidth` coinciden en 753 o 1265 px cuando la barra de
desplazamiento vertical reduce el área útil; no hay desborde horizontal.

## Ciclo e historial de solicitudes de P6

Controles que P6 deja funcionando, con el resultado real de cada comprobación.

- **Local**: ejecutado en la máquina de desarrollo (Windows 11, Docker Desktop,
  Node 22, JDK compilando con `release 21`), con PostgreSQL de Testcontainers
  y, para el recorrido visual, el Compose local.
- **CI**: [ejecución 33281169225](https://github.com/robertofabiot/moica-hackathon/actions/runs/33281169225) en verde sobre `cc2fb83`: backend, frontend, Docker Compose y Convenciones.
- **Rama**: `feature/solicitudes-servicio` nace de `develop` en `6cd875f`. PR #17,
  aprobado por `robertofabiot` e integrado en `develop` mediante el merge
  `a9884a9`; `fa43303` fue el HEAD final de la rama.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Migración `V40` | `EsquemaDeSolicitudesIT` contra PostgreSQL real | Sí | Sí | Crea `solicitud_servicio` y `cambio_estado_solicitud`. El dominio rechaza `BORRADOR` y `ARCHIVADA`. El cambio inicial admite `estado_anterior` nulo. Una transición al mismo estado choca. Las FK son `RESTRICT`. Los índices de bandeja, propiedad, estado e historial existen. `fecha_preferida` es `date`; los instantes, `timestamptz`; los identificadores, `bigint`. En el Compose local, `V90` ya estaba aplicada: Flyway aplicó `V40` fuera de orden (`spring.flyway.out-of-order=true`) |
| Envío válido y cambio inicial | `SolicitudServicioIT.enviaUnaSolicitudValidaYRegistraElCambioInicial` | Sí | Sí | `201`, estado `PENDIENTE`, historial de un cambio con `estadoAnterior` nulo. `estado_actual` coincide con el último historial. El cuerpo no lleva correo |
| Rechazos al crear | `SolicitudServicioIT` | Sí | Sí | Servicio propio: 409 `SERVICIO_PROPIO`. Inactivo: 409 `SERVICIO_INACTIVO`. Prestador no disponible: 409 `PRESTADOR_NO_DISPONIBLE`. Sin básica: 409 `VERIFICACION_BASICA_REQUERIDA`. Cuenta no activa: 403 `CUENTA_RESTRINGIDA`. Municipio de departamento no habilitado: 400 `MUNICIPIO_NO_DISPONIBLE`. Descripción vacía: 400 `VALIDACION`. Sin sesión: 401 `NO_AUTENTICADO` |
| Lectura y 404 anti-enumeración | `SolicitudServicioIT.losDosParticipantesLeenYUnTerceroRecibe404` | Sí | Sí | Cliente y prestador leen la ubicación. Un tercero recibe 404 `RECURSO_NO_ENCONTRADO`. Las bandejas separan enviadas y recibidas |
| Transiciones | `SolicitudServicioIT` | Sí | Sí | Aceptar y rechazar una pendiente; cancelar pendiente sin motivo; cancelar aceptada con motivo (cliente o prestador); completar. Actor o estado incorrectos: 409 `TRANSICION_NO_PERMITIDA`. Motivo ausente o en blanco: 400 `MOTIVO_OBLIGATORIO`. Definitivos no se reabren |
| Cuenta restringida y suspendida | `SolicitudServicioIT` | Sí | Sí | Restringida consulta y cancela; no crea, no acepta, no rechaza ni completa. Esas cuatro acciones exigen cuenta `ACTIVA`. Un prestador restringido que intenta rechazar una `PENDIENTE` recibe 403 `CUENTA_RESTRINGIDA`, el estado sigue `PENDIENTE` y no se agrega historial —`unPrestadorRestringidoNoRechazaUnaPendiente`—. Suspendida: 403 `ACCESO_DENEGADO` |
| Concurrencia | `ConcurrenciaDeSolicitudIT` | Sí | Sí | Aceptar y cancelar a la vez dejan un solo ganador y `estado_actual` igual al último historial. Dos aceptaciones: una 200 y una 409 |
| Sin fugas | `SolicitudServicioIT.elCuerpoNoFiltraCorreosContactosNiSecretos` | Sí | Sí | Detalle y bandejas no llevan `correoElectronico`, `@moica.test`, `claveHash`, `secretoTotp` ni `claveAlmacenamiento` |
| Frontend | `npm run test` | Sí | Sí | 202 pruebas en 23 archivos. Las 23 de solicitudes cubren formulario, validación, envío, error de negocio, bandejas, vacío, carga, detalle, aceptar, rechazar, motivo, completar, historial, 403, red y cuenta `RESTRINGIDA_TEMPORAL` (sin solicitar, aceptar, rechazar ni completar; conserva cancelar; el formulario por URL no se abre) |
| Verificación local completa | Comandos del criterio de salida | Sí | Sí | Backend: `./mvnw -B -ntp verify` — 143 unitarias, 338 de integración, Spotless limpio, SpotBugs 0. Frontend: `format:check`, `lint`, `typecheck`, `test` (202) y `build` en verde. Raíz: `git diff --check` y `docker compose --env-file .env.example config -q` se ejecutan en este mismo incremento |
| Capturas a tres tamaños | Chrome (MCP) a 375x812, 768x1024 y 1280x800 contra Vite `:5174` y el backend P6 en `:8082` | Sí | | Las 21 capturas del recorrido inicial ya están adjuntas al PR #17. Esta corrección añade 12 capturas de los estados visuales afectados: prestador restringido ante `PENDIENTE` (sin aceptar/rechazar), prestador restringido ante `ACEPTADA` (sin completar, con cancelar), cliente restringido en el detalle público (sin solicitar) y acceso directo a `/solicitar` (sin formulario), en 375×812, 768×1024 y 1280×800. `scrollWidth === clientWidth` en todos (en 1280, 1265 cuando hay barra vertical). Fuera del repo en `C:\Users\ervin\Desktop\moica-pr17-capturas-p6` (`p6-corr-01` a `p6-corr-12`). No se versionan |

No se implementaron chat, revelación de contactos, calificaciones, notificaciones,
pagos, mapas ni expiración automática. Aceptar solo dejó el estado listo para P7,
que es quien implementa el hilo y la revelación (ver «Chat y contactos de P7»).

## Chat y contactos de P7

Controles que P7 deja funcionando, con el resultado real de cada comprobación.

- **Local**: ejecutado en la máquina de desarrollo (Windows 11, Docker Desktop,
  Node 22, JDK compilando con `release 21`), con PostgreSQL de Testcontainers
  para las pruebas y el Compose local —publicado en `localhost:5433`— para el
  recorrido visual.
- **CI**: los tres trabajos del flujo «CI» quedaron en verde sobre `94b1227`
  —Backend (Java 21), Frontend (Node 22) y Entorno local (docker compose)—:
  [ejecución 33360207164](https://github.com/robertofabiot/moica-hackathon/actions/runs/33360207164).
  El check «Título y commits convencionales» también quedó en verde:
  [ejecución 33360259636](https://github.com/robertofabiot/moica-hackathon/actions/runs/33360259636).
- **Rama**: `feature/chat-contactos` nace de `develop` en `2dd8e9c`, el HEAD de
  esa rama al abrir el Pull Request. El
  [PR #21](https://github.com/robertofabiot/moica-hackathon/pull/21) fue
  aprobado por `robertofabiot` y quedó **fusionado en `develop`** el 31 de
  agosto de 2026 con el merge `0b17f46`. La rama remota ya no existe.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Migración `V41` | `EsquemaDeMensajesIT` contra PostgreSQL real | Sí | Sí | Crea `mensaje_solicitud` con identificador `BIGINT GENERATED ALWAYS AS IDENTITY`, `contenido` `TEXT` y `fecha_envio` `TIMESTAMPTZ`. Las dos claves foráneas son `RESTRICT`: no se borra la solicitud ni el remitente mientras haya mensajes. `ck_mensaje_solicitud_contenido` rechaza vacío, espacios y tabuladores o saltos de línea. Existe `ix_mensaje_solicitud_id_solicitud` sobre `(id_solicitud_servicio, fecha_envio, id_mensaje_solicitud)`. **No existe tabla `conversacion`** ni enum nativo. En el Compose local, `V90` ya estaba aplicada: Flyway aplicó `V41` fuera de orden |
| Hilo de los dos participantes | `ChatDeSolicitudIT.losDosParticipantesLeenYEscribenEnUnHiloAceptado` | Sí | Sí | Cliente y prestador escriben y leen exactamente los mismos mensajes en el mismo orden. El cuerpo identifica al remitente con su `nombreCompleto`, el mismo que ya viajaba en el historial |
| 404 anti-enumeración | `ChatDeSolicitudIT.unTerceroRecibe404AlLeerYAlEscribir` y `RevelacionDeContactosIT.unTerceroRecibe404YNoConfirmaQueElHiloExista` | Sí | Sí | Un tercero recibe 404 `RECURSO_NO_ENCONTRADO` al leer, al enviar y al pedir contactos, la misma respuesta que ante una solicitud inexistente, y no se escribe nada. `unParticipanteNoAlcanzaElHiloDeOtraSolicitud` comprueba además que dos clientes del mismo prestador no ven el hilo del otro |
| El hilo no existe antes de aceptar | `ChatDeSolicitudIT` | Sí | Sí | `PENDIENTE` y `RECHAZADA`: 409 `CHAT_NO_HABILITADO` al leer y al enviar. Una cancelación **desde `PENDIENTE`** tampoco lo abre —`unaCancelacionDesdePendienteNoAbreElHilo`—: el estado vigente no basta, se consulta el historial |
| Solo lectura tras cerrar | `ChatDeSolicitudIT` | Sí | Sí | Cancelar después de aceptar y completar conservan el historial legible para los dos participantes y responden 409 `CHAT_SOLO_LECTURA` al enviar. El número de mensajes no cambia |
| Validación del contenido | `ChatDeSolicitudIT` y `MensajeAEnviarTest` | Sí | Sí | Vacío, espacios y saltos de línea responden 400 `VALIDACION` y no escriben. 2000 caracteres se admiten; 2001, no. Los espacios exteriores no cuentan para el tope porque el recorte ocurre antes de validar |
| El remitente sale de la sesión | `ChatDeSolicitudIT.elRemitenteSaleDeLaSesionYNoDelCuerpo` | Sí | Sí | Un cuerpo que incluye `idRemitente` apuntando al prestador se ignora: la respuesta y la fila guardan el identificador del cliente de la sesión |
| Estado de cuenta | `ChatDeSolicitudIT` | Sí | Sí | `RESTRINGIDA_TEMPORAL` lee el hilo y conserva la revelación de contactos, pero enviar responde 403 `CUENTA_RESTRINGIDA` y no escribe. Suspendida: 403 `ACCESO_DENEGADO`. Sin sesión: 401 |
| Orden estable | `ChatDeSolicitudIT.elHiloConservaUnOrdenEstable` | Sí | Sí | Seis mensajes alternados vuelven en el mismo orden en dos lecturas distintas, ordenados por fecha y desempatados por identificador |
| Sin edición ni borrado | `ChatDeSolicitudIT.noExistenBorradoNiEdicionDeMensajes` | Sí | Sí | `DELETE` y `PUT` sobre `/mensajes` responden 405 `METODO_NO_PERMITIDO`. El hilo no cambia |
| Revelación de contactos | `RevelacionDeContactosIT` | Sí | Sí | El cliente los recibe tras la aceptación, en su orden de visualización (0, 1, 2). Antes de aceptar: 409 `CONTACTOS_NO_REVELADOS`. Cancelar o completar después de aceptar **no** los vuelve a ocultar. Sin contactos configurados: 200 con lista vacía, no un error |
| Los contactos son del cliente | `RevelacionDeContactosIT.elPrestadorNoRecibeSusPropiosContactosPorEstaSuperficie` | Sí | Sí | El prestador recibe 404 en esa ruta y sigue administrando los suyos en `GET /api/prestador/contactos`. `noHayNingunaRutaPublicaParaLosContactosDeUnPrestador` comprueba que el perfil público no los publica y que no existe una ruta por prestador |
| Sin fugas | `ChatDeSolicitudIT.elCuerpoNoFiltraCorreosHashesNiSecretos` y `RevelacionDeContactosIT.laRevelacionNoLlevaCorreosDeCuentaNiSecretos` | Sí | Sí | Ni el hilo ni la revelación llevan `correoElectronico`, `@moica.test`, `claveHash`, `secretoTotp`, `claveAlmacenamiento` ni `identificadorToken`. El detalle y las bandejas siguen sin contactos —`elDetalleDeLaSolicitudSigueSinLlevarContactos`— |
| Concurrencia entre enviar y cerrar | `ConcurrenciaDeChatIT` | Sí | Sí | Con la fila retenida por `FOR UPDATE` y las dos peticiones en cola: cancelar y completar siempre obtienen su 200, y el envío o se confirma antes (201) o se rechaza (409 `CHAT_SOLO_LECTURA`). En ambos desenlaces, **cero mensajes con `fecha_envio` posterior a la transición final** |
| Frontend | `npm run test` | Sí | Sí | 224 pruebas en 25 archivos: las 202 anteriores más 22 nuevas —14 en `ChatDeSolicitud.test.tsx` y 8 en `ContactosDelPrestador.test.tsx`—. Cubren carga, vacío, error y reintento, mensajes propios y ajenos, envío correcto, fallo que conserva el texto, doble envío impedido, mensaje en blanco, short polling y apagado del temporizador al desmontar, solo lectura en cancelada y completada, ausencia del chat cuando nunca se aceptó, cuenta restringida sin formulario, contactos visibles solo para el cliente, ocultos para el prestador y en estados no habilitados, y estado sin contactos |
| Sin regresiones de P6 | `DetalleDeSolicitud.test.tsx`, `MisSolicitudes.test.tsx` y `NuevaSolicitud.test.tsx` | Sí | Sí | Las 23 pruebas de P6 siguen verdes sin debilitarse: solo se añadieron al `beforeEach` del detalle las dos respuestas vacías de las superficies nuevas, para que esas pruebas sigan comprobando el ciclo y no el hilo |
| Verificación local completa | Comandos del criterio de salida | Sí | Sí | Backend: `./mvnw -B -ntp verify` — 148 unitarias, 379 de integración, Spotless limpio y SpotBugs sin hallazgos. Frontend: `format:check`, `lint`, `typecheck`, `test` (224) y `build` en verde. Raíz: `git diff --check` sin salida y `docker compose --env-file .env.example config -q` válido |
| Recorrido manual integrado | Compose local, backend en `:8081` y Vite en `:5173` | Sí | | Los diez pasos del recorrido, ejecutados contra la API real: el cliente envía, el prestador acepta, ambos intercambian mensajes, el cliente recibe los tres contactos (200), el prestador recibe 404 en esa misma ruta, un tercero recibe 404 al leer, al enviar y al pedir contactos, una cuenta restringida lee (200) y no envía (403 `CUENTA_RESTRINGIDA`), completar deja el historial legible y el envío en 409 `CHAT_SOLO_LECTURA`, y una solicitud cancelada desde `PENDIENTE` responde 409 `CHAT_NO_HABILITADO` y 409 `CONTACTOS_NO_REVELADOS` |
| Capturas a tres tamaños | Chrome headless con `Emulation.setDeviceMetricsOverride` (CDP) a 375x812, 768x1024 y 1280x800 | Sí | | 15 capturas de página completa: cinco escenarios por tres tamaños —cliente en aceptada con chat y contactos, prestador en la misma solicitud sin la sección de contactos, completada en solo lectura, cuenta restringida sin formulario y cancelada desde `PENDIENTE` sin chat ni contactos—. `scrollWidth === clientWidth` en las quince, con `medidas.json` que lo registra captura por captura. Fuera del repositorio, en la carpeta `moica-pr-p7-capturas` del escritorio; se adjuntan al Pull Request |

No se implementaron imágenes, audios, archivos ni documentos en el chat, edición
o borrado de mensajes, reacciones, grupos, llamadas, confirmaciones de lectura,
WebSockets, notificaciones, cifrado de extremo a extremo, calificaciones, pagos
ni mapas. El chat se actualiza por short polling: cinco segundos el hilo y veinte
el detalle, ambos detenidos al desmontar la pantalla.

El chat vive en la capacidad `chat` del backend, que le pregunta a `solicitud`
—mediante el DTO `ParticipacionEnSolicitud`— quién participa, si la solicitud
llegó a estar aceptada y si todavía admite mensajes; esa regla no se reescribe
en ninguna otra parte. En el frontend vive dentro de `capacidades/solicitud` y
no en una capacidad aparte porque allí la dependencia va al revés —el detalle
monta el chat y el chat necesita el detalle—, y separarlos dejaría a las dos
capacidades importándose entre sí.

## Calificaciones y reputacion de P8

Controles que P8 deja funcionando, con el resultado real de cada comprobación.

- **Local**: ejecutado en la máquina de desarrollo (Windows 11, Docker Desktop,
  Node 22, JDK compilando con `release 21`), con PostgreSQL de Testcontainers
  para las pruebas y el Compose local —publicado en `localhost:5433`— para el
  recorrido visual.
- **Revalidación local con Docker activo** (1 de septiembre de 2026, sobre
  `642108c`, el HEAD de la rama). Docker Desktop 4.89.0, Engine 29.7.2. En
  `backend`, `./mvnw -B -ntp verify`: **Surefire 160 y Failsafe 422, ambos con
  0 fallos, 0 errores y 0 omitidas**, Spotless limpio, SpotBugs
  `BugInstance size is 0` y `BUILD SUCCESS` en 4:27 min. Testcontainers 1.21.4
  encontró Docker por `npipe:////./pipe/docker_engine` y levantó de verdad
  `postgres:15-alpine`; los 39 informes de Failsafe registran `skipped="0"`,
  y los cuatro ITs de P8 corrieron —`EsquemaDeCalificacionesIT` 14,
  `CalificacionDeSolicitudIT` 18, `ReputacionPorRolIT` 9 y
  `ConcurrenciaDeCalificacionIT` 2—. En `frontend`, `format:check`, `lint`,
  `typecheck`, `test` (265 en 31 archivos) y `build` en verde. En la raíz,
  `docker compose --env-file .env.example config -q` válido y el Compose
  levantado: `moica_db` **healthy**, esta vez publicado en `localhost:5432`
  porque es lo que fija `.env.example`. El backend arrancó contra ese
  PostgreSQL en `:8081` y `/actuator/health` respondió `UP`.
- **CI**: los cuatro checks están en verde sobre `642108c`, el commit que cierra
  el código de P8: Backend (Java 21), Frontend (Node 22) y Entorno local (docker
  compose) en
  la [ejecución 33505099455](https://github.com/robertofabiot/moica-hackathon/actions/runs/33505099455),
  y «Título y commits convencionales» en la
  [ejecución 33505116945](https://github.com/robertofabiot/moica-hackathon/actions/runs/33505116945).
  Antes habían quedado en verde sobre `714471d`:
  [ejecución 33504722649](https://github.com/robertofabiot/moica-hackathon/actions/runs/33504722649)
  y [ejecución 33504722677](https://github.com/robertofabiot/moica-hackathon/actions/runs/33504722677).
  El commit `27a1c9f`, que solo añade esta evidencia, también quedó en verde:
  [CI 33525882979](https://github.com/robertofabiot/moica-hackathon/actions/runs/33525882979)
  y [Convenciones 33525883037](https://github.com/robertofabiot/moica-hackathon/actions/runs/33525883037).
- **Rama**: `feature/calificaciones-reputacion` nace de `develop` en `0b8ddb4`,
  comprobado con `git merge-base origin/develop HEAD`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Migración `V42` | `EsquemaDeCalificacionesIT` contra PostgreSQL real (14 pruebas) | Sí | Sí | Crea `calificacion_usuario` con identificador `BIGINT GENERATED ALWAYS AS IDENTITY`, `rol_calificado` `VARCHAR(30)`, `puntuacion` `SMALLINT`, `comentario` `TEXT` nulable y `fecha_creacion` `TIMESTAMPTZ`. Las tres claves foráneas son `RESTRICT`: no se borra la solicitud ni ninguna de las dos personas mientras haya calificaciones. `ck_calificacion_usuario_puntuacion` rechaza 0, 6 y −1; `ck_calificacion_usuario_participantes` rechaza calificarse a sí mismo; `ck_calificacion_usuario_rol` rechaza un rol fuera del dominio. Existe `ix_calificacion_usuario_calificado_rol` sobre `(id_calificado, rol_calificado, puntuacion)`. **No existe tabla `reputacion`** ni enum nativo. Comprobado además sobre el Compose local en la revalidación del 1 de septiembre de 2026: `flyway_schema_history` registra la versión `42` —«crear calificaciones de usuario»— con `success = true`, `to_regclass('public.calificacion_usuario')` la resuelve, y `pg_constraint` devuelve las nueve restricciones esperadas: la clave primaria, las tres foráneas, las dos unicidades y los tres `CHECK`. El arranque no aplicó nada más —«Schema "public" is up to date»— así que no tocó datos ajenos |
| Una calificación por participante | `EsquemaDeCalificacionesIT` y `CalificacionDeSolicitudIT` | Sí | Sí | `uq_calificacion_usuario_solicitud_calificador` impide que la misma persona emita dos; `uq_calificacion_usuario_solicitud_calificado` impide que reciba dos. Una solicitud admite exactamente dos filas, una de cada lado, y el tercer intento de cualquiera de los dos responde 409 `CALIFICACION_DUPLICADA` |
| Roles derivados del servidor | `CalificacionDeSolicitudIT.elClienteCalificaAlPrestadorYQuedaRegistradoComoPrestador` y `elPrestadorCalificaAlClienteYQuedaRegistradoComoCliente` | Sí | Sí | El cliente queda registrado calificando como `PRESTADOR` y el prestador como `CLIENTE`, comprobado en la respuesta **y** en la fila de PostgreSQL. `elCalificadoYElRolSalenDeLaSolicitudYNoDelCuerpo` envía `idCalificado`, `idCalificador` y `rolCalificado` manipulados y los tres se ignoran |
| Solo después de completar | `CalificacionDeSolicitudIT.noSePuedeCalificarAntesDeCompletar` | Sí | Sí | `PENDIENTE` y `ACEPTADA` responden 409 `SOLICITUD_NO_COMPLETADA` y no escriben ninguna fila. El estado de la consulta devuelve `solicitudCompletada: false` y `puedeCalificar: false` |
| Validación de la puntuación | `CalificacionDeSolicitudIT` y `CalificacionAEmitirTest` | Sí | Sí | 1 y 5 se admiten; 0, 6, −1 y la puntuación ausente responden 400 `VALIDACION` y no escriben. El rango se repite en PostgreSQL |
| Comentario opcional | `CalificacionDeSolicitudIT.elComentarioEsOpcionalYUnoDeEspaciosSeGuardaComoNulo` | Sí | Sí | Sin comentario se guarda `null`; un comentario de espacios y tabuladores también, tanto en la respuesta como en la fila. 2000 caracteres se admiten, 2001 no, y los espacios exteriores no cuentan porque el recorte ocurre antes de validar |
| 404 anti-enumeración | `CalificacionDeSolicitudIT.unTerceroRecibe404YNoPuedeConfirmarQueLaSolicitudExista` y `ReputacionPorRolIT.laReputacionDelClienteSoloLaVeElPrestadorParticipante` | Sí | Sí | Un tercero recibe 404 `RECURSO_NO_ENCONTRADO` al consultar, al calificar y al pedir la reputación del cliente. En esa última ruta **el propio cliente también recibe 404**: responde 200 a una sola persona, el prestador participante |
| Estado de cuenta | `CalificacionDeSolicitudIT` | Sí | Sí | `RESTRINGIDA_TEMPORAL` consulta su estado (200, `puedeCalificar: false`) pero al calificar recibe 403 `CUENTA_RESTRINGIDA` y no escribe. Suspendida: 403 `ACCESO_DENEGADO` en las dos rutas. Sin sesión: 401 |
| Sin edición ni borrado | `CalificacionDeSolicitudIT.noExistenEdicionNiBorradoDeUnaCalificacion` | Sí | Sí | `PUT` y `DELETE` sobre `/calificacion` responden 405 `METODO_NO_PERMITIDO`. La fila no cambia |
| Concurrencia y unicidad real | `ConcurrenciaDeCalificacionIT` | Sí | Sí | Una transacción externa adelanta la fila sin confirmar; la petición pasa su comprobación previa —la fila sin confirmar no es visible—, se bloquea en el índice único y, al confirmarse la externa, choca con la restricción real: responde **409 `CALIFICACION_DUPLICADA`, no 500**, y queda una sola fila con la puntuación de la que ganó. Un segundo caso dispara dos envíos simultáneos desde el mismo navegador: un 201, un 409 y una sola fila |
| Promedio, cantidad y desglose | `ReputacionPorRolIT` y `ReputacionPorRolTest` | Sí | Sí | Tres calificaciones de 5, 4 y 4 dan promedio `4.3` —13/3 redondeado a un decimal en el servidor—, cantidad 3 y desglose `5:1, 4:2, 3:0, 2:0, 1:0`. El desglose lleva siempre las cinco filas |
| Separación por rol | `ReputacionPorRolIT.laReputacionComoClienteYComoPrestadorNoSeMezclan` | Sí | Sí | En la misma solicitud, el cliente califica con 5 y el prestador con 2: la reputación pública del prestador queda en `5.0` y la del cliente en `2.0`. Las dos cifras conviven sin sumarse |
| Estado sin calificaciones | `ReputacionPorRolIT.quienNoTieneCalificacionesNoRecibeUnCeroSinoUnPromedioNulo` | Sí | Sí | `promedio: null`, `cantidad: 0` y las cinco filas del desglose en cero. **No se envía `0.0`**: no calificar no baja ningún promedio |
| Reputación en las tres superficies | `ReputacionPorRolIT` | Sí | Sí | El listado, el detalle y el perfil público llevan el mismo `4.3`. Dos servicios del mismo prestador comparten agregado, porque la reputación es de la persona. Un prestador sin calificaciones convive en el mismo listado mostrando promedio nulo |
| Sin fugas en lo público | `ReputacionPorRolIT.laReputacionPublicaNoLlevaComentariosNiIdentidades` | Sí | Sí | Ni el listado, ni el detalle, ni el perfil contienen el texto del comentario, `comentario`, `idCalificador`, `idCalificado`, `correoElectronico` ni el correo del cliente. `noExisteUnaRutaPublicaParaLaReputacionComoCliente` comprueba además que pedir el perfil público del cliente sigue respondiendo 404 |
| Una consulta por página, no por tarjeta | Revisión de `DescubrimientoDeServiciosService.buscar` | Sí | | El listado recolecta los `idPrestador` distintos y pide `reputacionesDePrestadores` una sola vez, con una consulta agrupada `GROUP BY id_calificado`. El perfil público calcula su agregado una vez y lo reparte entre sus tarjetas |
| Frontend | `npm run test` | Sí | Sí | 265 pruebas en 31 archivos: las 241 anteriores más 24 nuevas —13 en `CalificacionDeSolicitud.test.tsx`, 3 en `PrestadorPublico.test.tsx`, 4 en `ExplorarServicios.test.tsx`, 2 en `DetalleDeServicio.test.tsx` y 2 en `EstrellasCalificacion.test.tsx`—. Cubren la ausencia del formulario antes de `COMPLETADA`, la selección accesible con teclado, el envío válido, el comentario en blanco como `null`, el estado pendiente sin doble envío, el error que conserva lo escrito, el 409 del backend, el resumen inmutable, el rol del prestador, la cuenta restringida sin acción, el error de carga con reintento, y en lo público la tarjeta con reputación real, la tarjeta sin calificaciones, el detalle con promedio y desglose reales y el perfil del prestador |
| Sin cifras ficticias | `grep` sobre `frontend/src` y tres pruebas dedicadas | Sí | Sí | `CALIFICACION_DE_MUESTRA`, `RESENAS_DE_MUESTRA`, `RESENAS_DE_FICHA_DE_MUESTRA` y `DESGLOSE_DE_RESENAS_DE_MUESTRA` ya no existen en el código. Tres pruebas afirman que `4.8`, `(102)` y «120 reseñas» no aparecen en pantalla |
| Sin regresiones de P6 y P7 | `DetalleDeSolicitud.test.tsx`, `ChatDeSolicitud.test.tsx` y `ContactosDelPrestador.test.tsx` | Sí | Sí | Las pruebas anteriores siguen verdes sin debilitarse: solo se añadió al `beforeEach` la respuesta de la ruta nueva, igual que hizo P7 con `/mensajes` y `/contactos` |
| Verificación local completa | Comandos del criterio de salida | Sí | Sí | Backend: `./mvnw -B -ntp verify` — **160 unitarias y 422 de integración**, Spotless limpio y SpotBugs sin hallazgos, BUILD SUCCESS. Frontend: `format:check`, `lint`, `typecheck`, `test` (265) y `build` en verde. Raíz: `git diff --check` sin salida y `docker compose --env-file .env.example config -q` válido. **Repetida entera el 1 de septiembre de 2026 con Docker activo sobre `642108c`**, con los mismos conteos —160 y 422 en el backend, 0 omitidas; 265 en 31 archivos en el frontend— y el mismo resultado: Spotless limpio, `BugInstance size is 0` y `BUILD SUCCESS`. El detalle está en la lista de arriba |
| Recorrido manual integrado | Compose local, backend en `:8081` y Vite en `:5173`, guiado por CDP | Sí | | Los once pasos, contra la API real: se crean prestador, cliente y un segundo prestador; el cliente envía la solicitud, el prestador acepta; **antes de completar** el estado responde `solicitudCompletada: false` y `puedeCalificar: false`; el prestador completa y califica al cliente, y el rol derivado es `CLIENTE`; su segundo intento responde 409 `CALIFICACION_DUPLICADA`; el cliente califica tres solicitudes completadas con 5, 4 y 4 y el rol derivado es `PRESTADOR` en las tres; su segundo intento sobre la misma solicitud también responde 409. Los agregados observados fueron `PRESTADOR 4.3 (3)` y `CLIENTE 5.0 (1)`. La verificación básica de los dos prestadores se proyectó con SQL porque el expediente documental necesita los buckets de R2, que no están configurados en local; la verificación tiene sus propias pruebas de integración. **Repetido el 1 de septiembre de 2026 con Docker activo**, sobre `642108c` y directamente contra la API en `:8081` con `curl` —sin navegador y sin ningún doble—: los once pasos dieron **31 comprobaciones y 0 fallos**, con los mismos agregados `PRESTADOR 4.3 (3)`, desglose `1, 2, 0, 0, 0`, y `CLIENTE 5.0 (1)`. Se comprobaron además el 401 sin sesión, los tres 404 del tercero —leer, calificar y pedir la reputación del cliente— y que la cuenta restringida consulta su estado (200, `puedeCalificar: false`) pero al calificar recibe 403 `CUENTA_RESTRINGIDA` sin escribir fila |
| Capturas a tres tamaños | Chrome headless con `Emulation.setDeviceMetricsOverride` (CDP) a 375x812, 768x1024 y 1280x800 | Sí | | 15 capturas de página completa: cinco escenarios por tres tamaños —solicitud completada antes de calificar con el formulario, la misma después de calificar con el resumen inmutable, exploración con reputación real y estado vacío conviviendo, detalle con promedio y desglose reales, y perfil público del prestador—. `scrollWidth === clientWidth` en las quince, con `medidas.json` que lo registra captura por captura. Fuera del repositorio, en la carpeta `moica-pr-p8-capturas` del escritorio. Su procedencia quedó comprobada en la revalidación del 1 de septiembre de 2026: se tomaron a las 05:42–05:43, después de que Flyway aplicara `V42` a las 05:40:31 sobre el PostgreSQL de Docker Compose, y los agregados que registra `medidas.json` —`PRESTADOR 4.3 (3)` con desglose `1, 2, 0, 0, 0` y `CLIENTE 5.0 (1)`— son los mismos que devolvió la API en el recorrido repetido. **Todavía no están adjuntas al Pull Request**: adjuntarlas exige la interfaz web de GitHub, que no admite subida por `gh` |

No se implementaron edición ni borrado de calificaciones, respuestas del
calificado, reporte o moderación de una calificación —eso es P9 y P10—,
penalizaciones por no calificar, entidad o tabla materializada `Reputacion`,
listados públicos de comentarios, ordenar o filtrar el descubrimiento por
reputación, pagos ni mapas. La persistencia del botón «Guardar» sigue fuera de
alcance y no se amplió.

Las calificaciones viven en la capacidad `calificacion` del backend, que le
pregunta a `solicitud` —mediante el DTO `ParticipacionEnSolicitud`— quién
participa y en qué estado está el compromiso; esa regla no se reescribe en
ninguna otra parte. Dentro de la capacidad hay dos servicios a propósito:
`CalificacionDeSolicitudService` escribe y necesita preguntar por la solicitud, y
`ReputacionService` solo lee y no depende de ninguna otra capacidad. Esa
separación es lo que permite que el descubrimiento público pida la reputación de
un prestador sin que se forme un ciclo entre capacidades. En el frontend vive
dentro de `capacidades/solicitud`, por la misma razón que el chat: es el detalle
de la solicitud quien la monta.

## Reportes y casos de moderación de P9

Controles que P9 deja funcionando, con el resultado real de cada comprobación.

- **Local**: ejecutado en la máquina de desarrollo (Windows 11, Docker Desktop,
  Node 22, JDK compilando con `release 21`), con PostgreSQL de Testcontainers
  para las pruebas y el Compose local —publicado en `localhost:5433`— para el
  recorrido integrado.
- **Validación local con Docker activo** (2 de septiembre de 2026, sobre
  `29defa0`, ya con `develop` y el #27 integrados y con las correcciones de la
  revisión aplicadas; lo único posterior es este cambio documental, que no toca
  código). Docker Engine 29.7.2, Docker Desktop. En `backend`,
  `./mvnw -B -ntp verify`: **Surefire 167 y Failsafe 488, ambos con 0 fallos,
  0 errores y 0 omitidas**, Spotless limpio, SpotBugs sin hallazgos y
  `BUILD SUCCESS` en 5:54 min. Testcontainers 1.21.4
  levantó de verdad `postgres:15-alpine`; **no se usa H2 en ninguna prueba**.
  Los cuatro conjuntos de P9 corrieron —`EsquemaDeCasosDeModeracionIT` 35,
  `ReporteDeParticipanteIT` 28, `ConcurrenciaDeReporteIT` 3 y
  `ReporteAPresentarTest` 7—. En `frontend`, `format:check`, `lint`,
  `typecheck`, `test` (**298 en 33 archivos**) y `build` en verde. En la raíz,
  `git diff --check` sin hallazgos y
  `docker compose --env-file .env.example config -q` válido.
- **Recorrido integrado sin mocks** (misma fecha). Compose local con `moica_db`
  **healthy** en `localhost:5433`, backend real en `:8080` y Vite en `:5173`.
  Flyway aplicó `V50` y `V51` *out of order* sobre una base que estaba en `v90`
  y quedó en `v51`; `/actuator/health` respondió `{"status":"UP"}`. Las **48
  comprobaciones** del recorrido pasaron, incluidas las que se hacen
  directamente en PostgreSQL.
- **Rama**: `feature/casos-moderacion` nace de `develop` en `763a4ba`, el merge
  de P8, comprobado con `git merge-base origin/develop HEAD`. Después integra
  `origin/develop` en `428c1ddc` —los PR #26 y #27— mediante el merge `b1cc6c0`.
  El único conflicto estuvo en este documento, en las filas 3 a 7, y se resolvió
  conservando los dos lados: la lista de PR quedó como la unión ordenada y la
  evidencia del asistente de publicación y del perfil público se conserva
  íntegra junto a la de P9.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Migración `V50` | `EsquemaDeCasosDeModeracionIT` contra PostgreSQL real | Sí | Sí | Crea `medida_administrativa`, `caso_moderacion` e `historial_caso` con identificadores `IDENTITY`, `TIMESTAMPTZ` en las fechas, `motivo varchar(120)`, dominios como `VARCHAR` + `CHECK` —ningún enum nativo— y FK `ON DELETE RESTRICT`. Los tipos se comprueban uno a uno contra `information_schema.columns`. |
| Migración `V51` | `EsquemaDeCasosDeModeracionIT.laExtensionBtreeGistEstaInstalada` y `laRestriccionDeExclusionEstaDeclaradaSobreElCasoYElPeriodo` | Sí | Sí | `pg_extension` contiene `btree_gist`, y `pg_get_constraintdef` de `ex_historial_caso_vigencia` contiene `EXCLUDE USING gist`, `id_caso_moderacion WITH =`, `tstzrange`, `'[)'` y `WITH &&`. |
| Un caso por participante y solicitud | `EsquemaDeCasosDeModeracionIT.cadaParticipanteAbreComoMaximoUnCasoPorSolicitud` y `ReporteDeParticipanteIT.nadieReportaDosVecesLaMismaSolicitud` | Sí | Sí | La segunda inserción del mismo reportante viola `uq_caso_moderacion_solicitud_reportante`; por la API el segundo envío responde 409 `REPORTE_DUPLICADO` y la solicitud conserva un solo caso. |
| Máximo de dos casos por solicitud | `EsquemaDeCasosDeModeracionIT.unaSolicitudAdmiteUnCasoDeCadaParticipante` y `ReporteDeParticipanteIT.cadaParticipanteAbreSuPropioCasoSobreLaMismaSolicitud` | Sí | Sí | Cliente y prestador abren cada uno el suyo: la solicitud queda con dos casos y ninguno más cabe. |
| Nadie se reporta a sí mismo | `EsquemaDeCasosDeModeracionIT.nadiePuedeReportarseASiMismo` | Sí | Sí | `ck_caso_moderacion_participantes` rechaza `id_reportante = id_reportado`. Por la API no es formulable: el reportado se deriva de la solicitud. |
| Coherencia del cierre | `EsquemaDeCasosDeModeracionIT.elCierreExigeResultadoResolucionYFechaALaVez` | Sí | Sí | `CERRADO` sin resultado, resolución o fecha se rechaza; y un caso que no está cerrado tampoco puede arrastrar una decisión vigente. |
| Fecha final de medida posterior a la apertura | `EsquemaDeCasosDeModeracionIT.laFechaFinDeLaMedidaDebeSerPosteriorALaApertura` | Sí | Sí | `ck_caso_moderacion_fecha_fin_medida` rechaza una fecha anterior a `fecha_apertura`. |
| Una sola versión vigente por caso | `EsquemaDeCasosDeModeracionIT.soloExisteUnaVersionActualPorCaso` y `elIndiceDeVersionActualEsUnicoYParcial` | Sí | Sí | La segunda versión vigente se rechaza y el caso conserva una. `pg_indexes` confirma que `uq_historial_caso_version_actual` es `UNIQUE` sobre `id_caso_moderacion` y lleva `WHERE`. |
| Número de versión positivo y sin repetir | `EsquemaDeCasosDeModeracionIT.elNumeroDeVersionDebeSerPositivo` y `noSeRepiteUnNumeroDeVersionDentroDelMismoCaso` | Sí | Sí | `0` y `-1` violan `ck_historial_caso_numero_version`; repetir el `1` viola `uq_historial_caso_version`. |
| Actor coherente con su tipo | `EsquemaDeCasosDeModeracionIT.unEventoDelSistemaNoTieneActorYLosDemasSiLoTienen` | Sí | Sí | `SISTEMA` con actor identificado y `USUARIO` sin actor se rechazan; `SISTEMA` con actor nulo se admite. |
| Vigencia coherente con `es_version_actual` | `EsquemaDeCasosDeModeracionIT.laVersionActualNoTieneFinYUnaCerradaTerminaDespuesDeEmpezar` | Sí | Sí | La versión actual con fin declarado, la cerrada sin fin y la cerrada que termina antes de empezar se rechazan las tres. |
| Periodos SCD2 sin superposición | `EsquemaDeCasosDeModeracionIT.dosVersionesDelMismoCasoNoPuedenSuperponerse`, `dosVersionesConsecutivasCompartenElInstanteDeTransicion`, `laVersionActualExcluyeCualquierPeriodoPosterior` y `dosCasosDistintosPuedenTenerPeriodosIguales` | Sí | Sí | Una versión que empieza dentro del periodo de otra se rechaza; una que empieza justo donde la anterior terminó se admite, porque el intervalo es semiabierto; una versión vigente excluye cualquier periodo posterior; y dos casos distintos sí pueden tener periodos iguales, porque la exclusión es por caso. |
| Catálogo de medidas vacío | `EsquemaDeCasosDeModeracionIT.elCatalogoDeMedidasLlegaVacio` y `ReporteDeParticipanteIT.reportarNoCambiaLaSolicitudNiLasCuentasNiCreaMedidasNiAsignaAdministrador` | Sí | Sí | `medida_administrativa` se crea porque las otras dos tablas la referencian, pero P9 no siembra ninguna fila: elegir, gestionar y aplicar medidas es P10B. |
| Reportado derivado del servidor | `ReporteDeParticipanteIT.elClienteReportaAlPrestadorYElServidorDerivaAlReportado`, `elPrestadorReportaAlClienteYElServidorDerivaAlReportado` y `elCuerpoNoPuedeElegirAQuienSeReporta` | Sí | Sí | El cliente reporta al prestador y el prestador al cliente. Un cuerpo con `idReportado`, `idReportante` y `estadoActual` manipulados se ignora: el caso queda con la contraparte real y `ABIERTO`. |
| Estados que admiten reporte | `ReporteDeParticipanteIT.seReportaDesdeUnaSolicitudAceptada`, `seReportaDespuesDeCompletarLaSolicitud` y `seReportaDespuesDeCancelarUnaSolicitudQueEstuvoAceptada` | Sí | Sí | Los tres responden 201. Lo que decide es haber llegado a `ACEPTADA`, no el estado vigente. |
| Estados que no lo admiten | `ReporteDeParticipanteIT.noSeReportaDesdeUnaSolicitudPendiente`, `noSeReportaDesdeUnaSolicitudRechazada` y `noSeReportaDesdeUnaSolicitudCanceladaQueNuncaSeAcepto` | Sí | Sí | Los tres responden 409 `SOLICITUD_NO_REPORTABLE` y no escriben ninguna fila. La `CANCELADA` desde `PENDIENTE` se distingue de la que sí se aceptó por el historial de transiciones. |
| 404 anti-enumeración | `ReporteDeParticipanteIT.unTerceroNoDistingueUnaSolicitudAjenaDeUnaInexistente` | Sí | Sí | Un tercero recibe 404 `RECURSO_NO_ENCONTRADO` en las dos rutas y no puede confirmar que la solicitud exista. |
| Estado de cuenta | `ReporteDeParticipanteIT.unaCuentaRestringidaConservaElReporteYSuConsulta` y `unaCuentaSuspendidaNoLlegaSiquieraAlRecurso` | Sí | Sí | `RESTRINGIDA_TEMPORAL` consulta (200, `puedeReportar: true`), reporta (201) y vuelve a consultar su caso. Una suspendida recibe 403 `ACCESO_DENEGADO` en ambas rutas, desde la cadena de seguridad, y no escribe. |
| El caso propio es propio | `ReporteDeParticipanteIT.nadieVeElCasoQuePresentoLaContraparte` y `despuesDeReportarDevuelveElCasoPropioYCierraLaAccion` | Sí | Sí | Tras reportar el cliente, la consulta del prestador devuelve `casoAbierto: null` y `puedeReportar: true`. El caso ajeno no viaja como recurso ni deja motivo, descripción ni identificador en el nivel superior de la respuesta. |
| Primera versión SCD2 | `ReporteDeParticipanteIT.laPrimeraVersionSeCreaConLosValoresDeLaApertura`, `laVersionFotografiaElEstadoRealDeLaCuentaReportada` y `laFechaDeAperturaYLaDeInicioDeVigenciaSonElMismoInstante` | Sí | Sí | Versión 1, `USUARIO`, `CASO_ABIERTO`, `ABIERTO`, actor = reportante, afectado = reportado, `es_version_actual` con fin nulo, `detalle_cambio` no vacío, y responsable, medida, resultado, resolución y fecha de fin de medida en nulo. `estado_cuenta` es el estado **real** de la cuenta reportada: con una cuenta restringida la versión guarda `RESTRINGIDA_TEMPORAL` sin que el reporte la cambie. Las tres fechas salen del mismo reloj. |
| Atomicidad de caso e historial | `ReporteDeParticipanteIT.ningunCasoQuedaSinSuVersionInicial` y `ConcurrenciaDeReporteIT` | Sí | Sí | Ningún caso existe sin una versión 1 vigente, ni siquiera cuando hay reportes rechazados de por medio; y el envío que pierde la carrera no deja caso ni versión huérfanos. |
| Concurrencia y unicidad real | `ConcurrenciaDeReporteIT` (3 pruebas) | Sí | Sí | Una transacción externa adelanta el caso sin confirmar; la petición pasa su comprobación previa y espera en el índice único, y al confirmarse la externa recibe **409 y no 500**, sin dejar versión suelta. Dos envíos simultáneos del mismo participante dejan un caso y una versión. Los dos participantes reportando a la vez abren cada uno el suyo: dos casos y dos versiones. |
| Reportar no cambia nada más | `ReporteDeParticipanteIT.reportarNoCambiaLaSolicitudNiLasCuentasNiCreaMedidasNiAsignaAdministrador` | Sí | Sí | La solicitud sigue `ACEPTADA` sin transiciones nuevas, las dos cuentas siguen `ACTIVA`, no hay medidas ni administrador asignado, el caso no tiene resultado ni resolución y las sesiones vigentes no se tocan. |
| Sin edición ni borrado | `ReporteDeParticipanteIT.unReporteNoSeEditaNiSeRetira` | Sí | Sí | `PUT` y `DELETE` sobre `/caso-moderacion` responden 405 `METODO_NO_PERMITIDO`. El caso no cambia. |
| Respuesta sin datos administrativos | `ReporteDeParticipanteIT.laRespuestaNoExponeNadaAdministrativo` | Sí | Sí | La respuesta lleva exactamente ocho campos y ninguno es responsable, medida, resultado, resolución ni fecha de cierre o de fin de medida. |
| Validación de la entrada | `ReporteDeParticipanteIT` y `ReporteAPresentarTest` | Sí | Sí | Motivo o descripción ausentes, vacíos o de solo espacios responden 400 `VALIDACION`; 121 caracteres de motivo y 3001 de descripción también; 120 y 3000 se admiten. Los dos textos se recortan antes de validarse, así que los espacios exteriores no cuentan para el tope ni se guardan. |
| Frontend | `npm run test` | Sí | Sí | 298 pruebas en 33 archivos: las 279 del #27 más 19 de este PR en `ReporteDeSolicitud.test.tsx` —visibilidad según el historial de la solicitud, etiquetas y nombres accesibles, validación con `aria-invalid` y `aria-describedby`, contador atado a la descripción, cuerpo enviado, resumen posterior, error del backend con lo escrito conservado y reintento, conflicto 409 que refresca el estado, prevención del doble envío y de la ventana entre confirmación y refresco, y cuenta restringida—. |
| Sin regresiones de P6, P7, P8 y el #27 | `DetalleDeSolicitud.test.tsx`, `ChatDeSolicitud.test.tsx`, `ContactosDelPrestador.test.tsx`, `CalificacionDeSolicitud.test.tsx` y `NuevoServicio.test.tsx` | Sí | Sí | Las pruebas anteriores siguen verdes sin debilitarse: a las del detalle se les añadió la preparación de la ruta nueva, y el asistente de publicación del #27 se conserva íntegro tras el merge. |
| Verificación local completa | Comandos del criterio de salida | Sí | Sí | Backend: `./mvnw -B -ntp verify` — **167 unitarias y 488 de integración**, 0 fallos, 0 errores y 0 omitidas, Spotless limpio, SpotBugs sin hallazgos, `BUILD SUCCESS`. Frontend: `format:check`, `lint`, `typecheck`, `test` (298 en 33 archivos) y `build`, todos en verde. Raíz: `git diff --check` sin hallazgos y `docker compose --env-file .env.example config -q` válido. |
| Recorrido integrado sin mocks | Compose local, backend real en `:8080` y Vite en `:5173` | Sí | | Flyway aplicó `V50` y `V51` sobre la base existente y `/actuator/health` respondió `UP`. Contra la API real: en `PENDIENTE` la consulta dice `solicitudReportable: false` y el reporte responde 409; tras aceptar, el cliente reporta (201), el duplicado responde 409, la consulta pasa a `puedeReportar: false` y devuelve el caso; el prestador no ve el ajeno y abre el suyo; un tercero recibe 404. En PostgreSQL: caso `ABIERTO`, versión 1 vigente con `USUARIO`/`CASO_ABIERTO`/`ACTIVA`, sin administrador ni medida, ningún caso sin versión inicial, solicitud `ACEPTADA` sin transiciones nuevas, cuentas `ACTIVA`, `medida_administrativa` vacía y ninguna sesión revocada por moderación. **48 comprobaciones, todas en verde.** |
| Capturas a tres tamaños | Chrome headless con `Emulation.setDeviceMetricsOverride` (CDP) a 375x812, 768x1024 y 1280x800 | Sí | | 15 capturas de página completa: cinco escenarios por tres tamaños —solicitud aceptada antes de reportar, formulario con la validación a la vista, caso abierto tras reportar, solicitud que nunca fue aceptada y cuenta `RESTRINGIDA_TEMPORAL`—. En los quince casos `scrollWidth == clientWidth`, así que no hay desbordamiento horizontal; las medidas quedan en `medidas.json`. Los archivos están preparados en `moica-pr-capturas-p9`, **fuera del repositorio, y todavía no adjuntos al PR**: el entorno no puede subir imágenes a GitHub, y el PR lleva el Markdown exacto que hay que completar al soltarlos. |

No se implementaron bandeja ni navegación administrativa de casos, consulta
administrativa de solicitudes, chat o evidencias, asignación o reasignación de
administradores, cambios administrativos de estado, resoluciones `PROCEDENTE` o
`DESESTIMADO`, CRUD, semillas, selección, aplicación, revocación, sustitución o
expiración de medidas, cambios de `EstadoCuenta` o revocación de sesiones por
moderación, apelaciones, recomendaciones, umbrales, reincidencia o sanciones
automáticas, adjuntos o evidencia multimedia, ni edición o eliminación de
reportes. Todo eso es P10A, P10B o post-MVP.

Los casos viven en la capacidad `moderacion` del backend, que le pregunta a
`solicitud` —mediante el DTO `ParticipacionEnSolicitud`— quién participa y si el
compromiso llegó a existir; esa regla no se reescribe aquí, igual que hacen
`chat` y `calificacion`. `MedidaAdministrativa` no tiene entidad JPA: el caso y
la versión la referencian por identificador, y crear la entidad ahora sería
anticipar P10B. En el frontend vive dentro de `capacidades/solicitud`, por la
misma razón que el chat y la calificación: es el detalle de la solicitud quien
lo monta.

Dos decisiones que conviene no reabrir sin motivo. La primera: la condición para
reportar es **haber llegado a `ACEPTADA`**, no el estado vigente, porque una vez
que hubo trato el derecho a reportarlo no caduca. La segunda: una cuenta
`RESTRINGIDA_TEMPORAL` **conserva** el reporte, al contrario que calificar o
contratar, porque reportar es la vía por la que alguien pide ayuda.

**Revisión del incremento antes de la revisión humana.** Una pasada adversarial
sobre autorización y privacidad, estados válidos e inválidos, estado de cuenta,
duplicados y concurrencia, atomicidad, invariantes SCD2, contrato HTTP,
interfaz y regresiones del merge no encontró ningún defecto en el backend ni en
las migraciones. Sí corrigió cinco cosas, cada una con la prueba que falla sin
el arreglo: `dddf090` invalida el estado también cuando el backend responde 409,
porque si no la pantalla quedaba mostrando a la vez el aviso de reporte
duplicado y el formulario; `ccc0f67` mantiene el formulario bloqueado entre la
confirmación y el refresco, para que una conexión lenta no invite a un segundo
envío condenado al conflicto; `a74c0cd` deja neutra la ruta del reporte en las
pruebas del detalle, que la ejercitaban en su rama de error sin quererlo;
`4a513a4` ata el contador de caracteres a la descripción, que con dos campos de
texto quedaba sin dueño; y `29defa0` añade `SOLICITUD_NO_REPORTABLE` y
`REPORTE_DUPLICADO` al catálogo de códigos del contrato, donde faltaban pese a
estar documentados más arriba en el mismo archivo. `803a6f1` sustituyó además
una comprobación frágil de la prueba de privacidad —buscaba el número del caso
en todo el JSON, y las secuencias de identidad pueden coincidir sin que haya
filtración— por afirmaciones sobre el contenido que sí la protegen.

**Pendientes preexistentes, ajenos a P9.** El catálogo de códigos del contrato
arrastraba tres ausencias anteriores a este incremento —`CALIFICACION_DUPLICADA` y
`SOLICITUD_NO_COMPLETADA`, del #25, y `SUBCATEGORIA_NO_DISPONIBLE`, del #17—:
se dejaron anotadas y no se tocaron aquí, por quedar fuera del alcance de P9.
**Se cerraron en P10A**, que las añadió al catálogo final de
`Docs/Dev/ContratoDeApi.md` tras contrastarlas contra los servicios y las
pruebas que ya las emitían: `CatalogoDeServiciosService.exigirSubcategoria`
(400) y `CalificacionDeSolicitudService` (409 en los dos casos), cubiertas por
`ServicioPublicadoIT`, `CalificacionDeSolicitudIT` y
`ConcurrenciaDeCalificacionIT`. Fue una omisión de redacción: ningún
comportamiento cambió y no se añadieron pruebas para comprobar una lista de
Markdown. Y el contenedor `moica_pgadmin` sigue
reiniciándose por el correo `.local` de `MOICA_PGADMIN_EMAIL`. No afecta a
PostgreSQL, a Testcontainers ni a este incremento, y no se tocó.

## Maqueta de configuración de la cuenta

Controles de la maqueta de `/seguridad` (Configuración, pestañas, filas de
cuenta y segundo factor con el sistema de diseño), con el resultado real de
cada comprobación.

- **Local**: `npx vitest run` en el frontend sobre `2281c02`: 202 pruebas en 23
  archivos en verde. Las 18 de `SeguridadCuenta.test.tsx` y las 5 de
  `useVigilanciaDeSesion.test.tsx` siguen cubriendo validación, cambio de
  clave, ciclo TOTP, secreto descartado y vigilancia; los localizadores se
  adaptaron al formulario desplegable y al enlace Inicio.
- **CI**: los tres trabajos del flujo «CI» quedaron en verde sobre el commit
  final `8ad6cef` —Backend (Java 21), Frontend (Node 22) y Entorno local
  (docker compose)—:
  [ejecución 33332981626](https://github.com/robertofabiot/moica-hackathon/actions/runs/33332981626).
  El check «Título y commits convencionales» también quedó en verde:
  [ejecución 33333277266](https://github.com/robertofabiot/moica-hackathon/actions/runs/33333277266).
- **Revisión**: `ErvingMiranda` aprobó el PR el 30 de agosto de 2026 sobre
  `8ad6cef`.
- **Rama**: `feat/ui-configuracion-cuenta` nace de `develop` en `a9884a9`, el
  HEAD de esa rama al abrir el PR. El #18 se integró en `develop` mediante el
  merge `cfd8cd0`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Tokens nuevas en `/seguridad` | Revisión de `seguridad.module.css` | Sí | | Solo `--color-*`, `--gap-*`, `--radius-*`, `--shadow-*` y `--font-*`. No quedan alias `--moica-*` en esta hoja |
| `Entrada` y `Boton` | Revisión del JSX y `npm run test` | Sí | | Cambio de clave, activación y desactivación TOTP dejan de usar inputs y botones nativos de `formulario.module.css`. Desactivar usa `variante="contorno"` con color de error |
| Toggle de contraseña | Revisión de `Entrada.tsx` | Sí | | En `type="password"` aparece un control para mostrar u ocultar el valor. No suma pruebas nuevas |
| Pestañas y filas de maqueta | Revisión de `SeguridadCuenta.tsx` | Sí | | Título «Configuración». Pestañas Perfil, Cuenta, Notificaciones, Privacidad y Pagos; solo Cuenta está `aria-selected`. Correo, teléfono, idioma y zona horaria son presentacionales, sin endpoint |
| Contraseña por divulgación progresiva | `SeguridadCuenta.test.tsx` | Sí | | La fila muestra `********` y «Cambiar»; el formulario se monta al expandir. Las 18 pruebas de la pantalla siguen verdes tras adaptar el localizador |
| Segundo factor | `SeguridadCuenta.test.tsx` | Sí | | Sigue el ciclo activar / confirmar / desactivar / obligatorio administrativo. Vive en una tarjeta aparte bajo las filas de cuenta. El secreto de ejemplo no reaparece al salir y volver |
| Barra lateral | Revisión de CSS y JSX | Sí | | Se monta en `/seguridad` con destinos inicio, configuración y perfil. En escritorio `barraLateralMovil` queda `display: none` |
| Pruebas del frontend | `npx vitest run` | Sí | | 202 en verde, 23 archivos: las mismas de P6. `SeguridadCuenta.test.tsx` no suma casos; cambia el clic a «Cambiar» y la salida a «Inicio» |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `a9884a9`, el HEAD de `develop`. El PR #18 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | Sí | | Seis capturas adjuntas al PR #18, dos por cada tamaño: las filas de cuenta y el segundo factor a 375x812, 768x1024 y 1280x800 |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Este PR no dejó un `medidas.json` como los de P4, P4V, P5 y P6, así que no se anota `scrollWidth == clientWidth`: las capturas son la única evidencia |

El contrato de `/api/auth/clave` y del segundo factor no cambia. Correo,
teléfono, idioma y zona horaria no tienen escritura. Las pestañas que no son
Cuenta no navegan.

## Pie de página institucional y componentes base

Controles del pie de la portada y de dos piezas reutilizables del sistema de
diseño (`EstrellasCalificacion` e `InsigniaVerificado`), con el resultado real
de cada comprobación.

- **Local**: `npx vitest run` y `tsc -b --noEmit` en el frontend sobre `50f4b91`:
  230 pruebas en 28 archivos en verde. Las de sesión, hero y categorías de
  `Inicio.test.tsx` siguen cubriendo el recorrido anterior; se acotó «Eventos»
  al `region` de categorías porque el pie reutiliza esa etiqueta.
- **CI**: el #22 se abre contra `develop`. El merge-base con `origin/develop`
  es `0b17f46`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/ui-pie-de-pagina-y-componentes-base` nace de `develop`
  actualizado. El #22 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| `PieDePagina` | `PieDePagina.test.tsx` y revisión del JSX | Sí | | Landmark `contentinfo`, lockup con `moica-horizontal-blanco.png` hacia `/`, lema «La confianza se construye entre todos. Únete a la comunidad.», columnas Moica / Ayuda / Comunidad / Síguenos y copyright `© 2026 Moica. Todos los derechos reservados.` Fondo `--color-secondary-700`, texto `--color-neutral-0`. En teléfono las columnas son `details`/`summary`; desde 48 rem, cuadrícula con `padding: var(--gap-3xl) var(--gap-2xl)` |
| Lockup de marca | Revisión de `PieDePagina.tsx` y de los PNG | Sí | | El isotipo horizontal blanco conserva color y tipografía; no se invierte el SVG bn. `object-fit: contain` evita recortar el borde derecho |
| `EstrellasCalificacion` | `EstrellasCalificacion.test.tsx` | Sí | | Recibe `calificacion` y, opcional, `totalResenas`. Pinta estrella `--color-warning-500`, nota en `--font-weight-bold` y reseñas en `--color-neutral-600`. Anuncia «Calificación 4.8 de 5, 120 reseñas». Se exporta; no se monta todavía en tarjetas públicas |
| `InsigniaVerificado` | `InsigniaVerificado.test.tsx` | Sí | | Píldora con sello circular `--color-success-500` y el texto «Verificado», para no depender solo del color. Se exporta; no sustituye a `InsigniaDeVerificacion` del flujo documental |
| Integración en la portada | `Inicio.test.tsx` | Sí | | `<PieDePagina />` cierra `Inicio`. El `main` crece (`flex: 1`) para dejar el pie al fondo de la pantalla |
| Tokens | Revisión de los CSS Modules | Sí | | Solo variables de `src/estilos/global.css`. El divisor del pie usa `color-mix` sobre `--color-neutral-0` al 15 % |
| Pruebas del frontend | `npx vitest run` | Sí | | 230 en verde, 28 archivos: las 224 de P7 más 6 nuevas. `Inicio.test.tsx` no suma un archivo; afirma el pie dentro del caso del hero |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `0b17f46`, el HEAD de `develop`. El #22 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | **No** | | **Pendiente.** El espacio para teléfono, tableta y escritorio queda en el Pull Request; no hay todavía el juego de capturas ni `scrollWidth == clientWidth` |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Sin capturas no se anota desbordamiento |

No se implementan P8 ni escritura de reseñas. Los enlaces del pie son anclas de
maqueta (`#sobre-moica`, `#terminos`, …) salvo las redes, que abren Facebook,
Instagram y YouTube en una pestaña nueva. Las estrellas y la insignia quedan
listas para las superficies de reputación; cablearlas es trabajo de P8.

## Exploración pública de servicios

Controles del rediseño de `/explorar` según el sistema de diseño, con el
resultado real de cada comprobación.

- **Local**: `npx vitest run` en el frontend sobre `79d6fd7`: 230 pruebas en 28
  archivos en verde. `ExplorarServicios.test.tsx` conserva sus 8 casos
  (carga, error, vacío, «A convenir», texto inicial, limpiar, combinar filtros y
  detalle) y adapta los selectores a la maqueta. Los commits posteriores de la
  rama no suman archivos de prueba.
- **CI**: el #23 se abre contra `develop`. El merge-base con `origin/develop`
  es `4b512fc`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/ui-explorar-servicios` nace de `develop` actualizado. El
  #23 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Iconos de categoría | Revisión de `iconosDeCategorias.tsx` | Sí | | Ocho SVG inline (`viewBox="0 0 24 24"`, `stroke="currentColor"`): Hogar, Construcción, Transporte, Tecnología, Belleza, Eventos, Educación y Más categorías |
| Barra superior | Revisión de `ExplorarServicios.tsx` | Sí | | Barra sticky `--color-neutral-0` con borde `--color-neutral-200`. Píldora de búsqueda (`Entrada` fusionada, `IconoLupa`), selector de ciudad (`IconoPin`, «Managua, NIC»), campana con punto `--color-error-500` y avatar. En teléfono el buscador se compacta. Con sesión, el avatar abre un menú (perfil, servicios, solicitudes, seguridad, salir) |
| Sidebar de categorías y filtros | Revisión de `FiltrosPublicos.tsx` | Sí | | «Ver todas» en `--color-primary-500`. La categoría activa usa `--color-primary-50` / `--color-primary-700` y `--radius-md`. En `< 48rem` las categorías van en fila con `overflow-x: auto; scrollbar-width: none`. Ubicación y precio son desplegables. Categoría y municipio se aplican al instante a `useSearchParams` |
| Precio máximo | Revisión de `ExplorarServicios.tsx` | Sí | | El rango (Hasta C$200, C$500, C$1.000) filtra en el cliente sobre `precioReferencia`; no es un parámetro del contrato `GET /api/servicios`. Un precio nulo («A convenir») no entra en los topes |
| Tarjeta de servicio | `ExplorarServicios.test.tsx` | Sí | | Foto 16:9 o placeholder `--color-secondary-100` con icono de oficio. Título `--font-size-lg`. `EstrellasCalificacion` con maqueta 4.8 (102) —el listado público todavía no publica reputación—. Precio «Desde **C$450**» o «A convenir». `InsigniaVerificado` en la tarjeta. Grid 1 / 2 / 3 columnas desde 48 rem y 64 rem |
| Pie de página | Revisión del JSX | Sí | | `<PieDePagina />` cierra `/explorar` |
| URL y consulta | `ExplorarServicios.test.tsx` | Sí | | Sigue `texto`, `idCategoria` e `idMunicipio` en `useSearchParams` y `useServiciosPublicos`. Carga, error con reintentar y vacío se conservan. La subcategoría deja de exponerse en la maqueta |
| Pruebas del frontend | `npx vitest run` | Sí | | 230 en verde, 28 archivos: las mismas del #22. No se suman archivos de prueba |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `4b512fc`, el HEAD de `develop`. El #23 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | **No** | | **Pendiente.** El espacio para teléfono, tableta y escritorio queda en el Pull Request; no hay todavía el juego de capturas ni `scrollWidth == clientWidth` |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Sin capturas no se anota desbordamiento |

El contrato de `GET /api/servicios` no cambiaba en el #23 y la nota de las
tarjetas era entonces maqueta visual. **P8 la sustituyó por reputación real**:
`ResumenPublicoDeServicio` lleva ahora `reputacionPrestador`, y quien no tiene
calificaciones aparece como «Sin calificaciones», nunca como `0.0`. El detalle público se maqueta
en el #24; el perfil del prestador conserva su layout anterior.

## Detalle público de servicio

Controles del rediseño de `/explorar/servicios/:idServicio` según el sistema de
diseño, con el resultado real de cada comprobación.

- **Local**: `npx vitest run` en el frontend sobre `6a7666f`: 241 pruebas en 29
  archivos en verde. `DetalleDeServicio.test.tsx` aporta 11 casos (carga, error
  con reintentar, migas, ficha, precio «Desde», placeholder, galería, prestador,
  desglose, contratación y Guardar). `ExplorarServicios.test.tsx` y
  `NuevaSolicitud.test.tsx` siguen pasando contra el detalle. Formato, lint,
  typecheck y `vite build` en verde. `docker compose --env-file .env.example
  config -q` en verde. `./mvnw verify` no se ejecutó aquí: este PR no toca el
  backend y el entorno local no tiene JDK 21; el check de CI lo cubre.
- **CI**: el #24 se abre contra `develop`. El merge-base con `origin/develop`
  es `8084acb`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/ui-detalle-servicio` nace de `develop` actualizado. El
  #24 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Iconos | Revisión de `iconos.tsx` | Sí | | `IconoEstrella` relleno con `currentColor`, `IconoCheckCirculo` y `IconoGuardar`/`IconoMarcador` en trazo. `EstrellasCalificacion` reutiliza la estrella compartida |
| Migas de pan | `DetalleDeServicio.test.tsx` | Sí | | `Inicio` → `/`, categoría → `/explorar?idCategoria=…`, el nombre del servicio es la página actual. `IconoCasa` acompaña Inicio |
| Galería | `DetalleDeServicio.test.tsx` | Sí | | Imagen principal `min-height: 340px`, `object-fit: cover`, `--radius-xl`. Miniaturas con `aria-pressed`; la activa usa borde `--color-primary-500`. Sin imágenes, placeholder `--color-secondary-50` con el isotipo |
| Ficha de contratación | `DetalleDeServicio.test.tsx` | Sí | | Título `--font-size-2xl`. Nota de maqueta 4.8 (120 reseñas). Ubicación con `IconoPin`. Precio «Desde C$200» o «A convenir». Garantías con `IconoCheckCirculo`. `AccionDeSolicitud` conserva `admiteContratacion`. Botón Guardar `variante="contorno"` |
| Prestador | `DetalleDeServicio.test.tsx` | Sí | | Enlace a `/explorar/prestadores/:id`. `InsigniaVerificado` junto al nombre. `InsigniaResponsable` conserva significado y advertencia |
| Desglose de reseñas | `DetalleDeServicio.test.tsx` | Sí | | En el #24 era maqueta visual: 4.8 «De 5» y barras `--color-primary-500` para 5, 4 y 3 estrellas (80 / 30 / 10). **P8 lo sustituyó por el desglose real** del prestador, con las cinco filas y el estado «Sin calificaciones» cuando no hay ninguna |
| Pie de página | `DetalleDeServicio.test.tsx` | Sí | | `<PieDePagina />` cierra carga, error y detalle cargado |
| Layout | Revisión de `detalleServicio.module.css` | Sí | | Una columna en teléfono (orden galería → ficha → descripción → prestador → reseñas). Desde 64 rem, dos columnas; la ficha queda sticky |
| Pruebas del frontend | `npx vitest run` | Sí | | 241 en verde, 29 archivos: las 230 del #23 más 11 nuevas |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `8084acb`, el HEAD de `develop`. El #24 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | **No** | | **Pendiente.** El espacio para teléfono, tableta y escritorio queda en el Pull Request; no hay todavía el juego de capturas ni `scrollWidth == clientWidth` |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Sin capturas no se anota desbordamiento |

El contrato de `GET /api/servicios/{id}` no cambiaba en el #24 y la reputación de
la ficha era entonces maqueta visual. **P8 la sustituyó por reputación real**:
`DetallePublicoDeServicio` lleva ahora `reputacionPrestador`. Guardar sigue sin
persistir: solo alterna el estado local, y P8 no lo amplía.

## Perfil público del prestador

Controles del rediseño de `/explorar/prestadores/:idPrestador` según el sistema de
diseño, con el resultado real de cada comprobación.

- **Local**: `npx vitest run` en el frontend sobre `f22fe66`: 273 pruebas en 31
  archivos en verde. `PrestadorPublico.test.tsx` aporta 11 casos (carga, error con
  reintentar, reputación real, vacío sin cero, reseñas individuales, cabecera sin
  cifras de maqueta, servicios, portafolio, marco, seguir y contactar).
  `DetalleDeServicio.test.tsx` y `ExplorarServicios.test.tsx` abren el popover de
  `InsigniaResponsable` con hover. Formato, lint, typecheck y `vite build` en
  verde. `docker compose --env-file .env.example config -q` en verde. `./mvnw
  verify` no se ejecutó aquí: este PR no toca el backend; el check de CI lo cubre.
- **CI**: el #26 se abre contra `develop`. El merge-base con `origin/develop` es
  `763a4ba`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/ui-perfil-prestador-publico` nace de `develop` actualizado. El
  #26 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Iconos | Revisión de `iconos.tsx` | Sí | | `IconoUsuario`, `IconoMaletin`, `IconoPulgarArriba`, `IconoHerramienta`, `IconoReloj`, `IconoChevronDerecha` e `IconoCampana` en trazo, `viewBox="0 0 24 24"`, `currentColor` |
| Cabecera | `PrestadorPublico.test.tsx` | Sí | | Avatar circular de 7.5 rem (foto o iniciales). `InsigniaVerificado` junto al nombre. Oficio desde la primera subcategoría. `EstrellasCalificacion` con `reputacionPrestador` real. Ubicación `Municipio, NIC`. Descripción, cobertura e `InsigniaResponsable` |
| Métricas | `PrestadorPublico.test.tsx` | Sí | | Tipo de prestador, cantidad de servicios publicados y porcentaje de calificaciones de 4 o 5 estrellas. Sin calificaciones se lee «Sin calificaciones», nunca `0.0` ni `0 %` |
| Servicios | `PrestadorPublico.test.tsx` | Sí | | Filas compactas con icono, nombre, «Desde C$…» o «A convenir» y chevron a `/explorar/servicios/:id`. Contactar abre el primer servicio activo |
| Reseñas | `PrestadorPublico.test.tsx` | Sí | | Lista con nombre, iniciales, estrellas 1–5, fecha y comentario. Mientras no exista API pública de reseñas individuales, la sección pinta `RESENAS_PROVISIONALES` con esa forma. El encabezado sigue usando el agregado real |
| Insignia responsable | `DetalleDeServicio.test.tsx` y `ExplorarServicios.test.tsx` | Sí | | El significado y la advertencia viven en un popover (`role="tooltip"`) que se abre con hover o tap y se cierra con Escape o clic fuera. El botón anuncia `aria-expanded` |
| Portafolio | `PrestadorPublico.test.tsx` | Sí | | Trabajos con título, descripción y miniaturas cuando hay imágenes. Vacío honesto si no publicó ninguno |
| Layout | Revisión de `prestadorPublico.module.css` | Sí | | Una columna en teléfono (cabecera → servicios → reseñas → portafolio). Desde 64 rem, dos columnas con barra lateral y columna derecha sticky. `<PieDePagina />` cierra carga, error y perfil cargado. Solo tokens de `src/estilos/global.css` |
| Pruebas del frontend | `npx vitest run` | Sí | | 273 en verde, 31 archivos: las 265 del #25 más 8 de este PR |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `763a4ba`, el HEAD de `develop`. El #26 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | Sí | | Adjuntas al Pull Request: teléfono, tableta y escritorio |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Las capturas se adjuntaron; no se anotó `scrollWidth == clientWidth` |

El contrato de `GET /api/prestadores/{id}` no cambia: sigue siendo público, sin
contactos, y `reputacionPrestador` sigue siendo el agregado. El listado de reseñas
con nombre y comentario es provisional hasta que el backend publique esa
superficie. Seguir solo alterna estado local.

## Asistente de publicación de servicio

Controles del rediseño de `/prestador/servicios/nuevo` como asistente de cuatro
pasos, con el resultado real de cada comprobación.

- **Local**: `npx vitest run --maxWorkers=1` en el frontend sobre `baee29d`:
  279 pruebas en 32 archivos en verde. `NuevoServicio.test.tsx` aporta 8 casos
  (indicador, validación por paso, cancelar al listado, dropzone, fotos al
  publicar, precio inválido, alta con precio nulo y conservar datos al volver).
  `ServiciosPropios.test.tsx` conserva listado, edición e imágenes y deja de
  cubrir el alta en un solo formulario. Formato, lint, typecheck y `vite build`
  en verde. `docker compose --env-file .env.example config -q` en verde.
  `./mvnw verify` no se ejecutó aquí: este PR no toca el backend; el check de
  CI lo cubre.
- **CI**: el #27 se abre contra `develop`. El merge-base con `origin/develop` es
  `adad65e`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/ui-publicar-servicio-asistente` nace de `develop` actualizado.
  El #27 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Iconos | Revisión de `iconos.tsx` | Sí | | `IconoSubida`, `IconoCamara` e `IconoX` en trazo, `viewBox="0 0 24 24"`, `currentColor` |
| Layout | Revisión de `NuevoServicio.tsx` y `servicios.module.css` | Sí | | `BarraLateral` a la izquierda (ítem inicio, destinos a `/`, `/prestador` y `/seguridad`). Fondo `--color-neutral-50`. Tarjeta `--color-neutral-0`, `--radius-xl`, `--gap-xl`, `--shadow-sm`. En `< 48 rem` la barra queda oculta |
| Indicador de pasos | `NuevoServicio.test.tsx` | Sí | | Cuatro pasos: Información, Detalles, Precio, Publicar. Activo: círculo `--color-primary-500`. Completado: `--color-secondary-500` con check. Futuro: `--color-neutral-200` |
| Paso 1 | `NuevoServicio.test.tsx` | Sí | | Título con `Entrada`. Categoría y subcategoría encadenadas; sin categoría no hay subcategorías. No avanza si faltan nombre, categoría o subcategoría. Cancelar vuelve a `/prestador/servicios` |
| Paso 2 | `NuevoServicio.test.tsx` | Sí | | Descripción con contador `n/3000`. Dropzone de fotos (clic o arrastre) que guarda archivos locales; al publicar se envían a `POST /api/prestador/servicios/{id}/imagenes` |
| Paso 3 | `NuevoServicio.test.tsx` | Sí | | Precio opcional con prefijo `C$`. Vacío viaja como `null` y se lee «A convenir». Un `0` no deja avanzar |
| Paso 4 | `NuevoServicio.test.tsx` | Sí | | Resumen de nombre, categoría, precio y extracto. `Publicar servicio` es `type="submit"`. Tras el alta redirige a la edición del servicio creado |
| Edición | `ServiciosPropios.test.tsx` | Sí | | `/prestador/servicios/:id` conserva el formulario plano: nombre, descripción, categoría, subcategoría, precio y «Guardar cambios» |
| Pruebas del frontend | `npx vitest run --maxWorkers=1` | Sí | | 279 en verde, 32 archivos: las 273 del #26 más 6 netas de este PR |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `adad65e`, el HEAD de `develop`. El #27 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | Sí | | Adjuntas al Pull Request: teléfono, tableta y escritorio |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Las capturas se adjuntaron; no se anotó `scrollWidth == clientWidth` |

El contrato de `POST /api/prestador/servicios` no cambia. Las fotos del asistente
reutilizan `POST /api/prestador/servicios/{id}/imagenes` después de crear el
servicio. Un precio nulo sigue siendo «A convenir» en presentación. La edición
de un servicio ya publicado sigue en un solo formulario.

## Pantalla dedicada de mensajes

Controles del rediseño de la mensajería como pantalla propia en `/mensajes`
(bandeja y hilo en dos columnas), con el resultado real de cada comprobación.

- **Local**: `npm run format:check`, `npm run lint`, `npm run typecheck` y
  `npx vitest run` en el frontend sobre `766a1b7`: 324 pruebas en 37 archivos
  en verde. `Mensajes.test.tsx` aporta 13 casos (ruta protegida, vacío, carga,
  filtro de estados, búsqueda, hilo propio/ajeno, deep link con ítem activo,
  envío, fallo que conserva el texto, mensaje en blanco, solo lectura al
  completar, cuenta restringida y error con reintento). `presentacion.test.ts`
  aporta 7, `ItemConversacion.test.tsx` 4 y `BurbujaMensaje.test.tsx` 2.
  `docker compose --env-file .env.example config -q` en verde. `./mvnw verify`
  no se ejecutó aquí: el #29 no toca el backend; el check de CI lo cubre.
- **CI**: el #29 se abre contra `develop`. El merge-base con `origin/develop`
  es `6f7fd7c`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/pantalla-mensajes-chat` nace de `develop` actualizado. El
  #29 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Ruta protegida | `Mensajes.test.tsx` | Sí | | `RUTA_MENSAJES = '/mensajes'` dentro de `<RutaProtegida>`. Sin sesión redirige a iniciar sesión |
| Layout | Revisión de `Mensajes.tsx` y `mensajes.module.css` | Sí | | `BarraLateral` fija a la izquierda (ítem `mensajes` activo; destinos a `/panel`, `/mensajes`, `/prestador` y `/seguridad`). Fondo `--color-neutral-50`. Cuerpo `calc(100dvh - espaciado)` con dos paneles blancos y `--radius-xl`. En `< 48 rem` se ve la bandeja o el hilo; desde tableta, las dos columnas. La bandeja de escritorio mide 22.5 rem |
| Bandeja | `Mensajes.test.tsx` y `presentacion.test.ts` | Sí | | `useSolicitudesEnviadas` y `useSolicitudesRecibidas`. Solo `ACEPTADA`, `COMPLETADA` y `CANCELADA`. Contraparte por `idUsuario` frente a `idCliente`/`idPrestador`. Búsqueda por nombre o servicio. Fila con avatar (iniciales, `--color-secondary-100`), extracto y hora. Seleccionada: `--color-primary-50` y borde `--color-primary-500` |
| Hilo | `Mensajes.test.tsx` | Sí | | `useMensajes` (short polling) y `useEnvioDeMensaje`. Autoscroll al último. Vacío: «Selecciona una conversación para ver los mensajes». Enlace «Ver detalle del servicio» a `/solicitudes/:id`. Completada o cancelada: «Esta conversación ha finalizado y permanece en solo lectura» |
| Burbujas | `BurbujaMensaje.test.tsx` | Sí | | Ajenas a la izquierda (`--color-neutral-0`, borde `--color-neutral-200`). Propias a la derecha (`--color-secondary-700`, texto blanco, `--radius-lg` salvo la esquina inferior derecha) |
| Envío | `Mensajes.test.tsx` | Sí | | Placeholder «Escribe un mensaje...». Botón píldora `--color-primary-500` con `IconoEnviar`. No envía en blanco. Conserva el texto si falla la red. Cuenta `RESTRINGIDA_TEMPORAL` lee y no escribe |
| Componentes | Revisión de `src/capacidades/solicitud/componentes/` | Sí | | `ItemConversacion`, `BurbujaMensaje`, `ListaDeConversaciones` y `ConversacionActiva`. Iconos `IconoMensaje`, `IconoEnviar` e `IconoChevronIzquierda` en `src/comun/componentes/ui` |
| Pruebas del frontend | `npx vitest run` | Sí | | 324 en verde, 37 archivos: las 298 del #28 más 26 de este PR |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `6f7fd7c`, el HEAD de `develop`. El #29 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | Sí | | Adjuntas al Pull Request: teléfono, tableta y escritorio |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Las capturas se adjuntaron; no se anotó `scrollWidth == clientWidth` |

El contrato de `GET /api/solicitudes/enviadas`, `GET /api/solicitudes/recibidas`,
`GET /api/solicitudes/{id}/mensajes` y `POST /api/solicitudes/{id}/mensajes` no
cambia. El chat embebido en el detalle de la solicitud se conserva. No hay
WebSockets ni multimedia: el hilo sigue actualizándose por short polling.

## Configuración de la cuenta con sesión real

Controles del rediseño de `/seguridad` (barra lateral en escritorio, correo de
la sesión, pestañas y alias `/configuracion`), con el resultado real de cada
comprobación.

- **Local**: `npx prettier --check .`, `npx eslint .`, `npx tsc -b --noEmit` y
  `npx vitest run --maxWorkers=1` en el frontend sobre `2b41e8d`: 324 pruebas
  en 37 archivos en verde. `SeguridadCuenta.test.tsx` conserva sus 18 casos
  (validación y cambio de clave, ciclo TOTP, secreto descartado, vigilancia y
  redirecciones). `docker compose --env-file .env.example config -q` en verde.
  `./mvnw verify` no se ejecutó aquí: el #30 no toca el backend; el check de
  CI lo cubre.
- **CI**: el #30 se abre contra `develop`. El merge-base con `origin/develop`
  es `6b0f8d9`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/pantalla-configuracion-nueva` nace de `develop`
  actualizado. El #30 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Correo de la sesión | Revisión de `SeguridadCuenta.tsx` y `SeguridadCuenta.test.tsx` | Sí | | `useSesionActual()` pinta `usuario.correoElectronico`. Recorrido en el navegador contra la API local: se vio el correo de la sesión, no `usuario@ejemplo.com`. El botón «No se puede cambiar» está deshabilitado con `title="El correo no puede modificarse en el MVP"` |
| Teléfono, idioma y zona horaria | Revisión del JSX | Sí | | El teléfono sigue como `+505 0000 0000` con acción inactiva (el MVP no tiene endpoint de cambio). Idioma «Español» y zona «(UTC-6) Centroamérica» son selectores presentacionales deshabilitados |
| Contraseña y segundo factor | `SeguridadCuenta.test.tsx` | Sí | | «Cambiar» / «Cancelar» y el ciclo TOTP no cambian de contrato ni de textos. Las 18 pruebas de la pantalla siguen verdes |
| Pestañas | Revisión de `PestaniasDeConfiguracion.tsx` | Sí | | `role="tablist"` y `role="tab"`. Cuenta activa (`aria-selected`). Perfil navega a `/prestador`. Notificaciones, Privacidad y Pagos llevan `title="Próximamente disponible"` |
| Barra lateral | Revisión de CSS y JSX | Sí | | Ítem `configuracion` activo. Destinos a `/`, `/mensajes`, `/prestador` e `/seguridad`. Visible desde `48rem`; en teléfono `display: none`. En 375x812, `scrollWidth == clientWidth` (375/375) |
| Alias `/configuracion` | Revisión de `rutas.ts` y `App.tsx` | Sí | | `RUTA_CONFIGURACION = '/configuracion'` redirige a `/seguridad` |
| Tokens | Revisión de `seguridad.module.css` | Sí | | Solo `--color-*`, `--gap-*`, `--radius-*`, `--shadow-*`, `--font-*` y `--border-width-*`. Tarjeta `--color-neutral-0`, `--radius-xl`, borde `--color-neutral-200`, `--shadow-sm` |
| Pruebas del frontend | `npx vitest run --maxWorkers=1` | Sí | | 324 en verde, 37 archivos: las mismas del #29. No se suman casos |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `6b0f8d9`, el HEAD de `develop`. El #30 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | Sí | | Adjuntas al Pull Request: teléfono, tableta y escritorio |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | En teléfono se midió 375/375 durante el recorrido. No se dejó un `medidas.json` de los tres tamaños, así que no se anota el desbordamiento como comprobado en tableta y escritorio |

El contrato de `PUT /api/auth/clave` y de `/api/auth/segundo-factor` no cambia.
No hay formularios de edición de correo ni de teléfono: el backend no los
soporta en el MVP.

## Panel de actividad del prestador

Controles del dashboard en `/panel` (métricas, actividad reciente, próximas
tareas y atajos de gestión), con el resultado real de cada comprobación.

- **Local**: `npx prettier --check .`, `npx eslint .`, `npx tsc -b --noEmit` y
  `npx vitest run --maxWorkers=2` en el frontend sobre `f16e6b0`: 332 pruebas
  en 38 archivos en verde. `PanelUsuario.test.tsx` aporta 7 casos (sin sesión,
  cliente sin perfil, barra con ítem inicio, métricas reales, tope de cuatro
  actividades, verificar perfil y publicar el primer servicio). `Boton.test.tsx`
  suma 1 caso por el destino `to`. `docker compose --env-file .env.example config -q`
  en verde. `./mvnw verify` no se ejecutó aquí: el #31 no toca el backend; el
  check de CI lo cubre.
- **CI**: el #31 se abre contra `develop`. El merge-base con `origin/develop`
  es `5ec54b9`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/dashboard-usuario` nace de `develop` actualizado. El
  #31 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Ruta protegida | `PanelUsuario.test.tsx` | Sí | | `RUTA_PANEL = '/panel'` dentro de `<RutaProtegida>`. Sin sesión redirige a iniciar sesión |
| Solo prestadores | `PanelUsuario.test.tsx` | Sí | | Si `GET /api/prestador/perfil` responde 404, `<Navigate>` a `/explorar`. No se llama `GET /api/prestador/servicios` ni el perfil público hasta que existe el perfil (`useServiciosPropios({ enabled: Boolean(perfil.data) })`) |
| Métricas | `PanelUsuario.test.tsx` | Sí | | Servicios activos (`estado === 'ACTIVO'`), hilos de `conversacionesDeBandeja`, contrataciones (enviadas más recibidas) y calificación de `reputacionPrestador.promedio` o «—». `TarjetaMetrica` expone `aria-label` «título valor» |
| Actividad reciente | `PanelUsuario.test.tsx` | Sí | | Une enviadas y recibidas, ordena por `fechaCreacion` y deja 4. Vacío: «Todavía no tienes solicitudes en tus servicios» con enlace a `/prestador/servicios/nuevo` |
| Próximas tareas | `PanelUsuario.test.tsx` | Sí | | Pendientes recibidas → `/solicitudes`. Sin verificar → `/prestador`. Verificado sin servicios → `/prestador/servicios/nuevo`. Banner «Mejora tu visibilidad» con `Boton` a publicar |
| Barra lateral y portada | Revisión de JSX y `Inicio.test.tsx` | Sí | | Ítem `inicio` activo. Destinos a `/panel`, `/mensajes`, `/prestador` e `/seguridad`. Visible desde `48rem`; en teléfono `display: none`. La portada muestra «Ir a tu Panel principal →» con sesión plena; el menú de explorar también enlaza `/panel` |
| Tokens y columnas | Revisión de `panel.module.css` | Sí | | Solo tokens de `global.css`. Métricas en 2 columnas desde `48rem` y 4 desde `64rem`. Cuerpo `1.6fr 1fr` en escritorio |
| Pruebas del frontend | `npx vitest run --maxWorkers=2` | Sí | | 332 en verde, 38 archivos: las 324 del #30 más 8 de este PR |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `5ec54b9`, el HEAD de `develop`. El #31 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | Sí | | Adjuntas al Pull Request: teléfono, tableta y escritorio |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Las capturas se adjuntaron; no se anotó `scrollWidth == clientWidth` |

El contrato de `GET /api/prestador/perfil`, `GET /api/prestador/servicios`,
`GET /api/solicitudes/enviadas`, `GET /api/solicitudes/recibidas` y
`GET /api/prestadores/{id}` no cambia. Un cliente sin perfil de prestador no
ve el dashboard: entra a `/explorar`.

## Síntesis del estado actual en el README

Actualización de la sección «Estado actual» de `README.md` para sintetizar los nueve párrafos descriptivos en viñetas ejecutivas con foco en evaluación de hackathon, con el resultado real de cada comprobación.

- **Local**:
  - En `react-box` (distrobox): `npm run test` (332 pruebas en 38 archivos en verde), `npm run lint` (0 advertencias/errores), `npm run typecheck` (en verde), `npm run format:check` (todos los archivos limpios con Prettier) y `npm run build` (build de producción y PWA en verde).
  - En backend: `./mvnw -B -ntp verify` con Temurin JDK 21 (488 pruebas unitarias y de integración contra PostgreSQL real con Testcontainers en verde, SpotBugs 0 incidencias y Spotless en verde).
  - Docker: `docker compose --env-file .env.example config -q` en verde.
- **CI**: el #32 se abre contra `develop`. El merge-base con `origin/develop` es `4bacea4`, el HEAD actual de esa rama.
- **Rama**: `feature/actualizar-readme` nace de `develop` actualizado. El #32 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Síntesis del README | Revisión de `README.md` | Sí | | Se transforman los 9 párrafos extensos de «Estado actual» en viñetas concisas por capacidad funcional |
| Formato y linter frontend | `npm run lint` y `npm run format:check` | Sí | | En verde dentro del contenedor `react-box` |
| Tipos y build frontend | `npm run typecheck` y `npm run build` | Sí | | Compilación TypeScript y bundle PWA de Vite en verde |
| Pruebas de frontend | `npm run test` | Sí | | 332 de 332 pruebas en 38 archivos en verde |
| Pruebas de backend | `./mvnw -B -ntp verify` | Sí | | 488 de 488 pruebas en verde con Testcontainers y SpotBugs limpio |
| Entorno local docker | `docker compose config -q` | Sí | | Validación con `.env.example` en verde |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `4bacea4`, el HEAD de `develop`. El #32 se abre contra esa rama, no contra `main` |
| Capturas | Revisión de cambios | | | No aplica: cambio exclusivamente documental sin modificación de UI |

## Revisión y resolución administrativa de casos de P10A

Controles que P10A deja funcionando, con el resultado real de cada comprobación.
Es la sección a la que remiten las filas 1, 3, 4, 5, 6 y 7 para este incremento.

- **Local**: máquina de desarrollo (Windows 11 Pro, Docker Engine 29.7.2 con
  Docker Desktop, Node 22, JDK compilando con `release 21`), PostgreSQL de
  Testcontainers para las pruebas y el Compose local —`moica_db` **healthy** en
  `localhost:5433`— para el recorrido integrado.
- **Validación completa con Docker activo** (4 de septiembre de 2026, sobre el
  código de esta rama). En `backend`, `./mvnw -B -ntp verify`: **Surefire 167 y
  Failsafe 516, ambos con 0 fallos, 0 errores y 0 omitidas**, Spotless limpio,
  SpotBugs sin hallazgos y `BUILD SUCCESS` en 5:52 min. Testcontainers levantó
  `postgres:15-alpine` real; **no se usa H2 en ninguna prueba**. Las 516 de
  Failsafe son las 488 de P9 más las **28 de P10A**:
  `RevisionDeCasosIT` 25 y `ConcurrenciaDeRevisionDeCasosIT` 3. En `frontend`,
  `format:check`, `lint`, `typecheck`, `test` (**347 en 40 archivos**) y `build`
  en verde.
- **Recorrido integrado sin dobles** (misma fecha). Compose local, backend real
  en `:8080` —`/actuator/health` respondió `{"status":"UP"}`— y Vite en `:5173`.
  Un script contra la API registró dos cuentas administradoras con su segundo
  factor **activado y verificado de verdad** (TOTP calculado con HMAC-SHA1 sobre
  la clave manual que devuelve la API), un prestador y un cliente; publicó un
  servicio, abrió y aceptó una solicitud, cruzó tres mensajes, la completó, abrió
  el caso y lo llevó por asignación, revisión y cierre. Las **45 comprobaciones
  pasaron y 0 fallaron**, incluidas las que se hacen directamente en PostgreSQL.
  La verificación básica del prestador se proyectó con SQL porque el expediente
  documental necesita los buckets de R2, que no están configurados en local; la
  verificación tiene sus propias pruebas de integración.
- **Interfaz**: 9 capturas de página completa a **375x812, 768x1024 y 1280x800**
  —bandeja, expediente cerrado y expediente en revisión— tomadas con Chrome
  headless por CDP (`Emulation.setDeviceMetricsOverride`, que es la única vía que
  fija el viewport exacto) contra la aplicación real y con sesión administrativa
  verdadera. En las nueve, `scrollWidth == clientWidth`: **cero desbordamientos
  horizontales**. Quedan en `C:\Users\ervin\Desktop\moica-pr-capturas-p10a`,
  fuera del repositorio. Las seis del expediente se sustituyen por las capturas
  de las correcciones descritas abajo; las tres de bandeja se conservan como antecedente.
- **Rama**: `feature/admin-casos-moderacion` nace de `develop` en `4d6c2d6`, el
  merge del #32, sin conflictos y sin `merge` posteriores.
- **Sin migración nueva.** `V50` y `V51` de P9 ya crearon `caso_moderacion` e
  `historial_caso` con todo lo que la revisión administrativa necesita. P10A no
  toca el esquema: solo escribe en las columnas que ya existían.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Ninguna lectura administrativa sin rol y TOTP | `RevisionDeCasosIT.sinSesionNoSeLlegaANingunaRuta`, `unaCuentaCorrienteNoEntra` y `elRolSinSegundoFactorNoBasta`; recorrido integrado | Sí | Sí | Sin sesión, las cuatro rutas responden **401**. Una cuenta corriente recibe **403** en bandeja, expediente, mensajes, asignación y cierre. Una cuenta **con rol pero sin el segundo factor verificado en esa sesión** también recibe 403: el rol por sí solo no abre el área. La cadena de seguridad lo impone para todo `/api/admin/**` y `RevisionDeCasosService` lo repite como última red. |
| Ninguna escritura administrativa sin rol y TOTP | Las mismas pruebas, sobre `asignacion`, `revision` y `cierre` | Sí | Sí | Los tres verbos de escritura pasan por `exigirPermisosAdministrativos` antes de tocar nada; ninguna petición rechazada deja fila ni versión. |
| Chat solo dentro de un caso relacionado | `RevisionDeCasosIT.elChatSoloSeAlcanzaDesdeUnCaso` y `elAreaAdministrativaNoEscribeEnElChat`; recorrido integrado | Sí | Sí | El hilo cuelga de `/api/admin/casos/{id}/mensajes`, **no de la solicitud**: `/api/admin/solicitudes/{id}/mensajes` no existe (404) y la ruta del participante rechaza a quien no participa (404). Sin un caso abierto sobre esa solicitud no hay forma de leer la conversación, ni conociendo su identificador. Un `POST` al hilo desde el área responde **405** y el hilo sigue vacío: el área lee, no participa. |
| Un caso ajeno no se resuelve | `RevisionDeCasosIT.soloElResponsableDecide` y `sinResponsableNoSeInicaLaRevision`; recorrido integrado | Sí | Sí | Sin responsable, iniciar la revisión responde 409 `CASO_SIN_RESPONSABLE`. Con otro responsable, 403 `CASO_DE_OTRO_ADMINISTRADOR`, tanto al revisar como al cerrar, y el estado no se mueve. Asignar y reasignar sí las puede hacer cualquier administrador: repartir trabajo es coordinación y queda registrada en el historial. |
| Transiciones válidas y solo esas | `RevisionDeCasosIT.noSeCierraSinRevisar` y `unCasoCerradoNoSeMueve`; recorrido integrado | Sí | Sí | `ABIERTO`/`REABIERTO` → `EN_REVISION` y `EN_REVISION` → `CERRADO`. Cerrar sin revisar, volver a cerrar, revisar un cerrado o reasignar un cerrado responden 409 `TRANSICION_NO_PERMITIDA` con el estado real en el mensaje. `CERRADO` → `REABIERTO` no existe todavía: nace de una apelación y es P10B. |
| Asignación y reasignación | `RevisionDeCasosIT.asignarDejaResponsableYVersiona`, `reasignarCambiaElResponsable`, `reasignarAlMismoNoVersiona` y `noSeAsignaAQuienNoEsAdministrador` | Sí | Sí | Asignar deja responsable y **no cambia el estado**: el caso sigue `ABIERTO`. Reasignar cambia el responsable y añade una versión más, con «reasignó» en el detalle. Reasignar a quien ya lo tiene responde 200 y **no** crea versión repetida. Una cuenta sin rol responde 400 `ADMINISTRADOR_NO_VALIDO` y no deja ni fila ni versión. |
| Resolución completa y validada | `RevisionDeCasosIT.cerrarRegistraLaDecisionCompleta` y `laResolucionSeValidaEnLaFrontera`; recorrido integrado | Sí | Sí | Cerrar escribe resultado, resolución y fecha de cierre **juntos**, como exige `ck_caso_moderacion_cierre`. Una resolución de solo espacios se recorta antes de validarse y sale 400; un resultado inventado también. Ninguno de los dos rechazos mueve el estado ni añade versión. |
| Resolver no sanciona | `RevisionDeCasosIT.resolverNoSanciona`; recorrido integrado | Sí | Sí | Tras cerrar como `PROCEDENTE`, la cuenta reportada sigue **`ACTIVA`**, el caso no tiene medida vinculada, la versión vigente fotografía `estado_cuenta = ACTIVA` sin medida, y `medida_administrativa` **sigue vacía**. Elegir y aplicar la medida es P10B y siempre lo hace una persona, según la definición 11.3. |
| SCD2: una sola versión vigente | `RevisionDeCasosIT.cadaMutacionCierraLaVersionAnterior`, `ConcurrenciaDeRevisionDeCasosIT` (los tres casos); recorrido integrado | Sí | Sí | El recorrido completo deja cuatro versiones —`CASO_ABIERTO`, `RESPONSABLE_ASIGNADO`, `ESTADO_CASO_CAMBIADO`, `RESOLUCION_REGISTRADA`— con números 1 a 4, actores `USUARIO` y luego `ADMINISTRADOR`, y **exactamente una** con `es_version_actual`, la última, sin fecha de fin. Lo sostiene el índice parcial `uq_historial_caso_version_actual`. |
| SCD2: sin intervalos superpuestos | Las mismas pruebas | Sí | Sí | Cada versión cerrada termina **en el mismo instante** en que empieza la siguiente: la comprobación lo verifica fila a fila y además cuenta las parejas que solapan con los mismos rangos semiabiertos que usa `ex_historial_caso_vigencia`, y el resultado es **cero** en todos los escenarios, incluidos los concurrentes. |
| SCD2: atomicidad | `RevisionDeCasosIT.unaMutacionRechazadaNoVersiona` y `cadaMutacionCierraLaVersionAnterior` | Sí | Sí | Mutar el caso, cerrar la versión vigente y crear la siguiente ocurren en **una sola transacción**. Una mutación rechazada no deja rastro: mismo número de versiones, una vigente y cero solapes. El cierre de la versión anterior se vacía a la base **antes** de insertar la nueva, porque Hibernate ordena sus inserciones antes que sus actualizaciones y el índice de versión vigente rechazaría la que entra. |
| Concurrencia entre administradores | `ConcurrenciaDeRevisionDeCasosIT` | Sí | Sí | Dos asignaciones simultáneas: las dos responden 200 y quedan tres versiones encadenadas, una vigente, sin solapes, y la fila del caso coincide con su fotografía. Dos cierres simultáneos: **un 200 y un 409**, una sola resolución y cuatro versiones. Una reasignación cruzada con un cierre: los dos órdenes posibles se comprueban por separado —o el cierre gana y la reasignación recibe 409, o la reasignación gana y el cierre recibe 403— y en ninguno queda una decisión firmada por quien ya no llevaba el caso. El bloqueo pesimista sobre la fila del caso es lo que los ordena. |
| Privacidad del expediente | `RevisionDeCasosIT.elExpedienteNoFiltraDatosDeContacto` y `elDirectorioNoPublicaCorreos`; recorrido integrado | Sí | Sí | El expediente no lleva correos ni contactos de los participantes; los nombres son los mismos que ya publica el detalle de la solicitud. El directorio de administradores publica solo identificador y nombre, y exige rol. |
| Regresión de P9 | `EsquemaDeCasosDeModeracionIT` 35, `ReporteDeParticipanteIT` 28, `ConcurrenciaDeReporteIT` 3 y `ReporteAPresentarTest` 7 | Sí | Sí | Los cuatro conjuntos de P9 siguen en verde dentro del `verify` completo. La superficie del reportante no cambió: sigue viendo solo su caso, sin responsable, resultado, resolución ni historial. |
| Interfaz responsiva y accesible | Chrome headless por CDP a 375x812, 768x1024 y 1280x800; `BandejaDeCasos.test.tsx` (6) y `ExpedienteDeCaso.test.tsx` (9) | Sí | Sí | En las nueve capturas `scrollWidth == clientWidth`. La tabla usa encabezados de fila, los filtros anuncian su estado con `aria-pressed`, el hilo con `aria-expanded`, el resultado va en `fieldset`/`legend`, cada campo tiene `label` asociado y la marca de versión vigente lleva texto además del recuadro. Las imágenes sin texto alternativo propio quedan como decorativas en lugar de recibir una descripción inventada. |
| El hilo privado no se descarga sin pedirlo | `ExpedienteDeCaso.test.tsx` | Sí | Sí | Al abrir el expediente no se pide `/mensajes`; solo al pulsar «Ver los mensajes». Abrir una ficha no debe arrastrar de paso una conversación privada que quizá nadie va a mirar. |
| La pantalla no propone lo que la API rechaza | `ExpedienteDeCaso.test.tsx` | Sí | Sí | Sin ser responsable no aparecen «Iniciar la revisión» ni «Cerrar el caso», y se explica por qué. Un caso cerrado no ofrece reasignar ni decidir. Es experiencia, no seguridad: el backend rechaza igual, y por eso el aviso muestra el mensaje de la API tal cual cuando otra persona se adelanta. |

### Correcciones del historial y los avisos (4 de septiembre de 2026)

Continuación del PR #33 sobre `feature/admin-casos-moderacion`. Se conserva
arriba la validación inicial como antecedente; este apartado registra una sola
vez la evidencia de las correcciones, sin cambiar la fecha de corte del plan.

- **Responsable histórico**: el DTO, el contrato y la pantalla distinguen
  `idActor`/`nombreActor` de `idAdministradorResponsable`/
  `nombreAdministradorResponsable` de cada versión. La apertura deja responsable
  nulo. `RevisionDeCasosIT.elHistorialConservaElResponsableDeCadaVersion`
  comprueba dos asignaciones cruzadas, identificadores y nombres distintos
  (Lucía y Carlos), además de los responsables persistidos en las tres versiones.
  La regresión del expediente exige la etiqueta «Responsable entonces» junto
  al nombre correcto y su ausencia en la apertura.
- **Avisos después del refresco**: las regresiones esperan la resolución del
  expediente cerrado tras un 409 y el nuevo responsable tras un 403 que retira
  `puedeResolver`. Después comprueban el aviso y las acciones: el cerrado no
  permite asignar, revisar ni cerrar; el caso ajeno permite reasignar, pero no
  resolver. Una reasignación posterior exitosa recupera el formulario de cierre
  y retira el aviso anterior. Esta última comprobación falló antes de corregir
  `errorMasReciente`, que filtraba errores antes de elegir la última mutación.
- **Backend recuperado, sin repetición innecesaria**: la tarea local de Claude
  `bgvfx6ksh.output` terminó a las 19:18:42 con `BUILD SUCCESS` y código 0.
  Se contrastó con los XML de Surefire/Failsafe y SpotBugs en `backend/target`,
  posteriores a los últimos cambios Java (19:07:21): **167 unitarias y 517 de
  integración**, sin fallos, errores ni omitidas; **26** en `RevisionDeCasosIT`
  y **3** de concurrencia. SpotBugs informa cero defectos. No se modificó Java
  después de esa validación completa con Docker.
- **Frontend comprobado al cierre**: `format:check`, `lint`, `typecheck` y
  `build` terminaron con código 0. `npm run test` completó 348/350: fallaron por
  espera la navegación al expediente en `BandejaDeCasos.test.tsx` y el cambio
  de cuenta en `PanelAdministrativo.test.tsx`. La repetición
  `npm run test -- --maxWorkers=2` pasó **350/350 en 40 archivos**, sin modificar
  los límites de espera ni esas pruebas. Incluye las **18 de P10A**: 6 de
  bandeja y 12 de expediente. Vite conserva el aviso de bundle mayor de 500 kB;
  el build y la generación de la PWA terminaron correctamente.
- **Capturas actualizadas**: seis expedientes reales (`/admin/casos/6` cerrado y
  `/admin/casos/7` en revisión), con sesión y segundo factor reales, a 375x812,
  768x1024 y 1280x800. Las seis muestran «Responsable entonces» y cumplen
  `scrollWidth == clientWidth`; el cerrado ofrece solo leer mensajes, y el de
  revisión conserva asignación y cierre. Se reutilizó el script CDP de P10A
  porque Browser integrado falló al navegar (`ERR_CONNECTION_REFUSED` y bloqueo
  de su página de error `data:`), aunque Vite respondía 200. No se reinstaló ni
  reorganizó la configuración. [Capturas del expediente](Evidencias/P10A/).

| Tamaño | Caso cerrado | Caso en revisión |
|---|---|---|
| Móvil, 375x812 | [Captura](Evidencias/P10A/expediente-cerrado-movil-375x812.png) | [Captura](Evidencias/P10A/expediente-en-revision-movil-375x812.png) |
| Tableta, 768x1024 | [Captura](Evidencias/P10A/expediente-cerrado-tableta-768x1024.png) | [Captura](Evidencias/P10A/expediente-en-revision-tableta-768x1024.png) |
| Escritorio, 1280x800 | [Captura](Evidencias/P10A/expediente-cerrado-escritorio-1280x800.png) | [Captura](Evidencias/P10A/expediente-en-revision-escritorio-1280x800.png) |

Estas comprobaciones amplían las filas de asignación, SCD2, interfaz responsiva
y acciones disponibles de la tabla anterior. Los controles automáticos del SHA
final se consultan en el CI del PR #33; la aprobación previa de Roberto sobre
`3a2ed8f` no constituye revisión de estas correcciones.

### Omisiones documentales cerradas en este PR

`Docs/Dev/ContratoDeApi.md` arrastraba tres códigos descritos en sus apartados
pero ausentes del catálogo final. Se contrastaron contra los servicios y las
pruebas que ya los emiten y se añadieron sin tocar ningún comportamiento:
`SUBCATEGORIA_NO_DISPONIBLE` (400, `CatalogoDeServiciosService.exigirSubcategoria`,
`ServicioPublicadoIT`), `SOLICITUD_NO_COMPLETADA` y `CALIFICACION_DUPLICADA`
(409, `CalificacionDeSolicitudService`, `CalificacionDeSolicitudIT` y
`ConcurrenciaDeCalificacionIT`). Era una omisión de redacción que P9 dejó
anotada por quedar fuera de su alcance; no se añadieron pruebas para comprobar
una lista de Markdown.

### Pendiente preexistente, ajeno a P10A

El contenedor `moica_pgadmin` sigue reiniciándose por el correo `.local` de
`MOICA_PGADMIN_EMAIL`. No afecta a PostgreSQL, a Testcontainers ni a este
incremento, y no se tocó.

## Perfil privado del prestador

Controles del rediseño de `/prestador` (perfil privado: formulario, imagen,
disponibilidad, verificación, contactos y portafolio) según el sistema de
diseño, con el resultado real de cada comprobación.

- **Local**: `npx prettier --check .`, `npx eslint .`, `npx tsc -b --noEmit` y
  `npx vitest run` en el frontend sobre `1044ae7`: 332 pruebas en 38 archivos
  en verde. `PerfilPrestador.test.tsx` conserva sus 12 casos, `ImagenDePerfil.test.tsx`
  6, `Portafolio.test.tsx` 11 y `Verificacion.test.tsx` 16. No se suman casos.
  `docker compose --env-file .env.example config -q` en verde. `./mvnw verify`
  no se ejecutó aquí: el #34 no toca el backend; el check de CI lo cubre.
- **CI**: el #34 se abre contra `develop`. El merge-base con `origin/develop`
  es `769218b`, el HEAD actual de esa rama. Los enlaces de CI se anotan cuando
  existan; no se describen de memoria.
- **Rama**: `feature/ui-perfil-prestador-privado` nace de `develop` actualizado.
  El #34 se abre contra esa rama, no contra `main`.

Una casilla vacía significa que ahí no aplica, no que fallara.

| Control | Cómo se comprueba | Local | CI | Evidencia |
|---|---|---|---|---|
| Ruta protegida | `PerfilPrestador.test.tsx` | Sí | | `RUTA_PRESTADOR = '/prestador'` dentro de `<RutaProtegida>`. Sin sesión redirige a iniciar sesión. Un fallo al cargar el perfil ofrece «Reintentar» |
| Layout | Revisión de `PerfilPrestador.tsx` y `prestador.module.css` | Sí | | `BarraLateral` (ítem `perfil` activo; destinos a `/panel`, `/mensajes`, `/prestador` y `/seguridad`). Fondo `--color-neutral-50`. Contenido `max-width: 58rem`. Visible desde `48rem`; en teléfono `display: none`. Pie «Volver al inicio» a `/` |
| Sin perfil | `PerfilPrestador.test.tsx` | Sí | | Solo encabezado y formulario. `h1` «Tu perfil de prestador»; `h2` «Crea tu perfil»; botón «Crear perfil». No se montan imagen, verificación, disponibilidad, contactos ni portafolio |
| Con perfil | `PerfilPrestador.test.tsx` | Sí | | Formulario con `h2` «Datos de tu perfil». Identidad agrupa `ImagenDePerfil` y `Disponibilidad`. Luego verificación, contactos y portafolio |
| Aviso de privacidad | `PerfilPrestador.test.tsx` | Sí | | Si `nivelVerificacion === 'SIN_VERIFICAR'`: «Tu perfil todavía es privado.» Fondo `#FFFDE7`, borde `#FFE082`. No se muestra con verificación vigente |
| Formulario | Revisión de `FormularioDePerfil.tsx` | Sí | | `Boton` y `Entrada`. Tipos Independiente, Emprendimiento y PYME. Municipio del catálogo territorial. El contrato de creación y edición no cambia |
| Imagen y disponibilidad | `ImagenDePerfil.test.tsx` y `PerfilPrestador.test.tsx` | Sí | | Avatar circular 5.5 rem con borde blanco. La previsualización local y los textos accesibles de subida se conservan. Disponibilidad: «Marcarme como no disponible» / «Volver a estar disponible» |
| Verificación | `Verificacion.test.tsx` | Sí | | Acento izquierdo `--color-secondary-500`. `InsigniaVerificado` solo si el nivel no es `SIN_VERIFICAR`. Los 16 casos del expediente e historial siguen verdes |
| Contactos y portafolio | `PerfilPrestador.test.tsx` y `Portafolio.test.tsx` | Sí | | Alta de contacto con `htmlFor="nuevo-contacto"`. Portafolio: alta, orden, imágenes y borrado. Contratos de API sin cambio |
| Tokens | Revisión de CSS | Sí | | Tokens de `global.css`, salvo el banner crema/ámbar del aviso de privacidad. Tarjetas `--color-neutral-0`, `--radius-xl`, `--shadow-sm` |
| Pruebas del frontend | `npx vitest run` | Sí | | 332 en verde, 38 archivos: las mismas del #31. No se suman casos |
| Nace de `develop` | `git merge-base origin/develop HEAD` | Sí | | Devuelve `769218b`, el HEAD de `develop`. El #34 se abre contra esa rama, no contra `main` |
| Capturas a tres tamaños | Chrome a 375x812, 768x1024 y 1280x800 | Sí | | Adjuntas al Pull Request: teléfono, tableta y escritorio |
| Medición de desbordamiento | `scrollWidth` frente a `clientWidth` | **No** | | **No registrada.** Las capturas se adjuntaron; no se anotó `scrollWidth == clientWidth` |

El contrato de `GET`/`PUT`/`POST /api/prestador/perfil`, imagen de perfil,
disponibilidad, contactos, portafolio y verificación no cambia. Un perfil
`SIN_VERIFICAR` sigue siendo privado.
