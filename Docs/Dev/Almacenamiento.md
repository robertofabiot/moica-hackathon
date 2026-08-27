# Almacenamiento de archivos

Decisión de proveedor, arquitectura, configuración del bucket y procedimiento de
comprobación. Complementa `Docs/Core/prompt.md` §5, que fija el contrato
—«servicio de almacenamiento configurable», clave opaca, nada de binarios en
PostgreSQL— sin nombrar un proveedor concreto.

## Decisión

**Cloudflare R2 queda adoptado como proveedor de almacenamiento de objetos de
Moica.** Se accede mediante su compatibilidad con S3, con **AWS SDK for Java
v2** (`software.amazon.awssdk:s3`), que es lo que documenta Cloudflare.

Motivos, en una línea cada uno: es compatible con S3, así que el código no queda
atado a un proveedor; no cobra por transferencia de salida, que es justo lo que
hace una galería de imágenes; y su plan gratuito cubre de sobra un MVP.

La decisión afecta al **cómo**, no al **qué**: las reglas de privacidad,
formatos y validación las siguen fijando `prompt.md` y el diccionario de datos.

## Dos superficies separadas

| Superficie | Qué guarda | Acceso | Incremento |
|---|---|---|---|
| **Pública** | Imagen de perfil e imágenes de los trabajos del portafolio | Lectura anónima por URL; escritura solo desde el backend | P4 (implementada) |
| **Privada** | Documentos del expediente de verificación | Sin lectura anónima; entrega por URL prefirmada de vida corta | P4V (implementada) |

**No se mezclan en el mismo bucket.** Son dos buckets distintos, con dos
credenciales distintas y dos tokens distintos: un fallo de configuración en el
bucket público no puede exponer un documento de identidad. Tampoco comparten
código: cada superficie tiene su interfaz, su adaptador y sus propiedades, y
ninguna de las dos puede escribir por error en la otra.

En PostgreSQL, cada superficie guarda algo distinto, según el diccionario de
datos:

- Pública: **la URL** (`perfil_prestador.url_imagen_perfil`,
  `imagen_trabajo_portafolio.url_imagen`).
- Privada: **una clave opaca** y metadatos
  (`documento_verificacion_prestador.clave_almacenamiento`), nunca una URL
  permanente.

En ninguna de las dos se guarda el binario.

## Arquitectura

```
prestador / portafolio -> AlmacenamientoDeImagenesPublicas   -> AlmacenamientoR2        -> bucket público
     (services)                   (interfaz)                        (SDK de AWS)

verificacion           -> AlmacenamientoDeDocumentosPrivados -> AlmacenamientoPrivadoR2 -> bucket privado
     (services)                   (interfaz)                        (SDK de AWS)
```

- Los servicios de perfil, portafolio y verificación **no conocen el SDK**:
  hablan con su interfaz. La pública ofrece `guardar`, `eliminar` y `claveDe`;
  la privada ofrece `guardar`, `eliminar` y `accesoTemporalDeLectura`.
- Son **dos interfaces y no una parametrizada** porque sus operaciones no
  coinciden: en la pública no existe un acceso temporal —sería un rodeo sobre
  algo que ya es público— y en la privada no existe una URL pública que guardar
  ni de la que volver a la clave.
- Cada interfaz existe porque tiene dos implementaciones reales: su adaptador de
  R2 en ejecución y un doble en memoria en las pruebas de integración.
- Un fallo del proveedor sale siempre como el mismo error uniforme
  —`503 ALMACENAMIENTO_NO_DISPONIBLE`—, sin endpoint, sin credenciales y sin
  detalle interno. El detalle se registra en el servidor.
- **Sin credenciales la aplicación arranca igual**, pero cualquier operación con
  imágenes responde ese mismo 503. Una configuración *a medias*, en cambio,
  detiene el arranque: solo puede ser un error de despliegue.

### Configuración que exige R2

`AlmacenamientoR2` construye el cliente con lo que R2 necesita y que no es lo
que trae el SDK por omisión:

