import estilos from './App.module.css';

/**
 * Raiz de la aplicacion.
 *
 * P1 solo necesita comprobar que React, TypeScript y los estilos funcionan.
 * La navegacion y las pantallas reales llegan con sus propios incrementos.
 */
export default function App() {
  return (
    <main className={estilos.contenedor}>
      <img className={estilos.logotipo} src="/icono-192.png" alt="Logotipo de Moica" width={96} height={96} />
      <h1 className={estilos.titulo}>Moica</h1>
      <p className={estilos.lema}>La confianza se construye entre todos</p>
    </main>
  );
}
