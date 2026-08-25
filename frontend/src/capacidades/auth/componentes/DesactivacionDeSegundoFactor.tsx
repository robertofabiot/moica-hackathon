import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../api';
import { esquemaDeDesactivacion, type CamposDeDesactivacion } from '../esquemas';
import { useDesactivacionDeSegundoFactor } from '../hooks/useSeguridadCuenta';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import seccion from '../paginas/seguridad.module.css';

/**
 * Desactivación del segundo factor.
 *
 * Pide contraseña y código a la vez: quien baje la protección de la cuenta debe demostrar que tiene
 * los dos factores en ese momento. Al terminar, el backend revoca todas las sesiones y hay que
 * volver a entrar.
 */
export default function DesactivacionDeSegundoFactor() {
  const desactivacion = useDesactivacionDeSegundoFactor();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeDesactivacion>({
    resolver: zodResolver(esquemaDeDesactivacion),
    mode: 'onBlur',
  });

  const enviar = handleSubmit((campos) => desactivacion.mutate(campos));

  const fallo = desactivacion.error;

  return (
    <div className={seccion.acciones}>
      <p className={estilos.pista}>
        Al desactivarlo se cierran todas tus sesiones y tu cuenta vuelve a protegerse solo con la
        contraseña.
      </p>

      {fallo instanceof ErrorDeApi && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {fallo.message}
        </p>
      )}

      <form className={estilos.formulario} onSubmit={enviar} noValidate>
        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="claveActualParaDesactivar">
            Contraseña actual
          </label>
          <input
            id="claveActualParaDesactivar"
            className={claseDeEntrada(errors.claveActual !== undefined)}
            type="password"
            autoComplete="current-password"
            aria-invalid={errors.claveActual !== undefined}
            aria-describedby={errors.claveActual ? 'error-claveActualParaDesactivar' : undefined}
            {...register('claveActual')}
          />
          {errors.claveActual && (
            <p className={estilos.error} id="error-claveActualParaDesactivar">
              {errors.claveActual.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="codigoParaDesactivar">
            Código de verificación
          </label>
          <input
            id="codigoParaDesactivar"
            className={claseDeEntrada(errors.codigo !== undefined)}
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            aria-invalid={errors.codigo !== undefined}
            aria-describedby={errors.codigo ? 'error-codigoParaDesactivar' : undefined}
            {...register('codigo')}
          />
          {errors.codigo && (
            <p className={estilos.error} id="error-codigoParaDesactivar">
              {errors.codigo.message}
            </p>
          )}
        </div>

        <button className={estilos.boton} type="submit" disabled={desactivacion.isPending}>
          {desactivacion.isPending ? 'Desactivando…' : 'Desactivar el segundo factor'}
        </button>
      </form>
    </div>
  );
}
