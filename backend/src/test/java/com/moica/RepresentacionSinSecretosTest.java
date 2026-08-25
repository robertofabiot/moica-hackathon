package com.moica;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.auth.dto.ActivacionDeSegundoFactor;
import com.moica.auth.dto.SolicitudDeCambioDeClave;
import com.moica.auth.dto.SolicitudDeCodigoTotp;
import com.moica.auth.dto.SolicitudDeDesactivacionDeSegundoFactor;
import com.moica.auth.dto.SolicitudDeInicioSesion;
import com.moica.auth.seguridad.PropiedadesDeSegundoFactor;
import com.moica.auth.seguridad.PropiedadesDeSeguridad;
import com.moica.auth.service.AutenticacionService;
import com.moica.comun.almacenamiento.PropiedadesDeAlmacenamiento;
import com.moica.usuario.dto.SolicitudDeRegistro;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Ningún objeto que lleve un secreto debe revelarlo al convertirse en texto.
 *
 * <p>Es defensa en profundidad y no la protección principal: Moica no registra estos objetos. Lo
 * que se evita es que baste con interpolar uno —en un aviso de registro, en el mensaje de una
 * excepción o en un depurador— para que una contraseña, un JWT o un secreto TOTP acaben en un
 * archivo. La representación que el compilador genera para un {@code record} incluye todos sus
 * componentes, así que sin esta comprobación un componente sensible sale por omisión.
 *
 * <p>Cada valor sensible es un centinela distinto y reconocible: la prueba falla si aparece
 * literalmente, sin depender de cómo esté redactada la representación.
 */
class RepresentacionSinSecretosTest {

  private static final String CLAVE = "centinela-contrasena-en-claro";
  private static final String CLAVE_NUEVA = "centinela-contrasena-nueva";
  private static final String CODIGO = "142857";
  private static final String SECRETO_TOTP = "CENTINELASECRETOTOTPBASE32AAAAAA";
  private static final String TOKEN = "centinela.jwt.de-sesion";
  private static final String SECRETO_JWT = "centinela-secreto-jwt-de-al-menos-treinta-y-dos-bytes";
  private static final String CLAVE_DE_CIFRADO =
      Base64.getEncoder()
          .encodeToString("centinela-clave-de-cifrado-de-32".getBytes(StandardCharsets.UTF_8));

  @Test
  void ningunaSolicitudDeAccesoRevelaLaContrasenaNiElCodigo() {
    List<Object> solicitudes =
        List.of(
            new SolicitudDeRegistro("Persona de prueba", "persona@moica.test", CLAVE),
            new SolicitudDeInicioSesion("persona@moica.test", CLAVE),
            new SolicitudDeCambioDeClave(CLAVE, CLAVE_NUEVA),
            new SolicitudDeCodigoTotp(CODIGO),
            new SolicitudDeDesactivacionDeSegundoFactor(CLAVE, CODIGO));

    assertThat(solicitudes)
        .allSatisfy(
            solicitud ->
                assertThat(solicitud.toString())
                    .doesNotContain(CLAVE)
                    .doesNotContain(CLAVE_NUEVA)
                    .doesNotContain(CODIGO));
  }

  @Test
  void describeLoQueNoEsSecretoParaQueLaRepresentacionSigaSirviendo() {
    assertThat(new SolicitudDeInicioSesion("persona@moica.test", CLAVE).toString())
        .contains("persona@moica.test");
    assertThat(new SolicitudDeRegistro("Persona de prueba", "persona@moica.test", CLAVE).toString())
        .contains("Persona de prueba")
        .contains("persona@moica.test");
  }

  @Test
  void laActivacionNoRevelaNiLaClaveManualNiLaUriQueLaLleva() {
    ActivacionDeSegundoFactor activacion =
        new ActivacionDeSegundoFactor(
            SECRETO_TOTP,
            "otpauth://totp/Moica%3Apersona%40moica.test?secret=" + SECRETO_TOTP + "&issuer=Moica",
            6,
            30);

    assertThat(activacion.toString())
        .doesNotContain(SECRETO_TOTP)
        .doesNotContain("otpauth://")
        .contains("digitos=6");
  }

  @Test
  void laSesionIniciadaNoRevelaElTokenQueViajaEnLaCookie() {
    assertThat(new AutenticacionService.SesionIniciada(TOKEN, null).toString())
        .doesNotContain(TOKEN);
  }

  @Test
  void laConfiguracionNoRevelaNiElSecretoJwtNiLaClaveDeCifrado() {
    assertThat(new PropiedadesDeSeguridad(SECRETO_JWT, Duration.ofDays(7), false).toString())
        .doesNotContain(SECRETO_JWT)
        .contains("duracionDeSesion=PT168H");

    assertThat(
            new PropiedadesDeSegundoFactor(CLAVE_DE_CIFRADO, 6, Duration.ofSeconds(30), 1)
                .toString())
        .doesNotContain(CLAVE_DE_CIFRADO);
  }

  @Test
  void laConfiguracionDelAlmacenamientoNoRevelaElSecretoDelToken() {
    String secretoR2 = "centinela-secreto-del-token-r2";

    assertThat(
            new PropiedadesDeAlmacenamiento(
                    "cuenta", "access-key", secretoR2, "bucket", "https://imagenes.moica.ni")
                .toString())
        .doesNotContain(secretoR2)
        .contains("bucketPublico=bucket");
  }
}
