import estilos from './RutaNoEncontrada.module.css';

/**
 * Escena del 404: el mosaico de Moica ocupa el lugar del 0.
 *
 * El sendero, la persona y el pin no existen como vector en el repo; se
 * dibujan aquí para seguir el mockup hasta que llegue la ilustración original.
 */
export function IlustracionDeRutaNoEncontrada() {
  return (
    <svg
      className={estilos.dibujo}
      viewBox="0 0 720 300"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <ellipse className={estilos.sombra} cx="360" cy="278" rx="230" ry="14" />

      <path className={estilos.sendero} d="M70 232 C 108 208 88 158 128 164 S 168 214 208 198" />
      <g transform="translate(58 236)">
        <circle className={estilos.marcador} r="11" />
        <path className={estilos.cruz} d="M-4.5 -4.5 4.5 4.5 M4.5 -4.5 -4.5 4.5" />
      </g>

      <g className={estilos.chispaTeal} strokeLinecap="round">
        <line x1="188" y1="36" x2="188" y2="50" />
        <line x1="181" y1="43" x2="195" y2="43" />
      </g>
      <g className={estilos.chispaNaranja} strokeLinecap="round">
        <line x1="528" y1="34" x2="528" y2="48" />
        <line x1="521" y1="41" x2="535" y2="41" />
      </g>

      <text className={estilos.cifraTeal} x="78" y="228">
        4
      </text>
      <image href="/logotipo-mosaico.png" x="250" y="48" width="172" height="172" />
      <text className={estilos.cifraNaranja} x="430" y="228">
        4
      </text>

      <g transform="translate(548 198)">
        <circle className={estilos.cabello} cx="46" cy="5" r="8" />
        <circle className={estilos.cabello} cx="36" cy="20" r="13" />
        <circle className={estilos.rostro} cx="32" cy="22" r="8" />
        <circle className={estilos.cabello} cx="29" cy="20" r="1.6" />
        <rect
          className={estilos.camisa}
          x="22"
          y="32"
          width="24"
          height="30"
          rx="10"
          transform="rotate(-16 34 47)"
        />
        <rect className={estilos.pantalon} x="6" y="56" width="36" height="13" rx="6" />
        <rect className={estilos.pantalon} x="2" y="62" width="13" height="24" rx="6" />
        <path className={estilos.brazo} d="M44 44 C 58 50 62 66 54 78" />
      </g>

      <g transform="translate(598 155)">
        <ellipse className={estilos.camisa} cx="16" cy="14" rx="16" ry="13" />
        <circle className={estilos.camisa} cx="7" cy="28" r="3.5" />
        <text className={estilos.interrogacion} x="16" y="20">
          ?
        </text>
      </g>

      <g transform="translate(662 228)">
        <path
          className={estilos.pin}
          d="M0 -26 C11 -26 16 -15 16 -10 C16 1 0 20 0 20 C0 20 -16 1 -16 -10 C-16 -15 -11 -26 0 -26 Z"
        />
        <circle className={estilos.ojoDelPin} cx="0" cy="-12" r="5.5" />
      </g>
    </svg>
  );
}
