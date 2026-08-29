import estilos from './RutaNoEncontrada.module.css';

type Propiedades = {
  icono: string;
  persona: string;
  pregunta: string;
};

/**
 * El 404: cifras, icono de marca y las ilustraciones oficiales encima.
 *
 * La persona y el signo van en absoluto respecto a los números para que
 * descansen sobre el 4 naranja, como en el mockup.
 */
export function IlustracionDeRutaNoEncontrada({ icono, persona, pregunta }: Propiedades) {
  return (
    <h1 className={estilos.escena}>
      <span className={estilos.soloLectura}>404</span>
      <DecoracionDeEscena />
      <div className={estilos.numeros} aria-hidden="true">
        <span className={estilos.cuatroTeal}>4</span>
        <img className={estilos.iconoCentral} src={icono} alt="" />
        <span className={estilos.grupoNaranja}>
          <span className={estilos.cuatroNaranja}>4</span>
          <img className={estilos.persona} src={persona} alt="" />
          <img className={estilos.pregunta} src={pregunta} alt="" />
        </span>
      </div>
    </h1>
  );
}

function DecoracionDeEscena() {
  return (
    <svg className={estilos.decoracion} viewBox="0 0 720 300" aria-hidden="true">
      <ellipse className={estilos.sombra} cx="360" cy="286" rx="230" ry="12" />
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
      <g transform="translate(698 168)">
        <path
          className={estilos.pin}
          d="M0 -26 C11 -26 16 -15 16 -10 C16 1 0 20 0 20 C0 20 -16 1 -16 -10 C-16 -15 -11 -26 0 -26 Z"
        />
        <circle className={estilos.ojoDelPin} cx="0" cy="-12" r="5.5" />
      </g>
    </svg>
  );
}
