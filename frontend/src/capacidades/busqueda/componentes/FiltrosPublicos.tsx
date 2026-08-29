import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { useCategoriasPublicas, useDepartamentosPublicos } from '../hooks/useBusquedaPublica';
import type { FiltrosDeBusqueda } from '../tipos';
import propios from './filtros.module.css';

/**
 * Filtros del descubrimiento: texto, categoría o subcategoría y municipio.
 *
 * Se aplican al enviar el formulario para que cada combinación sea una consulta explícita.
 */
export default function FiltrosPublicos({
  filtros,
  onCambiar,
  onAplicar,
  onLimpiar,
}: {
  filtros: FiltrosDeBusqueda;
  onCambiar: (filtros: FiltrosDeBusqueda) => void;
  onAplicar: () => void;
  onLimpiar: () => void;
}) {
  const categorias = useCategoriasPublicas();
  const departamentos = useDepartamentosPublicos();
  const subcategorias =
    filtros.idCategoria === ''
      ? (categorias.data ?? []).flatMap((categoria) =>
          categoria.subcategorias.map((subcategoria) => ({
            ...subcategoria,
            nombreCategoria: categoria.nombre,
          }))
        )
      : ((categorias.data ?? [])
          .find((categoria) => String(categoria.idCategoriaServicio) === filtros.idCategoria)
          ?.subcategorias.map((subcategoria) => ({
            ...subcategoria,
            nombreCategoria: undefined as string | undefined,
          })) ?? []);

  return (
    <form
      className={propios.formulario}
      onSubmit={(evento) => {
        evento.preventDefault();
        onAplicar();
      }}
    >
      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="filtro-texto">
          Qué buscas
        </label>
        <input
          id="filtro-texto"
          className={claseDeEntrada(false)}
          type="search"
          value={filtros.texto}
          onChange={(evento) => onCambiar({ ...filtros, texto: evento.target.value })}
        />
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="filtro-categoria">
          Categoría
        </label>
        <select
          id="filtro-categoria"
          className={claseDeEntrada(false)}
          value={filtros.idCategoria}
          onChange={(evento) =>
            onCambiar({ ...filtros, idCategoria: evento.target.value, idSubcategoria: '' })
          }
        >
          <option value="">Todas</option>
          {(categorias.data ?? []).map((categoria) => (
            <option key={categoria.idCategoriaServicio} value={categoria.idCategoriaServicio}>
              {categoria.nombre}
            </option>
          ))}
        </select>
        <p className={estilos.pista}>
          Tres categorías de demostración. No es una taxonomía exhaustiva.
        </p>
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="filtro-subcategoria">
          Subcategoría
        </label>
        <select
          id="filtro-subcategoria"
          className={claseDeEntrada(false)}
          value={filtros.idSubcategoria}
          onChange={(evento) => onCambiar({ ...filtros, idSubcategoria: evento.target.value })}
        >
          <option value="">Todas</option>
          {subcategorias.map((subcategoria) => (
            <option
              key={subcategoria.idSubcategoriaServicio}
              value={subcategoria.idSubcategoriaServicio}
            >
              {subcategoria.nombreCategoria === undefined
                ? subcategoria.nombre
                : `${subcategoria.nombreCategoria}: ${subcategoria.nombre}`}
            </option>
          ))}
        </select>
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="filtro-municipio">
          Municipio
        </label>
        <select
          id="filtro-municipio"
          className={claseDeEntrada(false)}
          value={filtros.idMunicipio}
          onChange={(evento) => onCambiar({ ...filtros, idMunicipio: evento.target.value })}
        >
          <option value="">Todos</option>
          {(departamentos.data ?? []).flatMap((departamento) =>
            departamento.municipios.map((municipio) => (
              <option key={municipio.idMunicipio} value={municipio.idMunicipio}>
                {departamento.nombre}: {municipio.nombre}
              </option>
            ))
          )}
        </select>
      </div>

      <div className={propios.acciones}>
        <button className={estilos.boton} type="submit">
          Buscar
        </button>
        <button className={secciones.botonSecundario} type="button" onClick={onLimpiar}>
          Quitar filtros
        </button>
      </div>
    </form>
  );
}
