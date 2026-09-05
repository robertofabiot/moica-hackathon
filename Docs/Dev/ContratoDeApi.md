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
| `GET /api/solicitudes/{id}/mensajes` | Hilo de mensajes de la solicitud, en orden cronologico | Sesion plena; solo los dos participantes, y solo si llego a estar `ACEPTADA` |
| `POST /api/solicitudes/{id}/mensajes` | Agrega un mensaje al hilo | Sesion plena con cuenta `ACTIVA`; solo los dos participantes y solo mientras esta `ACEPTADA` |
| `GET /api/solicitudes/{id}/contactos` | Contactos externos del prestador revelados por la aceptacion | Sesion plena; **solo el cliente** participante, y solo si llego a estar `ACEPTADA` |
| `GET /api/solicitudes/{id}/calificacion` | A quien califica la sesion, en que rol, si puede y que escribio | Sesion plena; solo los dos participantes |
| `POST /api/solicitudes/{id}/calificacion` | Registra la calificacion de la sesion sobre la contraparte | Sesion plena con cuenta `ACTIVA`; solo los dos participantes y solo si esta `COMPLETADA` |
| `GET /api/solicitudes/{id}/reputacion-del-cliente` | Reputacion como cliente de quien contrato | Sesion plena; **solo el prestador** participante |
| `GET /api/solicitudes/{id}/caso-moderacion` | A quien puede reportar la sesion y que caso abrio, si abrio uno | Sesion plena; solo los dos participantes |
| `POST /api/solicitudes/{id}/caso-moderacion` | Abre el caso de moderacion sobre la contraparte | Sesion plena, tambien con cuenta `RESTRINGIDA_TEMPORAL`; solo los dos participantes y solo si la solicitud llego a `ACEPTADA` |
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
clasificacion por plataforma. Aqui **solo los ve y los administra su
propietario**. A un cliente se le revelan por
`GET /api/solicitudes/{id}/contactos` cuando el prestador acepta su solicitud;
esta ruta no cambia.

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
almacenamiento.

Las tres superficies publicas llevan ademas `reputacionPrestador`, el agregado
real del prestador que publica —no del servicio concreto—: no existe una
reputacion por servicio, asi que todas las tarjetas de una misma persona
muestran la misma cifra. Es el mismo objeto que se describe en «Calificaciones y
reputacion»: promedio, cantidad y desglose por estrellas, sin comentarios ni
identidades de quienes calificaron. `promedio` es `null` mientras no haya
ninguna calificacion; **no se envia `0.0`**. El listado resuelve los agregados
de todos los prestadores de la pagina con una sola consulta agrupada.

`admiteContratacion` avisa si hoy se podria solicitar. Es falso cuando el
prestador no esta disponible. Solicitar vive en `/api/solicitudes`; este campo
no revela contactos.

`GET /api/catalogos/departamentos` y `GET /api/catalogos/categorias` son
publicos para alimentar los filtros. La taxonomia de demostracion no se
presenta como exhaustiva.

## Solicitudes de servicio

El ciclo de contratacion opera sobre la sesion. Las acciones son explicitas
—aceptar, rechazar, cancelar, completar— y no un cambio generico de estado. No
hay `DELETE`. El chat, la revelacion de contactos, las calificaciones y los
reportes cuelgan de la solicitud, pero cada uno en su propia superficie
autorizada: ver «Chat y contactos de una solicitud», «Calificaciones y
reputacion» y «Reportes y casos de moderacion».

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

Aceptar habilita el hilo de mensajes y la revelacion de contactos, pero ninguno
de los dos viaja en estos cuerpos: viven en su propia superficie autorizada, la
de la seccion siguiente.

## Chat y contactos de una solicitud

El hilo es la solicitud: no hay recurso `conversacion` ni identificador de hilo.
Cada mensaje cuelga de `/api/solicitudes/{id}`, y el estado del chat se deriva
del estado de esa solicitud. Solo hay leer y escribir: **no existe `PUT`,
`PATCH` ni `DELETE`** de un mensaje. Tampoco imagenes, audios, archivos,
reacciones ni confirmaciones de lectura.

La actualizacion es por **short polling** desde el navegador —cinco segundos el
hilo, veinte el detalle—. El MVP no usa WebSockets.

### Cuando existe el hilo

El estado vigente no basta para saberlo. Una solicitud `CANCELADA` puede venir
de `PENDIENTE`, y entonces nunca hubo hilo, o de `ACEPTADA`, y entonces su
historial queda visible en solo lectura. Lo que decide es si la solicitud
**llego a estar `ACEPTADA` alguna vez**, cosa que solo sabe su historial de
cambios.