| Ajuste | Valor | Por qué |
|---|---|---|
| Endpoint | `https://<ID_CUENTA>.r2.cloudflarestorage.com` | R2 no está en las regiones de AWS |
| Región | `auto` | R2 no usa regiones |
| Acceso | Estilo de ruta | R2 no admite el estilo por subdominio de bucket |
| `chunkedEncodingEnabled` | `false` | Con codificación por trozos, `putObject` falla por firma |
| Checksums | Solo cuando el servicio los exige | R2 no admite los que el SDK calcula por omisión desde su versión 2.30 |
| Credenciales | Del entorno, estáticas | Nunca del código ni de un archivo versionado |
| Tiempos de espera | 10 s conexión, 30 s por intento, 60 s total | Un proveedor lento no puede colgar una petición de Moica |

El adaptador privado añade un `S3Presigner` con exactamente el mismo endpoint,
la misma región y el mismo estilo de ruta. Firmar es un cálculo local: no habla
con R2, así que no necesita cliente HTTP ni tiempos de espera, pero sí la misma
configuración, o la URL firmada apuntaría a otro sitio.

### Ciclo de vida de un objeto

Las llamadas de red no ocurren dentro de una transacción de base de datos. El
orden es lo que mantiene la coherencia:

1. **Al subir:** primero el objeto, después la fila. Si la fila falla, el objeto
   recién subido se retira como compensación y no queda huérfano.
2. **Al sustituir:** el objeto anterior se conserva hasta que la base ya apunta
   al nuevo. Solo entonces se retira.
3. **Al eliminar:** primero la fila, después el objeto.
4. **Si falla la limpieza posterior:** la base ya es coherente, así que no se
   convierte en error para quien lo pidió. Se registra un aviso con la clave
   —`Quedó pendiente de limpieza el objeto …`— y ese objeto queda suelto en el
   bucket hasta que alguien lo retire a mano. Es una fuga de espacio, nunca de
   datos ni de consistencia.
5. **Si la URL guardada ya no pertenece a la base pública:** no se borra nada.
   Solo se retira lo que el almacén sabe nombrar, porque pedirle al proveedor
   que elimine una clave deducida de un texto arbitrario de la base de datos
   sería borrar sin validar.

### Qué pasa al cambiar la base pública

Cambiar `MOICA_R2_URL_PUBLICA_BASE` —el paso normal al pasar del subdominio
`r2.dev` a un dominio propio— no migra las filas ya guardadas: conservan la URL
con el dominio anterior.

- **Las imágenes existentes se siguen viendo** mientras el dominio viejo siga
  sirviendo el bucket. Si se retira, esas filas quedan apuntando a una dirección
  que ya no responde.
- **Sus objetos dejan de poder retirarse solos.** `claveDe` no reconoce la URL,
  así que sustituir o eliminar esa imagen limpia la fila pero deja el objeto en
  el bucket. Queda registrado el aviso «Quedó sin retirar un objeto ya
  desreferenciado…», que nombra la causa sin escribir la URL en el registro.
- **Qué hacer:** conservar el dominio anterior mientras haya filas que lo usen,
  o actualizar esas filas con una migración cuando el equipo decida cambiarlo, y
  revisar el bucket por objetos sueltos. Moica **no** lleva una lista de
  dominios históricos: sería infraestructura para un problema que el MVP todavía
  no tiene.

### El expediente privado, paso a paso

El envío de un expediente es **una sola petición** con el nivel, los archivos y
el tipo de cada uno. La solicitud y sus documentos nacen juntos o no nacen: no
existe un estado `BORRADOR` ni un momento en el que exista una solicitud sin
expediente.

1. **Primero se comprueba todo lo que no necesita red:** el nivel que se pide,
   la propiedad del perfil, que no haya ya una solicitud abierta de ese nivel,
   que el expediente traiga el respaldo exigido y que cada archivo sea admisible
   por tamaño, tipo declarado y firma real. Un expediente que no pasa esto **no
   sube ni un byte**.
2. **Después se suben todos los archivos** al bucket privado, cada uno bajo su
   clave opaca.
3. **Por último se registran** la solicitud y sus documentos en una única
   transacción de base de datos.
4. **Si falla cualquier subida o la transacción**, se retiran *todos* los
   objetos que ese intento había subido. No queda una solicitud a medias ni un
   archivo huérfano. Si además fallara la limpieza, queda un aviso en el
   registro —sin la clave, que es dato privado— y el objeto se retira a mano
   localizándolo por su prefijo y su antigüedad.

