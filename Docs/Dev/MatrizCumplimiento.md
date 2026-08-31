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
| 1 | README técnico completo (requisitos, variables, estructura, scripts, comandos, endpoints) | En progreso | P1 → P11 | #3, #5, #9, #16, #17, #20, #21 | `70467f6`, `61b4af6`, `19bfaff` | Sus instrucciones se siguieron de principio a fin en una máquina real | `README.md` cubre requisitos, versiones, arquitectura (con diagrama), instalación rápida, comandos de validación y estructura del monorepo. El detalle profundo —variables de entorno, secreto JWT y conflicto de puertos en `Docs/Dev/GuiaEntornoLocal.md`; endpoints, modelo de sesión, CSRF, política de contraseña y forma de los errores en `Docs/Dev/ContratoDeApi.md`— vive en `Docs/Dev/`, enlazado desde la sección «Documentación» del README. El despliegue se completa en P11. P4 corrige la descripción del portafolio —lo administra el prestador, no se arma solo con servicios completados—, agrega el estado del perfil de prestador y suma `Docs/Dev/Almacenamiento.md` con la decisión de Cloudflare R2, la configuración del bucket y las seis variables nuevas. P4V documenta la segunda superficie de almacenamiento: el bucket privado, su token propio, sus seis variables y las diez comprobaciones que exige, además del flujo completo de verificación en el contrato de la API y la precisión sobre revocación en la definición funcional. P5 documenta las publicaciones propias, el descubrimiento público, `V31`/`V90` y el prefijo `servicios/` del almacén público, sin variables nuevas. Tras integrar el #15, el README y el contrato precisan que un prestador `NO_DISPONIBLE` conserva perfil y portafolio públicos, pero sin servicios listados ni contratación. P6 documenta el ciclo de solicitudes, `V40`, las rutas `/solicitudes` y las acciones explícitas del contrato, sin variables nuevas. El #20 agregó `Docs/Dev/PlanImplementacionMvp.md` y lo enlazó desde «Documentación». P7 documenta el chat y la revelación de contactos: `V41`, las tres rutas nuevas, la tabla de estados, los códigos `CHAT_NO_HABILITADO`, `CHAT_SOLO_LECTURA` y `CONTACTOS_NO_REVELADOS`, y el short polling. Sin variables ni dependencias nuevas; la licencia de inspección no se toca y pagos y mapas siguen fuera. |
| 2 | Modelo ER en 3FN y tres diagramas UML completos | Cumplido | P0 | #1, #2 | | | `Docs/Dev/DiagramaLogico.mmd`, `DiagramaConceptual.mmd`, `DiagramaClasesDominio.mmd`, `DiagramaCasosDeUso.tex`, `DiagramaActividades.tex`, `Moica - Diccionario de Datos.xlsx` |
| 3 | Interfaz navegable, validada y responsiva | En progreso | P1 → P11 | #3, #5, #7, #9, #10, #14, #15, #16, #17, #18, #21 | `117af69`, `9c80210`, `e3ba201`, `feff7ef`, `615cba3`, `7045359`, `5bdbd43`, `8cf5957`, `70b9063`, `83597a3`, `034f629`, `5786c35`, `5c13f37`, `e2c5d6d`, `cd68f8f`, `27ae045`, `d096558`, `f906a00`, `57dee03`, `b8bd22e`, `dfd9b50`, `afb6810`, `fea95bf`, `9e0fa07`, `c97fe7f`, `f26ae79`, `2281c02` | 224 pruebas de Vitest en 25 archivos sobre el #21: las 202 anteriores más 22 de P7 —14 en `ChatDeSolicitud.test.tsx` y 8 en `ContactosDelPrestador.test.tsx`—; recorrido manual integrado y 15 capturas a 375x812, 768x1024 y 1280x800, con `scrollWidth == clientWidth` en las quince. Antes: 202 pruebas de Vitest en 23 archivos sobre el #18, sin casos nuevos: las 179 anteriores más 23 de P6 —5 en `MisSolicitudes.test.tsx`, 7 en `NuevaSolicitud.test.tsx` y 11 en `DetalleDeSolicitud.test.tsx`—. Antes: 179 pruebas de Vitest en 20 archivos sobre el código combinado con la portada del #15: las 167 de P5 más las de sesión y hero que aportó Roberto y 7 de la integración —5 en `Inicio.test.tsx` (Explorar, hero, menú y segundo factor) y 2 en `ExplorarServicios.test.tsx` (texto inicial y limpiar filtros)—; recorrido visual del código combinado y 18 capturas a 375x812, 768x1024 y 1280x800, con `scrollWidth == clientWidth` en las dieciocho. Antes: 167 pruebas de Vitest en 20 archivos: las 154 anteriores más 13 de P5 —6 en `ExplorarServicios.test.tsx`, 6 en `ServiciosPropios.test.tsx` y 1 en `App.test.tsx`—. Antes: 154 pruebas de Vitest en 18 archivos: las 145 anteriores más 9 de este PR —3 en `Boton.test.tsx`, 2 en `Entrada.test.tsx` y 4 en `BarraLateral.test.tsx`—. Antes: 145 pruebas de Vitest en 15 archivos: las 116 anteriores más 29 de P4V —16 en `Verificacion.test.tsx` y 13 en `ColaDeVerificaciones.test.tsx`—; recorrido manual y 15 capturas a 375x812, 768x1024 y 1280x800 sobre `d096558`, con `scrollWidth == clientWidth` medido en las quince. Antes: 116 pruebas de Vitest en 13 archivos: las 82 anteriores más 34 de P4 —12 en `PerfilPrestador.test.tsx`, 11 en `Portafolio.test.tsx`, 6 en `ImagenDePerfil.test.tsx` y 5 en `comun/api.test.ts`—; recorrido manual y capturas a 375x812, 768x1024 y 1280x800 sobre `71adc75`, con `scrollWidth == clientWidth` medido en cada una. Antes: 82 pruebas de Vitest: las 39 de P2 más 43 de P3 —18 en `SeguridadCuenta.test.tsx`, 10 en `VerificacionSegundoFactor.test.tsx`, 10 en `PanelAdministrativo.test.tsx` y 5 en `useVigilanciaDeSesion.test.tsx`—; recorrido manual en Chrome a 375x812, 768x1024 y 1280x800 sobre `7045359` y de nuevo sobre `5bdbd43` | Registro, inicio de sesión, cierre de sesión y aviso de sesión vencida, con validación en el formulario y mensajes del backend por campo. Un fallo al cerrar (red, 403, 500 o tiempo agotado) conserva la sesión y permite reintentar. P3 añade `/seguridad` —cambio de contraseña y ciclo completo del segundo factor, con la clave manual y el QR entregados una sola vez—, `/verificar-segundo-factor` para la sesión provisional y `/admin` para el área administrativa; ninguna se queda colgada cuando falla la red. `RutaProtegida` y `RutaAdministrativa` llevan a cada sesión donde le corresponde según lo que le falte: sin sesión, a iniciarla; provisional, a verificar; sin rol, al acceso denegado. La vigilancia de la sesión vive en `App` y no en una pantalla, así que la sesión termina igual en cualquier ruta, y terminarla descarta la caché privada para que la cuenta siguiente no vea nada de la anterior. P4 añade `/prestador`: creación y edición del perfil, imagen, disponibilidad, contactos y portafolio con trabajos e imágenes, todo con sus estados de carga, éxito, error y sin conexión. La imagen de perfil previsualiza el **archivo local** con `URL.createObjectURL` mientras se sube, junto a la que sigue guardada, y no da por aceptado lo que el backend todavía no confirmó; la URL temporal se revoca al sustituir el archivo y al desmontar. La carga de archivos tiene su propia espera de 90 s, por encima del máximo del backend, en lugar de los 10 s de una petición corriente. El orden se cambia con botones de subir y bajar, accesibles con teclado y sin dependencias nuevas, y un aviso deja claro que el perfil sigue privado mientras esté `SIN_VERIFICAR`. P4V añade la sección de verificación dentro de `/prestador` —insignia, qué significa y qué no, aviso de privacidad, elección de varios documentos con su tipo, retirada antes de enviar, confirmación explícita, estado de la solicitud abierta e historial con el motivo de cada decisión— y la cola `/admin/verificaciones` con sus filtros, el detalle del expediente, la toma, la aprobación, el rechazo con motivo y la revocación con motivo y casilla de confirmación. Un conflicto de concurrencia se explica y vuelve a pedir la cola en lugar de dejar el estado viejo en pantalla. El #14 sustituye las tokens provisionales por la paleta naranja de marca (`--color-primary-500` = `#F57C00`), añade `Boton`, `Entrada` y `BarraLateral` en `src/comun/componentes/ui/` y maqueta `/iniciar-sesion` y `/registro` como tarjetas del diseño, sin cambiar el contrato de acceso. Los botones de redes y «olvidé la contraseña» se pintan deshabilitados: el MVP no tiene OAuth ni recuperación de clave. La barra lateral todavía no está enganchada a las pantallas autenticadas. P5 añade `/explorar`, `/explorar/servicios/:id` y `/explorar/prestadores/:id` sin sesión, y `/prestador/servicios` protegida: listado, alta, edición, activar/desactivar e imágenes. Un precio nulo se lee como «A convenir». La insignia pública lleva la advertencia de que no garantiza la calidad futura. El #15 aportó la portada, el encabezado y el 404; este PR los conserva y cablea «Explorar» a `/explorar`, el hero al filtro `texto` y «Mis servicios» en el menú de una sesión plena. Las categorías y la ubicación de la maqueta siguen siendo presentacionales. P6 añade «Mis solicitudes», el formulario desde el detalle público, las bandejas, el historial y las acciones contextuales. Los contactos siguen ocultos. El #18 maqueta `/seguridad` como Configuración: pestañas (solo Cuenta activa), filas de correo/teléfono/idioma/zona horaria de maqueta, contraseña detrás de «Cambiar», `<Entrada>` y `<Boton>` en cambio de clave y 2FA, y el segundo factor en tarjeta aparte. `Entrada` gana un toggle para ver la contraseña. La barra lateral se monta en esa ruta y queda oculta en escritorio. Las seis capturas de esta maqueta —dos por cada tamaño— quedaron adjuntas al #18; su medición de desbordamiento no se registró. P7 añade al detalle de la solicitud el hilo de mensajes y la sección de contactos: lista cronológica con los mensajes propios distinguidos por lado, borde, fondo **y** la etiqueta «Tú», estados de carga, vacío, error con reintento, formulario con etiqueta real y contador, indicador «Enviando…» que impide el doble envío, texto conservado si falla la red, aviso de solo lectura al cerrar la solicitud y aviso propio para una cuenta restringida. El hilo se abre en el último mensaje y se refresca por short polling. La sección de contactos solo se monta para el cliente autorizado, con su estado vacío honesto, y cada entrada se pinta como **texto**, nunca como enlace automático. |
| 4 | Ramas, Conventional Commits, Pull Requests y trazabilidad | En progreso | P0 → P11 | #1, #2, #3, #5, #7, #9, #10, #14, #15, #16, #17, #18, #19, #20, #21 | `eb77733`, `d1cba29` | Check «Título y commits convencionales» en verde; en el #7, sobre su commit final `44f2c11`, [ejecución 32800192187](https://github.com/robertofabiot/moica-hackathon/actions/runs/32800192187); `./mvnw verify` ejecutado además sobre cada commit del backend de P2 por separado | `Docs/Core/GIT_WORKFLOW.md` define ramas, tipos y promoción a `main`; P1 agrega `.github/pull_request_template.md` y la validación automática de título y commits del PR. P2 aporta siete commits atómicos que se pueden leer en orden: esquema, errores, registro, autenticación, ciclo de sesión y las dos entregas de interfaz. P3 aporta dieciséis en `feature/seguridad-permisos-2fa`: siete de implementación, cuatro documentales y cinco de la revisión correctiva —`f290d6b`, `8cf5957`, `70b9063`, `83597a3` y `17531b9`—, que entraron por el mismo PR en lugar de por un arreglo aparte. El #7 se abrió contra `develop`, `robertofabiot` lo aprobó una vez cerrada esa revisión y entró como el merge `21ef17c`; su rama quedó eliminada en el remoto. P4 aporta veintiún commits atómicos en `feature/perfil-portafolio`, legibles en orden: la decisión de almacenamiento, las cuatro migraciones, la integración con R2, cada capacidad del backend, sus pruebas, el traslado de la infraestructura compartida del frontend a `src/comun/`, la interfaz, sus pruebas, la corrección encontrada en el recorrido manual y cinco documentales. Los tres últimos son la ronda correctiva de la revisión de `robertofabiot` sobre `d3b85bb` —`e2c5d6d`, `71adc75` y este cambio documental—, que entró por el mismo PR en lugar de por un arreglo aparte. P4V aporta nueve commits atómicos en `feature/verificacion-prestadores`, legibles en orden: la migración, el almacenamiento privado, el expediente propio, la revisión administrativa, las pruebas, las dos entregas de interfaz, la corrección responsiva encontrada en el recorrido manual y la documentación. El #11 corrigió la concurrencia sobre el nivel del perfil y se fusionó el 28 de agosto de 2026 **sin revisión registrada**; no se anota aquí una aprobación que no existió. Roberto revisó a posteriori el rango `9790215..f88edff` sin hallazgos; el comentario público en el PR queda pendiente y no bloqueó P5. El #14 integró el sistema de diseño y la maqueta de acceso. El #15 fusionó en `develop` (`1a994ca`) la portada y el 404. P5 nace de `develop` en `f195360` como `feature/servicios-busqueda-publica` y entra por el #16 a `develop` mediante `6cd875f`. Roberto aprobó el PR el 29 de agosto de 2026. La CI posterior al merge quedó verde en backend, frontend y Docker Compose: [ejecución 33264757393](https://github.com/robertofabiot/moica-hackathon/actions/runs/33264757393). P6 nace de ese `develop` como `feature/solicitudes-servicio` y se abre en el #17; `robertofabiot` lo aprobó y entró en `develop` mediante el merge `a9884a9`, con `fa43303` como HEAD final de la rama. El #18 nace de `develop` (`a9884a9`) como `feat/ui-configuracion-cuenta` y se abre contra esa rama, no contra `main`; `ErvingMiranda` lo aprobó el 30 de agosto de 2026 y entró como el merge `cfd8cd0`. El #19 sustituyó la licencia MIT por la de solo inspección y entró como `645fd2e`. El #20 incorporó `Docs/Dev/PlanImplementacionMvp.md` y actualizó el README y esta matriz; Roberto lo aprobó y entró como el merge `2dd8e9c`, tras el cual se eliminó su rama remota. P7 nace de ese `develop` (`2dd8e9c`) como `feature/chat-contactos`. |
| 5 | Matriz de cumplimiento mantenida | En progreso | P1 → P11 | #3, #5, #7, #9, #10, #12, #14, #16, #17, #18, #20, #21 | `eb77733`, `61b4af6`, `d74d2da`, `10ff485`, `b42a542`, `44f2c11`, `3ea4ef2`, `6ac22de`, `d3b85bb` | — | Este documento, creado en P1 y actualizado por cada PR. El #7 lo mantuvo con cuatro commits documentales: `d74d2da` abrió «Seguridad de la cuenta de P3» y puso las filas 7, 9 y 10 en su estado real; `10ff485` cambió el marcador del PR por `#7` y anotó su CI; `b42a542` registró la revisión correctiva y dejó por escrito los dos pendientes del segundo factor; `44f2c11` anotó el CI de esa revisión. Sobre ese commit final, [ejecución 32800192184](https://github.com/robertofabiot/moica-hackathon/actions/runs/32800192184) y [ejecución 32800192187](https://github.com/robertofabiot/moica-hackathon/actions/runs/32800192187) quedaron en verde. Las filas 3, 4 y 5 no se actualizaron dentro del #7: se completaron después de integrarlo, en un cambio documental aparte. El #9 abre «Perfil y portafolio de P4» con sus controles y su recorrido manual. La carga real contra R2 se declaró primero como **no comprobada**, sin darla por hecha; la revisión de `robertofabiot` la ejecutó contra el bucket `moica-publico-dev` y la fila se actualizó con lo que esa revisión documenta, sin extenderla a lo que no cubrió. El #10 abre «Verificación documental de P4V» con el mismo criterio: el bucket privado se declaró entonces **no comprobado contra R2 real** y se documentó el comportamiento sin esas variables. El 28 de agosto de 2026 se ejecutaron en local los pasos 3 y 4 del bucket público contra `moica-publico-dev`, y el #12 ejecutó los diez pasos del bucket privado contra `moica-privado-dev`. El #14 abre «Sistema de diseño y maqueta de acceso» y actualiza las filas 3, 4 y 5. El #16 abre «Servicios publicados y descubrimiento de P5». El #17 abre «Ciclo e historial de solicitudes de P6» y actualiza las filas 1, 3, 4, 5, 6, 7 y 11. El #18 abre «Maqueta de configuración de la cuenta» y actualiza las filas 3, 4 y 5. Sus seis capturas de `/seguridad` —dos por cada tamaño— quedaron adjuntas al PR; la medición de desbordamiento no se registró y no se anota como hecha. El #20 incorporó el plan del MVP y corrigió esa evidencia caducada. El #21 abre «Chat y contactos de P7» y actualiza las filas 1, 4, 5, 6, 7 y 11. |
| 6 | Validación de entradas y manejo uniforme de errores | Cumplido | P2 → P7 | #5, #9, #10, #16, #17, #21 | `fe1ab99`, `e2be568`, `525695c`, `b951af8`, `91a4117`, `b48f9de`, `b6fd613` | 17 pruebas de la política de contraseña sobre el DTO; 13 pruebas de integración de registro con casos negativos; 39 pruebas del frontend | Bean Validation en los DTO más un manejador global que traduce cualquier fallo —incluidos los de Spring MVC— a un cuerpo único (`instante`, `estado`, `codigo`, `mensaje`, `ruta` y, en validación, `errores` por campo). Los rechazos de la cadena de seguridad usan ese mismo cuerpo. Ninguna respuesta lleva trazas, SQL ni valores internos. P4 extiende ese formato a las cargas de archivo: tamaño, tipo declarado y firma binaria real se validan en el backend con sus propios códigos, y un fallo del proveedor de almacenamiento sale como un 503 uniforme que no revela endpoint, credenciales ni bucket. P4V hace lo mismo con los documentos del expediente —`DOCUMENTO_NO_ADMITIDO`, `DOCUMENTO_DEMASIADO_GRANDE`, `EXPEDIENTE_INCOMPLETO`— y añade los conflictos del flujo con su propio código: `SOLICITUD_ABIERTA_DUPLICADA`, `SOLICITUD_YA_TOMADA`, `NIVEL_YA_VIGENTE`, `VERIFICACION_BASICA_REQUERIDA` y `TRANSICION_NO_PERMITIDA`. Ninguna respuesta lleva claves de almacenamiento ni URL prefirmadas. P5 añade `SUBCATEGORIA_NO_DISPONIBLE` (400) y `PRESTADOR_NO_DISPONIBLE` (409), reutiliza `VERIFICACION_BASICA_REQUERIDA` al activar y conserva `precioReferencia: null` en la API: «A convenir» es solo presentación. P6 añade `SERVICIO_PROPIO`, `SERVICIO_INACTIVO` y `MOTIVO_OBLIGATORIO`, reutiliza `VERIFICACION_BASICA_REQUERIDA`, `PRESTADOR_NO_DISPONIBLE`, `TRANSICION_NO_PERMITIDA` y `CUENTA_RESTRINGIDA`, y valida descripción, municipio, ubicación y motivo en la frontera. P7 añade `CHAT_NO_HABILITADO`, `CHAT_SOLO_LECTURA` y `CONTACTOS_NO_REVELADOS`, reutiliza `CUENTA_RESTRINGIDA` y `RECURSO_NO_ENCONTRADO`, y valida el contenido del mensaje en la frontera: se recorta antes de validarlo, `@NotBlank` rechaza el mensaje en blanco y `@Size` el que pasa de 2000 caracteres. La base lo respalda con `ck_mensaje_solicitud_contenido`. |
| 7 | Protección de rutas y datos (rol, propiedad, estado de cuenta) | En progreso | P3 → P10B | #7, #9, #10, #16, #17, #21 | `14a2d1a`, `ce9cfcc`, `bc4bfeb`, `1b1cc1f`, `2b7bf91`, `4b4e3f0`, `5bdbd43`, `70b9063`, `83597a3`, `17531b9`, `b951af8`, `d15c888`, `dc0df7a`, `214d734` | 47 pruebas de integración de P4 sobre propiedad y estado de cuenta —13 de `PerfilPrestadorIT`, 8 de `MediosDeContactoIT`, 14 de `PortafolioIT` y 12 de `ImagenDePerfilIT`—, más 11 de `AreaAdministrativaIT`, 14 de `SesionProvisionalIT`, `CambioDeClaveIT.separaSinSesionDeSesionQueNoAlcanzaYDeContrasenaEquivocada`, 10 del frontend en `PanelAdministrativo.test.tsx` y 5 en `useVigilanciaDeSesion.test.tsx` | La cadena de seguridad cierra por omisión: lo que no se declara exige una sesión plena. `UsuarioAutenticado` relee en cada petición el rol, el estado de la cuenta y el segundo factor, así que retirar un permiso surte efecto en la petición siguiente. `/api/admin/**` exige rol administrativo **y** segundo factor verificado en esa sesión; una suspensión bloquea todo salvo consultar y cerrar la sesión. La propiedad del recurso se resuelve sin parámetros: cada endpoint de P3 opera sobre la cuenta de la sesión. En el navegador, terminar una sesión descarta toda la caché remota salvo la propia sesión, así que la cuenta que entra después no puede ver nada de la anterior. P4 aplica esa misma regla a todo lo del prestador: ninguna ruta lleva identificador de cuenta, así que el propietario siempre sale de la sesión; un recurso ajeno responde 404 y no 403 para no permitir enumerar identificadores; y una cuenta `RESTRINGIDA_TEMPORAL` conserva la lectura de lo suyo pero no puede modificarlo. Los contactos siguen ocultos para terceros y no se abrió ninguna superficie pública. P4V añade el caso más delicado: los documentos de identidad. El propietario envía y consulta metadatos, pero **no puede descargar sus propios archivos**; solo un administrador con segundo factor verificado en esa sesión abre uno, y con un acceso temporal que caduca y se autoriza en cada petición. Una solicitud ajena responde 404. Dentro del área administrativa hay una segunda capa: solo quien tomó una revisión puede aprobarla o rechazarla, y una toma concurrente choca con un 409 porque la fila se bloquea. P5 abre únicamente `GET` públicos de catálogos, listado, detalle y perfil; un recurso ajeno o no visible responde 404. Las escrituras de servicios exigen cuenta `ACTIVA` y propiedad. Un prestador `NO_DISPONIBLE` con verificación básica y cuenta operativa conserva el perfil y el portafolio públicos, pero `servicios` sale vacío y `admiteContratacion` es falso; el listado y el detalle público tampoco muestran esos servicios. P6 aplica la misma propiedad a las solicitudes: un tercero recibe 404; una cuenta restringida consulta y cancela, pero no crea, acepta, rechaza ni completa —esas cuatro acciones exigen cuenta `ACTIVA`—; una suspendida queda en 403 `ACCESO_DENEGADO`. Las transiciones se autorizan en el backend. P7 añade la superficie más delicada hasta ahora: el hilo y los contactos. Un tercero recibe 404 en las tres rutas y no puede confirmar que existan. El hilo solo existe si la solicitud **llegó a estar `ACEPTADA`**, cosa que se resuelve mirando el historial y no el estado vigente: una cancelación desde `PENDIENTE` nunca abre chat. Escribir exige cuenta `ACTIVA` y estado `ACEPTADA`; una restringida lee y no escribe; una suspendida queda en 403 `ACCESO_DENEGADO`. En `/contactos` el prestador también recibe 404: la revelación pertenece al cliente, y esa ruta responde 200 a una sola persona. El remitente sale siempre de la sesión y un `idRemitente` enviado en el cuerpo se ignora. La moderación llega en P10B. |
| 8 | Verificación documental de prestadores en dos niveles | Cumplido | P4V | #10 | `8f82231`, `33cd1f7`, `91a4117`, `b48f9de`, `669875f`, `cd68f8f`, `27ae045`, `d096558` | 109 pruebas nuevas del backend —40 unitarias del almacenamiento privado y 69 de integración: 13 de `EsquemaDeVerificacionIT`, 27 de `EnvioDeExpedienteIT`, 20 de `RevisionDeVerificacionIT` y 9 de `RevocacionDeVerificacionIT`— y 29 del frontend: 16 en `Verificacion.test.tsx` y 13 en `ColaDeVerificaciones.test.tsx`. Recorrido manual completo contra el backend local con PostgreSQL real | Los dos niveles y sus cinco estados, con revisión **siempre manual**: ninguna transición ocurre sin una petición de una cuenta administrativa con segundo factor verificado en esa sesión. La básica exige un documento de identidad; la profesional, una básica vigente y un respaldo que no sea identidad. El expediente se envía completo en una sola operación —no existe `BORRADOR`— y un fallo a mitad no deja ni solicitud ni archivos huérfanos. Revocar la básica deja `SIN_VERIFICAR` y anula la profesional en la misma transacción, con el mismo motivo, administrador e instante; esa profesional no revive al obtener otra básica. Las solicitudes y sus documentos resueltos se conservan como evidencia. Los archivos viven en un bucket privado con su propio token; PostgreSQL guarda clave opaca y metadatos, nunca el binario ni una URL, y el archivo solo se abre con un acceso temporal autorizado en cada petición. Detalle en «Verificación documental de P4V» |
| 9 | Autenticación de dos factores (TOTP) | Cumplido | P3 | #7 | `14a2d1a`, `ce9cfcc`, `bc4bfeb`, `1b1cc1f`, `2b7bf91`, `4b4e3f0`, `5bdbd43`, `f290d6b`, `8cf5957` | 22 pruebas de integración de `SegundoFactorIT`, 14 de `SesionProvisionalIT`, 9 unitarias de `AlgoritmoTotpTest` con reloj fijo, 7 de `CifradoDeSecretosTest`, 10 de `PropiedadesDeSegundoFactorTest`, 5 de `SegundoFactorUsuarioTest`, 5 de `RepresentacionSinSecretosTest` y 28 del frontend entre `SeguridadCuenta.test.tsx` y `VerificacionSegundoFactor.test.tsx` | Ciclo completo `PENDIENTE_ACTIVACION` → `ACTIVO` → `DESACTIVADO`, uno por cuenta (lo garantiza la clave primaria compartida). El algoritmo es RFC 6238 mediante `java-otp`; los dígitos, el periodo y la tolerancia viven solo en `moica.segundo-factor.*`. El secreto se genera con `SecureRandom`, se guarda cifrado con AES-GCM y nonce aleatorio, y se entrega una única vez al iniciar la activación; la respuesta que lo lleva pide `no-store` y el navegador lo descarta al dejar la pantalla. Obligatorio para el rol administrativo, opcional para el resto. **Pendiente de decisión del equipo:** un código aceptado admite reutilización dentro de su ventana y los intentos fallidos no están limitados (ver «Segundo factor: reutilización de código e intentos»). |
| 10 | Expiración y revocación de sesión | Cumplido | P2 → P3 | #5, #7 | `6f09fdd`, `b3bcfcc`, `feff7ef`, `14a2d1a`, `1b1cc1f`, `2b7bf91`, `83597a3` | 10 pruebas de integración de `CicloDeSesionIT`, 13 de `CambioDeClaveIT`, 14 de `SesionProvisionalIT`, 6 unitarias de `TokenDeSesionServiceTest` y 5 del frontend en `useVigilanciaDeSesion.test.tsx`; recorrido manual con la base de datos a la vista | Cada login crea una fila `sesion` con expiración de siete días configurable; el JWT solo la señala con su `jti` y su `exp` nunca la supera. Cada petición comprueba la fila: expirada o revocada responde 401 aunque el token siga vigente. Cerrar sesión registra `CIERRE_VOLUNTARIO`. P3 añade la revocación por `CAMBIO_CREDENCIALES`: cambiar la contraseña o desactivar el segundo factor revoca en una sola operación todas las sesiones de la cuenta, incluida la actual, apoyándose en el índice `ix_sesion_id_usuario`. En el navegador, la vigilancia de la sesión vive en `App` y no en una pantalla: vence, se revoca o se pierde igual en `/`, en `/seguridad` y en `/admin`, y un 401 de cualquier consulta autenticada la da por terminada. La revocación por medida administrativa llega en P10B. |
| 11 | Preparación para producción (contenedores, configuración por entorno, migraciones, healthcheck) | En progreso | P1 → P11 | #3, #9, #10, #12, #16, #17, #21 | `78518ff`, `286ca5f`, `715fd3d`, `0f464d2`, `525695c`, `33cd1f7` | `./mvnw verify` en CI; arranque local con Docker Compose | Configuración por variables de entorno comprobada en local incluso con el puerto 5432 ocupado, Flyway aplicando migraciones versionadas sobre PostgreSQL real y `GET /actuator/health` respondiendo `UP`. P4 agrega la configuración del almacenamiento de objetos por entorno: sin las variables `MOICA_R2_*` la aplicación arranca igual y solo las imágenes responden 503, mientras que una configuración a medias detiene el arranque con un mensaje que no revela ningún valor. La conexión con un bucket R2 real quedó comprobada en la revisión del #9 sobre `moica-publico-dev`: configuración, carga de imagen de perfil y de portafolio y persistencia de la URL. El 28 de agosto de 2026 se ejecutaron además los pasos 3 y 4 contra ese mismo bucket: sustituir cambia la URL y deja 404 el objeto anterior; eliminar deja `urlImagenPerfil` en `null` y 404 las dos URLs de la prueba. P4V agrega la segunda superficie con la misma política y dos límites propios: `MOICA_DOCUMENTO_TAMANO_MAXIMO` no admite más de 5 MB —es el tope de `ck_documento_verificacion_tamano`— y `MOICA_DOCUMENTO_URL_TEMPORAL_DURACION` no admite más de una hora; cualquiera de los dos por encima detiene el arranque. El 28 de agosto de 2026 el #12 ejecutó los diez pasos de `Almacenamiento.md` contra `moica-privado-dev`: sin lectura anónima, carga de los tres formatos, clave opaca en PostgreSQL, 403 al propietario y 404 a un ajeno, 302 con acceso temporal de cinco minutos que R2 acepta y luego rechaza, y compensación `503 ALMACENAMIENTO_NO_DISPONIBLE` sin filtrar el proveedor. P5 agrega `V31` y `V90` en el rango reservado, sin variables de entorno nuevas: las imágenes de servicio reutilizan el bucket público y el prefijo `servicios/`. P6 agrega `V40` en el rango `V40`–`V49`, también sin variables nuevas. P7 agrega `V41` en ese mismo rango, igualmente sin variables ni dependencias nuevas. Imágenes de producción, despliegue y proveedor corresponden a P11. |

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
  esa rama al abrir el Pull Request. PR #21, pendiente de revisión.

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
