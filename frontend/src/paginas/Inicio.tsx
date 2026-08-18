import estilos from './Inicio.module.css';

/**
 * Pantalla base de Moica.
 *
 * Presenta la marca y confirma que el renderizado funciona. Las pantallas
 * reales del producto llegan con sus incrementos.
 */
export default function Inicio() {
  return (
    <main className={estilos.contenedor}>
      <img
        className={estilos.logotipo}
        src="/icono-192.png"
        alt="Logotipo de Moica"
        width={96}
        height={96}
      />
      <h1 className={estilos.titulo}>Moica</h1>
      <p className={estilos.lema}>La confianza se construye entre todos</p>
    </main>
  );
}
