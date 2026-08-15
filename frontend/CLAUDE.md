# Directrices de Claude para el frontend

Complementan `../CLAUDE.md` y `../Docs/Dev/ESTANDARES_CODIGO.md`. Aplican a todo
el código de `frontend/`.

## Organización

- Organización **por capacidades**, no por tipos de archivo. Cada capacidad vive
  en `src/capacidades/<capacidad>/` con sus páginas, componentes, hooks y tipos.
- Capacidades previstas: `auth`, `usuario`, `prestador`, `portafolio`,
  `verificacion`, `servicio`, `busqueda`, `solicitud`, `chat`, `calificacion`,
  `moderacion` y `admin`.
- Una capacidad se crea **cuando su incremento la implementa**. Prohibido crear
  carpetas o componentes vacíos anticipando pantallas futuras.
- Lo genuinamente compartido va en `src/comun/`. Una capacidad no importa
  archivos internos de otra.
- Las pantallas que no pertenecen a ninguna capacidad, como el inicio y la ruta
  no encontrada, viven en `src/paginas/`.
- Los estilos globales y las variables CSS viven en `src/estilos/`.

## TypeScript

- Modo estricto siempre activo. Prohibido desactivar reglas del compilador para
  que algo compile.
- Prohibido `any`. Para un dato externo sin forma conocida se usa `unknown` y se
  valida antes de usarlo.
- Si en un caso concreto `any` fuera inevitable, se justifica con un comentario
  en esa misma línea. Sin justificación, no entra.
- Las respuestas de la API se tipan explícitamente en el frontend.
- `npm run typecheck` debe pasar antes de dar una tarea por terminada.

## Componentes

- Componentes pequeños, con una responsabilidad clara y un nombre que la diga.
- Los datos se obtienen en un hook; el componente pinta. Un componente que
  consulta, decide y además pinta una vista compleja se divide.
- Prohibido construir pantallas o componentes finales que todavía no pertenecen
  al incremento en curso.

## Estado

- **Estado remoto: TanStack React Query.** Toda petición a la API pasa por él,
  con sus estados de carga y error contemplados.
- **Estado local primero:** `useState` y `useReducer` mientras alcancen.
- **Zustand solo cuando exista estado global real**, compartido por partes
  distintas de la aplicación y que no sea caché de servidor. No se instala antes
  de necesitarlo.
- Prohibido Redux y el uso abusivo de Context.
- El chat usará short polling con `refetchInterval`. Prohibido WebSockets.

## Estilos

- **CSS Modules** (`Componente.module.css`), junto al componente.
- **Mobile-first:** la hoja base describe el teléfono y las `@media` amplían a
  tableta y escritorio, nunca al revés.
- Prohibido Tailwind, MUI, Bootstrap y cualquier framework visual.
- Los colores, tipografías y espaciados salen de `Docs/Design/`. Prohibido
  inventar una identidad visual, una paleta o un sistema de componentes que el
  equipo de diseño no haya documentado.

## Accesibilidad desde el inicio

- HTML semántico antes que `div`: `button` para acciones, `a` para navegación,
  `main`, `nav` y encabezados en orden.
- Todo control tiene nombre accesible: texto visible, `label` asociado o
  `aria-label`.
- Toda imagen informativa lleva `alt`; las decorativas, `alt=""`.
- El foco se ve siempre. Prohibido `outline: none` sin un reemplazo visible.
- Lo que se hace con el ratón se puede hacer con el teclado.
- Contraste suficiente y textos legibles en pantalla de teléfono.

## Antes de dar algo por terminado

`npm run format:check`, `npm run lint`, `npm run typecheck`, `npm run test` y
`npm run build`, todos en verde.
