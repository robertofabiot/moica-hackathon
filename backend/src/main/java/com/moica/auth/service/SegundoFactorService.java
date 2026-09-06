package com.moica.auth.service;

import com.moica.auth.dto.ActivacionDeSegundoFactor;
import com.moica.auth.dto.RespuestaDeSegundoFactor;
import com.moica.auth.entity.MotivoRevocacionSesion;
import com.moica.auth.entity.SegundoFactorUsuario;
import com.moica.auth.repository.SegundoFactorUsuarioRepository;
import com.moica.auth.seguridad.AlgoritmoTotp;
import com.moica.auth.seguridad.CifradoDeSecretos;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.usuario.dto.DatosDeUsuario;
import com.moica.usuario.service.UsuarioService;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida del segundo factor de una cuenta.
 *
 * <p>Recorre los tres estados del diccionario: se registra un secreto nuevo en {@code
 * PENDIENTE_ACTIVACION}, el primer código válido lo deja {@code ACTIVO} y desactivarlo lo lleva a
 * {@code DESACTIVADO}. Reactivarlo vuelve a empezar con un secreto distinto, porque el anterior
 * pudo quedarse en un teléfono que ya no se controla.
 *
 * <p>El secreto se guarda cifrado y solo sale de aquí una vez: en la respuesta que inicia la
 * activación, hacia la propia persona autenticada. Ni los registros, ni los errores, ni ninguna
 * consulta posterior lo devuelven.
 */
@Service
public class SegundoFactorService {

  private final SegundoFactorUsuarioRepository repositorio;
  private final CifradoDeSecretos cifrado;
  private final AlgoritmoTotp totp;
  private final UsuarioService usuarios;
  private final SesionService sesiones;

  public SegundoFactorService(
      SegundoFactorUsuarioRepository repositorio,
      CifradoDeSecretos cifrado,
      AlgoritmoTotp totp,
      UsuarioService usuarios,
      SesionService sesiones) {
    this.repositorio = repositorio;
    this.cifrado = cifrado;
    this.totp = totp;
    this.usuarios = usuarios;
    this.sesiones = sesiones;
  }

  /**
   * Indica si la cuenta debe presentar un código para completar sus sesiones.
   *
   * <p>Lo consulta la cadena de seguridad en cada petición autenticada: es lo que distingue una
   * sesión provisional de la sesión normal de alguien que no usa segundo factor.
   */
  @Transactional(readOnly = true)
  public boolean estaActivoEn(Long idUsuario) {
    return repositorio.findById(idUsuario).filter(SegundoFactorUsuario::estaActivo).isPresent();
  }

  /** Describe el estado del segundo factor de una cuenta. */
  @Transactional(readOnly = true)
  public RespuestaDeSegundoFactor consultar(Long idUsuario) {
    Optional<SegundoFactorUsuario> segundoFactor = repositorio.findById(idUsuario);
    boolean obligatorio = usuarios.obtener(idUsuario).esAdministrador();

    return segundoFactor
        .map(
            registro ->
                new RespuestaDeSegundoFactor(
                    registro.getEstadoSegundoFactor(), obligatorio, registro.getFechaActivacion()))
        .orElseGet(() -> new RespuestaDeSegundoFactor(null, obligatorio, null));
  }

