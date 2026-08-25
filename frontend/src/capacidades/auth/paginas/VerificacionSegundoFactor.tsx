import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../api';
import { esquemaDeCodigoTotp, type CamposDeCodigoTotp } from '../esquemas';
import { useCierreSesion } from '../hooks/useAcceso';
import { useVerificacionDeSesion } from '../hooks/useSeguridadCuenta';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';

/**
 * Segundo paso del inicio de sesión cuando la cuenta usa segundo factor.
 *
 * Hasta presentar un código válido, la sesión solo sirve para esto y para cerrarla; cualquier otra
 * cosa la rechaza el backend. Por eso la única salida alternativa que se ofrece es salir.
 */
export default function VerificacionSegundoFactor() {
  const verificacion = useVerificacionDeSesion();
  const cierre = useCierreSesion();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeCodigoTotp>({
    resolver: zodResolver(esquemaDeCodigoTotp),
    mode: 'onBlur',
  });

  const enviar = handleSubmit((campos) => verificacion.mutate(campos.codigo));

  const fallo = verificacion.error;
  const mensajeGeneral = fallo instanceof ErrorDeApi ? fallo.message : null;

  return (
    <main className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <header className={estilos.encabezado}>
          <h1 className={estilos.titulo}>Verifica tu segundo factor</h1>
          <p className={estilos.explicacion}>
            Tu contraseña era correcta. Escribe el código de tu aplicación autenticadora para
            terminar de entrar.
          </p>
        </header>

        {mensajeGeneral !== null && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {mensajeGeneral}
          </p>
        )}

        <form className={estilos.formulario} onSubmit={enviar} noValidate>
          <div className={estilos.campo}>
            <label className={estilos.etiqueta} htmlFor="codigo">
              Código de verificación
            </label>
            <input
              id="codigo"
              className={claseDeEntrada(errors.codigo !== undefined)}
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              autoFocus
              aria-invalid={errors.codigo !== undefined}
              aria-describedby={errors.codigo ? 'error-codigo' : undefined}
              {...register('codigo')}
            />
            {errors.codigo && (
              <p className={estilos.error} id="error-codigo">
                {errors.codigo.message}
              </p>
            )}
          </div>

          <button className={estilos.boton} type="submit" disabled={verificacion.isPending}>
            {verificacion.isPending ? 'Comprobando el código…' : 'Verificar y entrar'}
          </button>
        </form>

        {cierre.error !== null && cierre.error !== undefined && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {cierre.error.message}
          </p>
        )}

        <p className={estilos.pie}>
          ¿No puedes usar tu aplicación autenticadora?{' '}
          <button
            className={estilos.enlaceDeTexto}
            type="button"
            onClick={() => cierre.solicitarCierre()}
            disabled={cierre.isPending}
          >
            {cierre.isPending ? 'Saliendo…' : 'Salir de la sesión'}
          </button>
        </p>
      </div>
    </main>
  );
}
