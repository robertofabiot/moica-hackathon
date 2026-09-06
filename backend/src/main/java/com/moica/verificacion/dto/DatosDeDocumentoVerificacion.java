package com.moica.verificacion.dto;

import com.moica.verificacion.entity.DocumentoVerificacionPrestador;
import com.moica.verificacion.entity.TipoDocumentoVerificacion;
import java.time.OffsetDateTime;

/**
 * Metadatos de un documento del expediente.
 *
 * <p>Es todo lo que sale de la API sobre un archivo, tanto hacia su propietario como hacia la
 * persona que revisa. **No incluye la clave de almacenamiento**, ni una URL, ni el binario: la
 * clave es un dato privado del servidor y una URL permanente no existe. El archivo solo se alcanza
 * con el acceso temporal que un administrador pide en cada apertura.
 *
 * @param nombreOriginal el nombre que eligió el prestador, ya saneado al persistirse
 * @param tipoMime tipo real del contenido, comprobado contra su firma binaria al cargarlo
 */
public record DatosDeDocumentoVerificacion(
    Long idDocumentoVerificacion,
    TipoDocumentoVerificacion tipoDocumento,
    String nombreOriginal,
    String tipoMime,
    Integer tamanoBytes,
    OffsetDateTime fechaCarga) {

  public static DatosDeDocumentoVerificacion de(DocumentoVerificacionPrestador documento) {
    return new DatosDeDocumentoVerificacion(
        documento.getIdDocumentoVerificacion(),
        documento.getTipoDocumento(),
        documento.getNombreOriginal(),
        documento.getTipoMime(),
        documento.getTamanoBytes(),
        documento.getFechaCarga());
  }
}
