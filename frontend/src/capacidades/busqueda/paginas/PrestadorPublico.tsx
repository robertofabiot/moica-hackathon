import { useState, type ComponentType, type ReactNode, type SVGProps } from 'react';
import { Link, useNavigate, useParams } from 'react-router';

import logoHorizontal from '../../../assets/logos/moica-horizontal.png';
import logoIcono from '../../../assets/logos/moica-icono.svg';
import { ErrorDeApi } from '../../../comun/api';
import {
  BarraLateral,
  Boton,
  EstrellasCalificacion,
  IconoCampana,
  IconoCasa,
  IconoChevronDerecha,
  IconoHerramienta,
  IconoMaletin,
  IconoPin,
  IconoPulgarArriba,
  IconoReloj,
  IconoUsuario,
  InsigniaVerificado,
  PieDePagina,
} from '../../../comun/componentes/ui';
import { RUTA_INICIO_SESION, RUTA_SEGURIDAD, useSesionActual } from '../../auth';
import { RUTA_PRESTADOR } from '../../prestador';
import InsigniaResponsable from '../componentes/InsigniaResponsable';
import { usePrestadorPublico } from '../hooks/useBusquedaPublica';
import {
  conteoDeCalificaciones,
  inicialesDeNombre,
  nombreDeDisponibilidad,
  nombreDelTipoPrestador,
  notaVisible,
  porcentajeDeSatisfaccion,
  precioEnFila,
  profesionVisible,
  SIN_CALIFICACIONES,
} from '../presentacion';
import { RUTA_EXPLORAR, rutaDeDetalleDeServicio } from '../rutas';
import type {
  PerfilPublico,
  PrestadorPublico as DatosDePrestador,
  ReputacionPorRol,
  ResumenPublicoDeServicio,
} from '../tipos';
import estilos from './prestadorPublico.module.css';

/** Perfil público de un prestador verificado: presentación, portafolio y servicios, sin contactos. */
export default function PrestadorPublico() {
  const { idPrestador } = useParams();
  const identificador = Number(idPrestador);
  const perfil = usePrestadorPublico(Number.isInteger(identificador) ? identificador : undefined);

  if (perfil.isPending) {
    return (
      <MarcoDePagina>
        <p className={estilos.estado} role="status">
          Cargando el perfil…
        </p>
      </MarcoDePagina>
    );
  }

  if (perfil.isError || perfil.data === undefined) {
    return (
      <MarcoDePagina>
        <p className={`${estilos.aviso} ${estilos.avisoDeError}`} role="alert">
          {perfil.error instanceof ErrorDeApi
            ? perfil.error.message
            : 'Ese perfil no está disponible.'}{' '}
          <button
            className={estilos.reintentar}
            type="button"
            onClick={() => void perfil.refetch()}
          >
            Reintentar
          </button>
        </p>
        <p className={estilos.pieDeEstado}>
          <Link to={RUTA_EXPLORAR}>Volver a explorar</Link>
        </p>
      </MarcoDePagina>
    );
  }

  return <PerfilCargado perfil={perfil.data} />;
}

function PerfilCargado({ perfil }: { perfil: PerfilPublico }) {
  const { prestador, portafolio, servicios, admiteContratacion, reputacionPrestador } = perfil;

  return (
    <MarcoDePagina>
      <div className={estilos.columnas}>
        <div className={estilos.columnaPrincipal}>
          <div className={estilos.bloqueCabecera}>
            <CabeceraDePerfil perfil={perfil} />
          </div>
          <div className={estilos.bloquePortafolio}>
            <PortafolioPublico portafolio={portafolio} />
          </div>
        </div>

        <div className={estilos.columnaLateral}>
          <div className={estilos.bloqueServicios}>
            <ServiciosOfrecidos servicios={servicios} />
          </div>
          <div className={estilos.bloqueResenas}>
            <ResenasDeClientes reputacion={reputacionPrestador} />
          </div>
        </div>
      </div>

      <p className={estilos.vacio} role="status">
        {admiteContratacion
          ? nombreDeDisponibilidad(prestador.disponibilidad)
          : 'No está disponible para contratar ahora. No se muestran contactos.'}
      </p>
    </MarcoDePagina>
  );
}

function MarcoDePagina({ children }: { children: ReactNode }) {
  return (
    <div className={estilos.pagina}>
      <div className={estilos.barraLateral}>
        <BarraLateral
          itemActivo="inicio"
          destinos={{
            inicio: '/',
            perfil: RUTA_PRESTADOR,
            configuracion: RUTA_SEGURIDAD,
          }}
        />
      </div>
      <div className={estilos.envoltorio}>
        <EncabezadoDePantalla />
        <main className={estilos.principal}>{children}</main>
        <PieDePagina />
      </div>
    </div>
  );
}

