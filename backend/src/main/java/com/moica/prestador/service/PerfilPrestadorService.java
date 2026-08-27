package com.moica.prestador.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.catalogo.dto.UbicacionDeMunicipio;
import com.moica.catalogo.service.CatalogoTerritorialService;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.prestador.dto.DatosDePerfilPrestador;
import com.moica.prestador.dto.ResumenDePerfilPrestador;
import com.moica.prestador.dto.SolicitudDeDisponibilidad;
import com.moica.prestador.dto.SolicitudDePerfilPrestador;
import com.moica.prestador.entity.NivelVerificacionPrestador;
import com.moica.prestador.entity.PerfilPrestador;
import com.moica.prestador.repository.PerfilPrestadorRepository;
import com.moica.usuario.entity.EstadoCuenta;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reglas del perfil de prestador propio.
 *
 * <p>Toda operación parte del sujeto de la sesión: ningún método acepta decidir sobre qué cuenta
 * actuar, así que operar sobre el perfil de otra persona no es un permiso que se comprueba sino un
 * camino que no existe.
 *
 * <p>La definición vigente solo autoriza crear y actualizar el perfil; eliminarlo no existe a
 * propósito. El nivel de verificación no lo decide nadie desde aquí: {@link
 * #proyectarNivelDeVerificacion(Long, NivelVerificacionPrestador)} solo escribe lo que ya resolvió
 * una persona administradora en la capacidad {@code verificacion}.
 */
@Service
public class PerfilPrestadorService {

  private final PerfilPrestadorRepository repositorio;
  private final CatalogoTerritorialService catalogo;

  public PerfilPrestadorService(
      PerfilPrestadorRepository repositorio, CatalogoTerritorialService catalogo) {
    this.repositorio = repositorio;
    this.catalogo = catalogo;
  }

  /**
   * Crea el perfil de la cuenta de la sesión.
   *
   * @throws ErrorDeAplicacion si la cuenta no está activa, ya tiene perfil o el municipio no está
   *     disponible
   */
  @Transactional
  public DatosDePerfilPrestador crear(
      UsuarioAutenticado sujeto, SolicitudDePerfilPrestador solicitud) {
    exigirCuentaActiva(sujeto);

    if (repositorio.existsById(sujeto.idUsuario())) {
      throw perfilYaExiste();
    }
    UbicacionDeMunicipio ubicacion = municipioDisponible(solicitud.idMunicipioPrincipal());

    PerfilPrestador perfil =
        new PerfilPrestador(
            sujeto.idUsuario(),
            solicitud.nombrePublico(),
            solicitud.descripcion(),
            solicitud.tipoPrestador(),
            solicitud.idMunicipioPrincipal(),
            solicitud.descripcionCobertura());

    try {
      // La comprobación previa no basta: dos creaciones simultáneas desde la
      // misma cuenta solo las separa la clave primaria compartida con usuario.
      return DatosDePerfilPrestador.de(repositorio.saveAndFlush(perfil), ubicacion);
    } catch (DataIntegrityViolationException colision) {
      throw perfilYaExiste();
    }
  }

  /**
   * Devuelve el perfil de la cuenta de la sesión.
   *
   * @throws ErrorDeAplicacion si la cuenta todavía no creó su perfil
   */
  @Transactional(readOnly = true)
  public DatosDePerfilPrestador consultarPropio(UsuarioAutenticado sujeto) {
    PerfilPrestador perfil = perfilDe(sujeto.idUsuario());
    return DatosDePerfilPrestador.de(perfil, ubicacionDe(perfil));
  }

  /**
   * Sustituye los datos editables del perfil de la sesión.
   *
   * @throws ErrorDeAplicacion si la cuenta no está activa, no tiene perfil o el municipio no está
   *     disponible
   */
  @Transactional
  public DatosDePerfilPrestador actualizar(
      UsuarioAutenticado sujeto, SolicitudDePerfilPrestador solicitud) {
    exigirCuentaActiva(sujeto);

    PerfilPrestador perfil = perfilDe(sujeto.idUsuario());
    UbicacionDeMunicipio ubicacion = municipioDisponible(solicitud.idMunicipioPrincipal());

    perfil.actualizar(
        solicitud.nombrePublico(),
        solicitud.descripcion(),
        solicitud.tipoPrestador(),
        solicitud.idMunicipioPrincipal(),
        solicitud.descripcionCobertura());

    return DatosDePerfilPrestador.de(perfil, ubicacion);
  }

  /** Alterna entre {@code DISPONIBLE} y {@code NO_DISPONIBLE}. */
  @Transactional
  public DatosDePerfilPrestador cambiarDisponibilidad(
      UsuarioAutenticado sujeto, SolicitudDeDisponibilidad solicitud) {
    exigirCuentaActiva(sujeto);

    PerfilPrestador perfil = perfilDe(sujeto.idUsuario());
    perfil.cambiarDisponibilidad(solicitud.disponibilidad());

    return DatosDePerfilPrestador.de(perfil, ubicacionDe(perfil));
  }

  /**
   * Sustituye la URL de la imagen de perfil y devuelve la anterior, o {@code null} si no había.
   *
   * <p>Lo invoca la orquestación de imágenes después de subir el objeto nuevo: la URL anterior es
   * lo que le permite retirar el objeto viejo una vez que este cambio quedó persistido.
   */
  @Transactional
  public String actualizarUrlImagen(Long idPrestador, String urlImagenPerfil) {
    PerfilPrestador perfil = perfilDe(idPrestador);
    String anterior = perfil.getUrlImagenPerfil();
    perfil.cambiarUrlImagenPerfil(urlImagenPerfil);
    return anterior;
  }

  /**
   * Describe un perfil para otra capacidad, sin entregarle la entidad ni su repositorio.
   *
   * <p>Lo usa la revisión de expedientes, que necesita saber a qué perfil pertenece una solicitud y
   * qué nivel tiene hoy. Devuelve vacío si ese perfil no existe, para que quien pregunte decida qué
   * responder en lugar de recibir un error genérico.
   */
  @Transactional(readOnly = true)
  public Optional<ResumenDePerfilPrestador> resumirPerfil(Long idPrestador) {
    return repositorio.findById(idPrestador).map(ResumenDePerfilPrestador::de);
  }

  /**
   * Deja vigente en el perfil el nivel que resolvió una persona administradora.
   *
   * <p>No decide nada: la regla de qué nivel corresponde vive en la capacidad {@code verificacion},
   * que es quien conoce las solicitudes. Aquí solo se escribe, y por eso este método no comprueba
   * el estado de la cuenta: revocar la verificación de una cuenta restringida debe seguir siendo
   * posible.
   *
   * @throws ErrorDeAplicacion si el perfil no existe
   */
  @Transactional
  public void proyectarNivelDeVerificacion(
      Long idPrestador, NivelVerificacionPrestador nivelVerificacion) {
    perfilDe(idPrestador).proyectarNivelVerificacion(nivelVerificacion);
  }

  /**
   * Exige que la cuenta de la sesión pueda modificar su perfil y lo que cuelga de él.
   *
   * <p>Es la comprobación que comparten contactos, portafolio e imágenes: cuenta {@code ACTIVA} y
   * perfil ya creado. Las suspensiones no llegan hasta aquí —la cadena de seguridad las corta—,
   * pero la regla se aplica completa igualmente.
   *
   * @throws ErrorDeAplicacion si la cuenta no está activa o todavía no creó su perfil
   */
  @Transactional(readOnly = true)
  public void exigirQuePuedaModificarSuPerfil(UsuarioAutenticado sujeto) {
    exigirCuentaActiva(sujeto);
    exigirQueExistaElPerfil(sujeto.idUsuario());
  }

  /**
   * Exige que la cuenta tenga perfil, sin pedir nada sobre su estado.
   *
   * <p>Basta para las lecturas propias: una cuenta restringida conserva la consulta de lo suyo.
   *
   * @throws ErrorDeAplicacion si la cuenta todavía no creó su perfil
   */
  @Transactional(readOnly = true)
  public void exigirQueExistaElPerfil(Long idUsuario) {
    if (!repositorio.existsById(idUsuario)) {
      throw perfilNoEncontrado();
    }
  }

  private PerfilPrestador perfilDe(Long idUsuario) {
    return repositorio.findById(idUsuario).orElseThrow(PerfilPrestadorService::perfilNoEncontrado);
  }

  private UbicacionDeMunicipio ubicacionDe(PerfilPrestador perfil) {
    return catalogo
        .describirMunicipio(perfil.getIdMunicipioPrincipal())
        // La clave foránea garantiza el municipio; si faltara, el esquema
        // estaría corrupto y ocultarlo sería peor que fallar.
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "El perfil "
                        + perfil.getIdPrestador()
                        + " referencia un municipio que no existe"));
  }

  /**
   * El municipio elegido debe existir y pertenecer a un departamento habilitado.
   *
   * <p>El mensaje no distingue entre inexistente y deshabilitado: para quien elige en el formulario
   * ninguna de las dos cosas es una opción válida.
   */
  private UbicacionDeMunicipio municipioDisponible(Integer idMunicipio) {
    return catalogo
        .describirMunicipio(idMunicipio)
        .filter(UbicacionDeMunicipio::departamentoHabilitado)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.BAD_REQUEST,
                    "MUNICIPIO_NO_DISPONIBLE",
                    "El municipio elegido no está disponible en Moica."));
  }

  private static void exigirCuentaActiva(UsuarioAutenticado sujeto) {
    if (sujeto.estadoCuenta() != EstadoCuenta.ACTIVA) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "CUENTA_RESTRINGIDA",
          "Tu cuenta está restringida y por ahora no puede modificar su perfil de prestador.");
    }
  }

  private static ErrorDeAplicacion perfilYaExiste() {
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "PERFIL_YA_EXISTE",
        "Esta cuenta ya tiene un perfil de prestador. Puedes actualizarlo, pero no crear otro.");
  }

  private static ErrorDeAplicacion perfilNoEncontrado() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND,
        "PERFIL_NO_ENCONTRADO",
        "Esta cuenta todavía no tiene un perfil de prestador.");
  }
}
