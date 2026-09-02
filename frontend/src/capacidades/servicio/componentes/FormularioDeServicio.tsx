import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { useNavigate } from 'react-router';

import { ErrorDeApi } from '../../../comun/api';
import {
  Boton,
  Entrada,
  IconoCheckCirculo,
  IconoSubida,
  IconoX,
} from '../../../comun/componentes/ui';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { subirImagenDeServicio } from '../api';
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
import propios from '../paginas/servicios.module.css';
import { precioPropio } from '../presentacion';
import { RUTA_SERVICIOS } from '../rutas';
import type { ServicioPropio } from '../tipos';
import IndicadorDePasos, { type PasoDePublicacion } from './IndicadorDePasos';

const MAXIMO_DE_DESCRIPCION = 3000;
const LARGO_DEL_EXTRACTO = 160;

/**
 * Formulario para crear o editar un servicio publicado.
 *
 * La publicación nueva es un asistente de cuatro pasos. La edición sigue en una
 * sola vista: el prestador ya tiene el dato y solo ajusta campos sueltos.
 *
 * La categoría solo filtra el segundo selector; lo que se envía es la subcategoría.
 * El precio vacío viaja como nulo: «A convenir» es presentación, no un valor de la API.
 */
export default function FormularioDeServicio({
  servicio,
  alCrear,
}: {
  servicio?: ServicioPropio;
  alCrear?: (creado: ServicioPropio) => void;
}) {
  if (servicio === undefined) {
    return <AsistenteDeNuevoServicio alCrear={alCrear} />;
  }

  return <FormularioDeEdicionDeServicio servicio={servicio} />;
}