function EncabezadoDePantalla() {
  const sesion = useSesionActual();
  const usuario = sesion.data?.usuario;
  const inicial =
    usuario === undefined ? '' : inicialesDeNombre(usuario.nombreCompleto).slice(0, 1);

  return (
    <header className={estilos.barraSuperior}>
      <Link className={estilos.marca} to="/" aria-label="Moica, ir al inicio">
        <img className={estilos.logoIcono} src={logoIcono} alt="" />
        <img className={estilos.logoCompleto} src={logoHorizontal} alt="" />
      </Link>
      <div className={estilos.accionesDeBarra}>
        <button
          type="button"
          className={estilos.botonIcono}
          aria-label="Notificaciones, hay avisos"
        >
          <IconoCampana />
          <span className={estilos.puntoDeAviso} aria-hidden="true" />
        </button>
        {usuario === undefined || inicial === '' ? (
          <Link className={estilos.avatarBarra} to={RUTA_INICIO_SESION} aria-label="Iniciar sesión">
            <IconoUsuario />
          </Link>
        ) : (
          <Link
            className={estilos.avatarBarra}
            to={RUTA_PRESTADOR}
            aria-label={`Cuenta de ${usuario.nombreCompleto}`}
          >
            {inicial}
          </Link>
        )}
      </div>
    </header>
  );
}

function CabeceraDePerfil({ perfil }: { perfil: PerfilPublico }) {
  const { prestador, servicios, reputacionPrestador, admiteContratacion } = perfil;
  const estaVerificado = prestador.nivelVerificacion !== 'SIN_VERIFICAR';
  const navegar = useNavigate();
  const [sigue, setSigue] = useState(false);
  const primerServicio = servicios[0];
  const puedeContactar = admiteContratacion && primerServicio !== undefined;

  return (
    <section className={estilos.tarjetaCabecera} aria-labelledby="nombre-prestador-publico">
      <div className={estilos.filaCabecera}>
        <AvatarPublico prestador={prestador} />
        <div className={estilos.datosCabecera}>
          <div className={estilos.filaNombre}>
            <h1 className={estilos.nombre} id="nombre-prestador-publico">
              {prestador.nombrePublico}
            </h1>
            {estaVerificado ? <InsigniaVerificado /> : null}
          </div>
          <p className={estilos.profesion}>
            {profesionVisible(servicios, prestador.tipoPrestador)}
          </p>
          <div className={estilos.meta}>
            <EstrellasCalificacion
              calificacion={reputacionPrestador.promedio}
              totalCalificaciones={
                reputacionPrestador.cantidad === 0 ? undefined : reputacionPrestador.cantidad
              }
            />
            <p className={estilos.ubicacion}>
              <IconoPin className={estilos.iconoPin} />
              {prestador.municipioPrincipal.nombreMunicipio}, NIC
            </p>
          </div>
          <p className={estilos.presentacion}>{prestador.descripcion}</p>
          <p className={estilos.cobertura}>{prestador.descripcionCobertura}</p>
          <InsigniaResponsable prestador={prestador} />
        </div>
      </div>

      <MetricasDestacadas
        prestador={prestador}
        cantidadServicios={servicios.length}
        reputacion={reputacionPrestador}
      />

      <div className={estilos.acciones}>
        <Boton
          variante="primario"
          disabled={!puedeContactar}
          onClick={() => {
            if (primerServicio === undefined) {
              return;
            }
            void navegar(rutaDeDetalleDeServicio(primerServicio.idServicioPublicado));
          }}
        >
          Contactar
        </Boton>
        <Boton
          variante="contorno"
          aria-pressed={sigue}
          onClick={() => setSigue((actual) => !actual)}
        >
          {sigue ? 'Siguiendo' : 'Seguir'}
        </Boton>
      </div>
    </section>
  );
}

function AvatarPublico({ prestador }: { prestador: DatosDePrestador }) {
  if (prestador.urlImagenPerfil !== null) {
    return (
      <img
        className={estilos.avatar}
        src={prestador.urlImagenPerfil}
        alt={`Foto de ${prestador.nombrePublico}`}
      />
    );
  }

  const iniciales = inicialesDeNombre(prestador.nombrePublico);

  return (
    <span className={estilos.avatar} aria-hidden="true">
      {iniciales === '' ? <IconoUsuario /> : iniciales}
    </span>
  );
}

function MetricasDestacadas({
  prestador,
  cantidadServicios,
  reputacion,
}: {
  prestador: DatosDePrestador;
  cantidadServicios: number;
  reputacion: ReputacionPorRol;
}) {
  const satisfaccion = porcentajeDeSatisfaccion(reputacion);

  return (
    <ul className={estilos.metricas}>
      <li className={estilos.metrica}>
        <IconoUsuario className={estilos.iconoMetrica} />
        <p className={estilos.valorMetrica}>{nombreDelTipoPrestador(prestador.tipoPrestador)}</p>
        <p className={estilos.etiquetaMetrica}>Tipo de prestador</p>
      </li>
      <li className={estilos.metrica}>
        <IconoMaletin className={estilos.iconoMetrica} />
        <p className={estilos.valorMetrica}>{cantidadServicios}</p>
        <p className={estilos.etiquetaMetrica}>
          {cantidadServicios === 1 ? 'servicio publicado' : 'servicios publicados'}
        </p>
      </li>
      <li className={estilos.metrica}>
        <IconoPulgarArriba className={estilos.iconoMetrica} />
        {satisfaccion === null ? (
          <>
            <p className={estilos.valorMetrica}>—</p>
            <p className={estilos.etiquetaMetrica}>Sin calificaciones</p>
          </>
        ) : (
          <>
            <p className={estilos.valorMetrica}>{satisfaccion}%</p>
            <p className={estilos.etiquetaMetrica}>clientes satisfechos</p>
          </>
        )}
      </li>
    </ul>
  );
}

