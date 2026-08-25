import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';

import { ErrorDeApi } from '../../../comun/api';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { esquemaDePerfil, type CamposDePerfil, type DatosValidadosDePerfil } from '../esquemas';
import {
  useActualizacionDePerfil,
  useCatalogoTerritorial,
  useCreacionDePerfil,
} from '../hooks/usePerfilPrestador';
import type { PerfilPrestador } from '../tipos';

/**
 * Formulario con el que se crea y se edita el perfil de prestador.
 *
 * Es el mismo en los dos casos: sin perfil crea, con perfil actualiza. El municipio sale del
 * catálogo territorial, que hoy solo trae Managua pero conserva la estructura por departamento.
 */
export default function FormularioDePerfil({ perfil }: { perfil: PerfilPrestador | null }) {
  const catalogo = useCatalogoTerritorial();
  const creacion = useCreacionDePerfil();
  const actualizacion = useActualizacionDePerfil();
  const guardado = perfil === null ? creacion : actualizacion;

  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CamposDePerfil, unknown, DatosValidadosDePerfil>({
    resolver: zodResolver(esquemaDePerfil),
    mode: 'onBlur',
    defaultValues:
      perfil === null
        ? { tipoPrestador: 'INDEPENDIENTE', idMunicipioPrincipal: '' }
        : {
            nombrePublico: perfil.nombrePublico,
            descripcion: perfil.descripcion,
            tipoPrestador: perfil.tipoPrestador,
            // El selector trabaja con texto; el esquema lo convierte al enviar.
            idMunicipioPrincipal: String(perfil.municipioPrincipal.idMunicipio),
            descripcionCobertura: perfil.descripcionCobertura,
          },
  });

  const enviar = handleSubmit((campos) => {
    guardado.mutate(campos, {
      onError: (fallo) => {
        if (fallo instanceof ErrorDeApi) {
          fallo.errores.forEach((error) => {
            if (esCampoDelFormulario(error.campo)) {
              setError(error.campo, { message: error.mensaje });
            }
          });
        }
      },
    });
  });

  const fallo = guardado.error;
  const mensajeGeneral =
    fallo instanceof ErrorDeApi && fallo.errores.length === 0 ? fallo.message : null;

  return (
    <section className={secciones.seccion} aria-labelledby="titulo-datos-del-perfil">
      <h2 className={secciones.tituloDeSeccion} id="titulo-datos-del-perfil">
        {perfil === null ? 'Crea tu perfil' : 'Datos de tu perfil'}
      </h2>

      {mensajeGeneral !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeGeneral}
        </p>
      )}

      {guardado.isSuccess && (
        <p className={estilos.aviso} role="status">
          {perfil === null ? 'Tu perfil quedó creado.' : 'Guardamos tus cambios.'}
        </p>
      )}

      <form className={estilos.formulario} onSubmit={enviar} noValidate>
        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="nombrePublico">
            Nombre público
          </label>
          <input
            id="nombrePublico"
            className={claseDeEntrada(errors.nombrePublico !== undefined)}
            type="text"
            autoComplete="organization"
            aria-invalid={errors.nombrePublico !== undefined}
            aria-describedby={errors.nombrePublico ? 'error-nombrePublico' : 'pista-nombrePublico'}
            {...register('nombrePublico')}
          />
          <p className={estilos.pista} id="pista-nombrePublico">
            Tu nombre personal, profesional o el de tu negocio.
          </p>
          {errors.nombrePublico && (
            <p className={estilos.error} id="error-nombrePublico">
              {errors.nombrePublico.message}
            </p>
          )}
        </div>

        <fieldset className={estilos.campo}>
          <legend className={estilos.etiqueta}>¿Cómo trabajas?</legend>
          {OPCIONES_DE_TIPO.map((opcion) => (
            <label key={opcion.valor} htmlFor={`tipo-${opcion.valor}`}>
              <input
                id={`tipo-${opcion.valor}`}
                type="radio"
                value={opcion.valor}
                {...register('tipoPrestador')}
              />{' '}
              {opcion.etiqueta}
            </label>
          ))}
          {errors.tipoPrestador && (
            <p className={estilos.error} id="error-tipoPrestador">
              {errors.tipoPrestador.message}
            </p>
          )}
        </fieldset>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="idMunicipioPrincipal">
            Municipio principal
          </label>
          {/*
            Controlado con `Controller` y no con `register` a propósito: las
            opciones llegan después que el formulario, y al sustituirlas el
            navegador descarta el valor que ya no encuentra entre ellas y
            selecciona la primera. Con el `value` en cada render, React lo
            vuelve a aplicar en cuanto la opción existe.
          */}
          <Controller
            control={control}
            name="idMunicipioPrincipal"
            render={({ field }) => (
              <select
                id="idMunicipioPrincipal"
                className={claseDeEntrada(errors.idMunicipioPrincipal !== undefined)}
                aria-invalid={errors.idMunicipioPrincipal !== undefined}
                aria-describedby={
                  errors.idMunicipioPrincipal ? 'error-idMunicipioPrincipal' : undefined
                }
                name={field.name}
                ref={field.ref}
                value={field.value ?? ''}
                onChange={field.onChange}
                onBlur={field.onBlur}
              >
                <option value="" disabled>
                  {catalogo.isPending ? 'Cargando municipios…' : 'Elige tu municipio'}
                </option>
                {/*
                  Mientras el catálogo viaja, el municipio ya elegido necesita
                  su propia opción: sin ella el campo aparecería vacío durante
                  la carga.
                */}
                {perfil !== null && catalogo.data === undefined && (
                  <option value={perfil.municipioPrincipal.idMunicipio}>
                    {perfil.municipioPrincipal.nombreMunicipio}
                  </option>
                )}
                {(catalogo.data ?? []).map((departamento) => (
                  <optgroup key={departamento.idDepartamento} label={departamento.nombre}>
                    {departamento.municipios.map((municipio) => (
                      <option key={municipio.idMunicipio} value={municipio.idMunicipio}>
                        {municipio.nombre}
                      </option>
                    ))}
                  </optgroup>
                ))}
              </select>
            )}
          />
          {catalogo.isError && (
            <p className={estilos.error} role="alert">
              No pudimos cargar los municipios. Recarga la página e inténtalo otra vez.
            </p>
          )}
          {errors.idMunicipioPrincipal && (
            <p className={estilos.error} id="error-idMunicipioPrincipal">
              {errors.idMunicipioPrincipal.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="descripcion">
            Presentación
          </label>
          <textarea
            id="descripcion"
            className={claseDeEntrada(errors.descripcion !== undefined)}
            rows={5}
            aria-invalid={errors.descripcion !== undefined}
            aria-describedby={errors.descripcion ? 'error-descripcion' : 'pista-descripcion'}
            {...register('descripcion')}
          />
          <p className={estilos.pista} id="pista-descripcion">
            Cuenta tu experiencia y qué ofreces. Hasta 3000 caracteres.
          </p>
          {errors.descripcion && (
            <p className={estilos.error} id="error-descripcion">
              {errors.descripcion.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="descripcionCobertura">
            Cobertura
          </label>
          <textarea
            id="descripcionCobertura"
            className={claseDeEntrada(errors.descripcionCobertura !== undefined)}
            rows={3}
            aria-invalid={errors.descripcionCobertura !== undefined}
            aria-describedby={
              errors.descripcionCobertura ? 'error-descripcionCobertura' : 'pista-cobertura'
            }
            {...register('descripcionCobertura')}
          />
          <p className={estilos.pista} id="pista-cobertura">
            Barrios, sectores o puntos de referencia donde atiendes.
          </p>
          {errors.descripcionCobertura && (
            <p className={estilos.error} id="error-descripcionCobertura">
              {errors.descripcionCobertura.message}
            </p>
          )}
        </div>

        <button className={estilos.boton} type="submit" disabled={guardado.isPending}>
          {textoDelBoton(perfil !== null, guardado.isPending)}
        </button>
      </form>
    </section>
  );
}

const OPCIONES_DE_TIPO = [
  { valor: 'INDEPENDIENTE', etiqueta: 'Independiente' },
  { valor: 'EMPRENDIMIENTO', etiqueta: 'Emprendimiento' },
  { valor: 'PYME', etiqueta: 'PYME' },
] as const;

function textoDelBoton(existeElPerfil: boolean, enCurso: boolean): string {
  if (enCurso) {
    return existeElPerfil ? 'Guardando…' : 'Creando tu perfil…';
  }
  return existeElPerfil ? 'Guardar cambios' : 'Crear perfil';
}

function esCampoDelFormulario(campo: string): campo is keyof CamposDePerfil {
  return [
    'nombrePublico',
    'descripcion',
    'tipoPrestador',
    'idMunicipioPrincipal',
    'descripcionCobertura',
  ].includes(campo);
}
