import type { SVGProps } from 'react';

type PropiedadesDeIcono = SVGProps<SVGSVGElement>;

function Trazo({ children, ...rest }: PropiedadesDeIcono) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {children}
    </svg>
  );
}

export function IconoLupa() {
  return (
    <Trazo>
      <circle cx="11" cy="11" r="8" />
      <path d="m21 21-4.3-4.3" />
    </Trazo>
  );
}

export function IconoPin() {
  return (
    <Trazo>
      <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </Trazo>
  );
}

export function IconoCasa() {
  return (
    <Trazo>
      <path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" />
      <path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    </Trazo>
  );
}

/** Estrella rellena. El color lo pone el consumidor (`currentColor`). */
export function IconoEstrella(props: PropiedadesDeIcono) {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" {...props}>
      <path d="M12 2.6 14.7 8.2l6.2.9-4.5 4.4 1.1 6.2L12 16.8 6.5 19.7l1.1-6.2-4.5-4.4 6.2-.9L12 2.6Z" />
    </svg>
  );
}

/** Círculo con check. El acento naranja se aplica con `currentColor`. */
export function IconoCheckCirculo(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <circle cx="12" cy="12" r="10" />
      <path d="m9 12 2 2 4-4" />
    </Trazo>
  );
}

/** Marcador para guardar un servicio. */
export function IconoGuardar(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="M19 21 12 16 5 21V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />
    </Trazo>
  );
}

export { IconoGuardar as IconoMarcador };
