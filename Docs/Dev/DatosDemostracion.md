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
  nombres identifica exclusivamente esta semilla. No renombrar estos correos.
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

La sincronización es aditiva: si se acorta o vacía una lista de imágenes, las
asociaciones anteriores se conservan. El resumen `imagenesExistentes` cuenta las
posiciones del mapeo procesado y `serviciosSinMapeo` las entradas sin mapeo
efectivo; no representan un inventario total de imágenes conservadas.

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
textos alternativos que describen el mantenimiento visible. No se subieron,
duplicaron ni borraron objetos. La comparación de copias es visual, no una
afirmación de igualdad de bytes.

Para otros buckets o para completar contenido, configurar las variables siguientes
con una a tres claves públicas **ya existentes**, separadas por comas. Se acepta
el formato del helper actual `servicios/<32 hex>.(jpg|jpeg|png|webp)`; no URLs
firmadas, rutas privadas ni claves con parámetros. Las URLs se construyen desde
la base pública del entorno. Un mapeo inválido aborta sin escrituras y con mensaje
saneado. El seeder no hace solicitudes de red ni comprueba existencia en cada
arranque: el operador debe comprobar previamente la lectura anónima del objeto.

| Variable | Estado / material pendiente |
|---|---|
| `MOICA_SEED_DEMO_IMAGENES_PLOMERIA` | Falta al menos una clave pública de fugas, tuberías o grifería |
| `MOICA_SEED_DEMO_IMAGENES_ELECTRICIDAD` | Falta al menos una clave pública de luminarias o tomacorrientes |
| `MOICA_SEED_DEMO_IMAGENES_CARPINTERIA` | Falta al menos una clave pública de muebles o reparaciones de madera |
| `MOICA_SEED_DEMO_IMAGENES_MAQUILLAJE` | Falta al menos una clave pública de maquillaje social |
| `MOICA_SEED_DEMO_IMAGENES_BARBERIA` | Falta al menos una clave pública de cortes de cabello o barba |
| `MOICA_SEED_DEMO_IMAGENES_UNAS` | Falta al menos una clave pública de manicura |
| `MOICA_SEED_DEMO_IMAGENES_COMPUTADORAS` | Dos claves recuperadas; opcional sobrescribir su lista |
| `MOICA_SEED_DEMO_IMAGENES_DISENO` | Falta al menos una clave pública de menús o piezas gráficas |
| `MOICA_SEED_DEMO_IMAGENES_SOPORTE` | Falta al menos una clave pública de Wi-Fi, impresoras o equipos de oficina |

No se conocen aún las URLs/claves de esas ocho entradas: no se inventaron.
La ausencia de material no impide crear los seis perfiles y los nueve servicios.
Si falta también la base R2 comprobada, computadoras queda sin mapeo automático.
Las URLs reutilizadas comparten objetos con publicaciones anteriores: si su dueño
los elimina por el flujo normal, la demo también pierde esas imágenes. Este
seeder no altera la política de borrado ni crea copias para evitarlo.

## Carga de una sola vez en Railway

1. Integrar este PR por el flujo habitual con revisión ajena y desplegar el commit
   aprobado. Mantener `MOICA_SEED_DEMO_ENABLED=false` hasta preparar la carga.
2. En el proyecto Railway, entorno de demostración, servicio **backend → Variables**,
   poner `MOICA_SEED_DEMO_ENABLED=true`. Mantener las referencias privadas actuales
   de PostgreSQL. Confirmar la base pública R2; con la base comprobada se reutilizan
   las dos fotos sin variables adicionales. Completar los mapeos opcionales solo
   con objetos ya verificados públicamente. No copiar credenciales al frontend.
3. Aplicar los cambios y desplegar/reiniciar el backend. Esperar healthcheck sano.
   No habilitar dominio del backend, TCP Proxy de PostgreSQL ni endpoints temporales.
4. En **Deployments → despliegue activo → Logs**, localizar únicamente
   `Demo sincronizada: Resumen[...]`. En una base sin demo y con el bucket comprobado,
   el resumen esperado es `usuariosCreados=6`, `perfilesCreados=6`,
   `serviciosCreados=9`, `imagenesCreadas=2`, los cuatro contadores de existentes en
   cero y `serviciosSinMapeo=8`. Con datos previos se reparten entre creados y
   existentes. El mensaje se emite después del commit; nunca contiene correos,
   hashes, credenciales, claves privadas ni firmas.
5. Sin iniciar sesión, abrir `/explorar` en el dominio público del frontend.
   Comprobar las tres categorías, las nueve subcategorías y los municipios.
   Abrir plomería, computadoras y un servicio «A convenir». Abrir los perfiles de
   Julio y Camila para comprobar múltiples servicios y ambos tipos de insignia.
   Comprobar las dos imágenes de computadoras y anotar los IDs públicos de algunos
   servicios; la instalación puede tener además publicaciones ajenas a la demo.
6. En **backend → Variables**, volver a `MOICA_SEED_DEMO_ENABLED=false`.
7. Aplicar el cambio y hacer redeploy del backend. Confirmar salud, ausencia de un
   nuevo mensaje de sincronización y permanencia de los mismos servicios e IDs en
   `/explorar`. Apagar el bootstrap no elimina datos.

Para completar imágenes después: añadir los mapeos, repetir temporalmente
`true → deploy → verificar → false → redeploy`. La segunda ejecución conserva las
mismas cuentas, perfiles y publicaciones. Si no se cambió el mapeo, no crea ninguna
imagen adicional. No usar estas cuentas para representar prestadores reales ni
promoverlas a administración. Los recorridos autenticados siguen usando las
cuentas de prueba del flujo E2E, separado de esta semilla pública.

## Validación automatizada

`DemoIT` usa PostgreSQL real del Testcontainer existente: desactivación, creación,
repetición con IDs/hashes/fechas iguales, recuperación parcial, filtros y consulta
pública, relaciones e imágenes, nueve subcategorías, BCrypt, preservación de datos
ajenos, catálogo con IDs distintos, validación de claves, rollback y concurrencia.
Sus object keys sintéticos son fixtures y nunca se consultan en R2.
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
