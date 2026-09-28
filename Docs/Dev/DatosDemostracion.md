# Datos de demostración del marketplace

El bootstrap explícito `com.moica.demo.BootstrapDeDemo` prepara contenido ficticio
para `/explorar`, sus filtros, los detalles y los perfiles públicos. No es una
funcionalidad nueva del dominio. No añade endpoints, dependencias ni cambios de
esquema. V90 permanece exclusivamente como taxonomía; no existe V91 para estos datos.

## Activación y alcance

`MOICA_SEED_DEMO_ENABLED=false` es el valor por omisión. Sin activación no se
llama a la persistencia desde el arranque y el propio servicio también comprueba
la propiedad antes de escribir. Con `true`, un `ApplicationRunner` invoca un
servicio transaccional después de Flyway, siguiendo el bootstrap administrativo
existente. Solo el operador del backend configura este proceso.

El paquete `demo` está aislado: su repositorio SQL usa `JdbcTemplate`, ya incluido
en el stack, y no accede a repositorios de otras capacidades. No modifica las
reglas ni las rutas de registro, publicación o verificación de usuarios reales.

Los perfiles declaran explícitamente que son ficticios. Proyectar directamente
su verificación es una excepción controlada para la demo: **no hubo revisión
documental de estas personas ficticias**. No se crean documentos de identidad,
expedientes, solicitudes de verificación, administradores, contactos ni sesiones.
Las cuentas no tienen una contraseña utilizable conocida: se generan 256 bits
con `SecureRandom`, se codifican con el `PasswordEncoder` BCrypt ya usado por
MOICA y se descarta el original. El hash no se regenera al sincronizar.

## Contenido

Todos los usuarios quedan `ACTIVA`, los perfiles `DISPONIBLE` y los servicios
`ACTIVO`. Se resuelven el departamento habilitado Managua, sus municipios y cada
par categoría/subcategoría por nombre; nunca se asumen identificadores numéricos.

| Prestador ficticio | Municipio | Insignia | Servicios y precio de referencia |
|---|---|---|---|
| Julio Mendoza · Soluciones del Hogar | Managua | PROFESIONAL_VERIFICADO | Plomería: fugas y grifería, C$450; electricidad: luminarias y tomacorrientes, C$650 |
| Maderas del Patio | Ciudad Sandino | VERIFICADO_BASICO | Carpintería: muebles a medida y reparaciones, A convenir |
| Camila Ríos · Belleza a Domicilio | Managua | VERIFICADO_BASICO | Maquillaje social, C$900; manicura semipermanente, C$400 |
| Barbería La Esquina | Tipitapa | VERIFICADO_BASICO | Barbería/peluquería: corte y barba, C$250 |
| Punto Técnico Managua | Managua | PROFESIONAL_VERIFICADO | Reparación de computadoras: diagnóstico y mantenimiento, C$800; soporte técnico: Wi-Fi y equipos de oficina, A convenir |
| Lucía Vega · Diseño Local | Ticuantepe | VERIFICADO_BASICO | Diseño gráfico: menús y redes sociales, A convenir |

Son seis prestadores y exactamente nueve publicaciones, una por cada subcategoría
de V90. Los tres precios «A convenir» se guardan como `precio_referencia = NULL`.
Coberturas y descripciones detallan sectores de Managua, citas y materiales.

## Idempotencia y datos parciales

- Usuario: correo reservado `moica-demo-v1-<clave>@demo.moica.invalid`, con claves
  `julio`, `maderas`, `camila`, `barberia`, `tecnica` y `lucia`. Ese espacio de
  nombres identifica exclusivamente esta semilla. No renombrar estos correos. El
  registro público rechaza cualquier correo `.invalid`, así que nadie puede ocupar
  esas direcciones desde la aplicación. Una base anterior a esa regla podría
  tenerlas, y por eso el cargador tampoco adopta una cuenta reservada que haya
  abierto sesiones o configurado un segundo factor: las cuentas sembradas tienen
  una contraseña aleatoria que nadie conoce. Ese caso aborta la carga como
  cualquier otro conflicto de propiedad.