Los documentos de una solicitud **ya resuelta no se borran nunca**: junto con la
solicitud son la evidencia de qué se revisó y quién lo decidió. Rechazar o
revocar no retira nada del bucket.

### Acceso temporal a un documento

El bucket privado no tiene lectura anónima, así que no existe ninguna dirección
que un navegador pueda pedir sin permiso. Lo único que sale de él es una URL
**prefirmada** que caduca sola.

- La pide un administrador en
  `GET /api/admin/verificaciones/{id}/documentos/{idDocumento}/acceso`.
- La autorización se comprueba **en esa misma petición**: rol administrativo,
  segundo factor verificado en esa sesión y que el documento pertenezca de
  verdad a ese expediente.
- El endpoint responde **302** hacia la URL firmada en lugar de devolverla en un
  cuerpo JSON: así la dirección no pasa por el JavaScript de la aplicación, no
  entra en la caché de consultas y no queda escrita en una respuesta que alguien
  pueda copiar sin darse cuenta. La respuesta lleva `Cache-Control: no-store`.
- La URL **no se guarda en ninguna parte**: ni en PostgreSQL, ni en un registro,
  ni en el cuerpo de ninguna otra respuesta. Se firma en cada apertura.
- Dura `MOICA_DOCUMENTO_URL_TEMPORAL_DURACION`, `PT5M` por omisión. La
  aplicación no admite más de una hora: un acceso «temporal» más largo deja de
  serlo.
- **El propietario del expediente no puede abrir sus propios archivos.** Ve los
  metadatos —tipo, nombre saneado, tamaño y fecha— y nada más. El binario solo
  lo abre quien revisa.

### Claves

Opacas y no adivinables: `perfiles/<32 hex>.<ext>`, `trabajos/<32 hex>.<ext>` y
`expedientes/<32 hex>.<ext>`.

- El nombre original **no** se usa: puede llevar rutas, caracteres de control o
  datos personales.
- Los 32 dígitos hexadecimales son un UUID aleatorio: 128 bits, imposible de
  enumerar.
- La extensión sale del **formato real detectado**, no de lo que dijera el
  archivo.
- Los prefijos separan las superficies. En el expediente la clave tampoco se
  deriva del identificador del prestador: una clave calculada a partir de un
  identificador secuencial se podría enumerar.

### Validación de un documento del expediente

Las mismas tres comprobaciones que una imagen, con otra lista de formatos:

1. **Tamaño** contra `MOICA_DOCUMENTO_TAMANO_MAXIMO` (5 MB por omisión) →
   `413 DOCUMENTO_DEMASIADO_GRANDE`. Ese máximo solo puede bajarse: 5 MB es el
   tope que impone `ck_documento_verificacion_tamano` en PostgreSQL, y una
   configuración por encima admitiría archivos que la base rechazaría después.
2. **Tipo declarado** contra JPEG, PNG y **PDF** → `400 DOCUMENTO_NO_ADMITIDO`.
3. **Firma binaria real** del contenido, que debe corresponder con lo declarado
   → `400 DOCUMENTO_NO_ADMITIDO`.

Aquí sí entra PDF —un certificado o una constancia suele serlo— y no entra
WebP. Son dos dominios distintos a propósito: confundirlos dejaría un PDF
colgando de un perfil público.

El nombre original se conserva porque le sirve al prestador para reconocer qué
subió y a quien revisa para leer el expediente, pero **saneado**: sin ruta, sin
caracteres de control —que partirían una línea de registro en dos— y sin los
caracteres que Windows no admite en un nombre de archivo. La clave del objeto no
se deriva de él.

### Validación de una imagen

Todo en el backend, nunca solo en el navegador:

1. **Tamaño** contra `MOICA_IMAGEN_TAMANO_MAXIMO` (5 MB por omisión) →
   `413 IMAGEN_DEMASIADO_GRANDE`.
2. **Tipo declarado** contra JPEG, PNG y WebP → `400 IMAGEN_NO_ADMITIDA`.
3. **Firma binaria real** del contenido, que debe corresponder con lo declarado
   → `400 IMAGEN_NO_ADMITIDA`.

SVG y PDF no se admiten en esta superficie: un SVG puede ejecutar script en el
navegador de quien lo mire.

## Configurar el bucket

