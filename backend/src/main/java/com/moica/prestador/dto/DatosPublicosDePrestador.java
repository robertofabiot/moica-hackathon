package com.moica.prestador.dto;

import com.moica.catalogo.dto.UbicacionDeMunicipio;
import com.moica.prestador.entity.EstadoDisponibilidad;
import com.moica.prestador.entity.NivelVerificacionPrestador;
import com.moica.prestador.entity.PerfilPrestador;
import com.moica.prestador.entity.TipoPrestador;

/**
 * Superficie pública de un perfil de prestador.
 *
 * <p>No lleva contactos, correo, documentos ni observaciones administrativas. El municipio viaja
 * descrito para no exigir una segunda consulta. La advertencia de la insignia es la misma en todos
 * los niveles: la revisión documental no garantiza la calidad futura.
 */
public record DatosPublicosDePrestador(
    Long idPrestador,
    String nombrePublico,
    String urlImagenPerfil,
    String descripcion,
    TipoPrestador tipoPrestador,
    DatosDePerfilPrestador.MunicipioDelPerfil municipioPrincipal,
    String descripcionCobertura,
    EstadoDisponibilidad disponibilidad,
    NivelVerificacionPrestador nivelVerificacion,
    String significadoVerificacion,
    String advertenciaDeInsignia) {

  public static DatosPublicosDePrestador de(
      PerfilPrestador perfil, UbicacionDeMunicipio ubicacion) {
    return new DatosPublicosDePrestador(
        perfil.getIdPrestador(),
        perfil.getNombrePublico(),
        perfil.getUrlImagenPerfil(),
        perfil.getDescripcion(),
        perfil.getTipoPrestador(),
        DatosDePerfilPrestador.MunicipioDelPerfil.de(ubicacion),
        perfil.getDescripcionCobertura(),
        perfil.getDisponibilidad(),
        perfil.getNivelVerificacion(),
        SignificadoDeVerificacion.de(perfil.getNivelVerificacion()),
        SignificadoDeVerificacion.ADVERTENCIA);
  }
}
