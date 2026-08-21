import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link, useSearchParams } from 'react-router';

import { ErrorDeApi } from '../api';
import { esquemaDeInicioSesion, type CamposDeInicioSesion } from '../esquemas';
import { useInicioSesion } from '../hooks/useAcceso';
import {
  MOTIVO_CUENTA_CREADA,
  MOTIVO_SESION_VENCIDA,
  PARAMETRO_MOTIVO,
  RUTA_REGISTRO,
} from '../rutas';
import estilos from './formulario.module.css';

/**
 * Pantalla de inicio de sesión.
 *
 * Además de pedir las credenciales, explica por qué se llegó hasta aquí cuando la sesión venció o
 * cuando la cuenta acaba de crearse.
 */
export default function InicioSesion() {
  const [parametros] = useSearchParams();
  const motivo = parametros.get(PARAMETRO_MOTIVO);

  const inicio = useInicioSesion();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeInicioSesion>({
    resolver: zodResolver(esquemaDeInicioSesion),
    mode: 'onBlur',
  });

  const enviar = handleSubmit((campos) => inicio.mutate(campos));

  const fallo = inicio.error;
  const mensajeGeneral = fallo instanceof ErrorDeApi ? fallo.message : null;

  return (
    <main className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <header className={estilos.encabezado}>
          <h1 className={estilos.titulo}>Iniciar sesión en Moica</h1>
          <p className={estilos.explicacion}>Entra con el correo y la contraseña de tu cuenta.</p>
        </header>

        {motivo === MOTIVO_SESION_VENCIDA && (
          <p className={estilos.aviso} role="status">
            Tu sesión venció. Por seguridad, Moica no la renueva sola: inicia sesión otra vez para
            continuar.
          </p>
        )}

        {motivo === MOTIVO_CUENTA_CREADA && (
          <p className={estilos.aviso} role="status">
            Tu cuenta quedó creada. Inicia sesión para entrar.
          </p>
        )}

        {mensajeGeneral !== null && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {mensajeGeneral}
          </p>
        )}

        <form className={estilos.formulario} onSubmit={enviar} noValidate>
          <div className={estilos.campo}>
            <label className={estilos.etiqueta} htmlFor="correoElectronico">
              Correo electrónico
            </label>
            <input
              id="correoElectronico"
              className={claseDeEntrada(errors.correoElectronico !== undefined)}
              type="email"
              autoComplete="email"
              aria-invalid={errors.correoElectronico !== undefined}
              aria-describedby={errors.correoElectronico ? 'error-correoElectronico' : undefined}
              {...register('correoElectronico')}
            />
            {errors.correoElectronico && (
              <p className={estilos.error} id="error-correoElectronico">
                {errors.correoElectronico.message}
              </p>
            )}
          </div>

          <div className={estilos.campo}>
            <label className={estilos.etiqueta} htmlFor="clave">
              Contraseña
            </label>
            <input
              id="clave"
              className={claseDeEntrada(errors.clave !== undefined)}
              type="password"
              autoComplete="current-password"
              aria-invalid={errors.clave !== undefined}
              aria-describedby={errors.clave ? 'error-clave' : undefined}
              {...register('clave')}
            />
            {errors.clave && (
              <p className={estilos.error} id="error-clave">
                {errors.clave.message}
              </p>
            )}
          </div>

          <button className={estilos.boton} type="submit" disabled={inicio.isPending}>
            {inicio.isPending ? 'Entrando…' : 'Iniciar sesión'}
          </button>
        </form>

        <p className={estilos.pie}>
          ¿Todavía no tienes cuenta? <Link to={RUTA_REGISTRO}>Créala aquí</Link>
        </p>
      </div>
    </main>
  );
}

function claseDeEntrada(conError: boolean): string {
  const clases = [estilos.entrada];
  if (conError) {
    clases.push(estilos.entradaConError);
  }
  return clases.join(' ');
}
