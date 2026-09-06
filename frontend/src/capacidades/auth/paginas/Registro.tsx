import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';

import { Boton, Entrada } from '../../../comun/componentes/ui';
import { ErrorDeApi } from '../api';
import { esquemaDeRegistro, type CamposDeRegistro } from '../esquemas';
import { useRegistro } from '../hooks/useAcceso';
import { RUTA_INICIO_SESION } from '../rutas';
import estilos from './acceso.module.css';
import { ContinuacionConRedes } from './ContinuacionConRedes';

/**
 * Pantalla de creación de cuenta.
 *
 * Valida en el navegador con las mismas reglas que aplica el backend y, cuando este rechaza algo,
 * coloca su mensaje en el campo correspondiente. Al terminar no inicia sesión: lleva a hacerlo.
 */
export default function Registro() {
  const registro = useRegistro();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CamposDeRegistro>({
    resolver: zodResolver(esquemaDeRegistro),
    mode: 'onBlur',
  });

  const enviar = handleSubmit((campos) => {
    registro.mutate(
      {
        nombreCompleto: campos.nombreCompleto,
        correoElectronico: campos.correoElectronico,
        clave: campos.clave,
      },
      {
        onError: (fallo) => {
          if (fallo instanceof ErrorDeApi) {
            fallo.errores.forEach((error) => {
              if (esCampoDelFormulario(error.campo)) {
                setError(error.campo, { message: error.mensaje });
              }
            });
          }
        },
      }
    );
  });

  const fallo = registro.error;
  const mensajeGeneral =
    fallo instanceof ErrorDeApi && fallo.errores.length === 0 ? fallo.message : null;

  return (
    <main className={estilos.pantalla}>
      <div className={estilos.tarjeta}>
        <header className={estilos.encabezado}>
          <h1 className={estilos.titulo}>Crear cuenta</h1>
          <p className={estilos.subtitulo}>Únete a Moica</p>
        </header>

        {mensajeGeneral !== null && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {mensajeGeneral}
          </p>
        )}

        <form className={estilos.formulario} onSubmit={enviar} noValidate>
          <div className={estilos.campo}>
            <label className={estilos.etiqueta} htmlFor="nombreCompleto">
              Nombre completo
            </label>
            <Entrada
              id="nombreCompleto"
              type="text"
              autoComplete="name"
              mensajeDeError={errors.nombreCompleto?.message}
              {...register('nombreCompleto')}
            />
          </div>

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
              autoComplete="new-password"
              aria-describedby="pista-clave"
              mensajeDeError={errors.clave?.message}
              {...register('clave')}
            />
            <p className={estilos.pista} id="pista-clave">
              De 8 a 72 caracteres, con al menos una mayúscula, una minúscula, un número y un
              símbolo.
            </p>
          </div>

          <div className={estilos.campo}>
            <label className={estilos.etiqueta} htmlFor="confirmacionDeClave">
              Confirmar contraseña
            </label>
            <Entrada
              id="confirmacionDeClave"
              type="password"
              autoComplete="new-password"
              mensajeDeError={errors.confirmacionDeClave?.message}
              {...register('confirmacionDeClave')}
            />
          </div>

          <Boton className={estilos.enviar} type="submit" disabled={registro.isPending}>
            {registro.isPending ? 'Creando la cuenta…' : 'Registrarme'}
          </Boton>
        </form>

        <ContinuacionConRedes />

        <p className={estilos.pie}>
          ¿Ya tienes cuenta?{' '}
          <Link className={estilos.enlaceDestacado} to={RUTA_INICIO_SESION}>
            Iniciar sesión
          </Link>
        </p>
      </div>
    </main>
  );
}

function esCampoDelFormulario(campo: string): campo is keyof CamposDeRegistro {
  return campo === 'nombreCompleto' || campo === 'correoElectronico' || campo === 'clave';
}
