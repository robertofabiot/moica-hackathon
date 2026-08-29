# Contrato de la API

Endpoints, modelo de sesion, proteccion CSRF, politica de contraseña y forma de los errores.

## Endpoints

Todos los endpoints de negocio viven bajo `/api`, que es lo que reenvia el proxy de Vite en desarrollo y lo que comparte origen con el frontend en produccion.

| Metodo y ruta | Que hace | Quien puede |
|---|---|---|
| `POST /api/usuarios` | Registra una cuenta | Cualquiera |
| `POST /api/auth/sesion` | Inicia sesion y entrega la cookie de sesion | Cualquiera |
| `GET /api/auth/sesion` | Describe la sesion en curso | Sesion vigente, incluso provisional |
| `POST /api/auth/sesion/segundo-factor` | Completa la sesion en curso con el codigo TOTP | Sesion vigente, incluso provisional |
| `DELETE /api/auth/sesion` | Cierra la sesion y la revoca | Sesion vigente, incluso provisional |
| `PUT /api/auth/clave` | Cambia la contrasena y revoca todas las sesiones | Sesion plena |
| `GET /api/auth/segundo-factor` | Estado del segundo factor de la cuenta | Sesion plena |
| `POST /api/auth/segundo-factor` | Empieza la activacion y entrega el secreto una unica vez | Sesion plena |
| `POST /api/auth/segundo-factor/activacion` | Confirma la activacion con el primer codigo valido | Sesion plena |
| `POST /api/auth/segundo-factor/desactivacion` | Desactiva el segundo factor y revoca todas las sesiones | Sesion plena, sin rol administrativo |
| `GET /api/admin/resumen` | Describe la sesion administrativa en curso | Rol administrativo con segundo factor verificado |
| `GET /api/catalogos/departamentos` | Departamentos habilitados con sus municipios | Cualquiera |
| `POST /api/prestador/perfil` | Crea el perfil de prestador propio | Sesion plena con cuenta `ACTIVA` |
| `GET /api/prestador/perfil` | Devuelve el perfil propio | Sesion plena |
| `PUT /api/prestador/perfil` | Actualiza el perfil propio | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/disponibilidad` | Cambia entre `DISPONIBLE` y `NO_DISPONIBLE` | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/perfil/imagen` | Sube o sustituye la imagen de perfil (`multipart/form-data`) | Sesion plena con cuenta `ACTIVA` |
| `DELETE /api/prestador/perfil/imagen` | Quita la imagen de perfil | Sesion plena con cuenta `ACTIVA` |
| `GET /api/prestador/contactos` | Lista los medios de contacto propios | Sesion plena |
| `POST /api/prestador/contactos` | Agrega un medio de contacto | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/contactos/orden` | Reordena los contactos propios | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/contactos/{id}` | Edita un contacto propio | Sesion plena con cuenta `ACTIVA` |
| `DELETE /api/prestador/contactos/{id}` | Elimina un contacto propio | Sesion plena con cuenta `ACTIVA` |
| `GET /api/prestador/portafolio/trabajos` | Lista los trabajos propios con sus imagenes | Sesion plena |
| `POST /api/prestador/portafolio/trabajos` | Agrega un trabajo al portafolio | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/portafolio/trabajos/orden` | Reordena los trabajos propios | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/portafolio/trabajos/{id}` | Edita un trabajo propio | Sesion plena con cuenta `ACTIVA` |
| `DELETE /api/prestador/portafolio/trabajos/{id}` | Elimina un trabajo, sus imagenes y sus objetos | Sesion plena con cuenta `ACTIVA` |
| `POST /api/prestador/portafolio/trabajos/{id}/imagenes` | Sube una imagen al trabajo (`multipart/form-data`) | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/portafolio/trabajos/{id}/imagenes/orden` | Reordena las imagenes del trabajo | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/portafolio/trabajos/{id}/imagenes/{idImagen}` | Cambia el texto alternativo de una imagen | Sesion plena con cuenta `ACTIVA` |
| `DELETE /api/prestador/portafolio/trabajos/{id}/imagenes/{idImagen}` | Elimina una imagen y su objeto | Sesion plena con cuenta `ACTIVA` |
| `GET /api/prestador/verificacion` | Nivel de verificacion vigente del perfil propio y que puede solicitar | Sesion plena |
| `GET /api/prestador/verificacion/solicitudes` | Historial propio, con los metadatos de cada expediente | Sesion plena |
| `GET /api/prestador/verificacion/solicitudes/{id}` | Una solicitud propia | Sesion plena |
| `POST /api/prestador/verificacion/solicitudes` | Envia una solicitud con todo su expediente (`multipart/form-data`) | Sesion plena con cuenta `ACTIVA` |
| `GET /api/admin/verificaciones` | Cola de solicitudes, con filtros `estado` y `nivel` | Rol administrativo con segundo factor verificado |
| `GET /api/admin/verificaciones/{id}` | Detalle de un expediente | Rol administrativo con segundo factor verificado |
| `POST /api/admin/verificaciones/{id}/toma` | Toma una solicitud pendiente para revisarla | Rol administrativo con segundo factor verificado |
| `POST /api/admin/verificaciones/{id}/aprobacion` | Aprueba y proyecta el nivel en el perfil | Rol administrativo con segundo factor verificado |
| `POST /api/admin/verificaciones/{id}/rechazo` | Rechaza con motivo obligatorio | Rol administrativo con segundo factor verificado |
| `POST /api/admin/verificaciones/{id}/revocacion` | Revoca una verificacion concedida, con motivo obligatorio | Rol administrativo con segundo factor verificado |
| `GET /api/admin/verificaciones/{id}/documentos/{idDocumento}/acceso` | Redirige a un acceso temporal al documento | Rol administrativo con segundo factor verificado |
| `GET /api/catalogos/categorias` | Categorias de servicio con sus subcategorias | Cualquiera |
| `GET /api/prestador/servicios` | Lista los servicios propios, activos e inactivos | Sesion plena |
| `POST /api/prestador/servicios` | Prepara un servicio propio, siempre inactivo | Sesion plena con cuenta `ACTIVA` |
| `GET /api/prestador/servicios/{id}` | Devuelve un servicio propio | Sesion plena |
| `PUT /api/prestador/servicios/{id}` | Edita nombre, descripcion, subcategoria y precio | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/servicios/{id}/estado` | Activa o desactiva un servicio propio | Sesion plena con cuenta `ACTIVA` |
| `POST /api/prestador/servicios/{id}/imagenes` | Sube una imagen al servicio (`multipart/form-data`) | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/servicios/{id}/imagenes/orden` | Reordena las imagenes del servicio | Sesion plena con cuenta `ACTIVA` |
| `PUT /api/prestador/servicios/{id}/imagenes/{idImagen}` | Cambia el texto alternativo de una imagen | Sesion plena con cuenta `ACTIVA` |
| `DELETE /api/prestador/servicios/{id}/imagenes/{idImagen}` | Elimina una imagen y su objeto | Sesion plena con cuenta `ACTIVA` |
| `GET /api/servicios` | Lista publica con filtros `texto`, `idCategoria`, `idSubcategoria` e `idMunicipio` | Cualquiera |
| `GET /api/servicios/{id}` | Detalle publico de un servicio visible | Cualquiera |
| `GET /api/prestadores/{id}` | Perfil publico, portafolio y servicios activos | Cualquiera |
| `POST /api/solicitudes` | Envia una solicitud a un servicio ajeno | Sesion plena con cuenta `ACTIVA` |
| `GET /api/solicitudes/enviadas` | Bandeja de solicitudes enviadas como cliente | Sesion plena |
| `GET /api/solicitudes/recibidas` | Bandeja de solicitudes recibidas en los servicios propios | Sesion plena |
| `GET /api/solicitudes/{id}` | Detalle e historial de una solicitud propia | Sesion plena; solo los dos participantes |
| `POST /api/solicitudes/{id}/aceptacion` | El prestador destinatario acepta una pendiente | Sesion plena con cuenta `ACTIVA` |
| `POST /api/solicitudes/{id}/rechazo` | El prestador destinatario rechaza una pendiente | Sesion plena con cuenta `ACTIVA` |
| `POST /api/solicitudes/{id}/cancelacion` | Cancela segun actor y estado; motivo si esta `ACEPTADA` | Sesion plena |
| `POST /api/solicitudes/{id}/completado` | El prestador destinatario marca una aceptada como completada | Sesion plena con cuenta `ACTIVA` |
| `GET /actuator/health` | Estado de la aplicacion | Cualquiera |

