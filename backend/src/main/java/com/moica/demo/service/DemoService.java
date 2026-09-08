package com.moica.demo.service;

import com.moica.comun.almacenamiento.PropiedadesDeAlmacenamiento;
import com.moica.demo.DatosDeDemostracion;
import com.moica.demo.ImagenesDeDemostracion;
import com.moica.demo.PropiedadesDeDemo;
import com.moica.demo.repository.DemoRepository;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Proyección controlada para demo, nunca un sustituto del flujo de verificación real. */
@Service
public class DemoService {
  private final DemoRepository repositorio;
  private final PasswordEncoder codificador;
  private final PropiedadesDeDemo propiedades;
  private final PropiedadesDeAlmacenamiento almacenamiento;

  public DemoService(
      DemoRepository repositorio,
      PasswordEncoder codificador,
      PropiedadesDeDemo propiedades,
      PropiedadesDeAlmacenamiento almacenamiento) {
    this.repositorio = repositorio;
    this.codificador = codificador;
    this.propiedades = propiedades;
    this.almacenamiento = almacenamiento;
  }

  public record Resumen(
      int usuariosCreados,
      int perfilesCreados,
      int serviciosCreados,
      int imagenesCreadas,
      int usuariosExistentes,
      int perfilesExistentes,
      int serviciosExistentes,
      int imagenesExistentes,
      int serviciosSinMapeo) {}

  @Transactional
  public Resumen sincronizar() {
    if (!propiedades.enabled()) {
      return new Resumen(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    validarMapeo();
    repositorio.bloquearEjecuciones();
    Map<String, Long> usuarios = new HashMap<>();
    int nuevosUsuarios = 0;
    int nuevosPerfiles = 0;
    for (var p : DatosDeDemostracion.PRESTADORES) {
      var encontrados = repositorio.usuarios(p);
      Long id;
      if (encontrados.isEmpty()) {
        id = repositorio.crearUsuario(p, hashAleatorio());
        nuevosUsuarios++;
      } else {
        id = unico(encontrados);
        if (!repositorio.coincidePropietario(id, p)) {
          throw new IllegalStateException(
              "Conflicto de propiedad en una cuenta reservada de demo.");
        }
        repositorio.sincronizarUsuario(id);
      }
      if (!repositorio.existePerfil(id)) {
        nuevosPerfiles++;
      }
      repositorio.sincronizarPerfil(id, p, repositorio.municipio(p.municipio()));
      usuarios.put(p.clave(), id);
    }
    return sincronizarServicios(usuarios, nuevosUsuarios, nuevosPerfiles);
  }

  private Resumen sincronizarServicios(
      Map<String, Long> usuarios, int nuevosUsuarios, int nuevosPerfiles) {
    int nuevosServicios = 0;
    int nuevasImagenes = 0;
    int totalImagenes = 0;
    int sinMapeo = 0;
    for (var s : DatosDeDemostracion.SERVICIOS) {
      Long prestador = usuarios.get(s.prestador());
      Integer subcategoria = repositorio.subcategoria(s);
      var encontrados = repositorio.servicios(prestador, subcategoria);
      Long id;
      if (encontrados.isEmpty()) {
        id = repositorio.crearServicio(prestador, subcategoria, s);
        nuevosServicios++;
      } else {
        id = unico(encontrados);
        repositorio.sincronizarServicio(id, s);
      }
      List<String> claves = propiedades.clavesDe(s.clave());
      if (claves.isEmpty()) {
        claves = ImagenesDeDemostracion.existentes(s.clave(), almacenamiento.urlPublicaBase());
      }
      if (claves.isEmpty()) {
        sinMapeo++;
      }
      for (int orden = 0; orden < claves.size(); orden++) {
        String url = almacenamiento.urlPublicaDe(claves.get(orden));
        String texto =
            ImagenesDeDemostracion.textoAlternativo(claves.get(orden), s.nombre(), orden);
        var imagenes = repositorio.imagenes(id, orden);
        if (imagenes.isEmpty()) {
          repositorio.crearImagen(id, url, texto, orden);
          nuevasImagenes++;
        } else {
          repositorio.sincronizarImagen(unico(imagenes), url, texto);
        }
        totalImagenes++;
      }
    }
    return new Resumen(
        nuevosUsuarios,
        nuevosPerfiles,
        nuevosServicios,
        nuevasImagenes,
        DatosDeDemostracion.PRESTADORES.size() - nuevosUsuarios,
        DatosDeDemostracion.PRESTADORES.size() - nuevosPerfiles,
        DatosDeDemostracion.SERVICIOS.size() - nuevosServicios,
        totalImagenes - nuevasImagenes,
        sinMapeo);
  }

  private void validarMapeo() {
    var permitidas =
        DatosDeDemostracion.SERVICIOS.stream().map(DatosDeDemostracion.Servicio::clave).toList();
    for (String servicio : propiedades.imagenes().keySet()) {
      List<String> claves = propiedades.clavesDe(servicio);
      if (!permitidas.contains(servicio)
          || claves.size() > 3
          || new HashSet<>(claves).size() != claves.size()
          || claves.stream()
              .anyMatch(clave -> !clave.matches("servicios/[a-f0-9]{32}\\.(jpg|jpeg|png|webp)"))) {
        throw new IllegalStateException(
            "Mapeo de imágenes demo inválido: usar hasta tres claves públicas de servicios por entrada.");
      }
      if (!claves.isEmpty()) {
        validarBasePublica();
      }
    }
  }

  private void validarBasePublica() {
    URI base;
    try {
      base = URI.create(almacenamiento.urlPublicaBase());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("La base pública de imágenes demo no es válida.");
    }
    if (!"https".equals(base.getScheme())
        || base.getHost() == null
        || base.getRawUserInfo() != null
        || base.getRawQuery() != null
        || base.getRawFragment() != null
        || base.getHost().endsWith("r2.cloudflarestorage.com")) {
      throw new IllegalStateException(
          "El mapeo demo exige la base HTTPS pública, sin credenciales ni firmas.");
    }
  }

  private String hashAleatorio() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return codificador.encode(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
  }

  private Long unico(List<Long> encontrados) {
    if (encontrados.size() != 1) {
      throw new IllegalStateException(
          "Datos demo ambiguos; no se modifican ni eliminan duplicados existentes.");
    }
    return encontrados.getFirst();
  }
}