| Estado de la solicitud | Leer el hilo | Enviar | Contactos externos |
|---|---|---|---|
| `PENDIENTE` | 409 `CHAT_NO_HABILITADO` | 409 `CHAT_NO_HABILITADO` | 409 `CONTACTOS_NO_REVELADOS` |
| `ACEPTADA` | Los dos participantes | Los dos, con cuenta `ACTIVA` | Solo el cliente participante |
| `RECHAZADA` | 409 `CHAT_NO_HABILITADO` | 409 `CHAT_NO_HABILITADO` | 409 `CONTACTOS_NO_REVELADOS` |
| `CANCELADA` desde `PENDIENTE` | 409 `CHAT_NO_HABILITADO` | 409 `CHAT_NO_HABILITADO` | 409 `CONTACTOS_NO_REVELADOS` |
| `CANCELADA` tras `ACEPTADA` | Los dos, solo lectura | 409 `CHAT_SOLO_LECTURA` | Sigue revelado al cliente |
| `COMPLETADA` | Los dos, solo lectura | 409 `CHAT_SOLO_LECTURA` | Sigue revelado al cliente |

Cancelar o completar **no vuelve a ocultar** los contactos: el compromiso
existio. Y ni la disponibilidad del prestador ni una revocacion posterior de su
verificacion cierran un hilo ya abierto; eso dejaria un compromiso aceptado sin
forma de coordinarse.

### Quien puede que

Un tercero recibe **404 `RECURSO_NO_ENCONTRADO`** en las tres rutas, igual que
en el resto de recursos propios: no puede confirmar que el hilo, los mensajes o
los contactos existan.

En `/contactos` el **prestador tambien recibe 404**. La revelacion es un recurso
del cliente; el prestador administra sus propios medios en
`GET /api/prestador/contactos`. Asi esa ruta responde 200 a una sola persona.

Una cuenta `RESTRINGIDA_TEMPORAL` **lee** el hilo y conserva la revelacion, pero
al enviar recibe 403 `CUENTA_RESTRINGIDA`. Una cuenta suspendida no llega:
la cadena responde 403 `ACCESO_DENEGADO`.

### Enviar un mensaje

`POST /api/solicitudes/{id}/mensajes` con el cuerpo `MensajeAEnviar`:

| Campo | Obligatorio | Limite |
|---|---|---|
| `contenido` | Si | Texto, maximo 2000 caracteres de aplicacion |

El diccionario modela `contenido` como `TEXT` sin maximo; los 2000 caracteres
son un limite de la aplicacion, como en el resto de textos libres de Moica. El
contenido se recorta antes de validarlo, de modo que un mensaje de espacios
queda vacio y responde 400 `VALIDACION`. La base lo respalda con
`ck_mensaje_solicitud_contenido`.

**El remitente sale siempre de la sesion.** El cuerpo no lo lleva y un
`idRemitente` enviado por el navegador se ignora: escribir en nombre de otra
persona no es una peticion que se pueda formular.

La respuesta es 201 con el mensaje creado: identificador, solicitud, remitente,
su `nombreCompleto` —el mismo que ya viaja en el historial—, contenido e
instante. Nunca correo, estado de cuenta ni datos administrativos.

El envio **bloquea la fila de la solicitud** antes de comprobar su estado y no
la suelta hasta confirmar. Por eso solo hay dos desenlaces posibles cuando
alguien escribe justo mientras la solicitud se cierra: o el mensaje queda
confirmado antes de la transicion, o la transicion gana y el envio responde 409.
Nunca aparece un mensaje con instante posterior al cierre.

### Leer el hilo

`GET /api/solicitudes/{id}/mensajes` devuelve la lista completa ordenada por
`fechaEnvio` y, a igualdad de instante, por `idMensajeSolicitud`: el orden es
estable entre dos lecturas.

### Contactos revelados

`GET /api/solicitudes/{id}/contactos` devuelve las entradas libres que el
prestador configuro como `MedioContactoPrestador`, en su orden de
visualizacion. **Una lista vacia es una respuesta legitima:** significa que el
prestador no publico ningun contacto, no que falte permiso.

Solo se revelan esas entradas. Nunca el correo de la cuenta ni ningun dato
tomado de la autenticacion. No existe ninguna ruta para consultar los contactos
de un prestador cualquiera, y el descubrimiento publico, el perfil publico, las
bandejas y el detalle de la solicitud siguen sin llevarlos.