**Sesion plena** es la que no esta pendiente del segundo factor y pertenece a una cuenta que no esta
suspendida. Es tambien lo que exige por omision cualquier ruta que no aparezca en esta tabla: la
cadena de autorizacion cierra todo lo que no se declara.

## Como se autentica una peticion

1. Al iniciar sesion, Moica crea una fila en `sesion` con un identificador aleatorio y una fecha de expiracion (siete dias por omision, configurable con `MOICA_SESION_DURACION`).
2. Ese identificador viaja como `jti` dentro de un JWT firmado, y el JWT viaja en la cookie `moica_sesion`, que es `HttpOnly`, `SameSite=Lax` y `Secure` en produccion. **El token no se guarda en `localStorage` ni en `sessionStorage`.**
3. En cada peticion autenticada, el backend lee el `jti` y comprueba la fila: debe existir, no haber expirado y no haber sido revocada. Si falla cualquiera de las tres, la respuesta es 401.

Por eso cerrar sesion tiene efecto inmediato aunque el JWT conserve una expiracion futura: la fuente de verdad es la fila, no el token.

## Sesion provisional y segundo factor

Cuando la cuenta tiene el segundo factor en estado `ACTIVO`, acertar el correo y la contrasena **no
termina de abrir la sesion**. `POST /api/auth/sesion` responde 201 y entrega la cookie, pero el
cuerpo lo dice sin rodeos:

```json
{
  "usuario": { "...": "..." },
  "sesion": {
    "fechaInicio": "2026-08-24T11:02:10.501-06:00",
    "fechaExpiracion": "2026-08-31T11:02:10.501-06:00",
    "segundoFactorRequerido": true,
    "segundoFactorVerificado": false,
    "pendienteDeSegundoFactor": true
  }
}
```

Mientras `pendienteDeSegundoFactor` sea `true`, esa sesion solo sirve para tres cosas: consultarse
(`GET /api/auth/sesion`), presentar su codigo (`POST /api/auth/sesion/segundo-factor`) y cerrarse
(`DELETE /api/auth/sesion`). Cualquier otra ruta protegida responde 403.

Las dos condiciones son necesarias: `segundoFactorVerificado: false` en una cuenta **sin** segundo
factor activo describe una sesion normal y completa, no una provisional.

