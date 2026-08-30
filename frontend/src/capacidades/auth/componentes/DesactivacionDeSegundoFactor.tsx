import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { Boton, Entrada } from '../../../comun/componentes/ui';
import { ErrorDeApi } from '../api';
import { esquemaDeDesactivacion, type CamposDeDesactivacion } from '../esquemas';
import { useDesactivacionDeSegundoFactor } from '../hooks/useSeguridadCuenta';
import estilos from '../paginas/seguridad.module.css';

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
    <div className={estilos.acciones}>
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
          <Entrada
            id="claveActualParaDesactivar"
            type="password"
            autoComplete="current-password"
            mensajeDeError={errors.claveActual?.message}
            {...register('claveActual')}
          />
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="codigoParaDesactivar">
            Código de verificación
          </label>
          <Entrada
            id="codigoParaDesactivar"
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            mensajeDeError={errors.codigo?.message}
            {...register('codigo')}
          />
        </div>

        <Boton
          className={`${estilos.botonDeFormulario} ${estilos.botonDestructivo}`}
          variante="contorno"
          type="submit"
          disabled={desactivacion.isPending}
        >
          {desactivacion.isPending ? 'Desactivando…' : 'Desactivar el segundo factor'}
        </Boton>
      </form>
    </div>
  );
}
