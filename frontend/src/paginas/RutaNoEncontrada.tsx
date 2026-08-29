import { Link, useNavigate } from 'react-router';

import persona404 from '../assets/ilustraciones/ilustracion_persona404.png';
import signoPregunta from '../assets/ilustraciones/signo_pregunta.png';
import iconoMoica from '../assets/logos/moica-icono.svg';
import logoVertical from '../assets/logos/moica-vertical.svg';
import { Boton, IconoCasa } from '../comun/componentes/ui';
import { IlustracionDeRutaNoEncontrada } from './IlustracionDeRutaNoEncontrada';
import estilos from './RutaNoEncontrada.module.css';

/**
 * Respuesta a una dirección que no existe.
 *
 * El lockup vertical va arriba. El 404 usa el icono de marca como «0» y las
 * ilustraciones oficiales se posan sobre los números.
 */
export default function RutaNoEncontrada() {
  const navegar = useNavigate();

  return (
    <main className={estilos.contenedor}>
      <Link className={estilos.marca} to="/" aria-label="Moica, ir al inicio">
        <img className={estilos.logoVertical} src={logoVertical} alt="" />
      </Link>

      <div className={estilos.contenido}>
        <IlustracionDeRutaNoEncontrada
          icono={iconoMoica}
          persona={persona404}
          pregunta={signoPregunta}
        />

        <h2 className={estilos.titulo}>Ups, parece que nos desconectamos</h2>
        <p className={estilos.subtitulo}>
          La página que buscas no está disponible,
          <br />
          pero todavía hay muchas conexiones por encontrar.
        </p>

        <Boton className={estilos.accion} forma="pildora" onClick={() => navegar('/')}>
          <IconoCasa />
          Volver a explorar
        </Boton>
      </div>
    </main>
  );
}
