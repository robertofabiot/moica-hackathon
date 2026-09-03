import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router';

import logoHorizontal from '../../../assets/logos/moica-horizontal.png';
import logoIcono from '../../../assets/logos/moica-icono.svg';
import { RUTA_ADMIN } from '../../admin';
import { RUTA_SEGURIDAD, useCierreSesion, useSesionActual } from '../../auth';
import { RUTA_EXPLORAR } from '../../busqueda';
import { usePrestadorPublico } from '../../busqueda/hooks/useBusquedaPublica';
import { RUTA_PRESTADOR } from '../../prestador';
import { usePerfilPrestador } from '../../prestador/hooks/usePerfilPrestador';
import { RUTA_NUEVO_SERVICIO, RUTA_SERVICIOS } from '../../servicio';
import { useServiciosPropios } from '../../servicio/hooks/useServiciosPropios';
import { RUTA_MENSAJES, RUTA_SOLICITUDES, rutaDeSolicitud } from '../../solicitud';
import {
  useSolicitudesEnviadas,
  useSolicitudesRecibidas,
} from '../../solicitud/hooks/useSolicitudes';
import {
  conversacionesDeBandeja,
  fechaVisible,
  nombreDelEstado,
} from '../../solicitud/presentacion';
import type { EstadoSolicitud } from '../../solicitud/tipos';
import {
  BarraLateral,
  Boton,
  EstrellasCalificacion,
  IconoCampana,
  IconoCasa,
  IconoEstrella,
  IconoMaletin,
  IconoMensaje,
  IconoReloj,
  TarjetaMetrica,
} from '../../../comun/componentes/ui';
import { actividadReciente, inicialDe, primerNombreDe, tareasProximas } from '../presentacion';
import { RUTA_PANEL } from '../rutas';
import estilos from './panel.module.css';

const DESTINOS_DE_BARRA = {
  inicio: RUTA_PANEL,
  mensajes: RUTA_MENSAJES,
  perfil: RUTA_PRESTADOR,
  configuracion: RUTA_SEGURIDAD,
};

/**
 * Panel de actividad: métricas, solicitudes recientes y atajos según el estado real.
 *
 * No inventa cifras. Si la cuenta todavía no es prestadora, el conteo de servicios
 * queda en 0 y no se llama a `GET /api/prestador/servicios`.
 */
