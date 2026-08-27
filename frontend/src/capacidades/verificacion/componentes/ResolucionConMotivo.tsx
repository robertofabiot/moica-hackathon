import { zodResolver } from '@hookform/resolvers/zod';
import { useId } from 'react';
import { useForm } from 'react-hook-form';

import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import { esquemaDeRevocacion, type CamposDeRevocacion } from '../esquemas';
import propios from './verificacion.module.css';

/**
 * El formulario con el que se cierra una solicitud en negativo.
 *
 * Lo comparten el rechazo y la revocación porque son la misma exigencia: sin un motivo escrito,
 * quien presentó el expediente no sabría qué corregir ni por qué perdió su insignia. El backend lo
 * vuelve a exigir y PostgreSQL también, con {@code ck_solicitud_verificacion_observacion}.
 *
 * La revocación añade una casilla de confirmación: es la única acción que retira algo ya concedido
 * y, cuando se revoca la básica, arrastra también la profesional.
 */
export default function ResolucionConMotivo({
  titulo,
  advertencia,
  textoDeAccion,
  exigeConfirmacion,
  textoDeConfirmacion,
  enCurso,
  alConfirmar,
  alCancelar,
}: {
  titulo: string;
  advertencia?: string;
  textoDeAccion: string;
  exigeConfirmacion?: boolean;
  textoDeConfirmacion?: string;
  enCurso: boolean;
  alConfirmar: (observacion: string) => void;
  alCancelar: () => void;
}) {
  const identificador = useId();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeRevocacion>({
    resolver: zodResolver(esquemaDeRevocacion),
    // Sin confirmación exigida la casilla no se pinta, así que nace ya marcada
    // y el esquema la da por cumplida.
    defaultValues: { observacion: '', confirmo: exigeConfirmacion !== true },
  });

  return (
    <form
      className={estilos.formulario}
      onSubmit={(evento) => void handleSubmit((campos) => alConfirmar(campos.observacion))(evento)}
      noValidate
    >
      <h4 className={propios.subtitulo}>{titulo}</h4>

      {advertencia !== undefined && (
        <p className={propios.explicacionDeLaInsignia}>{advertencia}</p>
      )}

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor={`${identificador}-observacion`}>
          Motivo
        </label>
        <textarea
          className={claseDeEntrada(errors.observacion !== undefined)}
          id={`${identificador}-observacion`}
          rows={3}
          disabled={enCurso}
          aria-invalid={errors.observacion !== undefined}
          aria-describedby={
            errors.observacion === undefined ? undefined : `${identificador}-error-observacion`
          }
          {...register('observacion')}
        />
        {errors.observacion !== undefined && (
          <p className={estilos.error} id={`${identificador}-error-observacion`} role="alert">
            {errors.observacion.message}
          </p>
        )}
        <p className={estilos.pista}>Lo verá quien presentó el expediente.</p>
      </div>

      {exigeConfirmacion === true && (
        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor={`${identificador}-confirmo`}>
            <input
              id={`${identificador}-confirmo`}
              type="checkbox"
              disabled={enCurso}
              aria-invalid={errors.confirmo !== undefined}
              {...register('confirmo')}
            />{' '}
            {textoDeConfirmacion}
          </label>
          {errors.confirmo !== undefined && (
            <p className={estilos.error} role="alert">
              {errors.confirmo.message}
            </p>
          )}
        </div>
      )}

      <div className={propios.acciones}>
        <button className={estilos.boton} type="submit" disabled={enCurso}>
          {enCurso ? 'Guardando…' : textoDeAccion}
        </button>
        <button
          className={secciones.botonSecundario}
          type="button"
          onClick={alCancelar}
          disabled={enCurso}
        >
          Cancelar
        </button>
      </div>
    </form>
  );
}
