# Despliegue publico de demostracion — P11-A

MOICA se prepara para la Hackathon en Railway Free/Trial, con dominio HTTPS del
proveedor. Esto no certifica una infraestructura comercial definitiva. P11-B/C
continuan en `feature/preparar-entrega-mvp`; no corresponde promover a `main`.

## Arquitectura

```text
Internet -- HTTPS --> Railway -- HTTP --> frontend (Nginx + React/PWA)
                                           /api/* y /actuator/health
                                                  |
                                             red privada
                                                  |
                                               backend
                                             Spring Boot
                                               /     \
                                     PostgreSQL       Cloudflare R2
                                      privado         publico / privado
```

Solo `frontend` necesita dominio publico. No habilitar dominio del backend ni
TCP Proxy de PostgreSQL. Los tres servicios deben compartir proyecto y entorno.
El navegador usa rutas relativas `/api/...`; no se configura CORS ni URL de API
en React. El destino privado se inyecta en Nginx al arrancar.

## Artefactos y construccion

Desde la raiz, con Docker Linux y Node 22:

```bash
docker build -t moica-backend:p11 backend
docker build -t moica-frontend:p11 frontend
node scripts/smoke-produccion.mjs
```

El backend usa Java 21 JDK para Maven Wrapper y una JRE 21 para ejecutar el JAR
como UID 10001. El frontend usa Node 22, `npm ci`, `npm run build` y Nginx.
Cada contexto tiene una lista explicita de archivos permitidos en `.dockerignore`:
no entra `.env`, configuracion local de asistentes, dependencias locales ni Git.
El build de backend omite ejecutar tests dentro de la imagen; el job backend de
CI ejecuta `verify` completo, y el job de produccion arranca las imagenes.

`compose.smoke.yml` es exclusivamente una validacion desechable. El script crea
un proyecto con nombre aleatorio y PostgreSQL vacio, y retira solamente sus
contenedores y volumen al finalizar. Usa `.env.example`, nunca `.env`. El unico
puerto publicado se enlaza a `127.0.0.1:18080`; se cambia con `MOICA_SMOKE_PORT`.
`--no-build` reutiliza imagenes ya construidas. No usar este Compose en Railway.
`docker-compose.yml` conserva PostgreSQL y pgAdmin para desarrollo.

Para comprobar un checkout limpio, clonar la rama en otra carpeta, verificar
`git status --short` vacio y ejecutar el script anterior. Tambien ejecutar:

```bash
cd backend
./mvnw -B -ntp verify
cd ../frontend
npm ci
npm run format:check
npm run lint
npm run typecheck
npm run test
npm run build
```

En PowerShell, usar `mvnw.cmd` y `npm.cmd` si la politica de scripts lo requiere.

## Variables del backend

Se configuran solo en Railway. No pegar secretos en Git, PR, logs o capturas.
No copiar las claves publicas de desarrollo a este entorno.

| Variable | Requisito / tratamiento |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Obligatoria: `prod` |
| `MOICA_DB_HOST` | Obligatoria: referencia `${{PostgreSQL.PGHOST}}`, verificar dominio privado |
| `MOICA_DB_PORT` | Obligatoria: `${{PostgreSQL.PGPORT}}` |
| `MOICA_DB_NOMBRE` | Obligatoria: `${{PostgreSQL.PGDATABASE}}` |
| `MOICA_DB_USUARIO` | Obligatoria: `${{PostgreSQL.PGUSER}}` |
| `MOICA_DB_CLAVE` | Secreta: `${{PostgreSQL.PGPASSWORD}}` |
| `MOICA_JWT_SECRETO` | Secreta, aleatoria, al menos 32 bytes |
| `MOICA_TOTP_CLAVE_CIFRADO` | Secreta y estable, Base64 de 16/24/32 bytes; recomendado 32 |
| `MOICA_COOKIE_SEGURA` | Obligatoria: `true`; `prod` rechaza `false` |
| `MOICA_SOPORTE_CANAL` | Obligatoria, publica: `novastudio26moica@gmail.com` |
| `PORT` | Configurar `8080` para vincular healthcheck y proxy al mismo puerto |
| `MOICA_BACKEND_PORT` | Opcional; tiene prioridad sobre `PORT`, omitir en Railway para evitar divergencias |
| `MOICA_SESION_DURACION` | Opcional: `P7D`; no hay refresh token |
| `MOICA_EXPIRACION_MEDIDAS_PERIODO` | Opcional: `PT1M` |
| `MOICA_ADMIN_CORREO` | Opcional, dato personal; cuenta previamente registrada para bootstrap |

El nombre `PostgreSQL` en las referencias debe coincidir con el servicio creado.
Se reutilizan las propiedades JDBC existentes; no hace falta convertir
`DATABASE_URL` ni mantener dos formas de configurar la conexion.