## Calificaciones y reputacion

Cuando el prestador marca la solicitud como `COMPLETADA`, cada participante
puede calificar **una sola vez** a la contraparte. La calificacion cuelga de la
solicitud, igual que el hilo de mensajes: no existe una ruta para calificar a
una persona cualquiera. Solo hay consultar y crear; **no existe `PUT`, `PATCH`
ni `DELETE`**: en el MVP una calificacion no se edita ni se borra.

No hay recurso `reputacion` ni tabla que la almacene. El promedio, la cantidad y
el desglose se calculan desde las calificaciones cada vez que se piden, y se
mantienen **separados por rol**: la reputacion como cliente y como prestador son
cifras distintas de la misma persona y nunca se suman.

### Cuando se puede calificar

| Estado de la solicitud | Consultar el estado | Calificar |
|---|---|---|
| `PENDIENTE` | Los dos participantes | 409 `SOLICITUD_NO_COMPLETADA` |
| `ACEPTADA` | Los dos participantes | 409 `SOLICITUD_NO_COMPLETADA` |
| `RECHAZADA` | Los dos participantes | 409 `SOLICITUD_NO_COMPLETADA` |
| `CANCELADA` | Los dos participantes | 409 `SOLICITUD_NO_COMPLETADA` |
| `COMPLETADA` | Los dos participantes | Los dos, con cuenta `ACTIVA`, una vez cada uno |

`COMPLETADA` es definitivo, asi que una vez alcanzado la ventana no se cierra:
no hay plazo para calificar. **Calificar es opcional y no calificar no
penaliza**: no baja ningun promedio, no genera aviso y no tiene efecto alguno.

### Quien puede que

Un tercero recibe **404 `RECURSO_NO_ENCONTRADO`** en las tres rutas, igual que
en el resto de recursos propios: no puede confirmar que la solicitud exista.

En `/reputacion-del-cliente` el **cliente tambien recibe 404**. La reputacion
como cliente se publica a una sola persona, el prestador destinatario, y solo
desde una solicitud en la que participa. Los perfiles de cliente no son
publicos y este incremento no los convierte en tales.

Una cuenta `RESTRINGIDA_TEMPORAL` **consulta** su estado y la calificacion que
ya hubiera emitido, pero al calificar recibe 403 `CUENTA_RESTRINGIDA`. Una
cuenta suspendida no llega: la cadena responde 403 `ACCESO_DENEGADO`.

### Registrar una calificacion

`POST /api/solicitudes/{id}/calificacion` con el cuerpo `CalificacionAEmitir`:

| Campo | Obligatorio | Limite |
|---|---|---|
| `puntuacion` | Si | Entero de 1 a 5 |
| `comentario` | No | Texto, maximo 2000 caracteres de aplicacion |

El diccionario modela `comentario` como `TEXT` sin maximo; los 2000 caracteres
son un limite de la aplicacion, como en el resto de textos libres de Moica. El
comentario se recorta antes de validarlo y, si queda vacio, **se guarda como
`null`**: un comentario de espacios no es un comentario.

**El calificado y su rol salen siempre de la solicitud.** El cuerpo no los
lleva, y un `idCalificado`, un `idCalificador` o un `rolCalificado` enviados por
el navegador se ignoran. El cliente califica al propietario del servicio como
`PRESTADOR` y el prestador califica al cliente como `CLIENTE`; calificarse a si
misma no es una peticion formulable —una solicitud sobre un servicio propio ya
se rechaza al crearse— y `ck_calificacion_usuario_participantes` lo respalda en
la base.

La respuesta es 201 con la calificacion creada: identificador, solicitud, las
dos personas por identificador, rol, puntuacion, comentario e instante.

Rechazos al calificar:

| Situacion | HTTP | Codigo |
|---|---|---|
| Sin sesion | 401 | `NO_AUTENTICADO` |
| Cuenta suspendida | 403 | `ACCESO_DENEGADO` |
| Cuenta restringida | 403 | `CUENTA_RESTRINGIDA` |
| Solicitud inexistente o ajena | 404 | `RECURSO_NO_ENCONTRADO` |
| Solicitud que no esta `COMPLETADA` | 409 | `SOLICITUD_NO_COMPLETADA` |
| Esta persona ya califico esa solicitud | 409 | `CALIFICACION_DUPLICADA` |
| Puntuacion ausente o fuera de 1 a 5 | 400 | `VALIDACION` |
| Comentario que supera los 2000 caracteres | 400 | `VALIDACION` |

