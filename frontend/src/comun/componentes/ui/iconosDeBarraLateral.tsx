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

function Campana() {
  return (
    <Trazo>
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </Trazo>
  );
}

function Casa() {
  return (
    <Trazo>
      <path d="M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8" />
      <path d="M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
    </Trazo>
  );
}

function Calendario() {
  return (
    <Trazo>
      <path d="M8 2v4" />
      <path d="M16 2v4" />
      <rect width="18" height="18" x="3" y="4" rx="2" />
      <path d="M3 10h18" />
    </Trazo>
  );
}

function Burbuja() {
  return (
    <Trazo>
      <path d="M7.9 20A9 9 0 1 0 4 16.1L2 22Z" />
    </Trazo>
  );
}

function Usuario() {
  return (
    <Trazo>
      <circle cx="12" cy="8" r="5" />
      <path d="M20 21a8 8 0 0 0-16 0" />
    </Trazo>
  );
}

function Tarjeta() {
  return (
    <Trazo>
      <rect width="20" height="14" x="2" y="5" rx="2" />
      <line x1="2" x2="22" y1="10" y2="10" />
    </Trazo>
  );
}

function Engranaje() {
  return (
    <Trazo>
      <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" />
      <circle cx="12" cy="12" r="3" />
    </Trazo>
  );
}

export type IdentificadorDeItemDeBarraLateral =
  'notificaciones' | 'inicio' | 'calendario' | 'mensajes' | 'perfil' | 'pagos' | 'configuracion';

const ICONOS: Record<IdentificadorDeItemDeBarraLateral, () => ReactElement> = {
  notificaciones: Campana,
  inicio: Casa,
  calendario: Calendario,
  mensajes: Burbuja,
  perfil: Usuario,
  pagos: Tarjeta,
  configuracion: Engranaje,
};

export function IconoDeBarraLateral({
  identificador,
}: {
  identificador: IdentificadorDeItemDeBarraLateral;
}) {
  const Icono = ICONOS[identificador];
  return <Icono />;
}
