package com.moica.auth.service;

import com.moica.auth.entity.MotivoRevocacionSesion;
import com.moica.auth.entity.Sesion;
import com.moica.auth.repository.SesionRepository;
import com.moica.auth.seguridad.PropiedadesDeSeguridad;
import com.moica.comun.error.ErrorDeAplicacion;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida de las sesiones: abrirlas, comprobar si siguen valiendo y revocarlas.
 *
 * <p>La fila {@code sesion} es la fuente de verdad. El JWT solo la señala mediante su {@code jti},
 * así que una sesión revocada deja de conceder acceso en la petición siguiente aunque el token aún
 * no haya alcanzado su expiración.
 */
@Service
public class SesionService {

  private static final SecureRandom ALEATORIO = new SecureRandom();

  /**
   * 32 bytes aleatorios: 43 caracteres en Base64 sin relleno, dentro del VARCHAR(64) de la tabla.
   */
  private static final int BYTES_DEL_IDENTIFICADOR = 32;

  private final SesionRepository repositorio;
  private final PropiedadesDeSeguridad propiedades;

  public SesionService(SesionRepository repositorio, PropiedadesDeSeguridad propiedades) {
    this.repositorio = repositorio;
    this.propiedades = propiedades;
  }

  /** Abre una sesión que expira al cabo de la duración configurada. */
  @Transactional
  public Sesion abrir(Long idUsuario) {
    OffsetDateTime inicio = OffsetDateTime.now();

    Sesion sesion =
        new Sesion(
            idUsuario,
            generarIdentificadorDeToken(),
            inicio,
            inicio.plus(propiedades.duracionDeSesion()));

    return repositorio.save(sesion);
  }

  /**
   * Devuelve la sesión que señala un {@code jti} solo si sigue concediendo acceso.
   *
   * <p>Vacío significa las tres cosas a la vez: no existe, expiró o fue revocada. Ninguna de ellas
   * autentica.
   */
  @Transactional(readOnly = true)
  public Optional<Sesion> buscarVigente(String identificadorToken) {
    return repositorio
        .findByIdentificadorToken(identificadorToken)
        .filter(sesion -> sesion.estaVigente(OffsetDateTime.now()));
  }

  /**
   * Recupera una sesión por su identificador.
   *
   * @throws ErrorDeAplicacion si la sesión no existe
   */
  @Transactional(readOnly = true)
  public Sesion obtener(Long idSesion) {
    return repositorio
        .findById(idSesion)
        .orElseThrow(
            () ->
                new ErrorDeAplicacion(
                    HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", "La sesión no existe."));
  }

  /**
   * Revoca una sesión, dejando constancia del instante y del motivo.
   *
   * <p>Revocar una sesión ya revocada no cambia nada: se conserva la primera revocación.
   */
  @Transactional
  public void revocar(Long idSesion, MotivoRevocacionSesion motivo) {
    repositorio
        .findById(idSesion)
        .filter(sesion -> sesion.getFechaRevocacion() == null)
        .ifPresent(sesion -> sesion.revocar(OffsetDateTime.now(), motivo));
  }

  private static String generarIdentificadorDeToken() {
    byte[] bytes = new byte[BYTES_DEL_IDENTIFICADOR];
    ALEATORIO.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