function ServiciosOfrecidos({ servicios }: { servicios: ResumenPublicoDeServicio[] }) {
  return (
    <section className={estilos.tarjetaLateral} aria-labelledby="titulo-servicios-publicos">
      <h2 className={estilos.tituloDeSeccion} id="titulo-servicios-publicos">
        Servicios
      </h2>
      {servicios.length === 0 ? (
        <p className={estilos.vacio}>No hay servicios activos en este momento.</p>
      ) : (
        <ul className={estilos.listaDeServicios}>
          {servicios.map((servicio, indice) => {
            const Icono = iconoParaServicio(servicio.nombreCategoria, indice);
            return (
              <li key={servicio.idServicioPublicado}>
                <Link
                  className={estilos.filaDeServicio}
                  to={rutaDeDetalleDeServicio(servicio.idServicioPublicado)}
                >
                  <span className={estilos.iconoDeServicio}>
                    <Icono />
                  </span>
                  <span className={estilos.datosDeServicio}>
                    <span className={estilos.nombreDeServicio}>{servicio.nombre}</span>
                    <span className={estilos.precioDeServicio}>
                      {precioEnFila(servicio.precioReferencia)}
                    </span>
                  </span>
                  <IconoChevronDerecha className={estilos.chevron} />
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

function ResenasDeClientes({ reputacion }: { reputacion: ReputacionPorRol }) {
  const nota = notaVisible(reputacion.promedio);

  return (
    <section className={estilos.tarjetaLateral} aria-labelledby="titulo-resenas-publico">
      <h2 className={estilos.tituloDeSeccion} id="titulo-resenas-publico">
        Reseñas de clientes
      </h2>
      {nota === null ? (
        <p className={estilos.vacio}>
          {SIN_CALIFICACIONES} todavía. Este prestador aún no completó servicios calificados.
        </p>
      ) : (
        <article className={estilos.testimonio}>
          <span className={estilos.avatarCliente} aria-hidden="true">
            <IconoUsuario />
          </span>
          <div className={estilos.cuerpoTestimonio}>
            <p className={estilos.origenResena}>Clientes de Moica</p>
            <EstrellasCalificacion
              calificacion={reputacion.promedio}
              totalCalificaciones={reputacion.cantidad}
            />
            <p className={estilos.notaDeResena} aria-hidden="true">
              {nota}
            </p>
            <p className={estilos.comentarioResena}>
              Según {conteoDeCalificaciones(reputacion.cantidad)} de solicitudes completadas.
            </p>
          </div>
        </article>
      )}
    </section>
  );
}

function PortafolioPublico({ portafolio }: { portafolio: PerfilPublico['portafolio'] }) {
  return (
    <section className={estilos.tarjetaPortafolio} aria-labelledby="titulo-portafolio-publico">
      <h2 className={estilos.tituloDeSeccion} id="titulo-portafolio-publico">
        Portafolio
      </h2>
      {portafolio.length === 0 ? (
        <p className={estilos.vacio}>Este prestador todavía no publicó trabajos.</p>
      ) : (
        <ul className={estilos.portafolio}>
          {portafolio.map((trabajo) => (
            <li key={trabajo.idTrabajo} className={estilos.trabajo}>
              {trabajo.imagenes.length > 0 ? (
                <div className={estilos.miniaturas}>
                  {trabajo.imagenes.map((imagen) => (
                    <img
                      key={imagen.idImagenTrabajoPortafolio}
                      className={estilos.miniatura}
                      src={imagen.urlImagen}
                      alt={imagen.textoAlternativo ?? trabajo.titulo}
                      loading="lazy"
                    />
                  ))}
                </div>
              ) : null}
              <h3 className={estilos.tituloDeTrabajo}>{trabajo.titulo}</h3>
              <p className={estilos.descripcionTrabajo}>{trabajo.descripcion}</p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

type IconoDeFila = ComponentType<SVGProps<SVGSVGElement>>;

const ICONOS_POR_TURNO: IconoDeFila[] = [IconoCasa, IconoHerramienta, IconoMaletin, IconoReloj];

function iconoParaServicio(nombreCategoria: string, indice: number): IconoDeFila {
  const clave = nombreCategoria.toLowerCase();
  if (clave.includes('hogar')) {
    return IconoCasa;
  }
  if (clave.includes('construc') || clave.includes('repara')) {
    return IconoHerramienta;
  }
  if (clave.includes('urgen') || clave.includes('emergen')) {
    return IconoReloj;
  }
  return ICONOS_POR_TURNO[indice % ICONOS_POR_TURNO.length] ?? IconoMaletin;
}
