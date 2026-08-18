import { Link } from 'react-router';

import estilos from './RutaNoEncontrada.module.css';

/**
 * Respuesta a una direccion que no existe.
 *
 * Explica lo ocurrido y ofrece una salida, en lugar de dejar la pantalla en
 * blanco.
 */
export default function RutaNoEncontrada() {
  return (
    <main className={estilos.contenedor}>
      <h1 className={estilos.titulo}>Página no encontrada</h1>
      <p className={estilos.explicacion}>
        La dirección que abriste no existe en Moica o dejó de estar disponible.
      </p>
      <Link className={estilos.enlace} to="/">
        Volver al inicio
      </Link>
    </main>
  );
}
