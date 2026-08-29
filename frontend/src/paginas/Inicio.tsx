import { Boton, Entrada, IconoLupa, IconoPin } from '../comun/componentes/ui';
import { EncabezadoDeInicio } from './EncabezadoDeInicio';
import estilos from './Inicio.module.css';

const CATEGORIAS_POPULARES = [
  'Hogar',
  'Construcción',
  'Transporte',
  'Tecnología',
  'Eventos',
  'Más',
] as const;

/**
 * Pantalla de aterrizaje de Moica.
 *
 * El encabezado reacciona a la sesión porque el acceso actual sigue viviendo
 * aquí; el resto es la maqueta pública de exploración.
 */
export default function Inicio() {
  return (
    <div className={estilos.pagina}>
      <EncabezadoDeInicio />
      <main>
        <SeccionHero />
        <hr className={estilos.divisorDeSeccion} />
        <SeccionCategorias />
      </main>
    </div>
  );
}

function SeccionHero() {
  return (
    <section className={estilos.hero} aria-labelledby="titulo-hero">
      <h1 id="titulo-hero" className={estilos.tituloHero}>
        Encuentra servicios confiables en tu comunidad
      </h1>
      <p className={estilos.subtituloHero}>
        Conectamos personas con trabajadores independientes y negocios locales.
      </p>

      <form
        className={estilos.barraDeBusqueda}
        role="search"
        onSubmit={(evento) => evento.preventDefault()}
      >
        <div className={estilos.campoDeBarra}>
          <Entrada
            variante="fusionada"
            type="search"
            name="servicio"
            autoComplete="off"
            aria-label="Qué servicio necesitas"
            placeholder="¿Qué servicio necesitas?"
            icono={<IconoLupa />}
          />
        </div>
        <span className={estilos.divisorDeBarra} aria-hidden="true" />
        <div className={estilos.campoDeBarra}>
          <Entrada
            variante="fusionada"
            type="text"
            name="ubicacion"
            autoComplete="off"
            aria-label="Ubicación"
            placeholder="Managua, NIC"
            defaultValue="Managua, NIC"
            icono={<IconoPin />}
          />
        </div>
        <Boton type="submit" forma="pildora">
          Buscar
        </Boton>
      </form>
    </section>
  );
}

function SeccionCategorias() {
  return (
    <section className={estilos.categorias} aria-labelledby="titulo-categorias">
      <h2 id="titulo-categorias" className={estilos.tituloDeCategorias}>
        Categorías populares
      </h2>
      <ul className={estilos.cuadricula}>
        {CATEGORIAS_POPULARES.map((categoria) => (
          <li key={categoria} className={estilos.tarjeta}>
            <span className={estilos.etiquetaDeCategoria}>{categoria}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