Se hace **una vez**, desde el panel de Cloudflare. La aplicación **no** crea
buckets ni pide permisos administrativos sobre la cuenta.

1. **Crear el bucket.** R2 → *Create bucket*. Nombre sugerido:
   `moica-publico-dev` para desarrollo y `moica-publico` para producción.
   Ubicación automática.
2. **Hacerlo público.** Bucket → *Settings* → *Public access* → *R2.dev
   subdomain* → *Allow Access*. Cloudflare entrega una dirección
   `https://pub-<hash>.r2.dev`: ese es el valor de
   `MOICA_R2_URL_PUBLICA_BASE`.
   > `r2.dev` es **solo para desarrollo**: tiene límite de peticiones y no
   > pasa por la caché de Cloudflare. En producción se conecta un dominio
   > propio (*Settings* → *Custom Domains*) y se cambia esa misma variable. El
   > código no depende de cuál sea.
3. **Crear el token de API.** R2 → *Manage API tokens* → *Create API token*:
   - Permiso: **Object Read & Write** (no *Admin*).
   - Alcance: **Apply to specific buckets only** → únicamente el bucket
     público.
   - Sin TTL o con el que fije el equipo.

   Cloudflare muestra el *Access Key ID* y el *Secret Access Key* **una sola
   vez**: van a `MOICA_R2_ACCESS_KEY_ID` y `MOICA_R2_SECRET_ACCESS_KEY` del
   `.env` local, nunca al repositorio.
4. **Copiar el ID de cuenta.** Aparece en la barra lateral de R2 y en el
   endpoint S3 que muestra el panel: es `MOICA_R2_ID_CUENTA`.

### Permisos mínimos recomendados

| Sí | No |
|---|---|
| Lectura y escritura de objetos | Permisos de administración de la cuenta |
| Solo el bucket público | Todos los buckets de la cuenta |
| — | Crear o borrar buckets |
| — | Cualquier acceso al bucket privado |

El token del bucket privado es **otro**, con su propio alcance.

## Configurar el bucket privado

También se hace **una vez**, desde el panel de Cloudflare, y con un token
distinto del público.

1. **Crear el bucket.** R2 → *Create bucket*. Nombre sugerido:
   `moica-privado-dev` para desarrollo y `moica-privado` para producción.
   Ubicación automática.
2. **No hacerlo público.** Es el paso que importa: **no** se habilita el
   subdominio `r2.dev` ni se conecta un dominio propio. El bucket no debe tener
   ninguna dirección de lectura anónima. Si alguna vez se habilitó, se retira.
3. **Crear un token de API propio.** R2 → *Manage API tokens* → *Create API
   token*:
   - Permiso: **Object Read & Write** (no *Admin*).
   - Alcance: **Apply to specific buckets only** → únicamente el bucket privado.
   - **Nunca el mismo token del bucket público**, ni uno con alcance a los dos.

   El *Access Key ID* y el *Secret Access Key* van a
   `MOICA_R2_PRIVADO_ACCESS_KEY_ID` y `MOICA_R2_PRIVADO_SECRET_ACCESS_KEY`,
   nunca al repositorio.
4. **Copiar el ID de cuenta** en `MOICA_R2_PRIVADO_ID_CUENTA`. Hoy es el mismo
   de la cuenta de Cloudflare; se declara aparte para que las dos superficies
   puedan vivir en cuentas distintas sin tocar código.

La aplicación **no crea ni administra buckets**: trabaja contra uno ya
aprovisionado.

## Variables de entorno

Documentadas sin valor en `.env.example`. Las cinco primeras van juntas: o todas
o ninguna.

| Variable | Qué es |
|---|---|
| `MOICA_R2_ID_CUENTA` | Identificador de la cuenta de Cloudflare |
| `MOICA_R2_ACCESS_KEY_ID` | Identificador del token de API |
| `MOICA_R2_SECRET_ACCESS_KEY` | Secreto del token. **Nunca se versiona** |
| `MOICA_R2_BUCKET_PUBLICO` | Nombre del bucket ya aprovisionado |
| `MOICA_R2_URL_PUBLICA_BASE` | Origen HTTPS desde el que se leen las imágenes, sin barra final |
| `MOICA_IMAGEN_TAMANO_MAXIMO` | Máximo por imagen (`5MB` por omisión) |

