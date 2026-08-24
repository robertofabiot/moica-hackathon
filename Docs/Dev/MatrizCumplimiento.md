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
| 1 | README técnico completo (requisitos, variables, estructura, scripts, comandos, endpoints) | En progreso | P1 → P11 | #3, #5 | `70467f6`, `61b4af6` | Sus instrucciones se siguieron de principio a fin en una máquina real | `README.md` cubre requisitos, versiones, variables, estructura del monorepo, arranque de cada pieza, endpoints de acceso, comandos de validación y build de la PWA. El detalle de configuración local (secreto JWT, conflicto de puertos, healthcheck) vive en `Docs/Dev/GuiaEntornoLocal.md`, y el modelo de sesión, la protección CSRF, la política de contraseña y la forma de los errores en `Docs/Dev/ContratoDeApi.md`; ambos enlazados desde el README. El despliegue se completa en P11. |
| 2 | Modelo ER en 3FN y tres diagramas UML completos | Cumplido | P0 | #1, #2 | | | `Docs/Dev/DiagramaLogico.mmd`, `DiagramaConceptual.mmd`, `DiagramaClasesDominio.mmd`, `DiagramaCasosDeUso.tex`, `DiagramaActividades.tex`, `Moica - Diccionario de Datos.xlsx` |
| 3 | Interfaz navegable, validada y responsiva | En progreso | P1 → P11 | #3, #5 | `117af69`, `9c80210`, `e3ba201`, `feff7ef`, `615cba3`, `7045359` | 39 pruebas de Vitest; recorrido manual en Chrome a 375x812, 768x1024 y 1280x800 sobre `7045359` | Registro, inicio de sesión, cierre de sesión y aviso de sesión vencida, con validación en el formulario y mensajes del backend por campo. Un fallo al cerrar (red, 403, 500 o tiempo agotado) conserva la sesión y permite reintentar. Las pantallas de perfil, servicios y solicitudes llegan con sus incrementos. |
| 4 | Ramas, Conventional Commits, Pull Requests y trazabilidad | En progreso | P0 → P11 | #1, #2, #3, #5 | `eb77733`, `d1cba29` | Check «Título y commits convencionales» en verde; `./mvnw verify` ejecutado además sobre cada commit del incremento por separado | `Docs/Core/GIT_WORKFLOW.md` define ramas, tipos y promoción a `main`; P1 agrega `.github/pull_request_template.md` y la validación automática de título y commits del PR. P2 aporta siete commits atómicos que se pueden leer en orden: esquema, errores, registro, autenticación, ciclo de sesión y las dos entregas de interfaz. |
| 5 | Matriz de cumplimiento mantenida | En progreso | P1 → P11 | #3, #5 | `eb77733`, `61b4af6` | — | Este documento, creado en P1 y actualizado por cada PR. |
| 6 | Validación de entradas y manejo uniforme de errores | Cumplido | P2 | #5 | `fe1ab99`, `e2be568` | 17 pruebas de la política de contraseña sobre el DTO; 13 pruebas de integración de registro con casos negativos; 39 pruebas del frontend | Bean Validation en los DTO más un manejador global que traduce cualquier fallo —incluidos los de Spring MVC— a un cuerpo único (`instante`, `estado`, `codigo`, `mensaje`, `ruta` y, en validación, `errores` por campo). Los rechazos de la cadena de seguridad usan ese mismo cuerpo. Ninguna respuesta lleva trazas, SQL ni valores internos. |
| 7 | Protección de rutas y datos (rol, propiedad, estado de cuenta) | Pendiente | P3 → P10B | | | | |
| 8 | Verificación documental de prestadores en dos niveles | Pendiente | P4V | | | | |
| 9 | Autenticación de dos factores (TOTP) | Pendiente | P3 | | | | |
| 10 | Expiración y revocación de sesión | En progreso | P2 → P3 | #5 | `6f09fdd`, `b3bcfcc`, `feff7ef` | 10 pruebas de integración de `CicloDeSesionIT` y 6 unitarias de `TokenDeSesionServiceTest`; recorrido manual con la base de datos a la vista | Cada login crea una fila `sesion` con expiración de siete días configurable; el JWT solo la señala con su `jti` y su `exp` nunca la supera. Cada petición comprueba la fila: expirada o revocada responde 401 aunque el token siga vigente. Cerrar sesión registra `CIERRE_VOLUNTARIO` y caduca la cookie. La revocación por cambio de credenciales y por medida administrativa llega en P3 y P10B. |
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