### Cloudflare R2

| Superficie | Variables necesarias para demostrarla |
|---|---|
| Imagenes publicas | `MOICA_R2_ID_CUENTA`, `MOICA_R2_ACCESS_KEY_ID`, `MOICA_R2_SECRET_ACCESS_KEY`, `MOICA_R2_BUCKET_PUBLICO`, `MOICA_R2_URL_PUBLICA_BASE` |
| Expedientes privados | `MOICA_R2_PRIVADO_ID_CUENTA`, `MOICA_R2_PRIVADO_ACCESS_KEY_ID`, `MOICA_R2_PRIVADO_SECRET_ACCESS_KEY`, `MOICA_R2_BUCKET_PRIVADO` |

Los pares `ACCESS_KEY_ID`/`SECRET_ACCESS_KEY` son credenciales secretas y distintas
por bucket. IDs de cuenta y buckets son configuracion interna. La base URL de
imagenes es publica. No dar ninguna de estas variables al servicio frontend.

Cada grupo es completo o ausente; una configuracion parcial impide arrancar.
Sin un grupo, el backend arranca pero esa superficie devuelve 503. Esto permite
CI sin credenciales y **no demuestra integracion real con R2**.

Opcionales: `MOICA_IMAGEN_TAMANO_MAXIMO=5MB`,
`MOICA_DOCUMENTO_TAMANO_MAXIMO=5MB` (solo reducir),
`MOICA_DOCUMENTO_URL_TEMPORAL_DURACION=PT5M` (maximo una hora).

Para esta demo esta aprobado conservar `r2.dev` en el bucket publico, con sus
limites; no se compra un dominio. Es una excepcion de demostracion a la orientacion
comercial de [Almacenamiento.md](Almacenamiento.md), no un cambio de proveedor.
El bucket privado nunca habilita `r2.dev` ni dominio publico; conserva acceso
prefirmado temporal tras autorizacion administrativa y TOTP.

## Variables del frontend

| Variable | Configuracion |
|---|---|
| `MOICA_BACKEND_UPSTREAM` | Obligatoria: `${{backend.RAILWAY_PRIVATE_DOMAIN}}:${{backend.PORT}}` |
| `PORT` | Puerto Nginx, `8080` por omision; dominio publico debe apuntar al mismo |
| `MOICA_PUBLIC_SCHEME` | `https` por omision, protocolo del origen externo |
| `MOICA_PUBLIC_PORT` | `443` por omision, puerto del origen externo |

No agregar `http://` ni rutas al upstream. Nginx resuelve el nombre por el DNS
del contenedor y lo actualiza cada diez segundos; tolera nuevas IP tras redeploy.
La plantilla se procesa con `envsubst` limitado a esas variables y al DNS, sin
reemplazar las variables propias de Nginx. No se sirve la plantilla al navegador.

## Railway desde cero

1. El propietario inicia sesion y conecta GitHub si Railway lo solicita. Elegir
   Free/Trial, sin activar Hobby, Pro ni addons pagados. Confirmar en el panel
   que hay credito y acceso de red adecuado para R2.
2. Crear proyecto MOICA y agregar PostgreSQL desde **New → Database → PostgreSQL**.
   Mantener el volumen persistente y la red privada. No agregar pgAdmin.
3. Crear `backend` desde `robertofabiot/moica-hackathon`, rama
   `feature/preparar-entrega-mvp`, **Settings → Root Directory: `/backend`**.
   Railway detecta `Dockerfile`. No poner comandos de build/start adicionales:
   se usa el `ENTRYPOINT` de la imagen.
4. Configurar las variables del backend y R2 en **Variables**. Configurar
   **Healthcheck Path: `/actuator/health`**, timeout inicial 240 segundos.
   No asignar dominio publico. Desplegar y esperar salud correcta.
5. Crear `frontend` desde el mismo repositorio/rama, **Root Directory: `/frontend`**,
   Dockerfile detectado, sin override del comando de inicio. Configurar sus
   variables, **Healthcheck Path: `/healthz`** y puerto `8080`.
6. Solo en `frontend`, **Settings → Networking → Generate Domain**; usar el
   dominio HTTPS que genere Railway. No agregar dominio propio.
7. Verificar que no exista acceso publico/TCP Proxy de PostgreSQL ni del backend.
   Anotar dominio, revision Git desplegada, estado de los tres servicios y consumo
   observado. No sacar capturas con valores de variables.
8. Registrar una cuenta administrativa ordinaria en MOICA. Configurar su correo
   en `MOICA_ADMIN_CORREO`, reiniciar backend, activar TOTP desde esa cuenta y
   comprobar `/admin`. Retirar la variable despues del bootstrap; el rol persiste.

PostgreSQL en Railway se aprovisiona mediante su plantilla y volumen. No asumir
que eso incluye SLA, backups o alta disponibilidad comerciales. Registrar la
version de PostgreSQL realmente provisionada y validar Flyway contra ella.

