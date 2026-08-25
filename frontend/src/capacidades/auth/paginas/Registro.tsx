import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router';

import { ErrorDeApi } from '../api';
import { esquemaDeRegistro, type CamposDeRegistro } from '../esquemas';
import { useRegistro } from '../hooks/useAcceso';
import { RUTA_INICIO_SESION } from '../rutas';
import { claseDeEntrada } from './estilosDeFormulario';
import estilos from './formulario.module.css';

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
          <h1 className={estilos.titulo}>Crear cuenta en Moica</h1>
          <p className={estilos.explicacion}>
            Con una cuenta puedes contratar servicios y, cuando quieras, ofrecer los tuyos.
          </p>
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
            <input
              id="nombreCompleto"
              className={claseDeEntrada(errors.nombreCompleto !== undefined)}
              type="text"
              autoComplete="name"
              aria-invalid={errors.nombreCompleto !== undefined}
              aria-describedby={errors.nombreCompleto ? 'error-nombreCompleto' : undefined}
              {...register('nombreCompleto')}
            />
            {errors.nombreCompleto && (
              <p className={estilos.error} id="error-nombreCompleto">
                {errors.nombreCompleto.message}
              </p>
            )}
          </div>

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
              autoComplete="new-password"
              aria-invalid={errors.clave !== undefined}
              aria-describedby={errors.clave ? 'error-clave' : 'pista-clave'}
              {...register('clave')}
            />
            <p className={estilos.pista} id="pista-clave">
              De 8 a 72 caracteres, con al menos una mayúscula, una minúscula, un número y un
              símbolo.
            </p>
            {errors.clave && (
              <p className={estilos.error} id="error-clave">
                {errors.clave.message}
              </p>
            )}
          </div>

          <div className={estilos.campo}>
            <label className={estilos.etiqueta} htmlFor="confirmacionDeClave">
              Repetir contraseña
            </label>
            <input
              id="confirmacionDeClave"
              className={claseDeEntrada(errors.confirmacionDeClave !== undefined)}
              type="password"
              autoComplete="new-password"
              aria-invalid={errors.confirmacionDeClave !== undefined}
              aria-describedby={
                errors.confirmacionDeClave ? 'error-confirmacionDeClave' : undefined
              }
              {...register('confirmacionDeClave')}
            />
            {errors.confirmacionDeClave && (
              <p className={estilos.error} id="error-confirmacionDeClave">
                {errors.confirmacionDeClave.message}
              </p>
            )}
          </div>

          <button className={estilos.boton} type="submit" disabled={registro.isPending}>
            {registro.isPending ? 'Creando la cuenta…' : 'Crear cuenta'}
          </button>
        </form>

        <p className={estilos.pie}>
          ¿Ya tienes cuenta? <Link to={RUTA_INICIO_SESION}>Inicia sesión</Link>
        </p>
      </div>
    </main>
  );
}

function esCampoDelFormulario(campo: string): campo is keyof CamposDeRegistro {
  return campo === 'nombreCompleto' || campo === 'correoElectronico' || campo === 'clave';
}
