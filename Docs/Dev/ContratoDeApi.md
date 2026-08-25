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

La regla es una sola: **401 significa que ya no hay sesion y 403 que la hay pero no alcanza**. Por
eso una contrasena o un codigo equivocados no devuelven 401 aunque sean credenciales: la sesion
sigue viva y responder 401 haria creer a la interfaz que acaba de morir.

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

Codigos que devuelve hoy la API: `VALIDACION`, `SOLICITUD_INVALIDA`, `CORREO_YA_REGISTRADO`, `CREDENCIALES_INVALIDAS`, `CUENTA_SUSPENDIDA`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, `CODIGO_INVALIDO`, `SEGUNDO_FACTOR_NO_ACTIVO`, `SEGUNDO_FACTOR_YA_ACTIVO`, `SEGUNDO_FACTOR_SIN_ACTIVACION_PENDIENTE`, `SEGUNDO_FACTOR_OBLIGATORIO`, `RECURSO_NO_ENCONTRADO`, `METODO_NO_PERMITIDO`, `TIPO_DE_CONTENIDO_NO_ADMITIDO` y `ERROR_INTERNO`. Ninguna respuesta de error lleva trazas, SQL, secretos TOTP, hashes ni valores internos.
