# Mapa de la documentación de Moica

Este documento es **navegación, no normativa**. No repite ninguna regla: dice qué
fuente manda por materia, cuándo abrirla, cómo localizar la sección concreta y
cuándo no hace falta abrirla. Sirve igual a una persona que se incorpora al
proyecto y a cualquier asistente de código (Claude, Codex, Cursor).

Si el mapa y una fuente discrepan, **manda la fuente** y se corrige el mapa.

## 1. Dónde se define la precedencia

El orden que decide qué documento manda cuando dos parecen contradecirse está
escrito en el apartado 2 de `PlanImplementacionMvp.md`, no aquí:

```
grep -n '^## 2\.' Docs/Dev/PlanImplementacionMvp.md
```

Es un apartado corto: se lee entero cuando hay una contradicción real y no se
abre por rutina. `Docs/Core/DocumentoBase.md` es histórico y no se programa a
partir de él.

## 2. Materia → fuente

| Materia | Fuente | Se abre cuando | No hace falta si |
|---|---|---|---|
| Alcance funcional, actores, estados, reglas de negocio | `Docs/Core/DefinicionProducto.md` | falta una regla que el apartado del incremento no fija | el apartado del incremento ya la fija |
| Orden del MVP, rama, commits esperados y criterio de salida | `Docs/Dev/PlanImplementacionMvp.md` §7 | al empezar o retomar un incremento | la tarea no pertenece a un incremento del plan |
| Permisos por acción y efectos del estado de cuenta | `Docs/Dev/PlanImplementacionMvp.md` §8 | se añade o cambia un endpoint, un rol o una guardia | no se toca autorización |
| Definition of Done común | `Docs/Dev/PlanImplementacionMvp.md` §13 | antes de cerrar y abrir el PR | — |
| Endpoints, DTO, códigos de error | `Docs/Dev/ContratoDeApi.md` | se crea o cambia una ruta, un DTO o un código | el cambio no cruza la frontera HTTP |
| Esquema: tipos, nulabilidad, dominios, claves | `Docs/Dev/Moica - Diccionario de Datos.xlsx`, `DiagramaLogico.mmd` y las migraciones de `backend/src/main/resources/db/migration` | se toca persistencia o se crea una migración | el cambio no toca el modelo de datos |
| Convenciones de código | `Docs/Dev/ESTANDARES_CODIGO.md` y el `CLAUDE.md` de `backend/` o `frontend/` | hay duda sobre estructura, nombres o capas | el código vecino ya resuelve la duda |
| Ramas, commits, PR y promoción a `main` | `Docs/Core/GIT_WORKFLOW.md` | hay duda sobre nomenclatura, destino de un PR o evidencia visual | — |
| Restricciones técnicas del MVP (stack, prohibiciones) | `Docs/Core/prompt.md` | se valora una dependencia, un patrón o una tecnología | el `CLAUDE.md` aplicable ya lo prohíbe o lo permite |
| Exclusiones expresas | `Docs/Core/post-mvp.md` | se duda de si algo pertenece al MVP | — |
| Qué está hecho de verdad y con qué evidencia | `Docs/Dev/MatrizCumplimiento.md` y los PR fusionados | al registrar evidencia o comprobar si algo ya existe | basta consultar el PR en GitHub |
| Levantar y usar el entorno local | `Docs/Dev/GuiaEntornoLocal.md` | cambian comandos, puertos o variables | — |
| Almacenamiento privado de archivos | `Docs/Dev/Almacenamiento.md` | se toca subida, entrega temporal o claves opacas | — |
| Despliegue productivo, Docker y smoke | `Docs/Dev/DespliegueProduccion.md` | se toca imagen, Nginx, perfil `prod` o smoke | — |
| Identidad visual, tokens, voz y accesibilidad | `Docs/Design/` y `Docs/Marketing/` | se crea pantalla o componente nuevo | se reutilizan componentes y tokens ya existentes |

## 3. Documentos caros

No se abren completos por rutina. El tamaño es indicativo y crece con el
proyecto; lo que no cambia es la forma de entrar: **localizar y leer el rango**.

| Documento | Orden de magnitud | Cómo entrar |
|---|---|---|
| `Docs/Dev/MatrizCumplimiento.md` | el mayor del repositorio, ~2 000 líneas | `grep -n '^## ' <ruta>` y leer solo la sección del incremento |
| `Docs/Dev/ContratoDeApi.md` | ~1 500 líneas | `grep -n '^#\{2,3\} ' <ruta>` y leer solo el recurso afectado |
| `Docs/Core/DefinicionProducto.md` | ~650 líneas | `grep -n '^## ' <ruta>` y leer solo la regla buscada |
| `Docs/Dev/PlanImplementacionMvp.md` | ~690 líneas | `grep -n '^#\{2,3\} ' <ruta>` y leer solo el apartado o el incremento |

## 4. Qué no se lee automáticamente

Se abren solo cuando resuelven una decisión concreta, nunca «para tener
contexto»:

- Los PDF de `Docs/Dev/` y `Docs/Design/`: son renders de las fuentes `.tex` y
  `.mmd`, que sí son legibles como texto.
- El UML completo (`DiagramaCasosDeUso.tex`, `DiagramaActividades.tex`,
  `DiagramaClasesDominio.mmd`).
- `Docs/Marketing/` y `Docs/Design/` cuando no se crea interfaz nueva.
- `Docs/Core/DocumentoBase.md`: documentación histórica.
- `Docs/Dev/capturas/`: evidencia adjunta, no fuente.
- El histórico de la matriz de cumplimiento: interesa la sección del incremento
  en curso, no la historia entera.

## 5. Cómo se localiza

```
grep -n '^#\{1,3\} ' <documento>          # índice de encabezados
grep -n 'TerminoBuscado' <documento>      # dónde se menciona
sed -n 'DESDE,HASTAp' <documento>         # leer solo ese rango
git diff -- <ruta>                        # revisar lo propio recién cambiado
git log --oneline -- <ruta>               # historia de un archivo concreto
```

Este mapa no fija rangos de líneas: quedarían falsos al primer cambio. Se navega
siempre por encabezado o por término.
