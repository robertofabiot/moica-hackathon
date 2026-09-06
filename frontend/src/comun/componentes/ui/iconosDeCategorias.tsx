import type { ReactElement, SVGProps } from 'react';

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

export function IconoHogar() {
  return (
    <Trazo>
      <path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" />
      <path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    </Trazo>
  );
}

export function IconoConstruccion() {
  return (
    <Trazo>
      <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
    </Trazo>
  );
}

export function IconoTransporte() {
  return (
    <Trazo>
      <path d="M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2" />
      <path d="M15 18H9" />
      <path d="M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14" />
      <circle cx="17" cy="18" r="2" />
      <circle cx="7" cy="18" r="2" />
    </Trazo>
  );
}

export function IconoTecnologia() {
  return (
    <Trazo>
      <rect width="18" height="12" x="3" y="4" rx="2" />
      <path d="M2 20h20" />
      <path d="M8 20v-2" />
      <path d="M16 20v-2" />
    </Trazo>
  );
}

export function IconoBelleza() {
  return (
    <Trazo>
      <path d="M12 5a3 3 0 1 1 3 9h-6a3 3 0 1 1 3-9Z" />
      <path d="M12 14v7" />
      <path d="M8 18h8" />
      <path d="m9 5 1-3" />
      <path d="m15 5-1-3" />
    </Trazo>
  );
}

export function IconoEventos() {
  return (
    <Trazo>
      <path d="M8 2v4" />
      <path d="M16 2v4" />
      <rect width="18" height="18" x="3" y="4" rx="2" />
      <path d="M3 10h18" />
      <path d="M8 14h.01" />
      <path d="M12 14h.01" />
      <path d="M16 14h.01" />
      <path d="M8 18h.01" />
      <path d="M12 18h.01" />
    </Trazo>
  );
}

export function IconoEducacion() {
  return (
    <Trazo>
      <path d="M21.42 10.922a1 1 0 0 0-.019-1.838L12.83 5.18a2 2 0 0 0-1.66 0L2.6 9.08a1 1 0 0 0 0 1.832l8.57 3.908a2 2 0 0 0 1.66 0z" />
      <path d="M22 10v6" />
      <path d="M6 12.5V16a6 3 0 0 0 12 0v-3.5" />
    </Trazo>
  );
}

export function IconoMasCategorias() {
  return (
    <Trazo>
      <rect width="7" height="7" x="3" y="3" rx="1" />
      <rect width="7" height="7" x="14" y="3" rx="1" />
      <rect width="7" height="7" x="14" y="14" rx="1" />
      <rect width="7" height="7" x="3" y="14" rx="1" />
    </Trazo>
  );
}

export type IdentificadorDeCategoriaVisual =
  | 'hogar'
  | 'construccion'
  | 'transporte'
  | 'tecnologia'
  | 'belleza'
  | 'eventos'
  | 'educacion'
  | 'mas';

const ICONOS: Record<IdentificadorDeCategoriaVisual, () => ReactElement> = {
  hogar: IconoHogar,
  construccion: IconoConstruccion,
  transporte: IconoTransporte,
  tecnologia: IconoTecnologia,
  belleza: IconoBelleza,
  eventos: IconoEventos,
  educacion: IconoEducacion,
  mas: IconoMasCategorias,
};

/**
 * Elige el icono temático a partir del nombre de categoría que envía el catálogo.
 */
function identificadorDeCategoriaVisual(nombre: string): IdentificadorDeCategoriaVisual {
  const normalizado = nombre.toLowerCase();
  if (normalizado.includes('hogar')) {
    return 'hogar';
  }
  if (normalizado.includes('construcc')) {
    return 'construccion';
  }
  if (normalizado.includes('transporte') || normalizado.includes('mudanza')) {
    return 'transporte';
  }
  if (normalizado.includes('tecnolog')) {
    return 'tecnologia';
  }
  if (normalizado.includes('belleza') || normalizado.includes('cuidado')) {
    return 'belleza';
  }
  if (normalizado.includes('evento')) {
    return 'eventos';
  }
  if (normalizado.includes('educaci') || normalizado.includes('tutoria')) {
    return 'educacion';
  }
  return 'mas';
}

export function IconoDeCategoria({ nombreCategoria }: { nombreCategoria: string }) {
  const Icono = ICONOS[identificadorDeCategoriaVisual(nombreCategoria)];
  return <Icono />;
}
