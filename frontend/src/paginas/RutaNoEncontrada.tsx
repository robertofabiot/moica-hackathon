import { useNavigate } from 'react-router';

import { Boton, IconoCasa } from '../comun/componentes/ui';
import estilos from './RutaNoEncontrada.module.css';

/**
 * Respuesta a una dirección que no existe.
 *
 * El 404 deja un hueco central para una ilustración posterior; la salida
 * vuelve al inicio con el botón principal, no con un enlace de texto.
 */
export default function RutaNoEncontrada() {
  const navegar = useNavigate();

  return (
    <main className={estilos.contenedor}>
      <p className={estilos.marca}>MOICA</p>

      <div className={estilos.contenido}>
        <h1 className={estilos.codigo} aria-label="404">
          <span aria-hidden="true">4</span>
          <span className={estilos.hueco} aria-hidden="true" />
          <span aria-hidden="true">4</span>
        </h1>

        <h2 className={estilos.titulo}>Ups, parece que nos desconectamos</h2>
        <p className={estilos.subtitulo}>
          La página que buscas no está disponible, pero todavía hay muchas conexiones por encontrar.
        </p>

        <Boton forma="pildora" onClick={() => navegar('/')}>
          <IconoCasa />
          Volver a explorar
        </Boton>
      </div>
    </main>
  );
}
