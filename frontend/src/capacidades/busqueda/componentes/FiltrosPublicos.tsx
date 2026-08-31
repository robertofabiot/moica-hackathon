import {
  IconoBelleza,
  IconoConstruccion,
  IconoEducacion,
  IconoEventos,
  IconoHogar,
  IconoMasCategorias,
  IconoTecnologia,
  IconoTransporte,
} from '../../../comun/componentes/ui';
import { useCategoriasPublicas, useDepartamentosPublicos } from '../hooks/useBusquedaPublica';
import type { CategoriaPublica, FiltrosDeBusqueda } from '../tipos';
import propios from './filtros.module.css';

const CATEGORIAS_VISUALES = [
  { clave: 'hogar', etiqueta: 'Hogar', Icono: IconoHogar, patron: /hogar/i },
  {
    clave: 'construccion',
    etiqueta: 'Construcción',
    Icono: IconoConstruccion,
    patron: /construcc/i,
  },
  {
    clave: 'transporte',
    etiqueta: 'Transporte',
    Icono: IconoTransporte,
    patron: /transporte|mudanza/i,
  },
  { clave: 'tecnologia', etiqueta: 'Tecnología', Icono: IconoTecnologia, patron: /tecnolog/i },
  { clave: 'belleza', etiqueta: 'Belleza', Icono: IconoBelleza, patron: /belleza|cuidado/i },
  { clave: 'eventos', etiqueta: 'Eventos', Icono: IconoEventos, patron: /evento/i },
  { clave: 'educacion', etiqueta: 'Educación', Icono: IconoEducacion, patron: /educaci|tutoria/i },
  { clave: 'mas', etiqueta: 'Más categorías', Icono: IconoMasCategorias, patron: null },
] as const;

const RANGOS_DE_PRECIO = [
  { valor: 'cualquiera', etiqueta: 'Cualquiera' },
  { valor: '200', etiqueta: 'Hasta C$200' },
  { valor: '500', etiqueta: 'Hasta C$500' },
  { valor: '1000', etiqueta: 'Hasta C$1.000' },
] as const;

/**
 * Categorías y filtros del descubrimiento público.
 *
 * El texto de búsqueda vive en la barra superior. Aquí se elige oficio, zona y
 * un rango de precio de maqueta. Categoría y municipio se aplican al confirmar
 * la búsqueda para no disparar una consulta por cada toque.
 */
export default function FiltrosPublicos({
  filtros,
  onCambiar,
  onAplicar,
  onLimpiar,
}: {
  filtros: FiltrosDeBusqueda;
  onCambiar: (filtros: FiltrosDeBusqueda) => void;
  onAplicar: (siguientes?: FiltrosDeBusqueda) => void;
  onLimpiar: () => void;
}) {
  const categorias = useCategoriasPublicas();
  const departamentos = useDepartamentosPublicos();
  const catalogo = categorias.data ?? [];

  function elegirCategoria(clave: (typeof CATEGORIAS_VISUALES)[number]['clave']) {
    const siguientes: FiltrosDeBusqueda = {
      ...filtros,
      idCategoria: clave === 'mas' ? '' : idCatalogoDe(clave, catalogo),
      idSubcategoria: '',
    };
    onCambiar(siguientes);
  }

  function verTodas() {
    const siguientes: FiltrosDeBusqueda = { ...filtros, idCategoria: '', idSubcategoria: '' };
    onCambiar(siguientes);
    onAplicar(siguientes);
  }

  return (
    <aside className={propios.barra} aria-label="Categorías y filtros">
      <section className={propios.seccion} aria-labelledby="titulo-categorias-explorar">
        <div className={propios.encabezadoDeSeccion}>
          <h2 className={propios.tituloDeSeccion} id="titulo-categorias-explorar">
            Categorías
          </h2>
          <button className={propios.verTodas} type="button" onClick={verTodas}>
            Ver todas
          </button>
        </div>

        <ul className={propios.listaDeCategorias}>
          {CATEGORIAS_VISUALES.map((categoria) => {
            const activa = categoriaEstaActiva(categoria.clave, filtros.idCategoria, catalogo);
            return (
              <li key={categoria.clave}>
                <button
                  type="button"
                  className={unirClases(
                    propios.botonCategoria,
                    activa ? propios.botonCategoriaActivo : undefined
                  )}
                  aria-pressed={activa}
                  onClick={() => elegirCategoria(categoria.clave)}
                >
                  <span className={propios.iconoDeCategoria}>
                    <categoria.Icono />
                  </span>
                  {categoria.etiqueta}
                </button>
              </li>
            );
          })}
        </ul>
      </section>

      <section className={propios.seccion} aria-labelledby="titulo-filtros-explorar">
        <h2 className={propios.tituloDeSeccion} id="titulo-filtros-explorar">
          Filtros
        </h2>

        <div className={propios.campo}>
          <label className={propios.etiqueta} htmlFor="filtro-municipio">
            Ubicación
          </label>
          <select
            id="filtro-municipio"
            className={propios.desplegable}
            value={filtros.idMunicipio}
            onChange={(evento) => onCambiar({ ...filtros, idMunicipio: evento.target.value })}
          >
            <option value="">Managua, NIC</option>
            {(departamentos.data ?? []).flatMap((departamento) =>
              departamento.municipios.map((municipio) => (
                <option key={municipio.idMunicipio} value={municipio.idMunicipio}>
                  {municipio.nombre}, NIC
                </option>
              ))
            )}
          </select>
        </div>

        <div className={propios.campo}>
          <label className={propios.etiqueta} htmlFor="filtro-precio">
            Precio
          </label>
          <select id="filtro-precio" className={propios.desplegable} defaultValue="cualquiera">
            {RANGOS_DE_PRECIO.map((rango) => (
              <option key={rango.valor} value={rango.valor}>
                {rango.etiqueta}
              </option>
            ))}
          </select>
        </div>

        <button className={propios.quitar} type="button" onClick={onLimpiar}>
          Quitar filtros
        </button>
      </section>
    </aside>
  );
}

function idCatalogoDe(
  clave: (typeof CATEGORIAS_VISUALES)[number]['clave'],
  catalogo: CategoriaPublica[]
): string {
  const definicion = CATEGORIAS_VISUALES.find((categoria) => categoria.clave === clave);
  if (definicion === undefined || definicion.patron === null) {
    return '';
  }
  const coincidencia = catalogo.find((categoria) => definicion.patron.test(categoria.nombre));
  return coincidencia === undefined ? '' : String(coincidencia.idCategoriaServicio);
}

function categoriaEstaActiva(
  clave: (typeof CATEGORIAS_VISUALES)[number]['clave'],
  idCategoria: string,
  catalogo: CategoriaPublica[]
): boolean {
  if (clave === 'mas' || idCategoria === '') {
    return false;
  }
  return idCatalogoDe(clave, catalogo) === idCategoria;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
