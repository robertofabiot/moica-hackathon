import { Link, useNavigate } from 'react-router';

import persona404 from '../assets/ilustraciones/ilustracion_persona404.png';
import signoPregunta from '../assets/ilustraciones/signo_pregunta.png';
import logoHorizontal from '../assets/logos/moica-horizontal.png';
import iconoMoica from '../assets/logos/moica-icono.svg';
import { Boton, IconoCasa } from '../comun/componentes/ui';
import { IlustracionDeRutaNoEncontrada } from './IlustracionDeRutaNoEncontrada';
import estilos from './RutaNoEncontrada.module.css';

/**
 * Respuesta a una dirección que no existe.
 *
 * El lockup horizontal (PNG de marca) va arriba. El 404 usa el icono como
 * «0» y las ilustraciones se abren hacia los lados de los números.
 */
export default function RutaNoEncontrada() {
  const navegar = useNavigate();

  return (
    <main className={estilos.contenedor}>
      <Link className={estilos.marca} to="/" aria-label="Moica, ir al inicio">
        <img className={estilos.logoHorizontal} src={logoHorizontal} alt="" />
      </Link>

      <IlustracionDeRutaNoEncontrada
        icono={iconoMoica}
        persona={persona404}
        pregunta={signoPregunta}
      />

      <div className={estilos.contenido}>
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