- Perfil: la clave compartida del usuario encontrado.
- Servicio: cuenta reservada y subcategoría resuelta. Cada cuenta demo publica
  uno por subcategoría. El título y la descripción sí pueden sincronizarse.
- Imagen: servicio encontrado y posición de la lista (`0`, `1`, `2`). Una URL
  corregida en la misma posición conserva el identificador de la asociación.

La ejecución toma un bloqueo transaccional PostgreSQL reservado a
`moica:seed:demo:v1`: dos arranques concurrentes se serializan. El conjunto se
confirma completo o revierte completo. La siguiente ejecución reutiliza IDs,
hashes y fechas; los `UPDATE` solo cambian filas cuyos campos semilla difieren.
No hay `TRUNCATE`, reinicios de secuencias, borrados de registros ni de objetos.

Un usuario sin perfil, un perfil sin publicaciones o un servicio sin imágenes se
completan. Los estados, cobertura, textos y precios de los datos propios se
sincronizan. Un correo reservado con nombre de cuenta inesperado o varias filas
para la misma clave natural provocan un error y rollback; no se intenta adoptar
cuentas dudosas ni borrar duplicados. Los correos y la clasificación forman la
identidad de la semilla: no editarlos manualmente para convertirla en datos reales.
Usuarios ajenos a esos seis correos, incluso con nombres o servicios iguales,
quedan intactos.

La sincronización es aditiva y por posición: cada clave de la lista crea o
reescribe la imagen de su posición y las posiciones que la lista no alcanza se
conservan. Por eso acortar una lista no retira las posiciones sobrantes y puede
repetir un objeto: pasar de `A,B` a `B` deja `B` en las posiciones `0` y `1`.
Una lista vacía no toca ninguna posición, con una excepción: en computadoras,
con la base R2 comprobada, vacío equivale al mapeo recuperado y reescribe las
posiciones `0` y `1` con las dos fotografías de la sección siguiente, aunque
antes se hubieran sobrescrito con otras claves.

El resumen `imagenesExistentes` cuenta las posiciones del mapeo procesado y
`serviciosSinMapeo` las entradas sin mapeo efectivo; no representan un
inventario total de imágenes conservadas.

## Imágenes públicas recuperadas

Inventario realizado durante esta implementación, el 7 de septiembre de 2026:
la conexión de solo lectura a la base local configurada no estuvo disponible
(`SQLSTATE 08001`). No se abrió PostgreSQL de Railway a Internet. Sí fue posible
listar `servicios/` del bucket público con las credenciales locales ya autorizadas.
Se encontraron seis objetos y se inspeccionaron anónimamente en el navegador.

Base pública comprobada:
<https://pub-a1129fb1410e4c84b7ac69e79d442ace.r2.dev>.

| Object key público existente | Contenido observado | Uso |
|---|---|---|
| `servicios/1ae971b72b92480d9a84822b145786cb.jpg` | Mantenimiento térmico de procesador de laptop | Computadoras, orden 0 |
| `servicios/addb04d5b1044a3fbd59eb45e15768ce.jpg` | Pasta térmica en procesador de escritorio | Computadoras, orden 1 |
| `servicios/402eeedbe52b46399c1167a93d265a8e.jpg` | Misma fotografía de laptop a simple vista | No repetirla |
| `servicios/50eb3495330e4283b6511253bafc32ac.jpg` | Misma fotografía de laptop a simple vista | No repetirla |
| `servicios/e7170d05eaf648fd97e40c567e8c8324.jpg` | Misma fotografía de escritorio a simple vista | No repetirla |
| `servicios/100bca4de0c1492788b65052a6b20f93.png` | Lámina de ejercicio de vocabulario en inglés | No pertinente |

`ImagenesDeDemostracion` reutiliza automáticamente las dos fotografías elegidas
**solo cuando `MOICA_R2_URL_PUBLICA_BASE` coincide con esa base comprobada**.
Así no se inventan URLs al apuntar otra instalación a otro bucket. Se incluyen
textos alternativos que describen el mantenimiento visible. En aquel inventario no
se subieron, duplicaron ni borraron objetos. La comparación de copias es visual,
no una afirmación de igualdad de bytes.

