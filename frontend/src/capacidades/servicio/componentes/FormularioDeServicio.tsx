import { zodResolver } from '@hookform/resolvers/zod';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../../../comun/api';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import {
  esquemaDeServicio,
  type CamposDeServicio,
  type DatosValidadosDeServicio,
} from '../esquemas';
import {
  useActualizacionDeServicio,
  useCategoriasDeServicio,
  useCreacionDeServicio,
} from '../hooks/useServiciosPropios';
import type { ServicioPropio } from '../tipos';

/**
 * Formulario para crear o editar un servicio publicado.
 *
 * La categoría solo filtra el segundo selector; lo que se envía es la subcategoría. El precio
 * vacío viaja como nulo: «A convenir» es presentación, no un valor de la API.
 */
export default function FormularioDeServicio({
  servicio,
  alCrear,
}: {
  servicio?: ServicioPropio;
  alCrear?: (creado: ServicioPropio) => void;
}) {
  const categorias = useCategoriasDeServicio();
  const creacion = useCreacionDeServicio();
  const actualizacion = useActualizacionDeServicio();
  const guardado = servicio === undefined ? creacion : actualizacion;

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CamposDeServicio, unknown, DatosValidadosDeServicio>({
    resolver: zodResolver(esquemaDeServicio),
    mode: 'onBlur',
    defaultValues:
      servicio === undefined
        ? { nombre: '', descripcion: '', idSubcategoriaServicio: '', precioReferencia: '' }
        : {
            nombre: servicio.nombre,
            descripcion: servicio.descripcion,
            idSubcategoriaServicio: String(servicio.idSubcategoriaServicio),
            precioReferencia:
              servicio.precioReferencia === null ? '' : String(servicio.precioReferencia),
          },
  });

  const [idCategoria, setIdCategoria] = useState(
    servicio === undefined ? '' : String(servicio.idCategoriaServicio)
  );

  const subcategorias = useMemo(() => {
    const lista = categorias.data ?? [];
    if (idCategoria === '') {
      return lista.flatMap((categoria) => categoria.subcategorias);
    }
    return (
      lista.find((categoria) => String(categoria.idCategoriaServicio) === idCategoria)
        ?.subcategorias ?? []
    );
  }, [categorias.data, idCategoria]);

  const enviar = handleSubmit((campos) => {
    const datos = {
      nombre: campos.nombre,
      descripcion: campos.descripcion,
      idSubcategoriaServicio: campos.idSubcategoriaServicio,
      precioReferencia: campos.precioReferencia,
    };
    if (servicio === undefined) {
      creacion.mutate(datos, {
        onSuccess: (creado) => alCrear?.(creado),
        onError: anotarErrores,
      });
      return;
    }
    actualizacion.mutate(
      { idServicio: servicio.idServicioPublicado, datos },
      { onError: anotarErrores }
    );

    function anotarErrores(fallo: Error) {
      if (fallo instanceof ErrorDeApi) {
        fallo.errores.forEach((error) => {
          if (esCampoDelFormulario(error.campo)) {
            setError(error.campo, { message: error.mensaje });
          }
        });
      }
    }
  });

  const fallo = guardado.error;
  const mensajeGeneral =
    fallo instanceof ErrorDeApi && fallo.errores.length === 0 ? fallo.message : null;

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-datos-del-servicio">
      <h2 className={secciones.tituloDeSeccion} id="titulo-datos-del-servicio">
        {servicio === undefined ? 'Nuevo servicio' : 'Datos del servicio'}
      </h2>

      {mensajeGeneral !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeGeneral}
        </p>
      )}

      {guardado.isSuccess && servicio !== undefined && (
        <p className={estilos.aviso} role="status">
          Los cambios se guardaron.
        </p>
      )}

      <form className={estilos.formulario} onSubmit={enviar} noValidate>
        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="nombre-servicio">
            Nombre
          </label>
          <input
            id="nombre-servicio"
            className={claseDeEntrada(errors.nombre !== undefined)}
            type="text"
            maxLength={150}
            autoComplete="off"
            aria-invalid={errors.nombre !== undefined}
            aria-describedby={errors.nombre ? 'error-nombre-servicio' : undefined}
            {...register('nombre')}
          />
          {errors.nombre && (
            <p className={estilos.error} id="error-nombre-servicio" role="alert">
              {errors.nombre.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="descripcion-servicio">
            Descripción
          </label>
          <textarea
            id="descripcion-servicio"
            className={claseDeEntrada(errors.descripcion !== undefined)}
            rows={6}
            maxLength={3000}
            aria-invalid={errors.descripcion !== undefined}
            aria-describedby={errors.descripcion ? 'error-descripcion-servicio' : undefined}
            {...register('descripcion')}
          />
          {errors.descripcion && (
            <p className={estilos.error} id="error-descripcion-servicio" role="alert">
              {errors.descripcion.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="categoria-servicio">
            Categoría
          </label>
          <select
            id="categoria-servicio"
            className={claseDeEntrada(false)}
            value={idCategoria}
            onChange={(evento) => setIdCategoria(evento.target.value)}
            disabled={categorias.isPending}
          >
            <option value="">Todas las categorías de demostración</option>
            {(categorias.data ?? []).map((categoria) => (
              <option key={categoria.idCategoriaServicio} value={categoria.idCategoriaServicio}>
                {categoria.nombre}
              </option>
            ))}
          </select>
          <p className={estilos.pista}>
            Este listado es de demostración: no pretende cubrir todos los oficios de Managua.
          </p>
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="subcategoria-servicio">
            Subcategoría
          </label>
          <select
            id="subcategoria-servicio"
            className={claseDeEntrada(errors.idSubcategoriaServicio !== undefined)}
            aria-invalid={errors.idSubcategoriaServicio !== undefined}
            aria-describedby={
              errors.idSubcategoriaServicio ? 'error-subcategoria-servicio' : undefined
            }
            {...register('idSubcategoriaServicio')}
          >
            <option value="">Elige una subcategoría</option>
            {subcategorias.map((subcategoria) => (
              <option
                key={subcategoria.idSubcategoriaServicio}
                value={subcategoria.idSubcategoriaServicio}
              >
                {subcategoria.nombre}
              </option>
            ))}
          </select>
          {errors.idSubcategoriaServicio && (
            <p className={estilos.error} id="error-subcategoria-servicio" role="alert">
              {errors.idSubcategoriaServicio.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="precio-servicio">
            Precio de referencia (opcional)
          </label>
          <input
            id="precio-servicio"
            className={claseDeEntrada(errors.precioReferencia !== undefined)}
            type="number"
            inputMode="decimal"
            min="0.01"
            step="0.01"
            aria-invalid={errors.precioReferencia !== undefined}
            aria-describedby={
              errors.precioReferencia ? 'error-precio-servicio' : 'pista-precio-servicio'
            }
            {...register('precioReferencia')}
          />
          <p className={estilos.pista} id="pista-precio-servicio">
            Si lo dejas vacío, en el descubrimiento se mostrará como «A convenir».
          </p>
          {errors.precioReferencia && (
            <p className={estilos.error} id="error-precio-servicio" role="alert">
              {errors.precioReferencia.message}
            </p>
          )}
        </div>

        <button className={estilos.boton} type="submit" disabled={guardado.isPending}>
          {guardado.isPending
            ? 'Guardando…'
            : servicio === undefined
              ? 'Crear servicio'
              : 'Guardar cambios'}
        </button>
      </form>
    </section>
  );
}

function esCampoDelFormulario(
  campo: string
): campo is 'nombre' | 'descripcion' | 'idSubcategoriaServicio' | 'precioReferencia' {
  return (
    campo === 'nombre' ||
    campo === 'descripcion' ||
    campo === 'idSubcategoriaServicio' ||
    campo === 'precioReferencia'
  );
}