La unicidad vive tambien en PostgreSQL, en
`uq_calificacion_usuario_solicitud_calificador`. La comprobacion previa cubre el
caso normal; la restriccion decide la carrera entre dos envios simultaneos, de
modo que el perdedor recibe 409 y no 500. Una solicitud admite como maximo dos
calificaciones, una de cada participante, y
`uq_calificacion_usuario_solicitud_calificado` lo respalda.

### Consultar el estado

`GET /api/solicitudes/{id}/calificacion` responde en cualquier estado de la
solicitud, para que la interfaz no tenga que deducir la regla:

| Campo | Que dice |
|---|---|
| `solicitudCompletada` | Si la solicitud llego a `COMPLETADA` |
| `idCalificado`, `nombreCalificado` | A quien califica esta sesion |
| `rolCalificado` | `CLIENTE` o `PRESTADOR`, derivado de la solicitud |
| `puedeCalificar` | Solicitud completada, sin calificacion previa y cuenta `ACTIVA` |
| `calificacionEmitida` | Lo que esta sesion ya califico, o `null` |

`nombreCalificado` es el mismo nombre que ya viaja en el detalle de la
solicitud: `nombrePublico` del perfil para el prestador y `nombreCompleto` para
el cliente. No se publican correos, contactos ni datos administrativos.

### Reputacion

El agregado tiene la misma forma en todas las superficies:

| Campo | Que dice |
|---|---|
| `rol` | `CLIENTE` o `PRESTADOR`; el agregado nunca mezcla los dos |
| `promedio` | Media redondeada a un decimal, o **`null` si no hay ninguna calificacion** |
| `cantidad` | Numero de calificaciones recibidas en ese rol |
| `desglose` | Las cinco filas, de cinco a una estrella, con `estrellas` y `cantidad` |

**Un promedio ausente es `null`, nunca `0.0`.** Quien no ha sido calificado no
tiene una nota pesima: no tiene nota. El desglose llega siempre completo, con
cero donde no hubo votos, para que ninguna pantalla tenga que reconstruir las
filas que faltan.

La reputacion como **prestador** es publica y viaja en `reputacionPrestador` de
`GET /api/servicios`, `GET /api/servicios/{id}` y `GET /api/prestadores/{id}`.
La reputacion como **cliente** solo se obtiene en
`GET /api/solicitudes/{id}/reputacion-del-cliente`, y solo la ve el prestador
participante. Ninguna de las dos publica comentarios, identidades de quienes
calificaron ni las solicitudes que las originaron: hacia fuera solo sale el
agregado. Ver la propia calificacion emitida es la unica excepcion, y es de la
sesion sobre su propia solicitud.

## Reportes y casos de moderacion

Desde una solicitud que **llego a estar aceptada**, cualquiera de los dos
participantes puede reportar a la contraparte. El reporte abre un
`CasoModeracion` —el expediente de la investigacion— y crea su primera version
`HistorialCaso` en la misma transaccion. El caso cuelga de la solicitud, igual
que el hilo de mensajes y la calificacion: no existe una ruta para reportar a
una persona cualquiera. Solo hay consultar y crear; **no existe `PUT`, `PATCH`
ni `DELETE`**: en el MVP un reporte no se edita ni se retira.

**Reportar no sanciona.** No cambia el estado de la solicitud, no toca ninguna
cuenta, no asigna administrador, no elige ni aplica una medida, no revoca
sesiones y no depende de reincidencia, severidad ni numero de casos. Segun la
definicion 11.3, en el MVP cada medida la elige una persona administradora.

Lo que ve el reportante es **su** expediente y solo el suyo. La bandeja
administrativa, la asignacion de responsable, los cambios de estado y las
resoluciones son otra superficie, la de P10A, descrita mas abajo. El catalogo de
medidas y su aplicacion siguen siendo P10B y todavia no existen.

### Cuando se puede reportar

Lo que decide no es el estado vigente sino **si la solicitud llego alguna vez a
`ACEPTADA`**: una vez que hubo trato, el derecho a reportarlo no caduca.

| Estado de la solicitud | Consultar el estado | Reportar |
|---|---|---|
| `PENDIENTE` | Los dos participantes | 409 `SOLICITUD_NO_REPORTABLE` |
| `RECHAZADA` | Los dos participantes | 409 `SOLICITUD_NO_REPORTABLE` |
| `CANCELADA` desde `PENDIENTE` | Los dos participantes | 409 `SOLICITUD_NO_REPORTABLE` |
| `ACEPTADA` | Los dos participantes | Los dos, una vez cada uno |
| `COMPLETADA` | Los dos participantes | Los dos, una vez cada uno |
| `CANCELADA` tras `ACEPTADA` | Los dos participantes | Los dos, una vez cada uno |