Para otros buckets o para completar contenido, configurar las variables siguientes
con una a tres claves públicas **ya existentes**, separadas por comas. Se acepta
el formato del helper actual `servicios/<32 hex>.(jpg|jpeg|png|webp)`; no URLs
firmadas, rutas privadas ni claves con parámetros. Las URLs se construyen desde
la base pública del entorno. Un mapeo inválido aborta sin escrituras y con mensaje
saneado. El seeder no hace solicitudes de red ni comprueba existencia en cada
arranque: el operador debe comprobar previamente la lectura anónima del objeto.

Las ocho variables de los servicios sin imagen recuperada ya tienen valor para la
base comprobada: son los objetos de la sección siguiente.
`MOICA_SEED_DEMO_IMAGENES_COMPUTADORAS` puede quedar vacía, porque reutiliza las
dos claves recuperadas; una lista propia las sobrescribe y vaciarla después las
restaura. En otro bucket esas ocho claves no existen: allí las entradas siguen sin
material y no deben copiarse por suposición.

La ausencia de material no impide crear los seis perfiles y los nueve servicios.
Si falta también la base R2 comprobada, computadoras queda sin mapeo automático.
Las dos fotografías de computadoras comparten objetos con publicaciones anteriores:
si su dueño los elimina por el flujo normal, la demo también pierde esas imágenes.
Este seeder no altera la política de borrado ni crea copias para evitarlo.

## Imágenes propias de la demostración

El 27 de septiembre de 2026 se subieron al bucket público —el que sirve la base
comprobada— ocho objetos nuevos, uno por cada servicio que no tenía imagen. Se
incorporaron exclusivamente como material de esta demostración, para ilustrar
servicios ficticios, y ninguna publicación real los usa. Ni los originales ni los
WebP generados se versionan en el repositorio.

| Variable | Object key | Servicio |
|---|---|---|
| `MOICA_SEED_DEMO_IMAGENES_PLOMERIA` | `servicios/53a6d16acc4249d5a513195de528b9dd.webp` | Reparación de fugas y cambio de grifería |
| `MOICA_SEED_DEMO_IMAGENES_ELECTRICIDAD` | `servicios/a0c5fa09279f40bab847ba667cf95d4a.webp` | Instalación de luminarias y tomacorrientes |
| `MOICA_SEED_DEMO_IMAGENES_CARPINTERIA` | `servicios/ced928be2fee42ee9784f4f66b98b93c.webp` | Muebles de madera a medida y reparaciones |
| `MOICA_SEED_DEMO_IMAGENES_MAQUILLAJE` | `servicios/62d3baf2d4f94e3fbeb805675ff983ef.webp` | Maquillaje social para tus ocasiones especiales |
| `MOICA_SEED_DEMO_IMAGENES_BARBERIA` | `servicios/4bceb8c69f1542df9e3081ba3dc96bac.webp` | Corte de cabello y perfilado de barba |
| `MOICA_SEED_DEMO_IMAGENES_UNAS` | `servicios/607439ed24da420d8169b1e2040cbf95.webp` | Manicura con esmaltado semipermanente |
| `MOICA_SEED_DEMO_IMAGENES_DISENO` | `servicios/ba8c842202014794893be48f43317667.webp` | Diseño de menús y piezas para redes sociales |
| `MOICA_SEED_DEMO_IMAGENES_SOPORTE` | `servicios/99b91fa0a5904a70bf1d7bc50664f113.webp` | Soporte de Wi-Fi y equipos para pequeñas oficinas |

La URL pública de cada objeto es la base comprobada seguida de `/` y su clave; por
ejemplo,
<https://pub-a1129fb1410e4c84b7ac69e79d442ace.r2.dev/servicios/53a6d16acc4249d5a513195de528b9dd.webp>.
El seeder no descarga ni sube imágenes: guarda en PostgreSQL la URL que construye
con esa base y la clave. Ni el backend ni el navegador piden nada a Pexels en
tiempo de ejecución. Estas ocho imágenes llevan el texto alternativo genérico del
seeder, `<nombre del servicio> — imagen ilustrativa 1`; solo las dos de
computadoras tienen una descripción propia.