Verificar el codigo completa **solo esa sesion**. Las que la misma cuenta tenga abiertas en otros
dispositivos siguen pendientes hasta que cada una presente el suyo.

## Ciclo del segundo factor

Los estados son los del dominio `EstadoSegundoFactor`: `PENDIENTE_ACTIVACION`, `ACTIVO` y
`DESACTIVADO`. Es opcional para una cuenta ordinaria y obligatorio para el rol administrativo.

1. `POST /api/auth/segundo-factor` genera un secreto nuevo y devuelve **la unica respuesta sensible
   del ciclo**: la clave manual en Base32 y la URI `otpauth://` equivalente, junto con los digitos y
   el periodo. Repetir la llamada sustituye el secreto pendiente, de modo que una activacion
   abandonada no deja nada utilizable.
2. `POST /api/auth/segundo-factor/activacion` confirma con el primer codigo valido. El estado pasa a
   `ACTIVO` y la sesion desde la que se activo queda verificada.
3. `POST /api/auth/segundo-factor/desactivacion` exige contrasena **y** codigo. Deja el estado en
   `DESACTIVADO` y revoca todas las sesiones de la cuenta como `CAMBIO_CREDENCIALES`.

Una reactivacion vuelve al paso 1 con un secreto distinto: el anterior pudo quedarse en un telefono
que ya no se controla.

Despues de activarlo, el secreto **no se puede recuperar**: `GET /api/auth/segundo-factor` devuelve
el estado, si es obligatorio y la fecha de activacion, y nada mas. En la base de datos se guarda
cifrado con AES-GCM y una clave que llega por `MOICA_TOTP_CLAVE_CIFRADO`.

Ninguna respuesta de la API se guarda en cache: la cadena de seguridad emite un `Cache-Control` con
`no-store` en todas. Sobre `POST /api/auth/segundo-factor`, que es la unica que lleva el secreto,
hay ademas una prueba que lo fija, para que retirar esa cabecera no pase inadvertido.

Un codigo aceptado **puede volver a presentarse mientras dure su ventana** y los intentos fallidos
no estan limitados. Las dos cosas son endurecimientos pendientes de decision del equipo, descritos
en «Segundo factor: reutilizacion de codigo e intentos» de
[la matriz de cumplimiento](MatrizCumplimiento.md).

Los codigos son de 6 digitos y duran 30 segundos, y se aceptan tambien los del periodo anterior y
el siguiente para absorber el desfase de reloj. Los tres valores estan centralizados en
`moica.segundo-factor.*` y son los mismos que anuncia la URI `otpauth://`.

## Cambio de contrasena

`PUT /api/auth/clave` recibe `claveActual` y `claveNueva`, comprueba la primera, guarda el hash de
la segunda y **revoca todas las sesiones de la cuenta en la misma operacion**, incluida aquella
desde la que se pidio el cambio. Responde 204 y caduca la cookie: despues hay que volver a iniciar
sesion, aqui y en cualquier otro dispositivo. El motivo que queda en la fila es
`CAMBIO_CREDENCIALES`.

La recuperacion de contrasena sigue fuera del MVP.

## Area administrativa

`/api/admin/**` exige **dos condiciones simultaneas**: que la cuenta tenga el rol administrativo y
que **esa** sesion haya verificado el segundo factor. Un usuario ordinario recibe 403; un
administrador sin segundo factor activo, tambien; y un administrador que todavia no ha presentado su
codigo en esa sesion, tambien. Solo entra el administrador con la sesion verificada.