Las cuatro del bucket privado van igualmente juntas: o todas o ninguna.

| Variable | Qué es |
|---|---|
| `MOICA_R2_PRIVADO_ID_CUENTA` | Identificador de la cuenta de Cloudflare del bucket privado |
| `MOICA_R2_PRIVADO_ACCESS_KEY_ID` | Identificador del token de API del bucket privado |
| `MOICA_R2_PRIVADO_SECRET_ACCESS_KEY` | Secreto de ese token. **Nunca se versiona** |
| `MOICA_R2_BUCKET_PRIVADO` | Nombre del bucket privado ya aprovisionado |
| `MOICA_DOCUMENTO_TAMANO_MAXIMO` | Máximo por documento (`5MB` por omisión; no admite más) |
| `MOICA_DOCUMENTO_URL_TEMPORAL_DURACION` | Duración del acceso temporal (`PT5M` por omisión; no admite más de una hora) |

No hay variable de URL pública para el bucket privado, y esa ausencia es la
propia decisión: no debe existir ninguna dirección desde la que se lea sin
permiso.

## Comprobación manual contra R2 real

Las pruebas automáticas usan dobles en memoria, así que **esto es lo único que
demuestra que las credenciales y los buckets están bien**. Se hace con el
backend arrancado y una sesión iniciada; `$T` es el token CSRF y `galletas.txt`
el archivo de cookies, como en el `curl` del contrato de la API.

### Bucket público

```bash
# 1. Carga: sube una imagen de perfil
curl -s -b galletas.txt -X PUT http://localhost:8080/api/prestador/perfil/imagen \
  -H "X-XSRF-TOKEN: $T" -F "archivo=@foto.png;type=image/png"
# Responde el perfil con `urlImagenPerfil` apuntando a la base pública.

# 2. Lectura pública: sin cookie ni credenciales, debe responder 200
curl -s -o /dev/null -w '%{http_code}\n' "<urlImagenPerfil de la respuesta>"

# 3. Sustitución: sube otra y comprueba que la URL cambia
curl -s -b galletas.txt -X PUT http://localhost:8080/api/prestador/perfil/imagen \
  -H "X-XSRF-TOKEN: $T" -F "archivo=@otra.jpg;type=image/jpeg"
# El objeto anterior deja de existir: repetir el paso 2 con la URL vieja da 404.

# 4. Eliminación
curl -s -b galletas.txt -X DELETE http://localhost:8080/api/prestador/perfil/imagen \
  -H "X-XSRF-TOKEN: $T"
# La URL vuelve a `null` y el objeto desaparece del bucket.
```

En el panel de Cloudflare, el bucket debe quedar sin objetos huérfanos al
terminar.

### Bucket privado

Diez comprobaciones, en este orden. Las primeras necesitan una sesión de
prestador con perfil creado; de la sexta en adelante, una sesión administrativa
con el segundo factor verificado (`$A` es su token CSRF y `admin.txt` su archivo
de cookies).

**Usa archivos ficticios preparados para la prueba.** Nunca subas un documento
de identidad real ni publiques capturas que lo muestren.

