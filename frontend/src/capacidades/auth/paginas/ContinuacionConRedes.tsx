import estilos from './acceso.module.css';

/**
 * Divisor y botones de redes del diseño.
 *
 * El MVP autentica solo con correo y contraseña (`Docs/Core/DefinicionProducto.md`).
 * Estos controles se pintan para fidelidad visual y quedan deshabilitados:
 * no hay OAuth ni proveedores externos en esta etapa.
 */
export function ContinuacionConRedes() {
  return (
    <div className={estilos.redes}>
      <p className={estilos.divisor}>o continúa con</p>
      <div className={estilos.botonesSociales}>
        <button
          type="button"
          className={estilos.botonSocial}
          disabled
          aria-label="Continuar con Google (no disponible en el MVP)"
        >
          <IconoGoogle />
        </button>
        <button
          type="button"
          className={estilos.botonSocial}
          disabled
          aria-label="Continuar con Facebook (no disponible en el MVP)"
        >
          <IconoFacebook />
        </button>
        <button
          type="button"
          className={estilos.botonSocial}
          disabled
          aria-label="Continuar con Apple (no disponible en el MVP)"
        >
          <IconoApple />
        </button>
      </div>
    </div>
  );
}

function IconoGoogle() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M21.6 12.23c0-.74-.07-1.45-.19-2.13H12v4.03h5.38a4.6 4.6 0 0 1-2 3.02v2.5h3.23c1.89-1.74 2.99-4.31 2.99-7.42Z"
      />
      <path
        fill="currentColor"
        d="M12 22c2.7 0 4.97-.9 6.63-2.35l-3.23-2.5c-.9.6-2.05.96-3.4.96-2.61 0-4.82-1.76-5.61-4.13H3.06v2.58A10 10 0 0 0 12 22Z"
      />
      <path
        fill="currentColor"
        d="M6.39 13.98A6 6 0 0 1 6.08 12c0-.69.12-1.35.31-1.98V7.44H3.06A10 10 0 0 0 2 12c0 1.61.39 3.14 1.06 4.56l3.33-2.58Z"
      />
      <path
        fill="currentColor"
        d="M12 5.89c1.47 0 2.79.5 3.83 1.5l2.87-2.87C16.96 2.89 14.7 2 12 2A10 10 0 0 0 3.06 7.44l3.33 2.58C7.18 7.65 9.39 5.89 12 5.89Z"
      />
    </svg>
  );
}

function IconoFacebook() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M14.5 8.5V6.8c0-.7.5-1.3 1.2-1.3h1.3V3h-2.2C12.2 3 10.5 4.8 10.5 7v1.5H8.5V11h2v10h3.5V11h2.3l.5-2.5h-2.8Z"
      />
    </svg>
  );
}

function IconoApple() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        fill="currentColor"
        d="M16.4 12.6c0-2.3 1.9-3.4 2-3.5-1.1-1.6-2.8-1.8-3.4-1.8-1.4-.2-2.8.9-3.5.9s-1.8-.8-3-.8c-1.5 0-3 .9-3.8 2.3-1.6 2.8-.4 7 1.2 9.3.8 1.1 1.7 2.3 2.9 2.3 1.2 0 1.6-.7 3-.7s1.8.7 3 .7 2-.1 2.9-2.3c1-.1.8-1.9.8-1.9s-2.1-.8-2.1-3.5Zm-2-5.5c.6-.8 1.1-1.9.9-3.1-1 .1-2.1.7-2.8 1.5-.6.7-1.2 1.8-1 2.9 1.1.1 2.2-.6 2.9-1.3Z"
      />
    </svg>
  );
}
