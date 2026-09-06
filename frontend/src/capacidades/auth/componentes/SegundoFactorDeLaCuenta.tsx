import { Boton } from '../../../comun/componentes/ui';
import { ErrorDeApi } from '../api';
import { useSegundoFactor } from '../hooks/useSeguridadCuenta';
import type { EstadoSegundoFactor } from '../tipos';
import estilos from '../paginas/seguridad.module.css';
import ActivacionDeSegundoFactor from './ActivacionDeSegundoFactor';
import DesactivacionDeSegundoFactor from './DesactivacionDeSegundoFactor';

/**
 * Estado del segundo factor de la cuenta y lo que se puede hacer con él.
 *
 * Con el segundo factor activo se ofrece desactivarlo; en cualquier otro estado, activarlo. Una
 * cuenta administrativa no puede desactivarlo, así que ahí no se ofrece: la regla la impone el
 * backend y esta pantalla solo evita proponer algo que va a rechazar.
 */
export default function SegundoFactorDeLaCuenta() {
  const segundoFactor = useSegundoFactor();

  return (
    <section className={estilos.seccion} aria-labelledby="titulo-segundo-factor">
      <h2 className={estilos.tituloDeSeccion} id="titulo-segundo-factor">
        Segundo factor (TOTP)
      </h2>
      <p className={estilos.explicacion}>
        Un código temporal de tu aplicación autenticadora que se pide al iniciar sesión, además de
        la contraseña.
      </p>

      {segundoFactor.isPending && (
        <p className={estilos.estado} role="status">
          Consultando el estado de tu segundo factor…
        </p>
      )}

      {segundoFactor.isError && (
        <div className={estilos.acciones}>
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {segundoFactor.error instanceof ErrorDeApi
              ? segundoFactor.error.message
              : 'No pudimos consultar tu segundo factor.'}
          </p>
          <Boton
            className={estilos.botonDeFormulario}
            variante="secundario"
            type="button"
            onClick={() => void segundoFactor.refetch()}
          >
            Reintentar
          </Boton>
        </div>
      )}

      {segundoFactor.data && (
        <>
          <p className={estilos.estado}>
            Estado:{' '}
            <span className={estilos.etiquetaDeEstado}>
              {descripcionDelEstado(segundoFactor.data.estado)}
            </span>
          </p>

          {segundoFactor.data.obligatorio && (
            <p className={estilos.avisoAdministrativo} role="note">
              Tu cuenta tiene permisos administrativos: el segundo factor es obligatorio y no se
              puede desactivar.
            </p>
          )}

          {segundoFactor.data.estado === 'ACTIVO' ? (
            segundoFactor.data.obligatorio ? null : (
              <DesactivacionDeSegundoFactor />
            )
          ) : (
            <ActivacionDeSegundoFactor />
          )}
        </>
      )}
    </section>
  );
}

function descripcionDelEstado(estado: EstadoSegundoFactor | null): string {
  switch (estado) {
    case 'ACTIVO':
      return 'Activo';
    case 'PENDIENTE_ACTIVACION':
      return 'Activación sin terminar';
    case 'DESACTIVADO':
      return 'Desactivado';
    default:
      return 'Sin configurar';
  }
}
