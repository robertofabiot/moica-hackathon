# Estándares de código de Moica

Reglas prácticas para escribir y revisar código en este repositorio. No es un
resumen de Clean Code ni de SOLID: es la traducción de esos principios a
decisiones concretas que se pueden comprobar en un Pull Request.

Precedencia: si algo aquí contradice `Docs/Core/DefinicionProducto.md`, el
diccionario de datos, el modelo lógico o `Docs/Core/prompt.md`, mandan esos
documentos.

---

## 1. Nombres

- Un nombre dice **qué representa**, no cómo está implementado:
  `solicitudRepository`, no `repo2`; `nivelVerificacion`, no `nv`.
- El dominio se nombra en español, igual que la documentación y el diccionario:
  `PerfilPrestador`, `SolicitudVerificacion`, `EstadoCuenta`. Las palabras
  técnicas del framework se quedan en inglés (`Controller`, `Service`,
  `Repository`, `Dto`, `useQuery`).
- Un mismo concepto se llama igual en la base de datos, el backend y el
  frontend. Si el diccionario dice `nivelVerificacion`, nadie inventa
  `verificationTier`.
- Los booleanos se leen como una afirmación: `estaDisponible`, `tieneTotpActivo`.
- Las abreviaturas solo se admiten si ya son del dominio (`TOTP`, `PWA`, `URL`).

**Cómo se comprueba:** revisión del PR y contraste con el diccionario de datos.

## 2. Responsabilidad única

- Una clase o módulo tiene **una razón para cambiar**. Si al describir una clase
  hace falta la palabra «y», probablemente son dos.
- Reparto fijo de responsabilidades en el backend:

| Capa | Hace | No hace |
|---|---|---|
| `controller` | Recibe la petición, valida el DTO, delega y devuelve una respuesta | Reglas de negocio, consultas, transacciones |
| `service` | Reglas de negocio, autorización de dominio, transacciones | Conocer HTTP, serializar respuestas |
| `repository` | Acceso a datos | Decidir reglas de negocio |
| `entity` | Estado persistente | Salir de la aplicación hacia el cliente |
| `dto` | Frontera de entrada y salida, con validaciones | Contener lógica de negocio |

- En el frontend, un componente que obtiene datos, decide reglas y además pinta
  una vista compleja se divide: hook para los datos, componente para la vista.

**Cómo se comprueba:** revisión del PR; un `controller` con `if` de negocio o un
`repository` importado desde un componente React se rechazan.

## 3. Tamaño de métodos y componentes

- Un método hace una cosa y cabe en la pantalla. Como referencia práctica: si
  supera unas 30 líneas o tiene más de tres niveles de anidamiento, se extrae.
- Un componente React que supera unas 150 líneas o mezcla varias pantallas se
  divide.
- Se prefiere la salida temprana (`return` o `throw`) a los `else` encadenados.

**Cómo se comprueba:** revisión del PR. No hay un umbral automatizado en el MVP;
es una guía, no una métrica que se persiga por sí misma.

## 4. Dirección de las dependencias

- El flujo permitido es `controller → service → repository → entity`. Nunca al
  revés: un `service` no llama a un `controller`, y una `entity` no conoce DTO.
- Entre capacidades (`usuario`, `prestador`, `solicitud`...), la comunicación
  pasa por el `service` de la otra capacidad, nunca por su `repository`.
- El código compartido vive en la capacidad `comun` (errores, seguridad,
  utilidades). `comun` no depende de ninguna capacidad concreta.
- En el frontend, una capacidad no importa archivos internos de otra: expone lo
  que comparte desde su punto de entrada.

**Cómo se comprueba:** revisión del PR sobre los `import`.

## 5. Validación en las fronteras

- Toda entrada externa se valida **en el backend**, con Bean Validation sobre el
  DTO. La validación del formulario en React mejora la experiencia, no sustituye
  al backend.
