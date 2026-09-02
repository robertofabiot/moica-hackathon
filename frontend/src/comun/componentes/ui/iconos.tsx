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

export function IconoLupa(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <circle cx="11" cy="11" r="8" />
      <path d="m21 21-4.3-4.3" />
    </Trazo>
  );
}

export function IconoPin(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z" />
      <circle cx="12" cy="10" r="3" />
    </Trazo>
  );
}

export function IconoCasa(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
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

/** Silueta de persona: años de experiencia o avatar de respaldo. */
export function IconoUsuario(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <circle cx="12" cy="8" r="5" />
      <path d="M20 21a8 8 0 0 0-16 0" />
    </Trazo>
  );
}

/** Maletín: servicios publicados o trabajos de mantenimiento. */
export function IconoMaletin(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <rect width="20" height="14" x="2" y="7" rx="2" />
      <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
    </Trazo>
  );
}

/** Pulgar arriba: clientes satisfechos. */
export function IconoPulgarArriba(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="M7 10v12" />
      <path d="M15 5.88 14 10h5.83a2 2 0 0 1 1.92 2.56l-2.33 8A2 2 0 0 1 17.5 22H4a2 2 0 0 1-2-2v-8a2 2 0 0 1 2-2h2.76a2 2 0 0 0 1.79-1.11L12 2a3.13 3.13 0 0 1 3 3.88Z" />
    </Trazo>
  );
}

/** Llave inglesa: reparaciones. */
export function IconoHerramienta(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
    </Trazo>
  );
}

/** Reloj: disponibilidad o atención de emergencia. */
export function IconoReloj(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <circle cx="12" cy="12" r="10" />
      <path d="M12 6v6l4 2" />
    </Trazo>
  );
}

/** Flecha hacia la derecha para filas de servicios. */
export function IconoChevronDerecha(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="m9 18 6-6-6-6" />
    </Trazo>
  );
}

/** Campana de notificaciones. */
export function IconoCampana(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </Trazo>
  );
}

/** Flecha hacia una bandeja: subir archivos o fotos. */
export function IconoSubida(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="M12 3v12" />
      <path d="m17 8-5-5-5 5" />
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
    </Trazo>
  );
}

/** Cámara: galería de fotos de un servicio. */
export function IconoCamara(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="M14.5 4h-5L7 7H4a2 2 0 0 0-2 2v9a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-3l-2.5-3z" />
      <circle cx="12" cy="13" r="3" />
    </Trazo>
  );
}

/** Equis: cerrar o quitar elementos. */
export function IconoX(props: PropiedadesDeIcono) {
  return (
    <Trazo {...props}>
      <path d="M18 6 6 18" />
      <path d="m6 6 12 12" />
    </Trazo>
  );
}

