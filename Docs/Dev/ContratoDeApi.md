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
| `GET /api/catalogos/departamentos` | Departamentos habilitados con sus municipios | Sesion plena |
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
| La cuenta esta restringida y pide modificar su perfil o su portafolio | 403 | `CUENTA_RESTRINGIDA` |

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
- `nivelVerificacion` es de **solo lectura**: es una proyeccion que actualizara
  el flujo de verificacion documental (P4V). Ningun DTO de P4 lo acepta, asi que
  enviarlo no tiene efecto.

Mientras el perfil este `SIN_VERIFICAR` **no aparece en ninguna superficie
publica**. P4 no crea todavia endpoints publicos de descubrimiento: eso llega
con P5.

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
ningun registro. Ver [Almacenamiento.md](Almacenamiento.md).

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

Codigos que devuelve hoy la API: `VALIDACION`, `SOLICITUD_INVALIDA`, `CORREO_YA_REGISTRADO`, `CREDENCIALES_INVALIDAS`, `CUENTA_SUSPENDIDA`, `CUENTA_RESTRINGIDA`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, `CODIGO_INVALIDO`, `SEGUNDO_FACTOR_NO_ACTIVO`, `SEGUNDO_FACTOR_YA_ACTIVO`, `SEGUNDO_FACTOR_SIN_ACTIVACION_PENDIENTE`, `SEGUNDO_FACTOR_OBLIGATORIO`, `PERFIL_YA_EXISTE`, `PERFIL_NO_ENCONTRADO`, `MUNICIPIO_NO_DISPONIBLE`, `ORDEN_INVALIDO`, `IMAGEN_NO_ADMITIDA`, `IMAGEN_DEMASIADO_GRANDE`, `ALMACENAMIENTO_NO_DISPONIBLE`, `RECURSO_NO_ENCONTRADO`, `METODO_NO_PERMITIDO`, `CONTENIDO_DEMASIADO_GRANDE`, `TIPO_DE_CONTENIDO_NO_ADMITIDO` y `ERROR_INTERNO`. Ninguna respuesta de error lleva trazas, SQL, secretos TOTP, hashes, claves de almacenamiento ni valores internos.