- Las reglas que no se pueden expresar con anotaciones (por ejemplo, «el servicio
  debe estar activo») se validan en el `service`, no en el `controller`.
- Ocultar un botón no es un control de seguridad. La autorización se aplica
  siempre en el backend.
- Los datos de un archivo subido se validan por tipo MIME y tamaño en el
  backend, nunca solo en el navegador.

**Cómo se comprueba:** pruebas negativas obligatorias por cada regla de
validación (entrada inválida, recurso ajeno, estado incorrecto).

## 6. Separación entre API, lógica y persistencia

- **Prohibido exponer entidades JPA** en un `controller`. La entrada y la salida
  son siempre DTO.
- El modelo de la API puede diferir del modelo de datos, y eso es deseable: la
  API no publica claves de almacenamiento, hashes, secretos TOTP, documentos de
  identidad ni observaciones administrativas.
- El frontend define sus propios tipos de la respuesta esperada; no reutiliza una
  forma «porque coincide hoy».

**Cómo se comprueba:** revisión del PR; una firma de `controller` que devuelve
una `entity` se rechaza.

## 7. Manejo explícito de errores

- Los errores esperables (recurso inexistente, permiso denegado, estado
  inválido) se representan con excepciones propias y se traducen en un manejador
  global a una respuesta uniforme.
- El cuerpo del error indica **qué pasó y qué hacer**, sin trazas, SQL, nombres
  de clase ni valores internos.
- Prohibido capturar una excepción y no hacer nada. Si se captura, se registra o
  se transforma.
- Un `catch (Exception e)` genérico solo se admite en el manejador global.
- En el frontend, cada consulta remota contempla sus estados de carga y error;
  no se asume que la respuesta siempre llega.

**Cómo se comprueba:** pruebas que verifiquen el código de estado y la forma del
cuerpo de error; revisión de que ninguna respuesta filtra detalle interno.

## 8. Código muerto y duplicación

- No se versiona código comentado, imports sin uso, variables sin uso ni
  archivos «por si acaso». El historial de Git ya conserva lo eliminado.
- La duplicación se elimina cuando **la tercera aparición** confirma que es la
  misma regla. Dos fragmentos parecidos que cambian por motivos distintos se
  dejan separados.
- Prohibido dejar `TODO` sin dueño: o se resuelve, o se anota en la sección de
  pendientes del PR.

**Cómo se comprueba:** `spotless:check` (imports sin uso) y `npm run lint`
(variables e imports sin uso) fallan el build.

## 9. Comentarios

- El comentario explica **por qué**, no qué. `// suma 1 al contador` sobra;
  `// el diccionario exige que la primera versión SCD2 nazca en la misma
  transacción` aporta.
- Se comenta lo que sorprende: una decisión del plan, una restricción del
  diccionario, un caso límite.
- Si un comentario es necesario para entender qué hace el código, primero se
  intenta mejorar el nombre o extraer un método.

**Cómo se comprueba:** revisión del PR.

## 10. Pruebas

- Cada regla de negocio nueva llega con al menos una prueba positiva y una
  negativa. La prueba negativa es la que demuestra que la regla existe.
- Casos que no pueden faltar cuando aplican: entrada inválida, recurso
  inexistente, actor sin permiso, propiedad ajena, estado no permitido.
- La persistencia se prueba contra **PostgreSQL real** con Testcontainers. H2 no
  demuestra checks, índices parciales ni exclusión temporal.
- El nombre de la prueba describe el comportamiento esperado, no el método:
  `rechazaSolicitudDeServicioPropio`, no `testEnviar`.
- Una prueba no depende de otra ni del orden de ejecución.

**Cómo se comprueba:** `./mvnw verify` y `npm run test` en local y en CI.

## 11. Seguridad de logs y respuestas

- Nunca se registran ni se devuelven: contraseñas, hashes, secretos TOTP,
  códigos de segundo factor, tokens de sesión, cookies, números de documento de
  identidad, claves de almacenamiento de archivos privados ni URL permanentes de
  esos archivos.