  /**
   * Genera un secreto nuevo y devuelve lo necesario para configurar la aplicación autenticadora.
   *
   * <p>Abandonar la activación no deja nada colgado: volver a empezar sustituye el secreto
   * pendiente, de modo que solo el último entregado puede confirmarse.
   *
   * @throws ErrorDeAplicacion si el segundo factor ya está activo. Cambiarlo exige desactivarlo
   *     antes, que es la operación que pide contraseña y código
   */
  @Transactional
  public ActivacionDeSegundoFactor iniciarActivacion(Long idUsuario) {
    DatosDeUsuario usuario = usuarios.obtener(idUsuario);

    Optional<SegundoFactorUsuario> existente = repositorio.findById(idUsuario);
    if (existente.filter(SegundoFactorUsuario::estaActivo).isPresent()) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SEGUNDO_FACTOR_YA_ACTIVO",
          "Tu segundo factor ya está activo. Desactívalo antes de configurar otro.");
    }

    String secreto = totp.generarSecreto();
    String secretoCifrado = cifrado.cifrar(secreto);

    existente.ifPresentOrElse(
        registro -> registro.reiniciarActivacion(secretoCifrado),
        () -> repositorio.save(new SegundoFactorUsuario(idUsuario, secretoCifrado)));

    return new ActivacionDeSegundoFactor(
        secreto,
        totp.uriDeConfiguracion(usuario.correoElectronico(), secreto),
        totp.digitos(),
        totp.periodoEnSegundos());
  }

  /**
   * Confirma la activación con el primer código válido.
   *
   * <p>Además da por verificada la sesión desde la que se activó: quien acaba de demostrar que
   * tiene la aplicación autenticadora no debe quedarse fuera de su propia cuenta.
   *
   * @throws ErrorDeAplicacion si no hay una activación pendiente o el código no es válido
   */
  @Transactional
  public RespuestaDeSegundoFactor confirmarActivacion(
      Long idUsuario, Long idSesion, String codigo) {

    SegundoFactorUsuario registro = obtenerRegistro(idUsuario);

    if (!registro.estaPendienteDeActivacion()) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "SEGUNDO_FACTOR_SIN_ACTIVACION_PENDIENTE",
          "No hay una activación en curso. Empieza de nuevo la configuración.");
    }

    exigirCodigoValido(registro, codigo);

    registro.activar(OffsetDateTime.now());
    sesiones.marcarSegundoFactorVerificado(idSesion);

    return new RespuestaDeSegundoFactor(
        registro.getEstadoSegundoFactor(),
        usuarios.obtener(idUsuario).esAdministrador(),
        registro.getFechaActivacion());
  }

  /**
   * Desactiva el segundo factor y revoca todas las sesiones de la cuenta.
   *
   * <p>Es un cambio de credenciales: quien tuviera una sesión abierta en otro dispositivo debe
   * volver a entrar. Un administrador no puede hacerlo mientras conserve el rol, porque {@code
   * /admin} solo admite sesiones con el segundo factor verificado.
   *
   * @throws ErrorDeAplicacion si la cuenta es administradora, si el segundo factor no está activo o
   *     si la contraseña o el código no son correctos
   */
  @Transactional
  public void desactivar(Long idUsuario, String claveActual, String codigo) {
    if (usuarios.obtener(idUsuario).esAdministrador()) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "SEGUNDO_FACTOR_OBLIGATORIO",
          "Las cuentas con permisos administrativos deben conservar el segundo factor activo.");
    }

    SegundoFactorUsuario registro = obtenerRegistro(idUsuario);
    if (!registro.estaActivo()) {
      throw segundoFactorNoActivo();
    }

    // Primero la contraseña: si no es correcta, no llega a probarse ningún
    // código y no se aprende nada sobre el segundo factor.
    usuarios.comprobarClave(idUsuario, claveActual);
    exigirCodigoValido(registro, codigo);

    registro.desactivar();
    sesiones.revocarTodasDe(idUsuario, MotivoRevocacionSesion.CAMBIO_CREDENCIALES);
  }

  /**
   * Comprueba un código contra el segundo factor activo de una cuenta.
   *
   * <p>Lo usa el paso que completa una sesión provisional. Deja registrado el instante de la última
   * verificación correcta, que es dato de auditoría del diccionario.
   *
   * @throws ErrorDeAplicacion si la cuenta no tiene el segundo factor activo o el código no es
   *     válido
   */
  @Transactional
  public void verificarCodigo(Long idUsuario, String codigo) {
    SegundoFactorUsuario registro =
        repositorio
            .findById(idUsuario)
            .filter(SegundoFactorUsuario::estaActivo)
            .orElseThrow(SegundoFactorService::segundoFactorNoActivo);

    exigirCodigoValido(registro, codigo);
    registro.registrarVerificacion(OffsetDateTime.now());
  }

  private void exigirCodigoValido(SegundoFactorUsuario registro, String codigo) {
    if (!totp.esCodigoValido(cifrado.descifrar(registro.getSecretoCifrado()), codigo)) {
      // 403 y no 401: la sesión sigue viva, lo que no se acredita es el segundo
      // factor. Un 401 haría que la interfaz creyera que la sesión murió.
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "CODIGO_INVALIDO",
          "El código no es válido. Revisa la aplicación autenticadora e inténtalo otra vez.");
    }
  }

  private SegundoFactorUsuario obtenerRegistro(Long idUsuario) {
    return repositorio.findById(idUsuario).orElseThrow(SegundoFactorService::segundoFactorNoActivo);
  }

  private static ErrorDeAplicacion segundoFactorNoActivo() {
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "SEGUNDO_FACTOR_NO_ACTIVO",
        "Esta cuenta no tiene el segundo factor activo.");
  }
}