Las dos `CANCELADA` se distinguen por el historial de transiciones, igual que
hace el chat para decidir si el hilo existe. No hay plazo: `COMPLETADA` y
`CANCELADA` son definitivos y la ventana no se cierra.

### Quien puede que

Un tercero recibe **404 `RECURSO_NO_ENCONTRADO`** en las dos rutas, igual que
en el resto de recursos propios: no puede confirmar que la solicitud exista.

Una cuenta `RESTRINGIDA_TEMPORAL` **conserva el reporte y su consulta**. Es lo
contrario que calificar: reportar es la via por la que alguien pide ayuda, y
quitarsela justo a quien ya arrastra una restriccion la dejaria sin recurso
frente a la contraparte. Una cuenta suspendida no llega: la cadena responde
403 `ACCESO_DENEGADO`, porque toda ruta de negocio exige sesion plena.

**Cada participante ve solo el caso que el mismo presento.** El que la
contraparte haya podido abrir sobre la misma solicitud no aparece por ningun
lado, ni siquiera como indicio de que existe.

### Abrir un caso

`POST /api/solicitudes/{id}/caso-moderacion` con el cuerpo `ReporteAPresentar`:

| Campo | Obligatorio | Limite |
|---|---|---|
| `motivo` | Si | Texto, maximo 120 caracteres |
| `descripcion` | Si | Texto, maximo 3000 caracteres de aplicacion |

Los 120 caracteres del motivo son el ancho de la columna `varchar(120)` del
diccionario. El diccionario modela `descripcion` como `TEXT` sin maximo; los
3000 caracteres son un limite de la aplicacion, el mismo que la descripcion de
una solicitud, que es el otro texto largo obligatorio de Moica. Los dos textos
se recortan antes de validarlos, asi que uno formado solo por espacios se
rechaza como vacio en lugar de guardarse en blanco.

**El reportado sale siempre de la solicitud.** El cuerpo no lo lleva, y un
`idReportado`, un `idReportante` o un `estadoActual` enviados por el navegador
se ignoran. Quien reporta es la sesion y el reportado es la contraparte;
reportarse a si misma no es una peticion formulable —una solicitud sobre un
servicio propio ya se rechaza al crearse— y `ck_caso_moderacion_participantes`
lo respalda en la base.

La respuesta es 201 con el caso abierto y solo con lo que el reportante
necesita para reconocer su expediente:

| Campo | Que dice |
|---|---|
| `idCasoModeracion` | Identificador del expediente |
| `idSolicitudServicio` | Solicitud que relaciona a las dos personas |
| `idReportado`, `nombreReportado` | A quien se reporto |
| `motivo`, `descripcion` | Lo que la persona escribio |
| `estadoActual` | Etapa vigente; recien abierto es siempre `ABIERTO` |
| `fechaApertura` | Instante en que se abrio |

**No viaja nada administrativo:** ni administrador responsable, ni medida
vinculada, ni resultado, ni resolucion, ni fechas de cierre o de fin de medida.
Son la decision de Moica sobre una persona, no el acuse del reporte.

Rechazos al reportar:

| Situacion | HTTP | Codigo |
|---|---|---|
| Sin sesion | 401 | `NO_AUTENTICADO` |
| Cuenta suspendida | 403 | `ACCESO_DENEGADO` |
| Solicitud inexistente o ajena | 404 | `RECURSO_NO_ENCONTRADO` |
| Solicitud que nunca llego a `ACEPTADA` | 409 | `SOLICITUD_NO_REPORTABLE` |
| Esta persona ya reporto esa solicitud | 409 | `REPORTE_DUPLICADO` |
| Motivo o descripcion ausentes o en blanco | 400 | `VALIDACION` |
| Motivo de mas de 120 o descripcion de mas de 3000 caracteres | 400 | `VALIDACION` |

La unicidad vive tambien en PostgreSQL, en
`uq_caso_moderacion_solicitud_reportante`. La comprobacion previa cubre el caso
normal; la restriccion decide la carrera entre dos envios simultaneos, de modo
que el perdedor recibe 409 y no 500. Una solicitud admite como maximo **dos**
casos: uno por cada participante.

### La primera version del historial

