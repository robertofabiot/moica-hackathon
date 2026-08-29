import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';

import { ErrorDeApi } from '../../../comun/api';
import { claseDeEntrada } from '../../../comun/estilos/estilosDeFormulario';
import estilos from '../../../comun/estilos/formulario.module.css';
import secciones from '../../../comun/estilos/secciones.module.css';
import {
  esquemaDeContratacion,
  type CamposDeContratacion,
  type DatosValidadosDeContratacion,
} from '../esquemas';
import { useCatalogoDeMunicipios, useCreacionDeSolicitud } from '../hooks/useSolicitudes';

/**
 * Formulario para enviar una solicitud a un servicio ajeno.
 */
export default function FormularioDeSolicitud({
  idServicioPublicado,
  alCrear,
}: {
  idServicioPublicado: number;
  alCrear: (idSolicitud: number) => void;
}) {
  const catalogo = useCatalogoDeMunicipios();
  const creacion = useCreacionDeSolicitud();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<CamposDeContratacion, unknown, DatosValidadosDeContratacion>({
    resolver: zodResolver(esquemaDeContratacion),
    mode: 'onBlur',
    defaultValues: {
      descripcionNecesidad: '',
      idMunicipio: '',
      indicacionUbicacion: '',
      fechaPreferida: '',
    },
  });

  const enviar = handleSubmit((campos) => {
    creacion.mutate(
      {
        idServicioPublicado,
        descripcionNecesidad: campos.descripcionNecesidad,
        idMunicipio: campos.idMunicipio,
        indicacionUbicacion: campos.indicacionUbicacion,
        fechaPreferida: campos.fechaPreferida,
      },
      {
        onSuccess: (creada) => alCrear(creada.idSolicitudServicio),
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

  const fallo = creacion.error;
  const mensajeGeneral =
    fallo instanceof ErrorDeApi && fallo.errores.length === 0 ? fallo.message : null;

  return (
    <form className={estilos.formulario} onSubmit={(evento) => void enviar(evento)} noValidate>
      {mensajeGeneral !== null && (
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {mensajeGeneral}
        </p>
      )}

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="descripcion-necesidad">
          Qué necesitas
        </label>
        <textarea
          className={claseDeEntrada(errors.descripcionNecesidad !== undefined)}
          id="descripcion-necesidad"
          rows={5}
          disabled={creacion.isPending}
          aria-invalid={errors.descripcionNecesidad !== undefined}
          aria-describedby={
            errors.descripcionNecesidad === undefined ? undefined : 'error-descripcion-necesidad'
          }
          {...register('descripcionNecesidad')}
        />
        {errors.descripcionNecesidad !== undefined && (
          <p className={estilos.error} id="error-descripcion-necesidad" role="alert">
            {errors.descripcionNecesidad.message}
          </p>
        )}
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="municipio-solicitud">
          Municipio
        </label>
        <select
          className={claseDeEntrada(errors.idMunicipio !== undefined)}
          id="municipio-solicitud"
          disabled={creacion.isPending || catalogo.isPending}
          aria-invalid={errors.idMunicipio !== undefined}
          aria-describedby={errors.idMunicipio === undefined ? undefined : 'error-municipio'}
          {...register('idMunicipio')}
        >
          <option value="">
            {catalogo.isPending ? 'Cargando municipios…' : 'Elige un municipio'}
          </option>
          {(catalogo.data ?? []).flatMap((departamento) =>
            departamento.municipios.map((municipio) => (
              <option key={municipio.idMunicipio} value={municipio.idMunicipio}>
                {departamento.nombre}: {municipio.nombre}
              </option>
            ))
          )}
        </select>
        {errors.idMunicipio !== undefined && (
          <p className={estilos.error} id="error-municipio" role="alert">
            {errors.idMunicipio.message}
          </p>
        )}
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="indicacion-ubicacion">
          Dirección, sector o referencia
        </label>
        <textarea
          className={claseDeEntrada(errors.indicacionUbicacion !== undefined)}
          id="indicacion-ubicacion"
          rows={3}
          disabled={creacion.isPending}
          aria-invalid={errors.indicacionUbicacion !== undefined}
          aria-describedby={
            errors.indicacionUbicacion === undefined ? undefined : 'error-indicacion-ubicacion'
          }
          {...register('indicacionUbicacion')}
        />
        {errors.indicacionUbicacion !== undefined && (
          <p className={estilos.error} id="error-indicacion-ubicacion" role="alert">
            {errors.indicacionUbicacion.message}
          </p>
        )}
      </div>

      <div className={estilos.campo}>
        <label className={estilos.etiqueta} htmlFor="fecha-preferida">
          Fecha preferida (opcional)
        </label>
        <input
          className={claseDeEntrada(errors.fechaPreferida !== undefined)}
          id="fecha-preferida"
          type="date"
          disabled={creacion.isPending}
          aria-invalid={errors.fechaPreferida !== undefined}
          {...register('fechaPreferida')}
        />
        {errors.fechaPreferida !== undefined && (
          <p className={estilos.error} role="alert">
            {errors.fechaPreferida.message}
          </p>
        )}
      </div>

      <button className={estilos.boton} type="submit" disabled={creacion.isPending}>
        {creacion.isPending ? 'Enviando solicitud…' : 'Enviar solicitud'}
      </button>

      <p className={secciones.explicacion}>
        El prestador verá esta solicitud. Los contactos siguen ocultos hasta que la acepte.
      </p>
    </form>
  );
}

function esCampoDelFormulario(
  campo: string
): campo is 'descripcionNecesidad' | 'idMunicipio' | 'indicacionUbicacion' | 'fechaPreferida' {
  return (
    campo === 'descripcionNecesidad' ||
    campo === 'idMunicipio' ||
    campo === 'indicacionUbicacion' ||
    campo === 'fechaPreferida'
  );
}
