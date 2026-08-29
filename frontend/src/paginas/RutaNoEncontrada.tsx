import { Link, useNavigate } from 'react-router';

import { Boton, IconoCasa } from '../comun/componentes/ui';
import { IlustracionDeRutaNoEncontrada } from './IlustracionDeRutaNoEncontrada';
import estilos from './RutaNoEncontrada.module.css';

/**
 * Respuesta a una dirección que no existe.
 *
 * El mosaico de marca hace de «0» en el 404. El sendero, la persona y el pin
 * reconstruyen el mockup: no hay un vector original de esa ilustración en el
 * repositorio.
 */
export default function RutaNoEncontrada() {
  const navegar = useNavigate();

  return (
    <main className={estilos.contenedor}>
      <Link className={estilos.marca} to="/" aria-label="Moica, ir al inicio">
        <img
          className={estilos.iconoDeMarca}
          src="/logotipo-mosaico.png"
          alt=""
          width={40}
          height={40}
        />
        <span className={estilos.nombreDeMarca}>MOICA</span>
      </Link>

      <div className={estilos.contenido}>
        <h1 className={estilos.escena}>
          <span className={estilos.soloLectura}>404</span>
          <IlustracionDeRutaNoEncontrada />
        </h1>

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
