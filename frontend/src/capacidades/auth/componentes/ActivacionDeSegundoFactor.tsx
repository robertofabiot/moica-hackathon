import { zodResolver } from '@hookform/resolvers/zod';
import { QRCodeSVG } from 'qrcode.react';
import { useEffect } from 'react';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../api';
import { esquemaDeCodigoTotp, type CamposDeCodigoTotp } from '../esquemas';
import {
  useActivacionDeSegundoFactor,
  useConfirmacionDeSegundoFactor,
} from '../hooks/useSeguridadCuenta';
import { claseDeEntrada } from '../paginas/estilosDeFormulario';
import estilos from '../paginas/formulario.module.css';
import seccion from '../paginas/seguridad.module.css';

/**
 * Activación del segundo factor, en sus dos pasos.
 *
 * El primero pide un secreto nuevo y lo muestra; el segundo lo confirma con un código. El secreto
 * vive solo en el resultado de la mutación: no se guarda en la caché de consultas, ni en
 * `localStorage`, ni en ningún estado global, y este componente lo descarta al desmontarse. Así
 * está a la vista únicamente mientras la activación en curso lo necesita, que es también lo que se
 * le promete a quien lo lee.
 *
 * El código QR es una comodidad. La clave manual siempre está a la vista, porque es la que permite
 * configurar la aplicación autenticadora sin depender de una cámara.
 */
export default function ActivacionDeSegundoFactor() {
  const activacion = useActivacionDeSegundoFactor();
  const confirmacion = useConfirmacionDeSegundoFactor();

  // Al salir de la pantalla —o al quedar el segundo factor activo, que también
  // desmonta este componente— el secreto se descarta en el acto, sin esperar a
  // que la caché de mutaciones lo recoja.
  const olvidarElSecreto = activacion.reset;
  useEffect(() => olvidarElSecreto, [olvidarElSecreto]);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CamposDeCodigoTotp>({
    resolver: zodResolver(esquemaDeCodigoTotp),
    mode: 'onBlur',
  });

  const enviar = handleSubmit((campos) => confirmacion.mutate(campos.codigo));

  if (!activacion.data) {
    return (
      <div className={seccion.acciones}>
        {activacion.error instanceof ErrorDeApi && (
          <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
            {activacion.error.message}
          </p>
        )}
        <button
          className={estilos.boton}
          type="button"
          onClick={() => activacion.mutate()}
          disabled={activacion.isPending}
        >
          {activacion.isPending ? 'Preparando la activación…' : 'Activar el segundo factor'}
        </button>
      </div>
    );
  }

  const datos = activacion.data;
  const falloAlConfirmar = confirmacion.error;

  return (
    <div className={seccion.acciones}>
      <ol className={seccion.pasos}>
        <li>Abre tu aplicación autenticadora y agrega una cuenta nueva.</li>
        <li>Escanea el código o escribe la clave a mano.</li>
        <li>
          Escribe abajo el código de {datos.digitos} dígitos que aparezca; cambia cada{' '}
          {datos.periodoEnSegundos} segundos.
        </li>
      </ol>

      <div className={seccion.codigoQr}>
        <QRCodeSVG
          value={datos.uriDeConfiguracion}
          size={192}
          title="Código QR para configurar tu aplicación autenticadora"
        />
      </div>

      <div className={estilos.campo}>
        <span className={estilos.etiqueta} id="etiqueta-clave-manual">
          Clave para escribir a mano
        </span>
        <p className={seccion.claveManual} aria-labelledby="etiqueta-clave-manual">
          {datos.claveManual}
        </p>
        <p className={estilos.pista}>
          Guárdala solo el tiempo que tardes en configurar la aplicación: cuando el segundo factor
          quede activo, Moica ya no podrá volver a mostrártela.
        </p>
      </div>

      {falloAlConfirmar instanceof ErrorDeApi && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {falloAlConfirmar.message}
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

        <button className={estilos.boton} type="submit" disabled={confirmacion.isPending}>
          {confirmacion.isPending ? 'Comprobando el código…' : 'Confirmar activación'}
        </button>
      </form>
    </div>
  );
}
