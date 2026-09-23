package com.moica.demo.repository;

import com.moica.demo.DatosDeDemostracion.Prestador;
import com.moica.demo.DatosDeDemostracion.Servicio;
import java.util.List;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/** SQL exclusivo del bootstrap de demo; no modifica esquema ni repositorios del dominio. */
@Repository
public class DemoRepository {
  private final JdbcOperations jdbc;

  public DemoRepository(JdbcOperations jdbc) {
    this.jdbc = jdbc;
  }

  public void bloquearEjecuciones() {
    // Bloqueo de transacción compartido por todos los arranques de este seeder.
    jdbc.execute("SELECT pg_advisory_xact_lock(hashtextextended('moica:seed:demo:v1', 0))");
  }

  public List<Long> usuarios(Prestador prestador) {
    return jdbc.queryForList(
        "SELECT id_usuario FROM usuario WHERE correo_electronico = ?",
        Long.class,
        prestador.correo());
  }

  public boolean coincidePropietario(Long id, Prestador prestador) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT nombre_completo = ? FROM usuario WHERE id_usuario = ?",
            Boolean.class,
            prestador.nombre(),
            id));
  }

  public Long crearUsuario(Prestador prestador, String hash) {
    return jdbc.queryForObject(
        """
        INSERT INTO usuario (nombre_completo, correo_electronico, clave_hash, estado_cuenta)
        VALUES (?, ?, ?, 'ACTIVA') RETURNING id_usuario
        """,
        Long.class,
        prestador.nombre(),
        prestador.correo(),
        hash);
  }

  public void sincronizarUsuario(Long id) {
    jdbc.update(
        """
        UPDATE usuario SET estado_cuenta = 'ACTIVA', fecha_fin_estado_cuenta = NULL,
          fecha_actualizacion = CURRENT_TIMESTAMP
        WHERE id_usuario = ? AND (estado_cuenta <> 'ACTIVA' OR fecha_fin_estado_cuenta IS NOT NULL)
        """,
        id);
  }

  public Integer municipio(String nombre) {
    return jdbc.queryForObject(
        """
        SELECT m.id_municipio FROM municipio m
        JOIN departamento d ON d.id_departamento = m.id_departamento
        WHERE d.nombre = 'Managua' AND d.habilitado = TRUE AND m.nombre = ?
        """,
        Integer.class,
        nombre);
  }

  public Integer subcategoria(Servicio servicio) {
    return jdbc.queryForObject(
        """
        SELECT s.id_subcategoria_servicio FROM subcategoria_servicio s
        JOIN categoria_servicio c ON c.id_categoria_servicio = s.id_categoria_servicio
        WHERE c.nombre = ? AND s.nombre = ?
        """,
        Integer.class,
        servicio.categoria(),
        servicio.subcategoria());
  }

  public boolean existePerfil(Long id) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM perfil_prestador WHERE id_prestador = ?)",
            Boolean.class,
            id));
  }

  public void sincronizarPerfil(Long id, Prestador p, Integer municipio) {
    jdbc.update(
        """
        INSERT INTO perfil_prestador AS actual
          (id_prestador, nombre_publico, descripcion, tipo_prestador, id_municipio_principal,
           descripcion_cobertura, disponibilidad, nivel_verificacion)
        VALUES (?, ?, ?, ?, ?, ?, 'DISPONIBLE', ?)
        ON CONFLICT (id_prestador) DO UPDATE SET
          nombre_publico = EXCLUDED.nombre_publico, descripcion = EXCLUDED.descripcion,
          tipo_prestador = EXCLUDED.tipo_prestador,
          id_municipio_principal = EXCLUDED.id_municipio_principal,
          descripcion_cobertura = EXCLUDED.descripcion_cobertura,
          disponibilidad = EXCLUDED.disponibilidad, nivel_verificacion = EXCLUDED.nivel_verificacion,
          fecha_actualizacion = CURRENT_TIMESTAMP
        WHERE (actual.nombre_publico, actual.descripcion, actual.tipo_prestador,
          actual.id_municipio_principal, actual.descripcion_cobertura, actual.disponibilidad,
          actual.nivel_verificacion) IS DISTINCT FROM
          (EXCLUDED.nombre_publico, EXCLUDED.descripcion, EXCLUDED.tipo_prestador,
          EXCLUDED.id_municipio_principal, EXCLUDED.descripcion_cobertura, EXCLUDED.disponibilidad,
          EXCLUDED.nivel_verificacion)
        """,
        id,
        p.nombre(),
        p.descripcion(),
        p.tipo(),
        municipio,
        p.cobertura(),
        p.verificacion());
  }

  public List<Long> servicios(Long prestador, Integer subcategoria) {
    // Cada cuenta reservada publica exactamente uno por subcategoría; el título puede corregirse.
    return jdbc.queryForList(
        """
        SELECT id_servicio_publicado FROM servicio_publicado
        WHERE id_prestador = ? AND id_subcategoria_servicio = ?
        """,
        Long.class,
        prestador,
        subcategoria);
  }

  public Long crearServicio(Long prestador, Integer subcategoria, Servicio s) {
    return jdbc.queryForObject(
        """
        INSERT INTO servicio_publicado
          (id_prestador, id_subcategoria_servicio, nombre, descripcion, precio_referencia, estado)
        VALUES (?, ?, ?, ?, ?, 'ACTIVO') RETURNING id_servicio_publicado
        """,
        Long.class,
        prestador,
        subcategoria,
        s.nombre(),
        s.descripcion(),
        s.precio());
  }

  public void sincronizarServicio(Long id, Servicio s) {
    jdbc.update(
        """
        UPDATE servicio_publicado SET nombre = ?, descripcion = ?, precio_referencia = ?,
          estado = 'ACTIVO', fecha_actualizacion = CURRENT_TIMESTAMP
        WHERE id_servicio_publicado = ? AND
          (nombre, descripcion, precio_referencia, estado) IS DISTINCT FROM (?, ?, ?, 'ACTIVO')
        """,
        s.nombre(),
        s.descripcion(),
        s.precio(),
        id,
        s.nombre(),
        s.descripcion(),
        s.precio());
  }

  public List<Long> imagenes(Long servicio, int orden) {
    return jdbc.queryForList(
        """
        SELECT id_imagen_servicio_publicado FROM imagen_servicio_publicado
        WHERE id_servicio_publicado = ? AND orden_visualizacion = ?
        """,
        Long.class,
        servicio,
        orden);
  }

  public void crearImagen(Long servicio, String url, String texto, int orden) {
    jdbc.update(
        """
        INSERT INTO imagen_servicio_publicado
          (id_servicio_publicado, url_imagen, texto_alternativo, orden_visualizacion)
        VALUES (?, ?, ?, ?)
        """,
        servicio,
        url,
        texto,
        orden);
  }

  public void sincronizarImagen(Long id, String url, String texto) {
    jdbc.update(
        """
        UPDATE imagen_servicio_publicado SET url_imagen = ?, texto_alternativo = ?
        WHERE id_imagen_servicio_publicado = ? AND
          (url_imagen, texto_alternativo) IS DISTINCT FROM (?, ?)
        """,
        url,
        texto,
        id,
        url,
        texto);
  }
}
