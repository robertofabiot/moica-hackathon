# Contrato de la API

Detalle del modelo de sesion, la proteccion CSRF, la politica de contraseña y la forma de los errores. La tabla de endpoints esta en el [README](../../README.md#api-de-acceso).

## Como se autentica una peticion

1. Al iniciar sesion, Moica crea una fila en `sesion` con un identificador aleatorio y una fecha de expiracion (siete dias por omision, configurable con `MOICA_SESION_DURACION`).
2. Ese identificador viaja como `jti` dentro de un JWT firmado, y el JWT viaja en la cookie `moica_sesion`, que es `HttpOnly`, `SameSite=Lax` y `Secure` en produccion. **El token no se guarda en `localStorage` ni en `sessionStorage`.**
3. En cada peticion autenticada, el backend lee el `jti` y comprueba la fila: debe existir, no haber expirado y no haber sido revocada. Si falla cualquiera de las tres, la respuesta es 401.

Por eso cerrar sesion tiene efecto inmediato aunque el JWT conserve una expiracion futura: la fuente de verdad es la fila, no el token.

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

Codigos que devuelve hoy la API: `VALIDACION`, `SOLICITUD_INVALIDA`, `CORREO_YA_REGISTRADO`, `CREDENCIALES_INVALIDAS`, `NO_AUTENTICADO`, `ACCESO_DENEGADO`, `RECURSO_NO_ENCONTRADO`, `METODO_NO_PERMITIDO`, `TIPO_DE_CONTENIDO_NO_ADMITIDO` y `ERROR_INTERNO`. Ninguna respuesta de error lleva trazas, SQL ni valores internos.