El caso y su version inicial **se confirman o se revierten juntos**: un
expediente sin la fotografia con la que nace no seria auditable. La version
llega con estos valores, y ninguno lo elige el navegador:

| Campo | Valor en la apertura |
|---|---|
| `numeroVersion` | `1` |
| `tipoActor` | `USUARIO` |
| `tipoEvento` | `CASO_ABIERTO` |
| `estadoCaso` | `ABIERTO` |
| `idActor` | El reportante |
| `idUsuarioAfectado` | El reportado |
| `estadoCuenta` | El estado **real y vigente** de la cuenta reportada |
| `esVersionActual` | `true` |
| `fechaFinVigencia` | `null` |
| `detalleCambio` | Texto no vacio que explica la apertura |
| Responsable, medida, resultado, resolucion y fecha de fin de medida | `null` |

`fechaApertura`, `fechaActualizacion` y `fechaInicioVigencia` salen del mismo
reloj de la operacion, de modo que el historial no empiece antes ni despues de
existir el expediente que describe. El historial no se publica al reportante:
solo lo lee el area administrativa, dentro del expediente del caso.

### Consultar el caso propio

`GET /api/solicitudes/{id}/caso-moderacion` responde en cualquier estado de la
solicitud, para que la interfaz no tenga que deducir la regla:

| Campo | Que dice |
|---|---|
| `solicitudReportable` | Si la solicitud llego alguna vez a `ACEPTADA` |
| `idReportado`, `nombreReportado` | A quien puede reportar esta sesion |
| `puedeReportar` | Solicitud reportable y sin caso propio todavia |
| `casoAbierto` | El caso que **esta sesion** presento, o `null` |

`nombreReportado` es el mismo nombre que ya viaja en el detalle de la
solicitud: `nombrePublico` del perfil para el prestador y `nombreCompleto` para
el cliente. No se publican correos, contactos ni datos administrativos.

## Revision administrativa de casos

Todo lo que sigue cuelga de `/api/admin/casos` y hereda las dos condiciones del
area administrativa: **rol administrativo y segundo factor verificado en esa
misma sesion**. Las impone la cadena de seguridad y el servicio las repite como
ultima red, tambien en las lecturas: una bandeja de casos son datos sobre
personas reportadas, no informacion publica.

Sin sesion la respuesta es 401. Con sesion pero sin rol, o con rol y sin el
segundo factor verificado en esa sesion, es 403 `ACCESO_DENEGADO`. Un caso que
no existe es 404 `CASO_NO_ENCONTRADO`.

**Resolver no sanciona.** Cerrar un caso como `PROCEDENTE` declara que amerita
una decision administrativa; no elige medida, no cambia el `EstadoCuenta` de
nadie y no revoca ninguna sesion. Elegir y aplicar la medida es P10B, y segun la
definicion 11.3 siempre lo hace una persona.

### Quien puede que

| Accion | Quien | Regla adicional |
|---|---|---|
| Consultar la bandeja y un expediente | Cualquier administrador con TOTP | Ninguna: revisar exige poder leer lo que aun no se tiene asignado |
| Consultar los mensajes del caso | Cualquier administrador con TOTP | Solo desde el caso; no existe ruta administrativa por solicitud |
| Asignar y reasignar | Cualquier administrador con TOTP | Repartir trabajo es coordinacion; queda en el historial |
| Iniciar la revision y cerrar | **Solo el responsable asignado** | 409 `CASO_SIN_RESPONSABLE` si no hay ninguno; 403 `CASO_DE_OTRO_ADMINISTRADOR` si lo lleva otra persona |

### Transiciones

Las que P10A admite son exactamente estas:

| Estado actual | Accion | Estado resultante |
|---|---|---|
| `ABIERTO` | `POST /{id}/revision` | `EN_REVISION` |
| `REABIERTO` | `POST /{id}/revision` | `EN_REVISION` |
| `EN_REVISION` | `POST /{id}/cierre` | `CERRADO` |

Cualquier otra responde **409 `TRANSICION_NO_PERMITIDA`** y el mensaje nombra el
estado real, que es lo que quien revisa necesita para entender que paso mientras
tenia la pantalla abierta. `CERRADO` a `REABIERTO` nace de aceptar una apelacion
y es P10B. Un caso cerrado tampoco se reasigna: su resolucion dejaria de decir
quien la firmo.

Cada accion es un recurso propio en lugar de un campo de estado que se
sobrescribe, igual que en la revision de verificaciones. **No existe `PUT`,
`PATCH` ni `DELETE`**: un caso es la evidencia de una investigacion y solo avanza
por sus transiciones.

