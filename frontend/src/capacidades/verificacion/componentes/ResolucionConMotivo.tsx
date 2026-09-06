import { zodResolver } from '@hookform/resolvers/zod';
import { useId } from 'react';
import { useForm } from 'react-hook-form';

import { Boton } from '../../../comun/componentes/ui';
import { esquemaDeRevocacion, type CamposDeRevocacion } from '../esquemas';
import revision from '../paginas/revision.module.css';

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
      className={revision.formularioDeMotivo}
      onSubmit={(evento) => void handleSubmit((campos) => alConfirmar(campos.observacion))(evento)}
      noValidate
    >
      <h4 className={revision.subtitulo}>{titulo}</h4>

      {advertencia !== undefined && <p className={revision.advertencia}>{advertencia}</p>}

      <div className={revision.campo}>
        <label className={revision.etiqueta} htmlFor={`${identificador}-observacion`}>
          Motivo
        </label>
        <textarea
          className={
            errors.observacion === undefined
              ? revision.motivoEscrito
              : `${revision.motivoEscrito} ${revision.motivoConError}`
          }
          id={`${identificador}-observacion`}
          rows={4}
          disabled={enCurso}
          placeholder="Describe con claridad qué debe corregir quien presentó el expediente."
          aria-invalid={errors.observacion !== undefined}
          aria-describedby={
            errors.observacion === undefined ? undefined : `${identificador}-error-observacion`
          }
          {...register('observacion')}
        />
        {errors.observacion !== undefined && (
          <p className={revision.error} id={`${identificador}-error-observacion`} role="alert">
            {errors.observacion.message}
          </p>
        )}
        <p className={revision.pista}>Lo verá quien presentó el expediente.</p>
      </div>

      {exigeConfirmacion === true && (
        <div className={revision.campo}>
          <label className={revision.confirmacionTactil} htmlFor={`${identificador}-confirmo`}>
            <input
              id={`${identificador}-confirmo`}
              type="checkbox"
              disabled={enCurso}
              aria-invalid={errors.confirmo !== undefined}
              {...register('confirmo')}
            />
            {textoDeConfirmacion}
          </label>
          {errors.confirmo !== undefined && (
            <p className={revision.error} role="alert">
              {errors.confirmo.message}
            </p>
          )}
        </div>
      )}

      <div className={revision.accionesDeResolucion}>
        <Boton className={revision.botonPeligro} type="submit" disabled={enCurso}>
          {enCurso ? 'Guardando…' : textoDeAccion}
        </Boton>
        <Boton variante="secundario" type="button" onClick={alCancelar} disabled={enCurso}>
          Cancelar
        </Boton>
      </div>
    </form>
  );
}