```bash
# 1. El bucket no admite lectura anónima. En el panel de Cloudflare, el bucket
#    privado no debe tener subdominio r2.dev ni dominio propio. Si alguna
#    dirección respondiera, la configuración está mal.

# 2. Carga: un expediente con los tres formatos, en una sola petición
curl -s -b galletas.txt -X POST \
  http://localhost:8080/api/prestador/verificacion/solicitudes \
  -H "X-XSRF-TOKEN: $T" \
  -F "nivelSolicitado=BASICA" \
  -F "archivo=@cedula.png;type=image/png" \
  -F "archivo=@reverso.jpg;type=image/jpeg" \
  -F "archivo=@constancia.pdf;type=application/pdf" \
  -F "tipoDocumento=IDENTIDAD" \
  -F "tipoDocumento=IDENTIDAD" \
  -F "tipoDocumento=CONSTANCIA"
# Responde 201 con la solicitud y los metadatos de sus tres documentos.

# 3. PostgreSQL guarda clave y metadatos, nunca el binario ni una URL
#    SELECT clave_almacenamiento, nombre_original, tipo_mime, tamano_bytes
#    FROM documento_verificacion_prestador;

# 4. El propietario no puede descargar su documento: no existe ninguna ruta
#    suya que lo entregue, y la administrativa le responde 403.
curl -s -o /dev/null -w '%{http_code}\n' -b galletas.txt \
  "http://localhost:8080/api/admin/verificaciones/$ID/documentos/$IDDOC/acceso"

# 5. Un usuario ajeno tampoco: consultar la solicitud desde otra cuenta
#    responde 404, no 403.
curl -s -o /dev/null -w '%{http_code}\n' -b otra.txt \
  "http://localhost:8080/api/prestador/verificacion/solicitudes/$ID"

# 6. Un administrador sin el segundo factor verificado en esa sesión: 403.

# 7. Un administrador con el segundo factor verificado obtiene el acceso
curl -s -D - -o /dev/null -b admin.txt \
  "http://localhost:8080/api/admin/verificaciones/$ID/documentos/$IDDOC/acceso"
# Responde 302 y la cabecera `Location` lleva la URL prefirmada.

# 8. El enlace funciona antes de vencer
curl -s -o /dev/null -w '%{http_code}\n' "<Location del paso 7>"   # 200

# 9. Y deja de funcionar después. Con PT5M, esperar algo más de cinco minutos
curl -s -o /dev/null -w '%{http_code}\n' "<el mismo Location>"      # 403

# 10. Compensación: repetir el paso 2 con el bucket inalcanzable —por ejemplo,
#     con un MOICA_R2_BUCKET_PRIVADO que no existe— y comprobar en el panel que
#     no queda ningún objeto de ese intento y que la base no creó la solicitud.
```

## Qué se comprueba solo y qué exige credenciales

| Se comprueba en `./mvnw verify` | Exige un bucket R2 real |
|---|---|
| Validación de tamaño, tipo y firma binaria | Que las credenciales sean válidas |
| Claves opacas, con prefijo y sin colisión | Que el bucket exista y admita escritura |
| Que se persista la URL o la clave, y nunca el binario | Que la lectura anónima funcione en el público y **no** funcione en el privado |
| Qué bucket, clave y tipo recibe el cliente S3 | Que la configuración de firma de R2 sea correcta |
| Compensación al fallar una carga o la persistencia | Que el objeto desaparezca del bucket al borrarlo o al compensar |
| Que la URL prefirmada lleve la duración configurada y apunte al bucket privado | Que R2 acepte esa firma y la rechace al vencer |
| Que un fallo salga como error uniforme, sin filtrar el proveedor | |

La firma de una URL prefirmada es un cálculo local, así que `AlmacenamientoPrivadoR2Test`
la genera **de verdad** —con un `S3Presigner` real y credenciales de mentira— y
comprueba que lleva `X-Amz-Expires` con los segundos configurados y que apunta
al bucket privado por estilo de ruta. Lo que eso no demuestra es que R2 acepte
esa firma: eso solo lo dice el proveedor.

Los dobles de almacenamiento **no** demuestran la integración externa. Mientras
el entorno no tenga credenciales, lo que falte de las comprobaciones de arriba
queda pendiente y así debe declararse en el PR.

**Estado del bucket público (PR #9).** Los pasos 1 y 2 —carga y lectura
pública— se ejecutaron en la revisión contra el bucket `moica-publico-dev`,
junto con la carga de imágenes de portafolio y la persistencia de la URL en
PostgreSQL. Los pasos 3 y 4 —sustitución y eliminación— **siguen sin ejecutarse
contra R2 real**.

**Estado del bucket privado (PR #10).** **Ninguno de los diez pasos se ha
ejecutado contra R2 real.** El entorno de desarrollo no tiene definidas las
variables `MOICA_R2_PRIVADO_*`, y P4V no pide secretos por ningún canal. Lo que
sí se comprobó en local, con el bucket privado **sin configurar**, es el
comportamiento que corresponde a esa situación: enviar un expediente responde
`503 ALMACENAMIENTO_NO_DISPONIBLE`, no crea ninguna fila y no filtra proveedor,
endpoint, bucket ni clave; y abrir un documento responde el mismo 503. Todo lo
demás del flujo —cola, toma, aprobación, rechazo, revocación y niveles— se
recorrió de extremo a extremo. Aprovisionar el bucket privado con su token y
ejecutar los diez pasos es lo que falta.