## Migraciones y salud

Flyway es el unico creador del esquema: `ddl-auto=validate`, sin `create` ni
edicion de migraciones publicadas. La base limpia aplica 15 migraciones:
V10, V11, V20–V23, V30, V31, V40–V42, V50–V52 y V90. La ultima version es V90,
la taxonomia demo preexistente; comprobar explicitamente V52 exitosa.
`out-of-order=true` permite actualizar instalaciones que ya tenian V90.

```sql
SELECT version, success FROM flyway_schema_history ORDER BY installed_rank;
```

Backend: `/actuator/health`, respuesta agregada `{"status":"UP"}`; incluye la
conexion con PostgreSQL, sin componentes ni detalles. Nginx publica solo esa
ruta exacta de Actuator. `/actuator/env` y demas rutas devuelven 404.

Ese cuerpo minimo no se obtiene solo con cerrar componentes y detalles: cuando
la plataforma parece orquestada, Spring habilita por su cuenta los grupos de
sondas y la respuesta pasa a `{"groups":["liveness","readiness"],"status":"UP"}`.
Se comprobo levantando la imagen del backend en Docker. No expone infraestructura,
pero hace depender el contrato publico del entorno, asi que `prod` desactiva los
grupos de forma explicita con `management.endpoint.health.probes.enabled=false`.
`SaludPublicaEnProduccionIT` fija ese cuerpo exacto y comprueba ademas que
`/actuator`, `env`, `beans`, `configprops`, `metrics`, `loggers`, `mappings`,
`threaddump`, `heapdump`, `info` y las dos rutas de sondas responden 401 en el
backend, sin depender de Nginx. Railway solo necesita el 200 de esa ruta.
Frontend: `/healthz`, 200 `ok`, comprueba Nginx sin depender del backend.
El smoke comprueba ambos; el healthcheck de despliegue no sustituye monitoreo
continuo ni prueba R2.

## Cookies, CSRF y proxy

`prod` rechaza cookies inseguras y el soporte placeholder. El JWT conserva
`HttpOnly`, `Secure`, `SameSite=Lax`, ruta `/`, sin atributo Domain ni refresh.
Cada peticion autentica consultando la sesion persistida; logout la revoca.
`XSRF-TOKEN` es deliberadamente legible por JavaScript y tambien `Secure/Lax`;
las mutaciones requieren `X-XSRF-TOKEN`.

Nginx elimina `Forwarded` y reconstruye `X-Forwarded-*`. Protocolo/puerto salen
de configuracion runtime, no de headers del navegador. Spring usa la estrategia
Tomcat `native` solo para pares privados/loopback; no se confia en todas las IP.
Esta frontera depende de que backend siga privado y de que solo servicios del
equipo se ejecuten en esa red. No usar este perfil con backend expuesto directo.
Railway termina TLS; Nginx no administra certificados.

La API lleva `Cache-Control: no-store`; no se modifican cookies, metodo o cuerpo.
Se permite multipart hasta 25 MB, igual al tope de transporte Spring. Assets
versionados usan cache inmutable; HTML, manifest y SW se revalidan. Archivos
inexistentes devuelven 404 y las rutas React reciben `index.html`.

## Verificacion publica pendiente de URL real

Registrar fecha, SHA y resultados, sin cookies ni credenciales:

1. Abrir home, explorar, filtros de busqueda y refrescar directamente `/explorar`.
   Revisar manifest, iconos, SW y assets con 200, sin errores de carga.
2. En Network comprobar que las llamadas son al mismo dominio `/api/...`, sin CORS.
   Consultar `/healthz` y `/actuator/health`; comprobar `/actuator/env` cerrado.
3. Registrar/login de cuenta de prueba por HTTPS. Inspeccionar solo los atributos
   de cookies; no capturar sus valores. Mutation sin CSRF → 403; con CSRF → exito.
   Logout → 204 y reutilizar la sesion revocada → 401.
4. Reiniciar/redeploy backend: datos y sesion permanecen, Flyway no aplica de nuevo
   ni destruye tablas. Comprobar V52/V90 y salud tras el reinicio.
5. Con credenciales R2 reales, cargar imagen ficticia y verificar lectura publica.
   Enviar expediente ficticio, comprobar rechazo anonimo/no autorizado y acceso
   temporal solo con administrador y TOTP. Seguir [Almacenamiento.md](Almacenamiento.md).
6. Guardar capturas fuera del repo y adjuntarlas al PR conforme a `GIT_WORKFLOW`.

El smoke local envia cookies explicitamente sobre HTTP de loopback para examinar
la configuracion productiva detras de un proxy que declara HTTPS. **No prueba TLS
publico ni el comportamiento de cookies en un navegador real**: los pasos anteriores
siguen siendo obligatorios. La auditoria PWA/Playwright profunda corresponde a P11-B.

