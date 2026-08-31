import { useState, type ReactNode, type SVGProps } from 'react';
import { Link } from 'react-router';

import logoHorizontalBlanco from '../../../assets/logos/moica-horizontal-blanco.png';
import estilos from './PieDePagina.module.css';

const COLUMNAS_DE_ENLACES = [
  {
    titulo: 'Moica',
    enlaces: [
      { etiqueta: 'Sobre nosotros', destino: '#sobre-moica' },
      { etiqueta: 'Cómo funciona', destino: '#como-funciona' },
      { etiqueta: 'Trabaja con nosotros', destino: '#trabaja-con-nosotros' },
      { etiqueta: 'Blog', destino: '#blog' },
    ],
  },
  {
    titulo: 'Ayuda',
    enlaces: [
      { etiqueta: 'Centro de ayuda', destino: '#centro-de-ayuda' },
      { etiqueta: 'Seguridad', destino: '#seguridad' },
      { etiqueta: 'Términos y condiciones', destino: '#terminos' },
      { etiqueta: 'Privacidad', destino: '#privacidad' },
    ],
  },
  {
    titulo: 'Comunidad',
    enlaces: [
      { etiqueta: 'Para empresas', destino: '#para-empresas' },
      { etiqueta: 'Conviértete en proveedor', destino: '#conviertete-en-proveedor' },
      { etiqueta: 'Recomendaciones', destino: '#recomendaciones' },
      { etiqueta: 'Eventos', destino: '#eventos' },
    ],
  },
] as const;

const REDES = [
  { nombre: 'Facebook', destino: 'https://www.facebook.com/' },
  { nombre: 'Instagram', destino: 'https://www.instagram.com/' },
  { nombre: 'YouTube', destino: 'https://www.youtube.com/' },
] as const;

/**
 * Pie de página institucional de Moica.
 *
 * En teléfono las columnas de enlaces se apilan en un acordeón nativo
 * (`details`/`summary`). Desde 48rem se despliegan en una cuadrícula horizontal
 * junto al lockup de marca.
 */
export function PieDePagina() {
  return (
    <footer className={estilos.pie}>
      <div className={estilos.interior}>
        <div className={estilos.superior}>
          <div className={estilos.lockup}>
            <Link className={estilos.marca} to="/" aria-label="Moica, ir al inicio">
              <img className={estilos.logotipo} src={logoHorizontalBlanco} alt="" />
            </Link>
            <p className={estilos.lema}>
              La confianza se construye entre todos. Únete a la comunidad.
            </p>
          </div>

          <nav className={estilos.columnas} aria-label="Enlaces institucionales">
            {COLUMNAS_DE_ENLACES.map((columna) => (
              <ColumnaAcordeon key={columna.titulo} titulo={columna.titulo}>
                <ul className={estilos.lista}>
                  {columna.enlaces.map((enlace) => (
                    <li key={enlace.etiqueta}>
                      <a className={estilos.enlace} href={enlace.destino}>
                        {enlace.etiqueta}
                      </a>
                    </li>
                  ))}
                </ul>
              </ColumnaAcordeon>
            ))}

            <ColumnaAcordeon titulo="Síguenos">
              <ul className={estilos.redes}>
                {REDES.map((red) => (
                  <li key={red.nombre}>
                    <a
                      className={estilos.redSocial}
                      href={red.destino}
                      target="_blank"
                      rel="noreferrer noopener"
                      aria-label={red.nombre}
                    >
                      <IconoDeRed nombre={red.nombre} />
                    </a>
                  </li>
                ))}
              </ul>
            </ColumnaAcordeon>
          </nav>
        </div>

        <hr className={estilos.divisor} />

        <p className={estilos.derechos}>© 2026 Moica. Todos los derechos reservados.</p>
      </div>
    </footer>
  );
}

function ColumnaAcordeon({ titulo, children }: { titulo: string; children: ReactNode }) {
  const [abierta, setAbierta] = useState(true);

  return (
    <details
      className={estilos.columna}
      open={abierta}
      onToggle={(evento) => {
        setAbierta(evento.currentTarget.open);
      }}
    >
      <summary className={estilos.titulo}>{titulo}</summary>
      {children}
    </details>
  );
}

function IconoDeRed({ nombre }: { nombre: (typeof REDES)[number]['nombre'] }) {
  if (nombre === 'Facebook') {
    return <IconoFacebook />;
  }
  if (nombre === 'Instagram') {
    return <IconoInstagram />;
  }
  return <IconoYouTube />;
}

type PropiedadesDeIcono = SVGProps<SVGSVGElement>;

function TrazoSocial({ children, ...rest }: PropiedadesDeIcono) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {children}
    </svg>
  );
}

function IconoFacebook() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M14 8h3V4h-3c-2.8 0-5 2.2-5 5v3H6v4h3v8h4v-8h3l1-4h-4V9c0-.6.4-1 1-1Z" />
    </svg>
  );
}

function IconoInstagram() {
  return (
    <TrazoSocial>
      <rect x="3" y="3" width="18" height="18" rx="5" />
      <circle cx="12" cy="12" r="4" />
      <circle cx="17.5" cy="6.5" r="0.8" fill="currentColor" stroke="none" />
    </TrazoSocial>
  );
}

function IconoYouTube() {
  return (
    <TrazoSocial>
      <path d="M22.5 7.2a3 3 0 0 0-2.1-2.1C18.6 4.7 12 4.7 12 4.7s-6.6 0-8.4.4A3 3 0 0 0 1.5 7.2 31 31 0 0 0 1.1 12a31 31 0 0 0 .4 4.8 3 3 0 0 0 2.1 2.1c1.8.4 8.4.4 8.4.4s6.6 0 8.4-.4a3 3 0 0 0 2.1-2.1 31 31 0 0 0 .4-4.8 31 31 0 0 0-.4-4.8Z" />
      <path d="m10 15.2 5.2-3.2L10 8.8v6.4Z" fill="currentColor" stroke="none" />
    </TrazoSocial>
  );
}