function AsistenteDeNuevoServicio({ alCrear }: { alCrear?: (creado: ServicioPropio) => void }) {
  const navegar = useNavigate();
  const categorias = useCategoriasDeServicio();
  const creacion = useCreacionDeServicio();
  const [paso, setPaso] = useState<PasoDePublicacion>(1);
  const [idCategoria, setIdCategoria] = useState('');
  const [errorDeCategoria, setErrorDeCategoria] = useState<string | undefined>();
  const [fotos, setFotos] = useState<File[]>([]);
  const [errorDeFotos, setErrorDeFotos] = useState<string | undefined>();
  const [arrastrando, setArrastrando] = useState(false);
  const [subiendoFotos, setSubiendoFotos] = useState(false);
  const [servicioCreado, setServicioCreado] = useState<ServicioPropio | null>(null);
  const entradaDeArchivoRef = useRef<HTMLInputElement>(null);

  const urlsPrevia = useMemo(() => fotos.map((f) => URL.createObjectURL(f)), [fotos]);

  useEffect(() => {
    return () => {
      urlsPrevia.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [urlsPrevia]);

  const {
    register,
    handleSubmit,
    setError,
    setValue,
    trigger,
    control,
    formState: { errors },
  } = useForm<CamposDeServicio, unknown, DatosValidadosDeServicio>({
    resolver: zodResolver(esquemaDeServicio),
    mode: 'onBlur',
    defaultValues: {
      nombre: '',
      descripcion: '',
      idSubcategoriaServicio: '',
      precioReferencia: '',
    },
  });

  const nombre = useWatch({ control, name: 'nombre' });
  const descripcion = useWatch({ control, name: 'descripcion' });
  const idSubcategoriaServicio = useWatch({ control, name: 'idSubcategoriaServicio' });
  const precioReferencia = useWatch({ control, name: 'precioReferencia' });

  const subcategorias = useMemo(() => {
    const lista = categorias.data ?? [];
    if (idCategoria === '') {
      return [];
    }
    return (
      lista.find((categoria) => String(categoria.idCategoriaServicio) === idCategoria)
        ?.subcategorias ?? []
    );
  }, [categorias.data, idCategoria]);

  const categoriaElegida = (categorias.data ?? []).find(
    (categoria) => String(categoria.idCategoriaServicio) === idCategoria
  );
  const subcategoriaElegida = subcategorias.find(
    (subcategoria) => String(subcategoria.idSubcategoriaServicio) === idSubcategoriaServicio
  );

  function agregarArchivos(archivos: FileList | null) {
    if (!archivos || archivos.length === 0) {
      return;
    }
    setErrorDeFotos(undefined);
    const formatosValidos = ['image/jpeg', 'image/png', 'image/webp'];
    const nuevos: File[] = [];
    let errorDetectado: string | undefined;

    Array.from(archivos).forEach((archivo) => {
      if (!formatosValidos.includes(archivo.type)) {
        errorDetectado = 'Solo se admiten formatos JPEG, PNG o WebP.';
        return;
      }
      if (archivo.size > 5 * 1024 * 1024) {
        errorDetectado = 'Las imágenes no deben superar los 5 MB.';
        return;
      }
      nuevos.push(archivo);
    });

    if (errorDetectado && nuevos.length === 0) {
      setErrorDeFotos(errorDetectado);
      return;
    }
    if (errorDetectado) {
      setErrorDeFotos(errorDetectado);
    }
    setFotos((actuales) => [...actuales, ...nuevos]);
  }

  function quitarFoto(indice: number) {
    setFotos((actuales) => actuales.filter((_, i) => i !== indice));
  }

  const publicar = handleSubmit((campos) => {
    creacion.mutate(
      {
        nombre: campos.nombre,
        descripcion: campos.descripcion,
        idSubcategoriaServicio: campos.idSubcategoriaServicio,
        precioReferencia: campos.precioReferencia,
      },
      {
        onSuccess: async (creado) => {
          if (fotos.length > 0) {
            setSubiendoFotos(true);
            for (const foto of fotos) {
              try {
                await subirImagenDeServicio(creado.idServicioPublicado, foto, '');
              } catch {
                // Continuar si alguna foto falla para no bloquear el flujo
              }
            }
            setSubiendoFotos(false);
          }
          setServicioCreado(creado);
        },
        onError: anotarErrores,
      }
    );

    function anotarErrores(fallo: Error) {
      if (!(fallo instanceof ErrorDeApi)) {
        return;
      }
      let destino: PasoDePublicacion | undefined;
      fallo.errores.forEach((error) => {
        if (esCampoDelFormulario(error.campo)) {
          setError(error.campo, { message: error.mensaje });
          const pasoDelCampo = pasoDeCampo(error.campo);
          if (destino === undefined || pasoDelCampo < destino) {
            destino = pasoDelCampo;
          }
        }
      });
      if (destino !== undefined) {
        setPaso(destino);
      }
    }
  });

  const fallo = creacion.error;
  const mensajeGeneral =
    fallo instanceof ErrorDeApi && fallo.errores.length === 0 ? fallo.message : null;

  async function avanzar() {
    const valido = await pasoActualEsValido();
    if (!valido) {
      return;
    }
    setPaso((actual) => (actual === 4 ? actual : ((actual + 1) as PasoDePublicacion)));
  }

  function retroceder() {
    setPaso((actual) => (actual === 1 ? actual : ((actual - 1) as PasoDePublicacion)));
  }

  async function pasoActualEsValido(): Promise<boolean> {
    if (paso === 1) {
      const categoriaValida = idCategoria !== '';
      setErrorDeCategoria(categoriaValida ? undefined : 'Elige una categoría.');
      const camposOk = await trigger(['nombre', 'idSubcategoriaServicio']);
      return camposOk && categoriaValida;
    }
    if (paso === 2) {
      return trigger('descripcion');
    }
    if (paso === 3) {
      return trigger('precioReferencia');
    }
    return true;
  }

  function alEnviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (paso !== 4) {
      void avanzar();
      return;
    }
    if (servicioCreado !== null) {
      alCrear?.(servicioCreado);
      return;
    }
    void publicar();
  }

  const registroDeNombre = register('nombre');
  const registroDeDescripcion = register('descripcion');
  const registroDeSubcategoria = register('idSubcategoriaServicio');
  const registroDePrecio = register('precioReferencia');

  return (
    <form className={propios.formularioAsistente} onSubmit={alEnviar} noValidate>
      <IndicadorDePasos pasoActual={paso} />

      {mensajeGeneral !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeGeneral}
        </p>
      )}

      {paso === 1 && (
        <div className={propios.cuerpoDePaso}>
          <h2 className={propios.tituloDePaso}>Información básica</h2>
          <div className={propios.campoAsistente}>
            <label className={propios.etiquetaAsistente} htmlFor="nombre-servicio">
              Título del servicio
            </label>
            <Entrada
              id="nombre-servicio"
              type="text"
              maxLength={150}
              autoComplete="off"
              placeholder="Ej. Pintura de interiores y acabados"
              mensajeDeError={errors.nombre?.message}
              {...registroDeNombre}
            />
          </div>
          <div className={propios.filaDeSelectores}>
            <div className={propios.campoAsistente}>
              <label className={propios.etiquetaAsistente} htmlFor="categoria-servicio">
                Categoría
              </label>
              <select
                id="categoria-servicio"
                className={unirClases(
                  propios.selectAsistente,
                  errorDeCategoria !== undefined ? propios.selectConError : undefined
                )}
                value={idCategoria}
                aria-invalid={errorDeCategoria !== undefined}
                aria-describedby={
                  errorDeCategoria !== undefined
                    ? 'error-categoria-servicio'
                    : 'pista-categoria-servicio'
                }
                onChange={(evento) => {
                  setIdCategoria(evento.target.value);
                  setErrorDeCategoria(undefined);
                  setValue('idSubcategoriaServicio', '');
                }}
                disabled={categorias.isPending}
              >
                <option value="">Elige una categoría</option>
                {(categorias.data ?? []).map((categoria) => (
                  <option key={categoria.idCategoriaServicio} value={categoria.idCategoriaServicio}>
                    {categoria.nombre}
                  </option>
                ))}
              </select>
              <p className={propios.pistaAsistente} id="pista-categoria-servicio">
                Este listado es de demostración: no pretende cubrir todos los oficios de Managua.
              </p>
              {errorDeCategoria !== undefined && (
                <p className={propios.errorAsistente} id="error-categoria-servicio" role="alert">
                  {errorDeCategoria}
                </p>
              )}
            </div>
            <div className={propios.campoAsistente}>
              <label className={propios.etiquetaAsistente} htmlFor="subcategoria-servicio">
                Subcategoría
              </label>
              <select
                id="subcategoria-servicio"
                className={unirClases(
                  propios.selectAsistente,
                  errors.idSubcategoriaServicio !== undefined ? propios.selectConError : undefined
                )}
                aria-invalid={errors.idSubcategoriaServicio !== undefined}
                aria-describedby={
                  errors.idSubcategoriaServicio ? 'error-subcategoria-servicio' : undefined
                }
                disabled={idCategoria === ''}
                {...registroDeSubcategoria}
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
                <p className={propios.errorAsistente} id="error-subcategoria-servicio" role="alert">
                  {errors.idSubcategoriaServicio.message}
                </p>
              )}
            </div>
          </div>
        </div>
      )}

      {paso === 2 && (
        <div className={propios.cuerpoDePaso}>
          <h2 className={propios.tituloDePaso}>Detalles</h2>
          <div className={propios.campoAsistente}>
            <div className={propios.cabeceraDeDescripcion}>
              <label className={propios.etiquetaAsistente} htmlFor="descripcion-servicio">
                Descripción
              </label>
              <span className={propios.contadorDeCaracteres} aria-live="polite">
                {descripcion.length}/{MAXIMO_DE_DESCRIPCION}
              </span>
            </div>
            <textarea
              id="descripcion-servicio"
              className={unirClases(
                propios.textareaAsistente,
                errors.descripcion !== undefined ? propios.textareaConError : undefined
              )}
              rows={4}
              maxLength={MAXIMO_DE_DESCRIPCION}
              aria-invalid={errors.descripcion !== undefined}
              aria-describedby={errors.descripcion ? 'error-descripcion-servicio' : undefined}
              {...registroDeDescripcion}
            />
            {errors.descripcion && (
              <p className={propios.errorAsistente} id="error-descripcion-servicio" role="alert">
                {errors.descripcion.message}
              </p>
            )}
          </div>

          <div className={propios.campoAsistente}>
            <label className={propios.etiquetaAsistente} htmlFor="fotos-servicio">
              Fotos del servicio
            </label>
            <input
              ref={entradaDeArchivoRef}
              id="fotos-servicio"
              type="file"
              multiple
              accept="image/jpeg,image/png,image/webp"
              className={propios.entradaOculta}
              onChange={(evento) => {
                agregarArchivos(evento.target.files);
                evento.target.value = '';
              }}
            />
            <div
              className={unirClases(
                propios.zonaDeSubida,
                arrastrando ? propios.zonaDeSubidaArrastrando : undefined
              )}
              role="button"
              tabIndex={0}
              onClick={() => entradaDeArchivoRef.current?.click()}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  entradaDeArchivoRef.current?.click();
                }
              }}
              onDragOver={(e) => {
                e.preventDefault();
                setArrastrando(true);
              }}
              onDragLeave={() => setArrastrando(false)}
              onDrop={(e) => {
                e.preventDefault();
                setArrastrando(false);
                agregarArchivos(e.dataTransfer.files);
              }}
              aria-label="Subir fotos: Haz clic o arrastra imágenes aquí"
            >
              <IconoSubida className={propios.iconoDeZona} />
              <p className={propios.tituloDeZona}>
                {fotos.length === 0 ? 'Subir fotos' : 'Agregar más fotos'}
              </p>
              <p className={propios.pistaDeZona}>
                Arrastra tus imágenes aquí o haz clic para buscarlas (JPEG, PNG o WebP, máx. 5 MB)
              </p>
              <Boton
                type="button"
                variante="contorno"
                className={propios.botonExaminar}
                onClick={(e) => {
                  e.stopPropagation();
                  entradaDeArchivoRef.current?.click();
                }}
              >
                Explorar archivos
              </Boton>
            </div>

            {errorDeFotos !== undefined && (
              <p className={propios.errorAsistente} role="alert">
                {errorDeFotos}
              </p>
            )}

            {fotos.length > 0 && (
              <div className={propios.galeriaPrevia}>
                <p className={propios.contadorFotos}>
                  {fotos.length} {fotos.length === 1 ? 'foto seleccionada' : 'fotos seleccionadas'}
                </p>
                <ul className={propios.listaDeMiniaturas}>
                  {fotos.map((foto, indice) => (
                    <li key={`${foto.name}-${indice}`} className={propios.itemDeMiniatura}>
                      <img
                        src={urlsPrevia[indice]}
                        alt={`Previsualización de ${foto.name}`}
                        className={propios.miniaturaFoto}
                      />
                      <div className={propios.detallesFoto}>
                        <span className={propios.nombreFoto}>{foto.name}</span>
                        <span className={propios.tamanoFoto}>
                          {(foto.size / (1024 * 1024)).toFixed(2)} MB
                        </span>
                      </div>
                      <button
                        type="button"
                        className={propios.botonQuitarFoto}
                        onClick={() => quitarFoto(indice)}
                        aria-label={`Quitar foto ${foto.name}`}
                        title="Quitar foto"
                      >
                        <IconoX />
                      </button>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      )}

      {paso === 3 && (
        <div className={propios.cuerpoDePaso}>
          <h2 className={propios.tituloDePaso}>Precio de referencia</h2>
          <div className={propios.campoAsistente}>
            <label className={propios.etiquetaAsistente} htmlFor="precio-servicio">
              Precio de referencia
            </label>
            <div className={propios.campoDePrecio}>
              <Entrada
                id="precio-servicio"
                type="number"
                inputMode="decimal"
                min="0.01"
                step="0.01"
                icono={<span className={propios.prefijoMoneda}>C$</span>}
                mensajeDeError={errors.precioReferencia?.message}
                aria-describedby={errors.precioReferencia ? undefined : 'pista-precio-servicio'}
                {...registroDePrecio}
              />
            </div>
            <p className={propios.pistaAsistente} id="pista-precio-servicio">
              Si lo dejas vacío, en la búsqueda pública se mostrará como «A convenir».
            </p>
          </div>
        </div>
      )}

      {paso === 4 && (
        <div className={propios.cuerpoDePaso}>
          <h2 className={propios.tituloDePaso}>Revisión y confirmación</h2>

          {servicioCreado !== null && (
            <div className={propios.avisoExito} role="status">
              <IconoCheckCirculo className={propios.iconoExito} />
              <div>
                <p className={propios.tituloExito}>
                  {fotos.length > 0
                    ? '¡Fotos subidas y servicio listo!'
                    : '¡Servicio listo para publicarse!'}
                </p>
                <p className={propios.detalleExito}>
                  Tu servicio «{servicioCreado.nombre}» ha sido registrado. Haz clic en Publicar
                  servicio para acceder a su administración.
                </p>
              </div>
            </div>
          )}

          <div className={propios.resumen}>
            <div className={propios.filaDeResumen}>
              <p className={propios.etiquetaDeResumen}>Nombre</p>
              <p className={propios.valorDeResumen}>{textoOVacio(nombre)}</p>
            </div>
            <div className={propios.filaDeResumen}>
              <p className={propios.etiquetaDeResumen}>Categoría</p>
              <p className={propios.valorDeResumen}>
                {textoDeCategoria(categoriaElegida?.nombre, subcategoriaElegida?.nombre)}
              </p>
            </div>
            <div className={propios.filaDeResumen}>
              <p className={propios.etiquetaDeResumen}>Precio</p>
              <p className={propios.valorDeResumen}>{textoDePrecio(precioReferencia)}</p>
            </div>
            <div className={propios.filaDeResumen}>
              <p className={propios.etiquetaDeResumen}>Descripción</p>
              <p className={propios.extractoDeResumen}>{extracto(descripcion)}</p>
            </div>
            <div className={propios.filaDeResumen}>
              <p className={propios.etiquetaDeResumen}>Fotos</p>
              {fotos.length > 0 ? (
                <div className={propios.resumenFotos}>
                  <p className={propios.valorDeResumen}>
                    {fotos.length}{' '}
                    {fotos.length === 1 ? 'foto lista para subir' : 'fotos listas para subir'}
                  </p>
                  <div className={propios.miniaturasResumen}>
                    {fotos.map((_, idx) => (
                      <img
                        key={idx}
                        className={propios.miniaturaResumen}
                        src={urlsPrevia[idx]}
                        alt={`Miniatura ${idx + 1}`}
                      />
                    ))}
                  </div>
                </div>
              ) : (
                <p className={propios.valorDeResumen}>Sin fotos (podrás subirlas después)</p>
              )}
            </div>
          </div>
        </div>
      )}

      <div className={propios.navegacionAsistente}>
        {paso === 1 ? (
          <Boton variante="contorno" onClick={() => navegar(RUTA_SERVICIOS)}>
            Cancelar
          </Boton>
        ) : (
          <Boton
            variante="contorno"
            onClick={servicioCreado !== null ? () => navegar(RUTA_SERVICIOS) : retroceder}
          >
            {servicioCreado !== null ? 'Mis servicios' : 'Atrás'}
          </Boton>
        )}
        {paso < 4 ? (
          <Boton variante="primario" onClick={() => void avanzar()}>
            Siguiente
          </Boton>
        ) : servicioCreado === null ? (
          <Boton variante="primario" type="submit" disabled={creacion.isPending || subiendoFotos}>
            {subiendoFotos
              ? 'Subiendo fotos…'
              : creacion.isPending
                ? 'Publicando…'
                : 'Publicar servicio'}
          </Boton>
        ) : (
          <Boton variante="primario" type="button" onClick={() => alCrear?.(servicioCreado)}>
            Publicar servicio
          </Boton>
        )}
      </div>
    </form>
  );
}

function FormularioDeEdicionDeServicio({ servicio }: { servicio: ServicioPropio }) {
  const categorias = useCategoriasDeServicio();
  const actualizacion = useActualizacionDeServicio();

  const {
    register,
    handleSubmit,
    setError,
    setValue,
    formState: { errors },
  } = useForm<CamposDeServicio, unknown, DatosValidadosDeServicio>({
    resolver: zodResolver(esquemaDeServicio),
    mode: 'onBlur',
    defaultValues: {
      nombre: servicio.nombre,
      descripcion: servicio.descripcion,
      idSubcategoriaServicio: String(servicio.idSubcategoriaServicio),
      precioReferencia: servicio.precioReferencia === null ? '' : String(servicio.precioReferencia),
    },
  });

  const [idCategoria, setIdCategoria] = useState(String(servicio.idCategoriaServicio));

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

  // El <select> nativo pierde el valor si las opciones todavía no existen. Cuando llega el
  // catálogo, se vuelve a aplicar la subcategoría guardada.
  useEffect(() => {
    if (!categorias.isSuccess) {
      return;
    }
    setValue('idSubcategoriaServicio', String(servicio.idSubcategoriaServicio));
  }, [categorias.isSuccess, servicio, setValue]);

  const enviar = handleSubmit((campos) => {
    actualizacion.mutate(
      {
        idServicio: servicio.idServicioPublicado,
        datos: {
          nombre: campos.nombre,
          descripcion: campos.descripcion,
          idSubcategoriaServicio: campos.idSubcategoriaServicio,
          precioReferencia: campos.precioReferencia,
        },
      },
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

  const fallo = actualizacion.error;
  const mensajeGeneral =
    fallo instanceof ErrorDeApi && fallo.errores.length === 0 ? fallo.message : null;

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-datos-del-servicio">
      <h2 className={secciones.tituloDeSeccion} id="titulo-datos-del-servicio">
        Datos del servicio
      </h2>

      {mensajeGeneral !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeGeneral}
        </p>
      )}

      {actualizacion.isSuccess && (
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

        <button className={estilos.boton} type="submit" disabled={actualizacion.isPending}>
          {actualizacion.isPending ? 'Guardando…' : 'Guardar cambios'}
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

function pasoDeCampo(
  campo: 'nombre' | 'descripcion' | 'idSubcategoriaServicio' | 'precioReferencia'
): PasoDePublicacion {
  if (campo === 'descripcion') {
    return 2;
  }
  if (campo === 'precioReferencia') {
    return 3;
  }
  return 1;
}

function textoOVacio(valor: string): string {
  const recortado = valor.trim();
  return recortado === '' ? '—' : recortado;
}

function textoDeCategoria(categoria: string | undefined, subcategoria: string | undefined): string {
  if (categoria !== undefined && subcategoria !== undefined) {
    return `${categoria} · ${subcategoria}`;
  }
  return subcategoria ?? categoria ?? '—';
}

function textoDePrecio(valor: string): string {
  const recortado = valor.trim();
  if (recortado === '') {
    return precioPropio(null);
  }
  const numero = Number(recortado);
  if (!Number.isFinite(numero) || numero <= 0) {
    return recortado;
  }
  return precioPropio(numero);
}

function extracto(texto: string): string {
  const recortado = texto.trim();
  if (recortado === '') {
    return '—';
  }
  if (recortado.length <= LARGO_DEL_EXTRACTO) {
    return recortado;
  }
  return `${recortado.slice(0, LARGO_DEL_EXTRACTO).trimEnd()}…`;
}

function unirClases(...partes: Array<string | undefined>): string {
  return partes.filter((parte) => parte !== undefined && parte !== '').join(' ');
}
