package com.moica.servicio.repository;

import com.moica.servicio.entity.ServicioPublicado;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicioPublicadoRepository extends JpaRepository<ServicioPublicado, Long> {

  List<ServicioPublicado> findByIdPrestadorOrderByNombreAscIdServicioPublicadoAsc(Long idPrestador);

  /** Buscar por clave y propietario a la vez es lo que impide operar sobre un servicio ajeno. */
  Optional<ServicioPublicado> findByIdServicioPublicadoAndIdPrestador(
      Long idServicioPublicado, Long idPrestador);

  /**
   * Servicios que un visitante puede ver: activos, de cuenta operativa, prestador disponible y
   * perfil al menos verificado básico.
   *
   * <p>Los parámetros nulos no filtran. El texto, cuando llega, ya viene en minúsculas y envuelto
   * en {@code %}. El orden es determinista: nombre y, si empatan, identificador.
   */
  @Query(
      """
      SELECT servicio FROM ServicioPublicado servicio
      WHERE servicio.estado = com.moica.servicio.entity.EstadoServicio.ACTIVO
        AND EXISTS (
          SELECT 1 FROM PerfilPrestador perfil, Usuario usuario
          WHERE perfil.idPrestador = servicio.idPrestador
            AND usuario.idUsuario = perfil.idPrestador
            AND perfil.disponibilidad
              = com.moica.prestador.entity.EstadoDisponibilidad.DISPONIBLE
            AND perfil.nivelVerificacion
              <> com.moica.prestador.entity.NivelVerificacionPrestador.SIN_VERIFICAR
            AND usuario.estadoCuenta = com.moica.usuario.entity.EstadoCuenta.ACTIVA
            AND (:idMunicipio IS NULL OR perfil.idMunicipioPrincipal = :idMunicipio)
        )
        AND (:idSubcategoria IS NULL
          OR servicio.idSubcategoriaServicio = :idSubcategoria)
        AND (:idCategoria IS NULL OR EXISTS (
          SELECT 1 FROM SubcategoriaServicio subcategoria
          WHERE subcategoria.idSubcategoriaServicio = servicio.idSubcategoriaServicio
            AND subcategoria.idCategoriaServicio = :idCategoria
        ))
        AND (:texto IS NULL
          OR LOWER(servicio.nombre) LIKE :texto
          OR LOWER(servicio.descripcion) LIKE :texto)
      ORDER BY servicio.nombre ASC, servicio.idServicioPublicado ASC
      """)
  List<ServicioPublicado> buscarPublicos(
      @Param("texto") String texto,
      @Param("idCategoria") Short idCategoria,
      @Param("idSubcategoria") Integer idSubcategoria,
      @Param("idMunicipio") Integer idMunicipio);

  @Query(
      """
      SELECT servicio FROM ServicioPublicado servicio
      WHERE servicio.idServicioPublicado = :idServicioPublicado
        AND servicio.estado = com.moica.servicio.entity.EstadoServicio.ACTIVO
        AND EXISTS (
          SELECT 1 FROM PerfilPrestador perfil, Usuario usuario
          WHERE perfil.idPrestador = servicio.idPrestador
            AND usuario.idUsuario = perfil.idPrestador
            AND perfil.disponibilidad
              = com.moica.prestador.entity.EstadoDisponibilidad.DISPONIBLE
            AND perfil.nivelVerificacion
              <> com.moica.prestador.entity.NivelVerificacionPrestador.SIN_VERIFICAR
            AND usuario.estadoCuenta = com.moica.usuario.entity.EstadoCuenta.ACTIVA
        )
      """)
  Optional<ServicioPublicado> buscarPublicoPorId(
      @Param("idServicioPublicado") Long idServicioPublicado);

  List<ServicioPublicado> findByIdPrestadorAndEstadoOrderByNombreAscIdServicioPublicadoAsc(
      Long idPrestador, com.moica.servicio.entity.EstadoServicio estado);
}
