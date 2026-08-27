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
| **Privada** | Documentos del expediente de verificación | Sin lectura anónima; entrega por URL prefirmada de vida corta | P4V (pendiente) |

**No se mezclan en el mismo bucket.** Son dos buckets distintos, con dos
credenciales distintas: un fallo de configuración en el bucket público no puede
exponer un documento de identidad. P4 implementa **solo** la superficie
pública; la privada llegará con su propio incremento, su propia variable de
entorno y sus URL prefirmadas.

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
prestador / portafolio  ->  AlmacenamientoDeImagenesPublicas  ->  AlmacenamientoR2  ->  R2
     (services)                    (interfaz)                      (SDK de AWS)
```

- Los servicios de perfil y portafolio **no conocen el SDK**: hablan con la
  interfaz `AlmacenamientoDeImagenesPublicas` (`guardar`, `eliminar`,
  `claveDe`).
- La interfaz existe porque tiene dos implementaciones reales: `AlmacenamientoR2`
  en ejecución y un doble en memoria en las pruebas de integración.
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

### Claves

Opacas y no adivinables: `perfiles/<32 hex>.<ext>` y `trabajos/<32 hex>.<ext>`.

- El nombre original **no** se usa: puede llevar rutas, caracteres de control o
  datos personales.
- Los 32 dígitos hexadecimales son un UUID aleatorio: 128 bits, imposible de
  enumerar.
- La extensión sale del **formato real detectado**, no de lo que dijera el
  archivo.
- Los prefijos separan las superficies dentro del bucket público.

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
| — | Cualquier acceso al futuro bucket privado de P4V |

El token del bucket privado de P4V será **otro**, con su propio alcance.

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

## Comprobación manual contra R2 real

Las pruebas automáticas usan un doble en memoria, así que **esto es lo único que
demuestra que las credenciales y el bucket están bien**. Se hace con el backend
arrancado y una sesión iniciada; `$T` es el token CSRF y `galletas.txt` el
archivo de cookies, como en el `curl` del contrato de la API.

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

## Qué se comprueba solo y qué exige credenciales

| Se comprueba en `./mvnw verify` | Exige un bucket R2 real |
|---|---|
| Validación de tamaño, tipo y firma binaria | Que las credenciales sean válidas |
| Claves opacas, con prefijo y sin colisión | Que el bucket exista y admita escritura |
| Que se persista la URL y nunca el binario | Que la lectura anónima funcione de verdad |
| Qué bucket, clave y tipo recibe el cliente S3 | Que la configuración de firma de R2 sea correcta |
| Compensación al fallar la persistencia | Que el objeto desaparezca del bucket al borrarlo |
| Que un fallo salga como error uniforme, sin filtrar el proveedor | |

El doble de almacenamiento **no** demuestra la integración externa. Mientras el
entorno no tenga credenciales, lo que falte de la comprobación de arriba queda
pendiente y así debe declararse en el PR.

**Estado en el PR #9.** Los pasos 1 y 2 —carga y lectura pública— se ejecutaron
en la revisión contra el bucket `moica-publico-dev`, junto con la carga de
imágenes de portafolio y la persistencia de la URL en PostgreSQL. Los pasos 3 y
4 —sustitución y eliminación— **siguen sin ejecutarse contra R2 real**, así que
que el objeto anterior desaparezca del bucket está probado contra el doble y no
contra el proveedor. Ejecutar esos dos pasos en un entorno con credenciales es
todo lo que falta.