### La bandeja

`GET /api/admin/casos` devuelve los casos del mas antiguo al mas reciente, que
es el orden en que conviene atenderlos. Sin parametros muestra lo que espera
decision: `ABIERTO`, `EN_REVISION` y `REABIERTO`. Con `estado` se piden otros
—`CERRADO`, para consultar una decision anterior— y con `mios=true` se acota a
los del responsable que consulta.

Cada fila lleva `idCasoModeracion`, `idSolicitudServicio`, quien reporto y a
quien con sus nombres, `motivo`, `estadoActual`, `resultadoActual`, el
responsable con su nombre y las fechas de apertura y de ultima actualizacion. La
descripcion del reporte y el historial no viajan aqui: son del expediente.

### El expediente

`GET /api/admin/casos/{id}` reune en una respuesta todo lo que hoy existe
vinculado al caso:

| Campo | Que lleva |
|---|---|
| `caso` | La misma fila de la bandeja |
| `descripcion` | Lo que escribio quien reporto |
| `resolucionActual` | La decision vigente, o `null` |
| `solicitud` | El detalle de la solicitud reportada, con su historial de transiciones |
| `imagenesDelServicio` | Las imagenes del servicio contratado |
| `historial` | Las versiones SCD2 del caso, de la mas antigua a la mas reciente |
| `puedeResolver` | Si la sesion es la responsable |

`imagenesDelServicio` es la unica evidencia material que Moica ya guarda del
trato; **no existe ninguna forma de adjuntar algo nuevo a un caso**. Se devuelven
aunque el servicio ya no este activo, porque el expediente describe lo que hubo.

No viajan correos, contactos ni documentos de verificacion, que tienen su propia
superficie autorizada. Los nombres son los mismos que ya publica el detalle de
la solicitud.

### Los mensajes del caso

`GET /api/admin/casos/{id}/mensajes` devuelve el hilo de la solicitud reportada,
en orden cronologico y con la misma forma que ven los participantes.

**Cuelga del caso y no de la solicitud, a proposito.** Sin un expediente que lo
justifique no hay forma de leer una conversacion privada desde el area
administrativa, ni siquiera conociendo el identificador de la solicitud. Es la
lectura acotada al contexto que exige la matriz de permisos del plan.

Solo se lee: no existe ninguna ruta para escribir en el hilo desde `/api/admin`.

### Asignar y reasignar

`POST /api/admin/casos/{id}/asignacion` con `{ "idAdministrador": 9 }`. La misma
ruta cubre la primera asignacion y un traspaso posterior; lo que cambia es el
detalle que queda en el historial.

Si esa cuenta no tiene rol administrativo la respuesta es **400
`ADMINISTRADOR_NO_VALIDO`**. Reasignar a quien ya lo tiene responde 200 y no
crea version: una fotografia identica a la vigente solo ensuciaria el historial.

Asignar no cambia el estado. Un caso puede tener responsable y seguir `ABIERTO`.

### Cerrar el caso

`POST /api/admin/casos/{id}/cierre` con `{ "resultado": "PROCEDENTE",
"resolucion": "..." }`. Los dos campos viajan juntos porque el cierre es un
bloque: `ck_caso_moderacion_cierre` exige resultado, resolucion y fecha de cierre
a la vez, y una decision sin explicacion no seria auditable meses despues.

`resultado` es `PROCEDENTE` o `DESESTIMADO`; no hay un tercer valor. La
resolucion se recorta antes de validarla —igual que el reporte—, asi que un texto
de solo espacios se rechaza con 400 `VALIDACION`, y no puede pasar de 3000
caracteres.

### Como versiona cada cambio

Toda mutacion administrativa hace lo mismo, **en una sola transaccion**: bloquea
la fila del caso, comprueba la transicion, la aplica, cierra la version vigente y
crea la siguiente. Si algo falla no queda un caso mutado sin historial ni dos
versiones diciendo ser la actual.

| Accion | `tipoEvento` de la version nueva |
|---|---|
| Asignar o reasignar | `RESPONSABLE_ASIGNADO` |
| Iniciar la revision | `ESTADO_CASO_CAMBIADO` |
| Cerrar | `RESOLUCION_REGISTRADA` |

La version nueva retrata el caso **ya mutado**: responsable, estado, resultado y
resolucion salen de la fila vigente, `tipoActor` es `ADMINISTRADOR` e `idActor`
es quien decidio. `numeroVersion` es el de la anterior mas uno.

