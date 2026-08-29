import estilos from './RutaNoEncontrada.module.css';

type Propiedades = {
  icono: string;
  persona: string;
  pregunta: string;
};

/**
 * El 404: cifras, icono de marca y las ilustraciones oficiales encima.
 *
 * La persona y el signo se anclan a la escena, no al dígito, para abrir la
 * composición hacia los lados.
 */
export function IlustracionDeRutaNoEncontrada({ icono, persona, pregunta }: Propiedades) {
  return (
    <h1 className={estilos.escena}>
      <span className={estilos.soloLectura}>404</span>
      <DecoracionDeEscena />
      <div className={estilos.numeros} aria-hidden="true">
        <span className={estilos.cuatroTeal}>4</span>
        <img className={estilos.iconoCentral} src={icono} alt="" />
        <span className={estilos.cuatroNaranja}>4</span>
      </div>
      <img className={estilos.persona} src={persona} alt="" />
      <img className={estilos.pregunta} src={pregunta} alt="" />
    </h1>
  );
}

function DecoracionDeEscena() {
  return (
    <svg className={estilos.decoracion} viewBox="0 0 720 300" aria-hidden="true">
      <ellipse className={estilos.sombra} cx="360" cy="286" rx="230" ry="12" />
      <path className={estilos.sendero} d="M28 248 C 78 214 52 168 108 172 S 158 228 208 210" />
      <g transform="translate(28 252)">
        <circle className={estilos.marcador} r="11" />
        <path className={estilos.cruz} d="M-4.5 -4.5 4.5 4.5 M4.5 -4.5 -4.5 4.5" />
      </g>
      <g className={estilos.destelloTeal} strokeLinecap="round">
        <line x1="148" y1="24" x2="158" y2="38" />
        <line x1="172" y1="20" x2="182" y2="34" />
      </g>
      <g className={estilos.destelloNaranja} strokeLinecap="round">
        <line x1="568" y1="38" x2="578" y2="24" />
        <line x1="590" y1="34" x2="600" y2="20" />
      </g>
      <g transform="translate(692 252)">
        <path
          className={estilos.pin}
          d="M0 -26 C11 -26 16 -15 16 -10 C16 1 0 20 0 20 C0 20 -16 1 -16 -10 C-16 -15 -11 -26 0 -26 Z"
        />
        <circle className={estilos.ojoDelPin} cx="0" cy="-12" r="5.5" />
      </g>
      <path className={estilos.senderoDerecho} d="M540 240 C 570 260 620 285 660 278 S 685 275 692 272" />
    </svg>
  );
}
