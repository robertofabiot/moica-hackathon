package com.moica.prestador.dto;

import com.moica.catalogo.dto.UbicacionDeMunicipio;
import com.moica.prestador.entity.EstadoDisponibilidad;
import com.moica.prestador.entity.NivelVerificacionPrestador;
import com.moica.prestador.entity.PerfilPrestador;
import com.moica.prestador.entity.TipoPrestador;
import java.time.OffsetDateTime;

/**
 * Vista del perfil de prestador que recibe su propietario.
 *
 * <p>Incluye el municipio ya descrito con su departamento, para que la interfaz no tenga que
 * cruzarlo contra el catálogo. El nivel de verificación viaja como lectura: mientras sea {@link
 * NivelVerificacionPrestador#SIN_VERIFICAR} el perfil es privado y la interfaz debe decirlo.
 */
public record DatosDePerfilPrestador(
    Long idPrestador,
    String nombrePublico,
    String urlImagenPerfil,
    String descripcion,
    TipoPrestador tipoPrestador,
    MunicipioDelPerfil municipioPrincipal,
    String descripcionCobertura,
    EstadoDisponibilidad disponibilidad,
    NivelVerificacionPrestador nivelVerificacion,
    OffsetDateTime fechaCreacion,
    OffsetDateTime fechaActualizacion) {

  /** El municipio principal con el nombre de su departamento, listo para mostrarse. */
  public record MunicipioDelPerfil(
      Integer idMunicipio, String nombreMunicipio, String nombreDepartamento) {

    public static MunicipioDelPerfil de(UbicacionDeMunicipio ubicacion) {
      return new MunicipioDelPerfil(
          ubicacion.idMunicipio(), ubicacion.nombreMunicipio(), ubicacion.nombreDepartamento());
    }
  }

  public static DatosDePerfilPrestador de(PerfilPrestador perfil, UbicacionDeMunicipio ubicacion) {
    return new DatosDePerfilPrestador(
        perfil.getIdPrestador(),
        perfil.getNombrePublico(),
        perfil.getUrlImagenPerfil(),
        perfil.getDescripcion(),
        perfil.getTipoPrestador(),
        MunicipioDelPerfil.de(ubicacion),
        perfil.getDescripcionCobertura(),
        perfil.getDisponibilidad(),
        perfil.getNivelVerificacion(),
        perfil.getFechaCreacion(),
        perfil.getFechaActualizacion());
  }
}
