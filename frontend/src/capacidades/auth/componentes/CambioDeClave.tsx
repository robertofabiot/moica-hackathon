import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';

import { Boton, Entrada } from '../../../comun/componentes/ui';
import { ErrorDeApi } from '../api';
import { esquemaDeCambioDeClave, type CamposDeCambioDeClave } from '../esquemas';
import { useCambioDeClave } from '../hooks/useSeguridadCuenta';
import estilos from '../paginas/seguridad.module.css';

/**
 * Formulario de cambio de contraseña.
 *
 * Al terminar no se queda en esta pantalla: el backend revoca todas las sesiones, incluida esta, y
 * el hook lleva de vuelta al inicio de sesión explicando por qué.
 */
export default function CambioDeClave() {
  const [editando, setEditando] = useState(false);
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
    <section aria-labelledby="titulo-contrasena">
      <div className={estilos.filaDeConfiguracion}>
        <div className={estilos.datosDeFila}>
          <h2 className={estilos.etiquetaDeFila} id="titulo-contrasena">
            Contraseña
          </h2>
          <p className={estilos.valorDeFila}>********</p>
        </div>
        <button
          type="button"
          className={estilos.accionDeFila}
          aria-expanded={editando}
          aria-controls="formulario-cambio-clave"
          onClick={() => setEditando((abierto) => !abierto)}
        >
          {editando ? 'Cancelar' : 'Cambiar'}
        </button>
      </div>

      {editando && (
        <div className={estilos.seccionDeFormulario} id="formulario-cambio-clave">
          <p className={estilos.explicacion}>
            Al cambiarla se cierran todas tus sesiones, también las de otros dispositivos. Tendrás
            que volver a entrar.
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
              <Entrada
                id="claveActual"
                type="password"
                autoComplete="current-password"
                mensajeDeError={errors.claveActual?.message}
                {...register('claveActual')}
              />
            </div>

            <div className={estilos.campo}>
              <label className={estilos.etiqueta} htmlFor="claveNueva">
                Contraseña nueva
              </label>
              <Entrada
                id="claveNueva"
                type="password"
                autoComplete="new-password"
                aria-describedby="pista-claveNueva"
                mensajeDeError={errors.claveNueva?.message}
                {...register('claveNueva')}
              />
              <p className={estilos.pista} id="pista-claveNueva">
                De 8 a 72 caracteres, con al menos una mayúscula, una minúscula, un número y un
                símbolo.
              </p>
            </div>

            <div className={estilos.campo}>
              <label className={estilos.etiqueta} htmlFor="confirmacionDeClave">
                Repetir contraseña nueva
              </label>
              <Entrada
                id="confirmacionDeClave"
                type="password"
                autoComplete="new-password"
                mensajeDeError={errors.confirmacionDeClave?.message}
                {...register('confirmacionDeClave')}
              />
            </div>

            <Boton
              className={estilos.botonDeFormulario}
              variante="primario"
              type="submit"
              disabled={cambio.isPending}
            >
              {cambio.isPending ? 'Cambiando la contraseña…' : 'Cambiar contraseña'}
            </Boton>
          </form>
        </div>
      )}
    </section>
  );
}
