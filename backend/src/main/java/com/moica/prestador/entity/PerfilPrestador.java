package com.moica.prestador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Perfil con el que una cuenta ofrece servicios en Moica.
 *
 * <p>Corresponde con la tabla {@code perfil_prestador} que crea la migración {@code V21}. Es una
 * especialización 0..1 de {@code usuario}: comparte su clave primaria, y esa clave compartida es lo
 * que garantiza que una cuenta tenga como máximo un perfil.
 *
 * <p>Todo perfil nace {@link EstadoDisponibilidad#DISPONIBLE} y {@link
 * NivelVerificacionPrestador#SIN_VERIFICAR}. El nivel de verificación solo lo cambia {@link
 * #proyectarNivelVerificacion(NivelVerificacionPrestador)}, que invoca el flujo de verificación
 * documental al aprobar o revocar una solicitud. Ningún endpoint del propietario llega hasta ahí.
 */
@Entity
@Table(name = "perfil_prestador")
public class PerfilPrestador {

  @Id
  @Column(name = "id_prestador")
  private Long idPrestador;

  @Column(name = "nombre_publico", nullable = false, length = 120)
  private String nombrePublico;

  /** Dirección pública de la imagen en el almacén de objetos. Nunca el binario. */
  @Column(name = "url_imagen_perfil", length = 500)
  private String urlImagenPerfil;

  @Column(name = "descripcion", nullable = false)
  private String descripcion;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_prestador", nullable = false, length = 30)
  private TipoPrestador tipoPrestador;

  @Column(name = "id_municipio_principal", nullable = false)
  private Integer idMunicipioPrincipal;

  @Column(name = "descripcion_cobertura", nullable = false)
  private String descripcionCobertura;

  @Enumerated(EnumType.STRING)
  @Column(name = "disponibilidad", nullable = false, length = 30)
  private EstadoDisponibilidad disponibilidad;

  @Enumerated(EnumType.STRING)
  @Column(name = "nivel_verificacion", nullable = false, length = 30)
  private NivelVerificacionPrestador nivelVerificacion;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private OffsetDateTime fechaCreacion;

  @Column(name = "fecha_actualizacion", nullable = false)
  private OffsetDateTime fechaActualizacion;

  /** Constructor que exige JPA. No debe usarse desde el código de la aplicación. */
  protected PerfilPrestador() {}

  /**
   * Crea el perfil de una cuenta, con los valores de nacimiento que fija el diccionario.
   *
   * @param idPrestador identificador de la cuenta propietaria, que es también la clave del perfil
   */
  public PerfilPrestador(
      Long idPrestador,
      String nombrePublico,
      String descripcion,
      TipoPrestador tipoPrestador,
      Integer idMunicipioPrincipal,
      String descripcionCobertura) {
    this.idPrestador = idPrestador;
    this.nombrePublico = nombrePublico;
    this.descripcion = descripcion;
    this.tipoPrestador = tipoPrestador;
    this.idMunicipioPrincipal = idMunicipioPrincipal;
    this.descripcionCobertura = descripcionCobertura;
    this.disponibilidad = EstadoDisponibilidad.DISPONIBLE;
    this.nivelVerificacion = NivelVerificacionPrestador.SIN_VERIFICAR;
  }

  /** Sustituye los datos editables del perfil. La disponibilidad y la imagen van aparte. */
  public void actualizar(
      String nombrePublico,
      String descripcion,
      TipoPrestador tipoPrestador,
      Integer idMunicipioPrincipal,
      String descripcionCobertura) {
    this.nombrePublico = nombrePublico;
    this.descripcion = descripcion;
    this.tipoPrestador = tipoPrestador;
    this.idMunicipioPrincipal = idMunicipioPrincipal;
    this.descripcionCobertura = descripcionCobertura;
  }

  public void cambiarDisponibilidad(EstadoDisponibilidad disponibilidad) {
    this.disponibilidad = disponibilidad;
  }

  /**
   * Deja vigente el nivel de verificación que decidió una persona administradora.
   *
   * <p>Es la única vía por la que cambia este campo, y la usa exclusivamente la capacidad {@code
   * verificacion} al aprobar o revocar una solicitud. El propietario del perfil no tiene ningún
   * camino hasta aquí: no existe un DTO de entrada que acepte el nivel.
   */
  public void proyectarNivelVerificacion(NivelVerificacionPrestador nivelVerificacion) {
    this.nivelVerificacion = nivelVerificacion;
  }

  /**
   * Sustituye la dirección de la imagen de perfil; {@code null} la quita.
   *
   * <p>Recibe la URL ya construida por el almacén: la entidad no sabe dónde viven los objetos.
   */
  public void cambiarUrlImagenPerfil(String urlImagenPerfil) {
    this.urlImagenPerfil = urlImagenPerfil;
  }

  @PrePersist
  void registrarInstanteDeCreacion() {
    OffsetDateTime ahora = OffsetDateTime.now();
    this.fechaCreacion = ahora;
    this.fechaActualizacion = ahora;
  }

  @PreUpdate
  void registrarInstanteDeActualizacion() {
    this.fechaActualizacion = OffsetDateTime.now();
  }

  public Long getIdPrestador() {
    return idPrestador;
  }

  public String getNombrePublico() {
    return nombrePublico;
  }

  public String getUrlImagenPerfil() {
    return urlImagenPerfil;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public TipoPrestador getTipoPrestador() {
    return tipoPrestador;
  }

  public Integer getIdMunicipioPrincipal() {
    return idMunicipioPrincipal;
  }

  public String getDescripcionCobertura() {
    return descripcionCobertura;
  }

  public EstadoDisponibilidad getDisponibilidad() {
    return disponibilidad;
  }

  public NivelVerificacionPrestador getNivelVerificacion() {
    return nivelVerificacion;
  }

  public OffsetDateTime getFechaCreacion() {
    return fechaCreacion;
  }

  public OffsetDateTime getFechaActualizacion() {
    return fechaActualizacion;
  }
}
