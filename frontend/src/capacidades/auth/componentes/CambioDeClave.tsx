import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../api';
import { esquemaDeCambioDeClave, type CamposDeCambioDeClave } from '../esquemas';
import { useCambioDeClave } from '../hooks/useSeguridadCuenta';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import seccion from '../paginas/seguridad.module.css';

/**
 * Formulario de cambio de contraseña.
 *
 * Al terminar no se queda en esta pantalla: el backend revoca todas las sesiones, incluida esta, y
 * el hook lleva de vuelta al inicio de sesión explicando por qué.
 */
export default function CambioDeClave() {
  const cambio = useCambioDeClave();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CamposDeCambioDeClave>({
    resolver: zodResolver(esquemaDeCambioDeClave),
    mode: 'onBlur',
  });

  const enviar = handleSubmit((campos) => {
    cambio.mutate(
      { claveActual: campos.claveActual, claveNueva: campos.claveNueva },
      {
        onError: (fallo) => {
          if (fallo instanceof ErrorDeApi) {
            fallo.errores.forEach((error) => {
              if (error.campo === 'claveNueva') {
                setError('claveNueva', { message: error.mensaje });
              }
            });
          }
        },
      }
    );
  });

  const fallo = cambio.error;
  const mensajeGeneral =
    fallo instanceof ErrorDeApi && fallo.errores.length === 0 ? fallo.message : null;

  return (
    <section className={seccion.seccion} aria-labelledby="titulo-contrasena">
      <h2 className={seccion.tituloDeSeccion} id="titulo-contrasena">
        Contraseña
      </h2>
      <p className={seccion.explicacion}>
        Al cambiarla se cierran todas tus sesiones, también las de otros dispositivos. Tendrás que
        volver a entrar.
      </p>

      {mensajeGeneral !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeGeneral}
        </p>
      )}

      <form className={estilos.formulario} onSubmit={enviar} noValidate>
        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="claveActual">
            Contraseña actual
          </label>
          <input
            id="claveActual"
            className={claseDeEntrada(errors.claveActual !== undefined)}
            type="password"
            autoComplete="current-password"
            aria-invalid={errors.claveActual !== undefined}
            aria-describedby={errors.claveActual ? 'error-claveActual' : undefined}
            {...register('claveActual')}
          />
          {errors.claveActual && (
            <p className={estilos.error} id="error-claveActual">
              {errors.claveActual.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="claveNueva">
            Contraseña nueva
          </label>
          <input
            id="claveNueva"
            className={claseDeEntrada(errors.claveNueva !== undefined)}
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.claveNueva !== undefined}
            aria-describedby={errors.claveNueva ? 'error-claveNueva' : 'pista-claveNueva'}
            {...register('claveNueva')}
          />
          <p className={estilos.pista} id="pista-claveNueva">
            De 8 a 72 caracteres, con al menos una mayúscula, una minúscula, un número y un símbolo.
          </p>
          {errors.claveNueva && (
            <p className={estilos.error} id="error-claveNueva">
              {errors.claveNueva.message}
            </p>
          )}
        </div>

        <div className={estilos.campo}>
          <label className={estilos.etiqueta} htmlFor="confirmacionDeClave">
            Repetir contraseña nueva
          </label>
          <input
            id="confirmacionDeClave"
            className={claseDeEntrada(errors.confirmacionDeClave !== undefined)}
            type="password"
            autoComplete="new-password"
            aria-invalid={errors.confirmacionDeClave !== undefined}
            aria-describedby={errors.confirmacionDeClave ? 'error-confirmacionDeClave' : undefined}
            {...register('confirmacionDeClave')}
          />
          {errors.confirmacionDeClave && (
            <p className={estilos.error} id="error-confirmacionDeClave">
              {errors.confirmacionDeClave.message}
            </p>
          )}
        </div>

        <button className={estilos.boton} type="submit" disabled={cambio.isPending}>
          {cambio.isPending ? 'Cambiando la contraseña…' : 'Cambiar contraseña'}
        </button>
      </form>
    </section>
  );
}