## Redeploy, rollback y limites demo

Redeploy reconstruye/arranca el servicio conservando el volumen de PostgreSQL.
Si un despliegue falla, consultar estado y logs saneados; no reinicializar la base.
Para rollback de aplicacion, usar el ultimo deployment correcto de cada servicio
en Railway y repetir health/smoke. P11-A no agrega migraciones, pero volver codigo
atras no deshace Flyway: confirmar compatibilidad antes de retroceder una version
futura con cambios de esquema. Conservar claves JWT/TOTP; cambiar la clave TOTP
deja ilegibles los secretos guardados.

Consultar credito, RAM, CPU, disco y reinicios en el panel antes de la demostracion.
Si hay OOM o credito agotado, detenerse y registrar limite/consumo/error antes de
solicitar otro plan. No activar Hobby automaticamente. Free/Trial no garantiza
disponibilidad permanente ni conservacion indefinida de datos; preparar respaldo
antes del vencimiento segun las opciones reales disponibles.

Fuentes oficiales consultadas el 5 de septiembre de 2026:
[Trial](https://docs.railway.com/pricing/free-trial),
[monorepo](https://docs.railway.com/deployments/monorepo),
[PostgreSQL](https://docs.railway.com/databases/postgresql),
[red privada](https://docs.railway.com/networking/private-networking/how-it-works),
[healthchecks](https://docs.railway.com/deployments/healthchecks),
[headers publicos](https://docs.railway.com/networking/public-networking/specs-and-limits),
[proxy en Spring Boot](https://docs.spring.io/spring-boot/how-to/webserver.html).

## Deuda y riesgos conocidos

`npm audit` informa `fast-uri` como severidad alta (GHSA-5jgf-p345-68v8 y tres
avisos mas de SSRF y confusion de host). Llega por `@hookform/resolvers` ->
`ajv` -> `fast-uri`, es decir como dependencia indirecta de **produccion**, no
de herramientas. Aun asi no alcanza al runtime, y esto es lo comprobado: el
codigo solo importa `@hookform/resolvers/zod`, de modo que `ajv` no entra en el
empaquetado; una busqueda sobre `dist/assets/*.js` no encuentra `ajv` ni
`fast-uri`. La imagen final tampoco ejecuta Node: Nginx sirve archivos
estaticos. Sin codigo de la biblioteca en el artefacto y sin Node en el runtime,
no hay ruta explotable en produccion. Queda como deuda para P11-B/C, que si
puede mover el lockfile; P11-A no lo toca para no cambiar dependencias mientras
se estabiliza el despliegue. Si mas adelante se usara `ajvResolver`, deja de ser
deuda y pasa a ser bloqueo.

Otros limites ya asumidos: el smoke local no prueba TLS publico ni el
comportamiento de cookies en un navegador real, y la integracion con R2 no se
demuestra sin credenciales reales.

## Evidencia de esta pasada

Ejecutado el 5 de septiembre de 2026 sobre `feature/preparar-entrega-mvp`, en
Windows 11 con Docker 29.7.2 y Node 22.23.2.

| Comprobacion | Resultado |
|---|---|
| `./mvnw -B -ntp verify` | BUILD SUCCESS en 8:32. 170 pruebas unitarias en 28 clases y 584 de integracion en 50 clases, sin fallos, errores ni omitidas. SpotBugs `BugInstance size is 0` |
| `npm run format:check`, `npm run lint`, `npm run typecheck` | Sin hallazgos |
| `npm run test -- --maxWorkers=2` | 392 pruebas en 44 archivos, todas en verde |
| `npm run build` | Correcto. PWA en modo `generateSW`, 7 entradas precargadas (843,33 KiB), `sw.js` y `workbox-*.js` generados |
| Imagen del backend | `moica-backend:p11`, 557 MB |
| Imagen del frontend | `moica-frontend:p11`, 94,3 MB |
| `nginx -t` | Correcto dentro del contenedor |
| `node scripts/smoke-produccion.mjs` | Los tres bloques en PASS y salida 0; retira sus contenedores y su volumen |

Migraciones sobre PostgreSQL nuevo: 15 en el orden
`10,11,20,21,22,23,30,31,40,41,42,50,51,52,90`, con V52 exitosa y V90 la ultima.
Tras reiniciar el backend el historial no cambia, el usuario creado sigue ahi y
la sesion continua siendo valida; el JWT revocado por el cierre de sesion se
rechaza aunque no hubiera vencido.

Queda pendiente Railway: exige que el propietario inicie sesion, asi que todavia
no se han verificado proyecto, plan, servicios, URL publica, metricas ni smoke
publico, y la integracion con R2 sigue sin credenciales reales. No dar P11-A por
completado mientras falte eso.
