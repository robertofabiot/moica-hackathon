import { useId } from 'react';

import estilos from './AccesoNoAutorizado.module.css';

/**
 * Escena vectorial de la pantalla de acceso denegado.
 *
 * Misma familia visual que el 404: sombra en el suelo, senderos punteados y
 * destellos en cruz, con un medallón de seguridad en lugar de las cifras.
 */
export function IlustracionDeAccesoDenegado() {
  const identificador = useId().replace(/:/g, '');
  const halo = `${identificador}-halo`;

  return (
    <svg className={estilos.ilustracion} viewBox="0 0 320 220" aria-hidden="true" focusable="false">
      <defs>
        <radialGradient id={halo} cx="160" cy="112" r="96" gradientUnits="userSpaceOnUse">
          <stop offset="0%" className={estilos.haloInicio} />
          <stop offset="100%" className={estilos.haloFin} />
        </radialGradient>
      </defs>

      <circle cx="160" cy="112" r="96" fill={`url(#${halo})`} />
      <ellipse className={estilos.sombra} cx="160" cy="204" rx="102" ry="10" />

      <path className={estilos.senderoTeal} d="M36 156 C 62 118 78 86 108 78 S 138 96 148 112" />
      <path
        className={estilos.senderoNaranja}
        d="M284 156 C 258 118 242 86 212 78 S 182 96 172 112"
      />

      <Destello clase={estilos.destelloTeal} x={54} y={58} tamano={8} />
      <Destello clase={estilos.destelloNaranja} x={268} y={52} tamano={8} />
      <Destello clase={estilos.destelloNaranja} x={78} y={168} tamano={5} />
      <Destello clase={estilos.destelloTeal} x={246} y={172} tamano={5} />

      <circle className={estilos.puntoNaranja} cx="70" cy="108" r="3.5" />
      <circle className={estilos.puntoTeal} cx="250" cy="104" r="3.5" />

      <circle className={estilos.medallonAro} cx="160" cy="112" r="68" />

      <path
        className={estilos.escudo}
        d="M160 58 C 178 66 198 72 206 74 L 206 122 C 206 150 184 168 160 180 C 136 168 114 150 114 122 L 114 74 C 122 72 142 66 160 58 Z"
      />

      <path className={estilos.aroDelCandado} d="M149 110 C 149 92 171 92 171 110" />
      <rect className={estilos.cuerpoDelCandado} x="144" y="108" width="32" height="24" rx="5" />
      <circle className={estilos.ojoDelCandado} cx="160" cy="116" r="3.25" />
      <rect className={estilos.ojoDelCandado} x="158.4" y="118" width="3.2" height="7" rx="1.2" />
    </svg>
  );
}

function Destello({
  x,
  y,
  clase,
  tamano,
}: {
  x: number;
  y: number;
  clase?: string;
  tamano: number;
}) {
  return (
    <g className={clase} transform={`translate(${x} ${y})`} strokeLinecap="round">
      <line x1={0} y1={-tamano} x2={0} y2={tamano} />
      <line x1={-tamano} y1={0} x2={tamano} y2={0} />
    </g>
  );
}