`estadoCuenta` es el estado real y vigente de la cuenta reportada en ese
instante. P10A nunca lo cambia: se copia porque el historial retrata tambien que
acceso tenia la persona cuando se tomo cada decision.

El fin de la version anterior es el mismo instante en que empieza la nueva. Como
el intervalo es semiabierto `[inicio, fin)`, los dos periodos se tocan sin
superponerse: `ex_historial_caso_vigencia` lo comprueba y
`uq_historial_caso_version_actual` garantiza que solo quede una vigente.

El bloqueo es lo que ordena a dos administradores simultaneos. Quien llega
segundo lee el estado que dejo el primero, de modo que un cierre repetido sale
como 409 y no como una segunda resolucion, y una reasignacion cruzada con un
cierre deja siempre uno de los dos desenlaces coherentes, nunca una decision
firmada por quien ya no llevaba el caso.

### Directorio de administradores

`GET /api/admin/administradores` devuelve `idAdministrador` y `nombreCompleto` de
las cuentas con rol, ordenadas por nombre. Lo consume la reasignacion. No lleva
correo, fecha de asignacion ni estado de cuenta: es un desplegable para elegir a
quien pasar un expediente, no un directorio de cuentas.

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

Codigos que devuelve hoy la API: `VALIDACION`, `SOLICITUD_INVALIDA`, `CORREO_YA_REGISTRADO`, `CREDENCIALES_INVALIDAS`, `CUENTA_SUSPENDIDA`, `CUENTA_RESTRINGIDA`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, `CODIGO_INVALIDO`, `SEGUNDO_FACTOR_NO_ACTIVO`, `SEGUNDO_FACTOR_YA_ACTIVO`, `SEGUNDO_FACTOR_SIN_ACTIVACION_PENDIENTE`, `SEGUNDO_FACTOR_OBLIGATORIO`, `PERFIL_YA_EXISTE`, `PERFIL_NO_ENCONTRADO`, `MUNICIPIO_NO_DISPONIBLE`, `SUBCATEGORIA_NO_DISPONIBLE`, `ORDEN_INVALIDO`, `IMAGEN_NO_ADMITIDA`, `IMAGEN_DEMASIADO_GRANDE`, `DOCUMENTO_NO_ADMITIDO`, `DOCUMENTO_DEMASIADO_GRANDE`, `DOCUMENTO_NO_ENCONTRADO`, `EXPEDIENTE_INCOMPLETO`, `SOLICITUD_ABIERTA_DUPLICADA`, `SOLICITUD_NO_ENCONTRADA`, `SOLICITUD_YA_TOMADA`, `NIVEL_YA_VIGENTE`, `VERIFICACION_BASICA_REQUERIDA`, `TRANSICION_NO_PERMITIDA`, `REVISION_DE_OTRO_ADMINISTRADOR`, `SERVICIO_PROPIO`, `SERVICIO_INACTIVO`, `PRESTADOR_NO_DISPONIBLE`, `MOTIVO_OBLIGATORIO`, `CHAT_NO_HABILITADO`, `CHAT_SOLO_LECTURA`, `CONTACTOS_NO_REVELADOS`, `SOLICITUD_NO_COMPLETADA`, `CALIFICACION_DUPLICADA`, `SOLICITUD_NO_REPORTABLE`, `REPORTE_DUPLICADO`, `CASO_NO_ENCONTRADO`, `CASO_SIN_RESPONSABLE`, `CASO_DE_OTRO_ADMINISTRADOR`, `ADMINISTRADOR_NO_VALIDO`, `ALMACENAMIENTO_NO_DISPONIBLE`, `RECURSO_NO_ENCONTRADO`, `METODO_NO_PERMITIDO`, `CONTENIDO_DEMASIADO_GRANDE`, `TIPO_DE_CONTENIDO_NO_ADMITIDO` y `ERROR_INTERNO`. `SUBCATEGORIA_NO_DISPONIBLE` responde 400; `SOLICITUD_NO_COMPLETADA` y `CALIFICACION_DUPLICADA` responden 409, como ya fijan «Descubrimiento publico» y «Registrar una calificacion». Los de la revision administrativa: `CASO_NO_ENCONTRADO` 404, `ADMINISTRADOR_NO_VALIDO` 400, `CASO_SIN_RESPONSABLE` 409 y `CASO_DE_OTRO_ADMINISTRADOR` 403. Ninguna respuesta de error lleva trazas, SQL, secretos TOTP, hashes, claves de almacenamiento, URL prefirmadas ni valores internos.