El rol no se solicita ni se concede desde la API: no hay registro publico de administradores ni
endpoint de promocion. Lo asigna el arranque a partir de `MOICA_ADMIN_CORREO`, sobre una cuenta
ordinaria ya registrada (ver [la guia de entorno local](GuiaEntornoLocal.md#rol-administrativo)).

La primera funcion del area es la cola de verificaciones documentales, descrita
mas abajo. La moderacion de casos llega con su propio incremento.

## Cuando es 401 y cuando es 403

| Situacion | Estado | Codigo |
|---|---|---|
| No llega cookie, o su token es invalido | 401 | `NO_AUTENTICADO` |
| La sesion expiro o fue revocada | 401 | `NO_AUTENTICADO` |
| Falta el token CSRF en una operacion mutable | 403 | `ACCESO_DENEGADO` |
| Sesion provisional en una ruta que no le corresponde | 403 | `ACCESO_DENEGADO` |
| Sesion valida sin el rol o sin el segundo factor que exige la ruta | 403 | `ACCESO_DENEGADO` |
| Contrasena actual incorrecta al cambiarla o al desactivar el segundo factor | 403 | `CREDENCIALES_INVALIDAS` |
| Codigo del segundo factor incorrecto, vencido o fuera de tolerancia | 403 | `CODIGO_INVALIDO` |
| La cuenta esta suspendida al iniciar sesion | 403 | `CUENTA_SUSPENDIDA` |
| La cuenta esta restringida y pide modificar su perfil, su portafolio o su expediente | 403 | `CUENTA_RESTRINGIDA` |
| Un administrador quiere resolver una revision que tomo otro | 403 | `REVISION_DE_OTRO_ADMINISTRADOR` |

La regla es una sola: **401 significa que ya no hay sesion y 403 que la hay pero no alcanza**. Por
eso una contrasena o un codigo equivocados no devuelven 401 aunque sean credenciales: la sesion
sigue viva y responder 401 haria creer a la interfaz que acaba de morir. Tampoco son 400: lo que
falla no es la forma de la solicitud sino una credencial que se presento con ella, y la RFC 9110
reserva el 403 justamente para «se recibieron credenciales y el servidor las considera
insuficientes».

Dentro del 403 hay dos situaciones que piden cosas distintas, y **el codigo es lo que las separa**:

| El 403 dice | Codigo | Que tiene que hacer quien lo recibe |
|---|---|---|
| A esta sesion le falta algo para operar aqui | `ACCESO_DENEGADO`, `SEGUNDO_FACTOR_OBLIGATORIO` | Resolver lo que falta: verificar el segundo factor, recargar para obtener el token CSRF o desistir |
| La sesion alcanza, pero el dato presentado no es correcto | `CREDENCIALES_INVALIDAS`, `CODIGO_INVALIDO` | Volver a escribirlo; la sesion sigue vigente |

Ningun cliente debe deducir de un 403 que la sesion termino: eso solo lo dice el 401.

## Perfil de prestador, contactos y portafolio

Todo lo de esta seccion opera **siempre sobre la cuenta de la sesion**. Ninguna
ruta lleva un identificador de cuenta: operar sobre el perfil de otra persona no
es una peticion que se pueda formular.

### Quien puede que

| Situacion | Lectura de lo propio | Mutacion de lo propio |
|---|---|---|
| Sin sesion | 401 `NO_AUTENTICADO` | 401 `NO_AUTENTICADO` |
| Sesion provisional (falta el segundo factor) | 403 `ACCESO_DENEGADO` | 403 `ACCESO_DENEGADO` |
| Cuenta `ACTIVA` | Si | Si |
| Cuenta `RESTRINGIDA_TEMPORAL` | Si | 403 `CUENTA_RESTRINGIDA` |
| Cuenta `SUSPENDIDA_TEMPORAL` o `SUSPENDIDA_PERMANENTE` | 403 `ACCESO_DENEGADO` | 403 `ACCESO_DENEGADO` |
| Recurso de **otro** prestador | 404 `RECURSO_NO_ENCONTRADO` | 404 `RECURSO_NO_ENCONTRADO` |

Un recurso ajeno responde **404 y no 403** a proposito: distinguirlos permitiria
enumerar identificadores para averiguar que contactos o trabajos existen.

### Ciclo del perfil

- Cada cuenta puede tener **como maximo un perfil**. Un segundo intento responde
  409 `PERFIL_YA_EXISTE`.
- Consultar el perfil de una cuenta que todavia no lo creo responde 404
  `PERFIL_NO_ENCONTRADO`; ese codigo es lo que distingue «aun no existe» de un
  fallo, y es lo que usa la interfaz para ofrecer el formulario de creacion.
- Todo perfil nace `DISPONIBLE` y `SIN_VERIFICAR`.
- El municipio principal debe existir y pertenecer a un departamento
  **habilitado**; si no, 400 `MUNICIPIO_NO_DISPONIBLE`. El mensaje no distingue
  entre inexistente y deshabilitado: para quien elige en el formulario, ninguna
  de las dos es una opcion valida.
- **No existe borrar el perfil.** La definicion vigente solo autoriza crearlo y
  actualizarlo.
- `nivelVerificacion` es de **solo lectura**: lo proyecta el flujo de
  verificacion documental al aprobar o revocar una solicitud. Ningun DTO del
  prestador lo acepta, asi que enviarlo no tiene efecto.

Mientras el perfil este `SIN_VERIFICAR` **no aparece en ninguna superficie
publica**. El descubrimiento de P5 solo entrega perfiles con al menos
verificacion basica, cuenta `ACTIVA` y —para listar un servicio— prestador
`DISPONIBLE` y servicio `ACTIVO`.

### Contactos

Entradas libres —un numero, un correo, un usuario o un enlace—, sin
clasificacion por plataforma. En P4 **solo los ve y los administra su
propietario**; se revelaran a un cliente cuando exista una solicitud aceptada,
en el incremento que las implemente.

### Orden

Reordenar envia la **lista completa** de identificadores, no un movimiento:

```json
{ "idsEnOrden": [7, 3, 12] }
```

Debe traer exactamente los elementos existentes, sin repetir ninguno; si sobra,
falta o se repite alguno, la respuesta es 400 `ORDEN_INVALIDO`. Asi la operacion
es idempotente, no depende de en que orden lleguen dos cambios y no puede dejar
posiciones huerfanas.

### Imagenes

Se suben con `multipart/form-data`. El navegador **no** sube directamente al
almacenamiento: siempre pasa por la API.

- Parte `archivo`: el binario. En la subida de una imagen de portafolio, el
  campo `textoAlternativo` viaja en el mismo formulario.
- Formatos admitidos: **JPEG, PNG y WebP**. SVG y PDF no se admiten en esta
  superficie publica.
- Maximo configurable por imagen, 5 MB por omision
  (`MOICA_IMAGEN_TAMANO_MAXIMO`).
- La validacion es del backend, nunca solo del navegador, y **no confia en la
  extension ni en el `Content-Type`**: comprueba tambien la firma binaria real
  del archivo. Una cabecera que no corresponde con el contenido se rechaza.

| Situacion | Estado | Codigo |
|---|---|---|
| Supera el maximo por imagen | 413 | `IMAGEN_DEMASIADO_GRANDE` |
| Formato no admitido, archivo vacio o firma que no corresponde | 400 | `IMAGEN_NO_ADMITIDA` |
| El almacenamiento no responde o no esta configurado | 503 | `ALMACENAMIENTO_NO_DISPONIBLE` |
| La peticion supera el tope de transporte multipart | 413 | `CONTENIDO_DEMASIADO_GRANDE` |

PostgreSQL guarda **unicamente la URL publica**, nunca el binario. Las claves de
los objetos son opacas y no se derivan del nombre original. El detalle del
proveedor —endpoint, credenciales, bucket— no aparece en ninguna respuesta ni en
ningun registro. Ver [Almacenamiento.md](Almacenamiento.md). Las imagenes de
servicio reutilizan el mismo almacén publico y el mismo tope; su prefijo es
`servicios/`.

## Servicios publicados y descubrimiento

La gestion propia opera **siempre sobre la cuenta de la sesion**. El
descubrimiento es publico: Spring Security abre unicamente los `GET` de
catalogos, listado, detalle y perfil publico. No se abren rutas propias,
administrativas ni mutables.

### Quien puede que (gestion propia)

La misma matriz de lectura y mutacion del perfil. Un servicio o imagen de
**otro** prestador responde 404 `RECURSO_NO_ENCONTRADO`, no 403.

No existe `DELETE` del servicio: se desactiva. Borrar fisicamente no es una
peticion que se pueda formular.

### Ciclo de un servicio propio

- Crear exige cuenta `ACTIVA` y perfil existente. El servicio **nace
  `INACTIVO`**, aunque el perfil ya este verificado: activar es una decision
  posterior.
- Un perfil `SIN_VERIFICAR` puede preparar servicios; permanecen inactivos.
- Activar exige, leidos con el perfil bloqueado para no competir con un cambio
  de disponibilidad o una revocacion:
  - cuenta `ACTIVA` (ya comprobada al exigir modificar);
  - prestador `DISPONIBLE`;
  - al menos `VERIFICADO_BASICO`.
- Sin verificacion basica, 409 `VERIFICACION_BASICA_REQUERIDA`. Sin
  disponibilidad, 409 `PRESTADOR_NO_DISPONIBLE`.
- Desactivar no exige verificacion ni disponibilidad.
- La subcategoria debe existir; si no, 400 `SUBCATEGORIA_NO_DISPONIBLE`.
- `precioReferencia` es opcional. Nulo se conserva nulo; «A convenir» es solo
  presentacion. Si viaja, debe ser mayor que cero y con como maximo dos
  decimales.

### Descubrimiento publico

`GET /api/servicios` combina `texto`, `idCategoria`, `idSubcategoria` e
`idMunicipio`. Los parametros ausentes o vacios no filtran. El orden es
determinista: nombre y, si empatan, identificador. No hay paginacion.

Solo aparecen servicios `ACTIVO` de cuentas `ACTIVA`, prestadores `DISPONIBLE` y
perfiles `VERIFICADO_BASICO` o `PROFESIONAL_VERIFICADO`. Un identificador que no
cumpla eso responde 404, igual que uno inexistente.

`GET /api/prestadores/{id}` es distinto: si la cuenta esta operativa y el
perfil tiene al menos verificacion basica, el perfil y su portafolio siguen
visibles aunque el prestador este `NO_DISPONIBLE`. En ese caso `servicios` sale
vacio y `admiteContratacion` es falso. Sus servicios tampoco aparecen en el
listado ni en el detalle publico.

El detalle y el perfil publico llevan `significadoVerificacion` y
`advertenciaDeInsignia`. La advertencia es la misma en todos los niveles: la
insignia no garantiza la calidad futura. No viajan contactos, correos privados,
documentos, numeros de identidad, observaciones administrativas ni claves de
almacenamiento. No se inventan reputaciones ni calificaciones.

`admiteContratacion` avisa si hoy se podria solicitar. Es falso cuando el
prestador no esta disponible. Solicitar vive en `/api/solicitudes`; este campo
no revela contactos.

`GET /api/catalogos/departamentos` y `GET /api/catalogos/categorias` son
publicos para alimentar los filtros. La taxonomia de demostracion no se
presenta como exhaustiva.

## Solicitudes de servicio

El ciclo de contratacion opera sobre la sesion. Las acciones son explicitas
—aceptar, rechazar, cancelar, completar— y no un cambio generico de estado. No
hay `DELETE`. El chat, la revelacion de contactos y las calificaciones no
viven aqui.

### Quien puede que

Solo el cliente solicitante y el prestador destinatario leen una solicitud, su
ubicacion escrita y su historial. Un tercero recibe 404 `RECURSO_NO_ENCONTRADO`,
igual que en el resto de recursos propios.

Una cuenta `RESTRINGIDA_TEMPORAL` consulta sus bandejas, el detalle y cancela
un compromiso existente. Enviar, aceptar, rechazar y completar exigen cuenta
`ACTIVA`. Una restringida no puede ejecutar ninguna de esas cuatro acciones.
Una cuenta suspendida no ejecuta acciones autenticadas de negocio: la cadena
responde 403 `ACCESO_DENEGADO`.

### Crear

`POST /api/solicitudes` exige sesion plena y cuenta `ACTIVA`. En la misma
transaccion nace `PENDIENTE` y el cambio inicial del historial
(`estadoAnterior` nulo, actor = cliente). Las pendientes no expiran.

El cuerpo `SolicitudDeContratacion`:

| Campo | Obligatorio | Limite |
|---|---|---|
| `idServicioPublicado` | Si | Identificador existente |
| `descripcionNecesidad` | Si | Texto, maximo 3000 caracteres de aplicacion |
| `idMunicipio` | Si | Municipio de un departamento habilitado |
| `indicacionUbicacion` | Si | Texto, maximo 2000 caracteres de aplicacion |
| `fechaPreferida` | No | `DATE` (`YYYY-MM-DD`). No se exige que sea futura |

Rechazos al crear:

| Situacion | HTTP | Codigo |
|---|---|---|
| Sin sesion | 401 | `NO_AUTENTICADO` |
| Cuenta restringida | 403 | `CUENTA_RESTRINGIDA` |
| Servicio inexistente | 404 | `RECURSO_NO_ENCONTRADO` |
| Servicio propio | 409 | `SERVICIO_PROPIO` |
| Servicio `INACTIVO` | 409 | `SERVICIO_INACTIVO` |
| Perfil sin verificacion basica | 409 | `VERIFICACION_BASICA_REQUERIDA` |
| Prestador no disponible o cuenta no operativa | 409 | `PRESTADOR_NO_DISPONIBLE` |
| Municipio de un departamento no habilitado | 400 | `MUNICIPIO_NO_DISPONIBLE` |
| Descripcion o ubicacion vacias | 400 | `VALIDACION` |

No se impide enviar varias solicitudes historicas al mismo servicio.

### Transiciones

| Estado actual | Accion | Actor | Resultado | Motivo |
|---|---|---|---|---|
| `PENDIENTE` | Aceptar | Prestador destinatario con cuenta `ACTIVA` | `ACEPTADA` | No |
| `PENDIENTE` | Rechazar | Prestador destinatario con cuenta `ACTIVA` | `RECHAZADA` | No |
| `PENDIENTE` | Cancelar | Cliente solicitante | `CANCELADA` | No |
| `ACEPTADA` | Cancelar | Cualquiera de los dos | `CANCELADA` | Obligatorio |
| `ACEPTADA` | Completar | Prestador destinatario con cuenta `ACTIVA` | `COMPLETADA` | No |

`RECHAZADA`, `CANCELADA` y `COMPLETADA` son definitivos. No se reabren.

Cada transicion ocurre en una transaccion, bloquea la fila, actualiza
`estadoActual` y escribe `CambioEstadoSolicitud` con el mismo instante. El
estado vigente coincide siempre con el ultimo cambio del historial. Dos
acciones simultaneas no dejan transiciones incompatibles.

Cancelar una `ACEPTADA` sin motivo o con motivo en blanco responde 400
`MOTIVO_OBLIGATORIO`. El tope del motivo es 2000 caracteres. Un actor o un
estado incorrectos responden 409 `TRANSICION_NO_PERMITIDA`. Una cuenta
restringida que intenta enviar, aceptar, rechazar o completar responde 403
`CUENTA_RESTRINGIDA` y no escribe historial.

Aceptar no revela correos ni contactos: solo deja el estado listo para el
incremento del chat.

### Lectura

`GET /api/solicitudes/enviadas` y `GET /api/solicitudes/recibidas` ordenan de
la mas reciente a la mas antigua. El detalle incluye el historial
cronologico. Ningun cuerpo lleva `correoElectronico`, contactos externos,
documentos, hashes, secretos TOTP ni claves de almacenamiento.

## Verificacion documental del prestador

El expediente propio se opera **siempre sobre la cuenta de la sesion**: ninguna
ruta de `/api/prestador/verificacion` lleva identificador de perfil. Resolver
una solicitud vive aparte, en `/api/admin/verificaciones`, y hereda las dos
condiciones del area administrativa.

### Niveles y estados

| Nivel del perfil | Como se alcanza |
|---|---|
| `SIN_VERIFICAR` | Estado inicial de todo perfil, y al que vuelve tras revocar la basica |
| `VERIFICADO_BASICO` | Un administrador aprobo una solicitud `BASICA` |
| `PROFESIONAL_VERIFICADO` | Sobre la basica vigente, un administrador aprobo una solicitud `PROFESIONAL` |

Una solicitud recorre `PENDIENTE`, `EN_REVISION`, `APROBADA`, `RECHAZADA` y
`REVOCADA`. **No existe `BORRADOR`.** Las unicas transiciones permitidas son:

```text
PENDIENTE   --toma-->       EN_REVISION
EN_REVISION --aprobacion--> APROBADA
EN_REVISION --rechazo-->    RECHAZADA
APROBADA    --revocacion--> REVOCADA
```

Cualquier otra responde `409 TRANSICION_NO_PERMITIDA` y no deja nada a medias.

### Enviar un expediente

`POST /api/prestador/verificacion/solicitudes` recibe `multipart/form-data` con:

- `nivelSolicitado`: `BASICA` o `PROFESIONAL`.
- una parte `archivo` por documento;
- un campo `tipoDocumento` por documento, **en el mismo orden** que los
  archivos. Los valores son los del dominio: `IDENTIDAD`, `CERTIFICACION`,
  `CONSTANCIA`, `REGISTRO_NEGOCIO` y `OTRO_RESPALDO`.

Es **una sola peticion** a proposito: la solicitud y su expediente nacen juntos
o no nacen. Si algo falla, no queda una solicitud pendiente sin documentos ni un
archivo suelto en el almacenamiento. El navegador **no** sube directamente al
proveedor: siempre pasa por la API.

Reglas que se aplican al recibirlo:

- Solo una cuenta `ACTIVA` puede enviar. Una restringida conserva la lectura de
  lo suyo.
- `BASICA` se solicita cuando el perfil esta `SIN_VERIFICAR` y exige **al menos
  un documento `IDENTIDAD`**.
- `PROFESIONAL` exige **una basica vigente** y **al menos un respaldo que no sea
  identidad**.
- Solo puede haber **una solicitud abierta** —`PENDIENTE` o `EN_REVISION`— del
  mismo nivel por perfil. Lo garantiza tambien un indice parcial en PostgreSQL,
  asi que dos envios simultaneos no crean dos.
- Una solicitud rechazada **puede reenviarse como una solicitud nueva**; la
  anterior y sus documentos se conservan.
- **No se pueden editar ni sustituir documentos despues de enviar.** Corregir
  algo significa presentar otra solicitud.
- No hay un maximo de documentos por expediente: el limite efectivo es el tope
  de transporte multipart (25 MB por peticion) y los 5 MB por archivo.

### Formatos y validacion

Solo `image/jpeg`, `image/png` y `application/pdf`. Se valida en el backend
—nunca solo en el navegador— el tamano, el tipo declarado y **la firma binaria
real**, que debe corresponder con lo declarado.

| Situacion | Estado | Codigo |
|---|---|---|
| Supera el maximo por documento | 413 | `DOCUMENTO_DEMASIADO_GRANDE` |
| Formato no admitido, archivo vacio o firma que no corresponde | 400 | `DOCUMENTO_NO_ADMITIDO` |
| Expediente sin documentos o sin el respaldo que exige el nivel | 400 | `EXPEDIENTE_INCOMPLETO` |
| Ya hay una solicitud abierta de ese nivel | 409 | `SOLICITUD_ABIERTA_DUPLICADA` |
| El perfil ya tiene ese nivel vigente | 409 | `NIVEL_YA_VIGENTE` |
| Se pide la profesional sin una basica vigente | 409 | `VERIFICACION_BASICA_REQUERIDA` |
| El almacenamiento privado no responde o no esta configurado | 503 | `ALMACENAMIENTO_NO_DISPONIBLE` |
| La peticion supera el tope de transporte multipart | 413 | `CONTENIDO_DEMASIADO_GRANDE` |

### Que ve cada quien

| | Propietario del perfil | Administrador con TOTP verificado |
|---|---|---|
| Estado, nivel, fechas y motivo de la decision | Si | Si |
| Metadatos de los documentos (tipo, nombre saneado, MIME, tamano) | Si | Si |
| A que cuenta pertenece el perfil (nombre y correo) | Su propia cuenta | Si |
| Que administrador tiene la revision | **No** | Si |
| Abrir el archivo | **No** | Si, con acceso temporal |
| Clave de almacenamiento o URL del archivo | **Nunca** | **Nunca** |

Consultar una solicitud de otro prestador responde **404 y no 403**, igual que
el resto de recursos propios: distinguirlos permitiria enumerar quien presento
expediente.

### Revisar y resolver

- Tomar una solicitud la pasa de `PENDIENTE` a `EN_REVISION` y la asigna a quien
  la tomo. Una toma concurrente responde `409 SOLICITUD_YA_TOMADA`; la fila se
  bloquea, asi que dos administradores no pueden quedarsela los dos.
- **Solo quien tomo la revision puede aprobarla o rechazarla.** Otro
  administrador recibe `403 REVISION_DE_OTRO_ADMINISTRADOR`.
- Aprobar proyecta el nivel en el perfil. Aprobar una profesional vuelve a
  comprobar que la basica siga vigente: si se revoco entre el envio y la
  revision, responde `409 VERIFICACION_BASICA_REQUERIDA` y hay que rechazarla
  indicando el motivo.
- Esa comprobacion vale tambien **mientras** se revoca la basica, no solo
  despues. Aprobar, rechazar y revocar toman antes la fila del perfil, asi que
  dos resoluciones sobre solicitudes distintas del mismo prestador se resuelven
  una detras de otra: la segunda ve lo que decidio la primera. Si la revocacion
  de la basica llega primero, la aprobacion profesional responde
  `409 VERIFICACION_BASICA_REQUERIDA`; si llega despues, arrastra tambien la
  profesional recien aprobada. Nunca queda un nivel que contradiga a las
  solicitudes.
- Editar el perfil, cambiar la disponibilidad o sustituir la imagen comparten esa
  misma fila y ese mismo turno. Guardar el perfil no devuelve una insignia
  retirada ni borra la que acaba de concederse, y una resolucion administrativa
  no revierte los datos que el propietario acaba de editar.
- **Rechazar y revocar exigen una observacion no vacia.** Sin ella la respuesta
  es `400 VALIDACION`, y PostgreSQL lo vuelve a exigir con
  `ck_solicitud_verificacion_observacion`.
- Rechazar una profesional **no toca la basica vigente**.
- Revocar **no exige haber tomado la solicitud**: la aprobo otra persona en otro
  momento y esa revision ya esta cerrada.

### Efectos de una revocacion

- Revocar una **profesional** degrada el perfil a `VERIFICADO_BASICO`.
- Revocar una **basica** devuelve el perfil a `SIN_VERIFICAR` y, **en la misma
  transaccion**, deja `REVOCADA` cualquier profesional aprobada de ese perfil,
  con el **mismo motivo, el mismo administrador y el mismo instante**.
- Una profesional revocada asi **no se reactiva sola** si el perfil vuelve a
  obtener la basica: recuperar la insignia exige una solicitud nueva.

Las solicitudes y los documentos resueltos **no se eliminan**: son la evidencia
de que se reviso y de quien lo decidio. El propietario nunca puede cambiar el
nivel de su perfil por ninguna via.

### Abrir un documento

`GET /api/admin/verificaciones/{id}/documentos/{idDocumento}/acceso` responde
**302** hacia una URL prefirmada de vida corta, con `Cache-Control: no-store`.
Se prefiere la redireccion a devolverla en un cuerpo JSON para que la direccion
firmada no pase por el JavaScript de la aplicacion ni entre en su cache. La URL
no se persiste en ninguna parte, caduca sola —`PT5M` por omision— y la
autorizacion se comprueba en **cada** peticion. Un documento que no pertenece a
ese expediente responde `404 DOCUMENTO_NO_ENCONTRADO`. Ver
[Almacenamiento.md](Almacenamiento.md).

## Proteccion CSRF

La proteccion CSRF esta activa para todas las operaciones mutables. El backend emite la cookie `XSRF-TOKEN` —legible por JavaScript a proposito— y espera recibirla de vuelta en la cabecera `X-XSRF-TOKEN`. El frontend lo hace solo; para probar con `curl` hay que repetir el tramite:

```bash
# 1. Cualquier respuesta trae la cookie con el token
curl -s -c galletas.txt -o /dev/null http://localhost:8080/api/auth/sesion

# 2. Se devuelve en la cabecera de la operacion mutable
TOKEN=$(grep XSRF-TOKEN galletas.txt | awk '{print $7}')
curl -s -b galletas.txt -X POST http://localhost:8080/api/usuarios   -H "Content-Type: application/json" -H "X-XSRF-TOKEN: $TOKEN"   -d '{"nombreCompleto":"Persona de prueba","correoElectronico":"persona@moica.test","clave":"Moica2026$segura"}'
```

Sin el paso 2 la respuesta es `403`.

## Politica de contraseña

De 8 a 72 caracteres, con al menos una mayuscula, una minuscula, un numero y un simbolo. No hace falta alternar tipos en cada caracter. El maximo lo impone BCrypt, que solo tiene en cuenta los primeros 72 bytes: una contraseña con acentos o emojis puede alcanzar ese limite antes de los 72 caracteres, y en ese caso se rechaza con una explicacion, no con un error del servidor.

La recuperacion de contraseña queda fuera del MVP.

## Forma de los errores

Todos los errores comparten cuerpo. El detalle por campo solo aparece cuando el fallo es de validacion:

```json
{
  "instante": "2026-08-21T11:18:40.222525-06:00",
  "estado": 400,
  "codigo": "VALIDACION",
  "mensaje": "Revisa los datos enviados.",
  "ruta": "/api/usuarios",
  "errores": [{ "campo": "correoElectronico", "mensaje": "Escribe un correo electronico valido." }]
}
```

Codigos que devuelve hoy la API: `VALIDACION`, `SOLICITUD_INVALIDA`, `CORREO_YA_REGISTRADO`, `CREDENCIALES_INVALIDAS`, `CUENTA_SUSPENDIDA`, `CUENTA_RESTRINGIDA`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, `CODIGO_INVALIDO`, `SEGUNDO_FACTOR_NO_ACTIVO`, `SEGUNDO_FACTOR_YA_ACTIVO`, `SEGUNDO_FACTOR_SIN_ACTIVACION_PENDIENTE`, `SEGUNDO_FACTOR_OBLIGATORIO`, `PERFIL_YA_EXISTE`, `PERFIL_NO_ENCONTRADO`, `MUNICIPIO_NO_DISPONIBLE`, `ORDEN_INVALIDO`, `IMAGEN_NO_ADMITIDA`, `IMAGEN_DEMASIADO_GRANDE`, `DOCUMENTO_NO_ADMITIDO`, `DOCUMENTO_DEMASIADO_GRANDE`, `DOCUMENTO_NO_ENCONTRADO`, `EXPEDIENTE_INCOMPLETO`, `SOLICITUD_ABIERTA_DUPLICADA`, `SOLICITUD_NO_ENCONTRADA`, `SOLICITUD_YA_TOMADA`, `NIVEL_YA_VIGENTE`, `VERIFICACION_BASICA_REQUERIDA`, `TRANSICION_NO_PERMITIDA`, `REVISION_DE_OTRO_ADMINISTRADOR`, `SERVICIO_PROPIO`, `SERVICIO_INACTIVO`, `PRESTADOR_NO_DISPONIBLE`, `MOTIVO_OBLIGATORIO`, `ALMACENAMIENTO_NO_DISPONIBLE`, `RECURSO_NO_ENCONTRADO`, `METODO_NO_PERMITIDO`, `CONTENIDO_DEMASIADO_GRANDE`, `TIPO_DE_CONTENIDO_NO_ADMITIDO` y `ERROR_INTERNO`. Ninguna respuesta de error lleva trazas, SQL, secretos TOTP, hashes, claves de almacenamiento, URL prefirmadas ni valores internos.
