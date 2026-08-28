import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link, useSearchParams } from 'react-router';

import { Boton, Entrada } from '../../../comun/componentes/ui';
import { ErrorDeApi } from '../api';
import { esquemaDeInicioSesion, type CamposDeInicioSesion } from '../esquemas';
import { useInicioSesion } from '../hooks/useAcceso';
import {
  MOTIVO_CREDENCIALES_CAMBIADAS,
  MOTIVO_CUENTA_CREADA,
  MOTIVO_SESION_VENCIDA,
  PARAMETRO_MOTIVO,
  RUTA_REGISTRO,
} from '../rutas';
import estilos from './acceso.module.css';
import { ContinuacionConRedes } from './ContinuacionConRedes';

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
          <h1 className={estilos.titulo}>Iniciar sesión</h1>
          <p className={estilos.subtitulo}>¡Bienvenido de nuevo!</p>
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

        {motivo === MOTIVO_CREDENCIALES_CAMBIADAS && (
          <p className={estilos.aviso} role="status">
            Cambiaste tus credenciales, así que Moica cerró todas tus sesiones, también las de otros
            dispositivos. Entra de nuevo para continuar.
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
            <Entrada
              id="correoElectronico"
              type="email"
              autoComplete="email"
              mensajeDeError={errors.correoElectronico?.message}
              {...register('correoElectronico')}
            />
          </div>

          <div className={estilos.campo}>
            <label className={estilos.etiqueta} htmlFor="clave">
              Contraseña
            </label>
            <Entrada
              id="clave"
              type="password"
              autoComplete="current-password"
              mensajeDeError={errors.clave?.message}
              {...register('clave')}
            />
          </div>

          {/*
            La sesión ya persiste en cookie HttpOnly. El recuadro es del diseño;
            no se envía al backend porque el contrato no tiene «recordarme».
          */}
          <div className={estilos.opciones}>
            <label className={estilos.recordarme}>
              <input type="checkbox" />
              Recordarme
            </label>
            <button
              type="button"
              className={estilos.enlaceInactivo}
              disabled
              title="La recuperación de contraseña no forma parte del MVP"
            >
              ¿Olvidaste tu contraseña?
            </button>
          </div>

          <Boton className={estilos.enviar} type="submit" disabled={inicio.isPending}>
            {inicio.isPending ? 'Entrando…' : 'Iniciar sesión'}
          </Boton>
        </form>

        <ContinuacionConRedes />

        <p className={estilos.pie}>
          ¿No tienes cuenta?{' '}
          <Link className={estilos.enlaceDestacado} to={RUTA_REGISTRO}>
            Regístrate
          </Link>
        </p>
      </div>
    </main>
  );
}