Preparación y subida. Cada JPG se redujo a 1600 px en su lado mayor, sin recortar
ni deformar, y se convirtió a WebP con calidad 80. Solo se conservó la autoría
EXIF que ya traía el archivo; el resto de metadatos se descartó. Cada clave es
nueva y aleatoria, con la forma de `ClavesDeImagen` (`servicios/<32 hex>.webp`).
Se subió por la API S3 de R2 después de comprobar que la clave no existía y con
`If-None-Match: *`, de modo que ningún objeto se sobrescribió ni se borró. Tras
cada subida se verificó con una petición firmada —tipo `image/webp`, tamaño y ETag
igual al MD5 del archivo— y con un GET anónimo a la URL pública: 200, `image/webp`
y el mismo SHA-256 que el archivo subido.

Procedencia. Las fotografías se descargaron de Pexels; sus condiciones de uso están
en <https://www.pexels.com/license/>. El identificador de cada foto sale de la URL de
descarga que Windows registró en el archivo (`Zone.Identifier`); la página y el
autor se comprobaron en Pexels a partir de ese identificador.

| Servicio | WebP | Fotografía en Pexels | Autor en Pexels |
|---|---|---|---|
| Plomería | 1067×1600, 22 KiB | [7220892](https://www.pexels.com/photo/a-person-holding-brown-doorknob-7220892/) | cottonbro studio |
| Electricidad | 1068×1600, 82 KiB | [5691590](https://www.pexels.com/photo/an-electrician-using-pliers-to-repair-the-ac-power-plugs-and-sockets-5691590/) | Ksenia Chernaya |
| Carpintería | 1600×1067, 124 KiB | [7483049](https://www.pexels.com/photo/carpenter-making-a-furniture-7483049/) | cottonbro studio |
| Maquillaje | 1067×1600, 92 KiB | [8558242](https://www.pexels.com/photo/make-up-artist-applying-make-up-to-her-client-8558242/) | Nataliya Vaitkevich |
| Barbería/peluquería | 1600×1068, 118 KiB | [7518732](https://www.pexels.com/photo/a-barber-cutting-a-client-s-beard-7518732/) | Pavel Danilyuk |
| Uñas | 1600×1067, 91 KiB | [34930151](https://www.pexels.com/photo/professional-manicure-service-at-salon-34930151/) | José Antonio Otegui Auzmendi |
| Diseño gráfico | 1067×1600, 47 KiB | [12903003](https://www.pexels.com/photo/graphic-designer-using-graphic-tablet-and-laptop-12903003/) | Mizuno K |
| Soporte técnico | 900×1600, 75 KiB | [37492296](https://www.pexels.com/photo/technician-repairing-office-printer-in-workshop-37492296/) | Bulat843 |

Los EXIF de plomería y carpintería nombran como autora a Dimenshtein Olga y el de
soporte lleva el copyright de Bulat843; esos campos se conservan en los WebP.

## Carga de una sola vez en Railway

1. Integrar este PR por el flujo habitual con revisión ajena y desplegar el commit
   aprobado. Mantener `MOICA_SEED_DEMO_ENABLED=false` hasta preparar la carga.
2. En el proyecto Railway, entorno de demostración, servicio **backend → Variables**,
   poner `MOICA_SEED_DEMO_ENABLED=true`. Mantener las referencias privadas actuales
   de PostgreSQL. Confirmar la base pública R2; con la base comprobada se reutilizan
   las dos fotos de computadoras sin variables adicionales, y las ocho variables de
   [Imágenes propias de la demostración](#imágenes-propias-de-la-demostración)
   completan los demás servicios. Cualquier otro mapeo, solo con objetos ya
   verificados públicamente. No copiar credenciales al frontend.
3. Aplicar los cambios y desplegar/reiniciar el backend. Esperar healthcheck sano.
   No habilitar dominio del backend, TCP Proxy de PostgreSQL ni endpoints temporales.
4. En **Deployments → despliegue activo → Logs**, localizar únicamente los
   mensajes `Demo sincronizada: Resumen[...]`. Cada réplica del backend ejecuta el
   bootstrap y registra su propio resumen, así que con N réplicas aparecen N
   mensajes; el bloqueo las serializa y solo la primera encuentra la base vacía.
   En una base sin demo, con el bucket comprobado y las ocho variables, la suma de
   creados de todos los resúmenes debe ser `usuariosCreados=6`, `perfilesCreados=6`,
   `serviciosCreados=9` e `imagenesCreadas=10`, con `serviciosSinMapeo=0` en cada
   uno; sin esas variables, `imagenesCreadas=2` y `serviciosSinMapeo=8`. Con una
   sola réplica, ese resumen trae los cuatro contadores de existentes en cero. Con
   varias, uno muestra esos creados y los demás los mismos totales como existentes
   (`6`, `6`, `9` y el total de imágenes) y los creados en cero: no es un fallo.
   Con datos previos se reparten entre creados y existentes. Cada mensaje se
   emite después del commit; nunca contiene correos, hashes, credenciales,
   claves privadas ni firmas.
5. Sin iniciar sesión, abrir `/explorar` en el dominio público del frontend.
   Comprobar las tres categorías, las nueve subcategorías y los municipios.
   Abrir plomería, computadoras y un servicio «A convenir». Abrir los perfiles de
   Julio y Camila para comprobar múltiples servicios y ambos tipos de insignia.
   Comprobar la fotografía de cada tarjeta y las dos imágenes de computadoras, y
   anotar los IDs públicos de algunos servicios; la instalación puede tener además
   publicaciones ajenas a la demo.
6. En **backend → Variables**, volver a `MOICA_SEED_DEMO_ENABLED=false`.
7. Aplicar el cambio y hacer redeploy del backend. Confirmar salud, que ninguna
   réplica emite un nuevo mensaje de sincronización y que siguen los mismos
   servicios e IDs en `/explorar`. Apagar el bootstrap no elimina datos.

Para completar imágenes después: añadir los mapeos, repetir temporalmente
`true → deploy → verificar → false → redeploy`. La segunda ejecución conserva las
mismas cuentas, perfiles y publicaciones. En una base que ya tiene la demo sin las
ocho imágenes, el resumen debe mostrar `imagenesCreadas=8`, `imagenesExistentes=2`
y `serviciosSinMapeo=0`, sin usuarios, perfiles ni servicios creados: así resultó
el ensayo local del 27 de septiembre de 2026 contra PostgreSQL 18 y la base
comprobada. Si no se cambió el mapeo, no crea ninguna imagen adicional. No usar estas cuentas para representar prestadores reales ni
promoverlas a administración. Los recorridos autenticados siguen usando las
cuentas de prueba del flujo E2E, separado de esta semilla pública.

## Validación automatizada

`DemoIT` usa PostgreSQL real del Testcontainer existente: desactivación, creación,
repetición con IDs/hashes/fechas iguales, recuperación parcial, filtros y consulta
pública, relaciones e imágenes, nueve subcategorías, BCrypt, preservación de datos
ajenos, catálogo con IDs distintos, validación de claves, rollback y concurrencia.
Sus object keys sintéticos son fixtures y nunca se consultan en R2.
`ArranqueDeDemoIT` verifica además el bootstrap real de Spring y realiza GET
anónimos a `/api/servicios`, sus filtros y detalles, y `/api/prestadores/{id}`:
los nueve servicios y seis perfiles responden públicamente sin datos privados.
`BootstrapDeDemoTest` comprueba activación por propiedad, valor ausente/false y
restricción del mapeo recuperado al bucket comprobado.

Con Docker activo y Java 21, desde `backend`:

```sh
./mvnw -B -ntp verify -Dtest='com.moica.demo.*Test' -Dit.test='com.moica.demo.*IT' -Dspotbugs.skip=true
./mvnw -B -ntp verify
```

Desde `frontend`, ejecutar los controles habituales:

```sh
npm run format:check
npm run lint
npm run typecheck
npm run test
npm run build
```

Los resultados de esta rama se registran en el PR y en la matriz de cumplimiento;
estos comandos documentan el procedimiento, no afirman una ejecución productiva.
