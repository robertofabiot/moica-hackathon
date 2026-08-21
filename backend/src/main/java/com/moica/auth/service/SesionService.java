package com.moica.auth.service;

import com.moica.auth.entity.Sesion;
import com.moica.auth.repository.SesionRepository;
import com.moica.auth.seguridad.PropiedadesDeSeguridad;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo de vida de las sesiones.
 *
 * <p>La fila {@code sesion} es la fuente de verdad: el JWT solo la señala mediante su {@code jti}.
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

  private static String generarIdentificadorDeToken() {
    byte[] bytes = new byte[BYTES_DEL_IDENTIFICADOR];
    ALEATORIO.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
