package com.moica.prestador;

import static org.assertj.core.api.Assertions.assertThat;

import com.moica.auth.EscenarioDeSeguridad;
import com.moica.comun.almacenamiento.AlmacenamientoDePrueba;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Punto de partida común de las pruebas de perfil, contactos, portafolio e imágenes de P4.
 *
 * <p>Amplía el escenario de seguridad con lo que todas necesitan: el catálogo limpio de restos de
 * otras pruebas, el doble de almacenamiento reiniciado, una sesión ya iniciada y fábricas de
 * imágenes mínimas con la firma binaria real de cada formato.
 */
public abstract class EscenarioDePrestador extends EscenarioDeSeguridad {

  protected static final String RUTA_PERFIL = "/api/prestador/perfil";
  protected static final String RUTA_DISPONIBILIDAD = "/api/prestador/disponibilidad";
  protected static final String RUTA_IMAGEN = "/api/prestador/perfil/imagen";
  protected static final String RUTA_CONTACTOS = "/api/prestador/contactos";
  protected static final String RUTA_TRABAJOS = "/api/prestador/portafolio/trabajos";
  protected static final String RUTA_CATALOGO = "/api/catalogos/departamentos";

  @Autowired protected AlmacenamientoDePrueba almacenamiento;

  @BeforeEach
  void prepararEscenarioDePrestador() {
    // El territorio que agregan algunas pruebas se retira; el de la migración
    // (Managua y sus municipios) se conserva, porque es parte del esquema.
    jdbc.update(
        """
        DELETE FROM municipio
        WHERE id_departamento <> (SELECT id_departamento FROM departamento WHERE nombre = 'Managua')
        """);
    jdbc.update("DELETE FROM departamento WHERE nombre <> 'Managua'");

    almacenamiento.reiniciar();
    iniciarSesion(navegador);
  }

  /** El identificador del municipio de Managua, resuelto contra el catálogo real. */
  protected Integer idMunicipioManagua() {
    return jdbc.queryForObject(
        """
        SELECT m.id_municipio FROM municipio m
        JOIN departamento d ON d.id_departamento = m.id_departamento
        WHERE d.nombre = 'Managua' AND m.nombre = 'Managua'
        """,
        Integer.class);
  }

  /** Un municipio real de un departamento no habilitado, creado para la prueba. */
  protected Integer municipioDeDepartamentoNoHabilitado() {
    jdbc.update("INSERT INTO departamento (nombre, habilitado) VALUES ('Rivas', FALSE)");
    jdbc.update(
        """
        INSERT INTO municipio (id_departamento, nombre)
        SELECT id_departamento, 'Tola' FROM departamento WHERE nombre = 'Rivas'
        """);
    return jdbc.queryForObject(
        "SELECT id_municipio FROM municipio WHERE nombre = 'Tola'", Integer.class);
  }

  /** Crea el perfil de la sesión actual y da por hecho que sale bien. */
  protected HttpResponse<String> crearPerfil() {
    HttpResponse<String> respuesta = navegador.post(RUTA_PERFIL, solicitudDePerfil());
    assertThat(respuesta.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    return respuesta;
  }

  protected Map<String, Object> solicitudDePerfil() {
    return Map.of(
        "nombrePublico", "Taller La Esperanza",
        "descripcion", "Reparaciones eléctricas a domicilio con diez años de experiencia.",
        "tipoPrestador", "INDEPENDIENTE",
        "idMunicipioPrincipal", idMunicipioManagua(),
        "descripcionCobertura", "Distritos I y II de Managua, alrededores de la UCA.");
  }

  protected void restringirCuenta(String correo) {
    jdbc.update(
        """
        UPDATE usuario
        SET estado_cuenta = 'RESTRINGIDA_TEMPORAL',
            fecha_fin_estado_cuenta = CURRENT_TIMESTAMP + INTERVAL '1 day'
        WHERE correo_electronico = ?
        """,
        correo);
  }

  protected void suspenderCuenta(String correo) {
    jdbc.update(
        """
        UPDATE usuario
        SET estado_cuenta = 'SUSPENDIDA_TEMPORAL',
            fecha_fin_estado_cuenta = CURRENT_TIMESTAMP + INTERVAL '1 day'
        WHERE correo_electronico = ?
        """,
        correo);
  }

  /** Un PNG mínimo pero con la firma real de ocho bytes. */
  protected static byte[] imagenPng() {
    return conFirma(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}, 64);
  }

  /** Un JPEG mínimo pero con la firma real de tres bytes. */
  protected static byte[] imagenJpeg() {
    return conFirma(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 64);
  }

  /** Un WebP mínimo: contenedor RIFF con la marca WEBP en los bytes 8 a 11. */
  protected static byte[] imagenWebp() {
    byte[] contenido = new byte[64];
    byte[] riff = {0x52, 0x49, 0x46, 0x46};
    byte[] webp = {0x57, 0x45, 0x42, 0x50};
    System.arraycopy(riff, 0, contenido, 0, riff.length);
    System.arraycopy(webp, 0, contenido, 8, webp.length);
    return contenido;
  }

  private static byte[] conFirma(byte[] firma, int tamano) {
    byte[] contenido = new byte[tamano];
    System.arraycopy(firma, 0, contenido, 0, firma.length);
    Arrays.fill(contenido, firma.length, tamano, (byte) 0x2A);
    return contenido;
  }
}