- Los identificadores de recursos privados no aparecen en respuestas públicas.
- No se hace log del cuerpo completo de una petición de autenticación ni de una
  carga de expediente.
- Ningún secreto se escribe en el código ni en un archivo versionado: van por
  variables de entorno y se documentan sin valor en `.env.example`.

**Cómo se comprueba:** revisión del PR y del diff antes de subir; la lista de
verificación de la plantilla de PR lo exige explícitamente.

## 12. Incorporación de dependencias

- Una dependencia entra en el mismo PR que la usa y con una función concreta.
  Prohibido «dejarla instalada para más adelante».
- Debe estar en la lista aprobada del plan (sección 12) o justificarse en el PR.
- Debe quedar fijada por `package-lock.json` o por la versión declarada en el
  `pom.xml`, y registrarse en el `README.md`.
- Prohibido en el MVP: Tailwind, MUI, Bootstrap, Redux, WebSockets, mapas,
  pasarelas de pago, OCR, biometría y proveedores externos de verificación.

Dependencias de infraestructura aprobadas después del plan:

| Dependencia | Para qué | Decisión |
|---|---|---|
| `software.amazon.awssdk:s3` | Almacenamiento de objetos en Cloudflare R2 por su compatibilidad con S3 | [Almacenamiento.md](Almacenamiento.md) |

**Cómo se comprueba:** `npm ci` respeta el lockfile; la revisión del PR
contrasta cada dependencia nueva contra el plan.

## 13. Abstracciones a tiempo

- No se crean interfaces con una sola implementación, clases vacías, capas
  «preparadas» ni carpetas de módulos que todavía no tienen código.
- No se aplica un patrón de diseño sin un problema actual que lo justifique.
- Un parámetro de configuración se agrega cuando alguien necesita cambiarlo, no
  por si acaso.
- Entre dos soluciones equivalentes, gana la más simple y explícita.

**Cómo se comprueba:** revisión del PR; una abstracción sin segundo caso de uso
se rechaza.

---

## Controles automáticos

Los controles se ejecutan igual en local y en CI. Un incumplimiento hace fallar
el check correspondiente; no existen solo como documentación.

### Backend

| Control | Herramienta | Comando |
|---|---|---|
| Formato de Java e imports sin uso | Spotless | `./mvnw spotless:check` |
| Análisis estático de defectos | SpotBugs | `./mvnw spotbugs:check` |
| Pruebas unitarias | Surefire | `./mvnw test` |
| Pruebas de integración con PostgreSQL | Failsafe + Testcontainers | `./mvnw verify` |

`./mvnw spotless:apply` corrige el formato automáticamente.

### Frontend

| Control | Herramienta | Comando |
|---|---|---|
| Formato | Prettier | `npm run format:check` |
| Lint | ESLint | `npm run lint` |
| Tipos | TypeScript | `npm run typecheck` |
| Pruebas | Vitest + Testing Library | `npm run test` |
| Build | Vite | `npm run build` |

`npm run format` corrige el formato automáticamente.

### Reglas sobre las supresiones

- Prohibido desactivar una regla de forma global para ocultar un problema.
- Una supresión puntual (`@SuppressFBWarnings`, `// eslint-disable-next-line`,
  `@SuppressWarnings`) se admite solo con un comentario que explique por qué el
  aviso no aplica en ese punto concreto.
- Prohibido usar `continue-on-error` en CI para aparentar un check en verde.
- `any` en TypeScript necesita justificación escrita en el propio código; si es
  un dato externo sin forma conocida, se usa `unknown` y se valida.

### Lo que todavía no se agrega

SonarQube, PMD, Checkstyle, Stylelint, umbrales de cobertura y plataformas
externas de calidad quedan fuera de esta etapa. Con Spotless, SpotBugs, ESLint,
Prettier, TypeScript y las pruebas hay barrera suficiente; añadir más
herramientas antes de tener código funcional solo generaría ruido.