export default function PanelUsuario() {
  const sesion = useSesionActual();
  const usuario = sesion.data?.usuario;
  const perfil = usePerfilPrestador();
  const servicios = useServiciosPropios({ enabled: Boolean(perfil.data) });
  const enviadas = useSolicitudesEnviadas();
  const recibidas = useSolicitudesRecibidas();
  const publico = usePrestadorPublico(perfil.data?.idPrestador);

  const primerNombre = primerNombreDe(usuario?.nombreCompleto ?? '');
  const solicitudesListas = !enviadas.isLoading && !recibidas.isLoading;
  const hilos = useMemo(
    () => conversacionesDeBandeja(enviadas.data, recibidas.data),
    [enviadas.data, recibidas.data]
  );
  const actividad = useMemo(
    () => actividadReciente(enviadas.data, recibidas.data, usuario?.idUsuario),
    [enviadas.data, recibidas.data, usuario?.idUsuario]
  );

  const serviciosPublicados = perfil.isLoading
    ? '—'
    : perfil.data == null
      ? 0
      : servicios.isLoading
        ? '—'
        : (servicios.data ?? []).filter((servicio) => servicio.estado === 'ACTIVO').length;

  const cantidadMensajes = solicitudesListas ? hilos.length : '—';
  const cantidadContrataciones = solicitudesListas
    ? (enviadas.data?.length ?? 0) + (recibidas.data?.length ?? 0)
    : '—';

  const promedio = publico.data?.reputacionPrestador.promedio ?? null;
  const calificacion =
    perfil.data == null || promedio === null ? (
      '—'
    ) : (
      <EstrellasCalificacion
        calificacion={promedio}
        totalCalificaciones={
          publico.data?.reputacionPrestador.cantidad === 0
            ? undefined
            : publico.data?.reputacionPrestador.cantidad
        }
      />
    );

  const esSoloCliente = perfil.data === null;
  const tareas = useMemo(
    () =>
      tareasProximas({
        pendientesRecibidas: (recibidas.data ?? []).filter(
          (solicitud) => solicitud.estadoActual === 'PENDIENTE'
        ).length,
        perfil: perfil.isLoading ? undefined : (perfil.data ?? null),
        cantidadDeServicios: servicios.data?.length ?? 0,
        serviciosConsultados: servicios.data !== undefined,
        destinoSolicitudes: RUTA_SOLICITUDES,
        destinoPerfil: RUTA_PRESTADOR,
        destinoNuevoServicio: RUTA_NUEVO_SERVICIO,
      }),
    [recibidas.data, perfil.isLoading, perfil.data, servicios.data]
  );

  return (
    <div className={estilos.pagina}>
      <div className={estilos.barraLateral}>
        <BarraLateral itemActivo="inicio" destinos={DESTINOS_DE_BARRA} />
      </div>
      <EncabezadoDelPanel
        nombreCompleto={usuario?.nombreCompleto ?? ''}
        esAdministrador={usuario?.esAdministrador === true}
      />
      <main className={estilos.principal}>
        <header className={estilos.bienvenida}>
          <h1 className={estilos.saludo}>
            {primerNombre === '' ? '¡Hola! 👋' : `¡Hola, ${primerNombre}! 👋`}
          </h1>
          <p className={estilos.subtitulo}>Este es el resumen de tu actividad.</p>
        </header>

        <section className={estilos.metricas} aria-label="Métricas clave">
          <TarjetaMetrica
            titulo="Servicios publicados"
            valor={serviciosPublicados}
            icono={<IconoMaletin />}
            destino={RUTA_SERVICIOS}
          />
          <TarjetaMetrica
            titulo="Mensajes"
            valor={cantidadMensajes}
            icono={<IconoMensaje />}
            destino={RUTA_MENSAJES}
          />
          <TarjetaMetrica
            titulo="Contrataciones"
            valor={cantidadContrataciones}
            icono={<IconoCasa />}
            destino={RUTA_SOLICITUDES}
          />
          <TarjetaMetrica titulo="Calificación" valor={calificacion} icono={<IconoEstrella />} />
        </section>

        <div className={estilos.cuerpo}>
          <section className={estilos.tarjeta} aria-labelledby="titulo-actividad">
            <h2 className={estilos.tituloDeTarjeta} id="titulo-actividad">
              Actividad reciente
            </h2>
            {enviadas.isLoading || recibidas.isLoading ? (
              <p className={estilos.estadoDeCarga} role="status">
                Cargando actividad…
              </p>
            ) : actividad.length === 0 ? (
              <p className={estilos.vacio}>
                Todavía no tienes solicitudes. Explora servicios para contratar o espera a que te
                escriban.{' '}
                <Link to={RUTA_EXPLORAR}>Explorar servicios</Link>
              </p>
            ) : (
              <ul className={estilos.listaActividad}>
                {actividad.map((item) => (
                  <li key={item.idSolicitudServicio} className={estilos.itemActividad}>
                    <Link
                      className={estilos.enlaceActividad}
                      to={rutaDeSolicitud(item.idSolicitudServicio)}
                    >
                      <span className={estilos.iconoActividad} aria-hidden="true">
                        <IconoReloj />
                      </span>
                      <span className={estilos.cuerpoActividad}>
                        <span className={estilos.descripcionActividad}>{item.descripcion}</span>
                        <time className={estilos.fechaActividad} dateTime={item.fechaCreacion}>
                          {fechaVisible(item.fechaCreacion)}
                        </time>
                      </span>
                      <span className={claseDeEstado(item.estado)}>
                        {nombreDelEstado(item.estado)}
                      </span>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
            <Link className={estilos.enlaceInferior} to={RUTA_SOLICITUDES}>
              Ver todas las solicitudes →
            </Link>
          </section>

          <div className={estilos.columnaTareas}>
            <section className={estilos.tarjeta} aria-labelledby="titulo-tareas">
              <h2 className={estilos.tituloDeTarjeta} id="titulo-tareas">
                Próximas tareas
              </h2>
              {tareas.length === 0 ? (
                <p className={estilos.vacio}>No tienes tareas pendientes. Vas al día.</p>
              ) : (
                <ul className={estilos.listaTareas}>
                  {tareas.map((tarea) => (
                    <li key={tarea.id}>
                      <Link className={estilos.enlaceTarea} to={tarea.destino}>
                        {tarea.texto}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <aside className={estilos.promocion}>
              <p className={estilos.tituloPromocion}>Mejora tu visibilidad</p>
              <p className={estilos.textoPromocion}>
                Ofrece tus habilidades y llega a más clientes en tu comunidad.
              </p>
              <Boton
                variante="primario"
                to={esSoloCliente ? RUTA_EXPLORAR : RUTA_NUEVO_SERVICIO}
              >
                {esSoloCliente ? 'Explorar servicios' : 'Publicar servicio'}
              </Boton>
            </aside>
          </div>
        </div>
      </main>
    </div>
  );
}

function EncabezadoDelPanel({
  nombreCompleto,
  esAdministrador,
}: {
  nombreCompleto: string;
  esAdministrador: boolean;
}) {
  const inicial = inicialDe(nombreCompleto);

  return (
    <header className={estilos.barraSuperior}>
      <Link className={estilos.marca} to="/" aria-label="Moica, ir al inicio">
        <img className={estilos.logoIcono} src={logoIcono} alt="" />
        <img className={estilos.logoCompleto} src={logoHorizontal} alt="" />
      </Link>
      <div className={estilos.accionesDeBarra}>
        <button type="button" className={estilos.botonIcono} aria-label="Notificaciones">
          <IconoCampana />
        </button>
        {inicial === '' ? null : (
          <MenuUsuarioAvatar
            nombreCompleto={nombreCompleto}
            inicial={inicial}
            esAdministrador={esAdministrador}
          />
        )}
      </div>
    </header>
  );
}

function MenuUsuarioAvatar({
  nombreCompleto,
  inicial,
  esAdministrador,
}: {
  nombreCompleto: string;
  inicial: string;
  esAdministrador: boolean;
}) {
  const [abierto, setAbierto] = useState(false);
  const cierre = useCierreSesion();
  const contenedorRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function manejarClicFuera(evento: MouseEvent) {
      if (contenedorRef.current && !contenedorRef.current.contains(evento.target as Node)) {
        setAbierto(false);
      }
    }
    if (abierto) {
      document.addEventListener('mousedown', manejarClicFuera);
    }
    return () => {
      document.removeEventListener('mousedown', manejarClicFuera);
    };
  }, [abierto]);

  return (
    <div ref={contenedorRef} className={estilos.menuUsuarioContenedor}>
      <button
        type="button"
        className={estilos.avatarBoton}
        aria-expanded={abierto}
        aria-haspopup="true"
        aria-label={`Cuenta de ${nombreCompleto}`}
        onClick={() => setAbierto((previo) => !previo)}
      >
        {inicial}
      </button>
      {abierto ? (
        <ul className={estilos.panelDeSesion} role="menu">
          <li>
            <Link
              className={estilos.opcionDeSesion}
              to={RUTA_PANEL}
              onClick={() => setAbierto(false)}
            >
              Panel principal
            </Link>
          </li>
          <li>
            <Link
              className={estilos.opcionDeSesion}
              to={RUTA_PRESTADOR}
              onClick={() => setAbierto(false)}
            >
              Mi perfil de prestador
            </Link>
          </li>
          <li>
            <Link
              className={estilos.opcionDeSesion}
              to={RUTA_SERVICIOS}
              onClick={() => setAbierto(false)}
            >
              Mis servicios
            </Link>
          </li>
          <li>
            <Link
              className={estilos.opcionDeSesion}
              to={RUTA_SOLICITUDES}
              onClick={() => setAbierto(false)}
            >
              Mis solicitudes
            </Link>
          </li>
          <li>
            <Link
              className={estilos.opcionDeSesion}
              to={RUTA_SEGURIDAD}
              onClick={() => setAbierto(false)}
            >
              Seguridad de la cuenta
            </Link>
          </li>
          {esAdministrador ? (
            <li>
              <Link
                className={estilos.opcionDeSesion}
                to={RUTA_ADMIN}
                onClick={() => setAbierto(false)}
              >
                Área administrativa
              </Link>
            </li>
          ) : null}
          <li>
            <button
              type="button"
              className={estilos.opcionDeSesion}
              onClick={() => {
                setAbierto(false);
                cierre.solicitarCierre();
              }}
              disabled={cierre.isPending}
            >
              {cierre.isPending ? 'Cerrando sesión…' : 'Cerrar sesión'}
            </button>
          </li>
        </ul>
      ) : null}
    </div>
  );
}

function claseDeEstado(estado: EstadoSolicitud): string {
  const extra =
    estado === 'PENDIENTE'
      ? estilos.estadoPendiente
      : estado === 'ACEPTADA'
        ? estilos.estadoAceptada
        : estado === 'COMPLETADA'
          ? estilos.estadoCompletada
          : undefined;
  return [estilos.estado, extra].filter((parte) => parte !== undefined && parte !== '').join(' ');
}
