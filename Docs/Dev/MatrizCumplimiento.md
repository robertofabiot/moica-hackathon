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
| 1 | README técnico completo (requisitos, variables, estructura, scripts, comandos, endpoints) | En progreso | P1 → P11 | #3, #5 | `70467f6`, `61b4af6` | Sus instrucciones se siguieron de principio a fin en una máquina real | `README.md` cubre requisitos, versiones, arquitectura (con diagrama), instalación rápida, comandos de validación y estructura del monorepo. El detalle profundo —variables de entorno, secreto JWT y conflicto de puertos en `Docs/Dev/GuiaEntornoLocal.md`; endpoints, modelo de sesión, CSRF, política de contraseña y forma de los errores en `Docs/Dev/ContratoDeApi.md`— vive en `Docs/Dev/`, enlazado desde la sección «Documentación» del README. El despliegue se completa en P11. |
| 2 | Modelo ER en 3FN y tres diagramas UML completos | Cumplido | P0 | #1, #2 | | | `Docs/Dev/DiagramaLogico.mmd`, `DiagramaConceptual.mmd`, `DiagramaClasesDominio.mmd`, `DiagramaCasosDeUso.tex`, `DiagramaActividades.tex`, `Moica - Diccionario de Datos.xlsx` |
| 3 | Interfaz navegable, validada y responsiva | En progreso | P1 → P11 | #3, #5 | `117af69`, `9c80210`, `e3ba201`, `feff7ef`, `615cba3`, `7045359` | 39 pruebas de Vitest; recorrido manual en Chrome a 375x812, 768x1024 y 1280x800 sobre `7045359` | Registro, inicio de sesión, cierre de sesión y aviso de sesión vencida, con validación en el formulario y mensajes del backend por campo. Un fallo al cerrar (red, 403, 500 o tiempo agotado) conserva la sesión y permite reintentar. Las pantallas de perfil, servicios y solicitudes llegan con sus incrementos. |
| 4 | Ramas, Conventional Commits, Pull Requests y trazabilidad | En progreso | P0 → P11 | #1, #2, #3, #5 | `eb77733`, `d1cba29` | Check «Título y commits convencionales» en verde; `./mvnw verify` ejecutado además sobre cada commit del incremento por separado | `Docs/Core/GIT_WORKFLOW.md` define ramas, tipos y promoción a `main`; P1 agrega `.github/pull_request_template.md` y la validación automática de título y commits del PR. P2 aporta siete commits atómicos que se pueden leer en orden: esquema, errores, registro, autenticación, ciclo de sesión y las dos entregas de interfaz. |
| 5 | Matriz de cumplimiento mantenida | En progreso | P1 → P11 | #3, #5 | `eb77733`, `61b4af6` | — | Este documento, creado en P1 y actualizado por cada PR. |
| 6 | Validación de entradas y manejo uniforme de errores | Cumplido | P2 | #5 | `fe1ab99`, `e2be568` | 17 pruebas de la política de contraseña sobre el DTO; 13 pruebas de integración de registro con casos negativos; 39 pruebas del frontend | Bean Validation en los DTO más un manejador global que traduce cualquier fallo —incluidos los de Spring MVC— a un cuerpo único (`instante`, `estado`, `codigo`, `mensaje`, `ruta` y, en validación, `errores` por campo). Los rechazos de la cadena de seguridad usan ese mismo cuerpo. Ninguna respuesta lleva trazas, SQL ni valores internos. |
| 7 | Protección de rutas y datos (rol, propiedad, estado de cuenta) | En progreso | P3 → P10B | #7 | `14a2d1a`, `ce9cfcc`, `bc4bfeb`, `1b1cc1f`, `2b7bf91`, `4b4e3f0`, `5bdbd43`, `70b9063`, `83597a3`, `17531b9` | 11 pruebas de integración de `AreaAdministrativaIT`, 14 de `SesionProvisionalIT`, `CambioDeClaveIT.separaSinSesionDeSesionQueNoAlcanzaYDeContrasenaEquivocada`, 10 del frontend en `PanelAdministrativo.test.tsx` y 5 en `useVigilanciaDeSesion.test.tsx` | La cadena de seguridad cierra por omisión: lo que no se declara exige una sesión plena. `UsuarioAutenticado` relee en cada petición el rol, el estado de la cuenta y el segundo factor, así que retirar un permiso surte efecto en la petición siguiente. `/api/admin/**` exige rol administrativo **y** segundo factor verificado en esa sesión; una suspensión bloquea todo salvo consultar y cerrar la sesión. La propiedad del recurso se resuelve sin parámetros: cada endpoint de P3 opera sobre la cuenta de la sesión. En el navegador, terminar una sesión descarta toda la caché remota salvo la propia sesión, así que la cuenta que entra después no puede ver nada de la anterior. Moderación y verificación documental llegan en P4V y P10B. |
| 8 | Verificación documental de prestadores en dos niveles | Pendiente | P4V | | | | |
| 9 | Autenticación de dos factores (TOTP) | Cumplido | P3 | #7 | `14a2d1a`, `ce9cfcc`, `bc4bfeb`, `1b1cc1f`, `2b7bf91`, `4b4e3f0`, `5bdbd43`, `f290d6b`, `8cf5957` | 22 pruebas de integración de `SegundoFactorIT`, 14 de `SesionProvisionalIT`, 9 unitarias de `AlgoritmoTotpTest` con reloj fijo, 7 de `CifradoDeSecretosTest`, 10 de `PropiedadesDeSegundoFactorTest`, 5 de `SegundoFactorUsuarioTest`, 5 de `RepresentacionSinSecretosTest` y 28 del frontend entre `SeguridadCuenta.test.tsx` y `VerificacionSegundoFactor.test.tsx` | Ciclo completo `PENDIENTE_ACTIVACION` → `ACTIVO` → `DESACTIVADO`, uno por cuenta (lo garantiza la clave primaria compartida). El algoritmo es RFC 6238 mediante `java-otp`; los dígitos, el periodo y la tolerancia viven solo en `moica.segundo-factor.*`. El secreto se genera con `SecureRandom`, se guarda cifrado con AES-GCM y nonce aleatorio, y se entrega una única vez al iniciar la activación; la respuesta que lo lleva pide `no-store` y el navegador lo descarta al dejar la pantalla. Obligatorio para el rol administrativo, opcional para el resto. **Pendiente de decisión del equipo:** un código aceptado admite reutilización dentro de su ventana y los intentos fallidos no están limitados (ver «Segundo factor: reutilización de código e intentos»). |
| 10 | Expiración y revocación de sesión | Cumplido | P2 → P3 | #5, #7 | `6f09fdd`, `b3bcfcc`, `feff7ef`, `14a2d1a`, `1b1cc1f`, `2b7bf91`, `83597a3` | 10 pruebas de integración de `CicloDeSesionIT`, 13 de `CambioDeClaveIT`, 14 de `SesionProvisionalIT`, 6 unitarias de `TokenDeSesionServiceTest` y 5 del frontend en `useVigilanciaDeSesion.test.tsx`; recorrido manual con la base de datos a la vista | Cada login crea una fila `sesion` con expiración de siete días configurable; el JWT solo la señala con su `jti` y su `exp` nunca la supera. Cada petición comprueba la fila: expirada o revocada responde 401 aunque el token siga vigente. Cerrar sesión registra `CIERRE_VOLUNTARIO`. P3 añade la revocación por `CAMBIO_CREDENCIALES`: cambiar la contraseña o desactivar el segundo factor revoca en una sola operación todas las sesiones de la cuenta, incluida la actual, apoyándose en el índice `ix_sesion_id_usuario`. En el navegador, la vigilancia de la sesión vive en `App` y no en una pantalla: vence, se revoca o se pierde igual en `/`, en `/seguridad` y en `/admin`, y un 401 de cualquier consulta autenticada la da por terminada. La revocación por medida administrativa llega en P10B. |
| 11 | Preparación para producción (contenedores, configuración por entorno, migraciones, healthcheck) | En progreso | P1 → P11 | #3 | `78518ff`, `286ca5f`, `715fd3d`, `0f464d2` | `./mvnw verify` en CI; arranque local con Docker Compose | Configuración por variables de entorno comprobada en local incluso con el puerto 5432 ocupado, Flyway aplicando migraciones versionadas sobre PostgreSQL real y `GET /actuator/health` respondiendo `UP`. Imágenes de producción, despliegue y proveedor corresponden a P11. |

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
