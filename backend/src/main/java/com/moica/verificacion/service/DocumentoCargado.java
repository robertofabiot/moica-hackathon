package com.moica.verificacion.service;

import com.moica.verificacion.entity.TipoDocumentoVerificacion;

/**
 * Un archivo ya validado y ya guardado en el almacenamiento privado, listo para persistirse.
 *
 * <p>No es un DTO de la API: no entra ni sale por ningún endpoint. Es lo que {@code
 * EnvioDeExpedienteService} entrega a {@code VerificacionDelPrestadorService} cuando la parte de
 * red del envío ya terminó y solo queda la transacción de base de datos. Por eso lleva la clave:
 * dentro del servidor es un dato normal, y lo que nunca la publica es el DTO de salida.
 *
 * @param claveAlmacenamiento clave opaca bajo la que el objeto ya está en el bucket privado
 * @param nombreOriginal nombre elegido por el prestador, ya saneado
 * @param tipoMime tipo real del contenido, comprobado contra su firma binaria
 */
public record DocumentoCargado(
    TipoDocumentoVerificacion tipoDocumento,
    String claveAlmacenamiento,
    String nombreOriginal,
    String tipoMime,
    int tamanoBytes) {}
