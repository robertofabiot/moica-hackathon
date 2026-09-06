package com.moica.comun.configuracion;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * El canal externo por el que Moica atiende a quien quiere apelar una medida.
 *
 * <p>Existe porque la decisión D-MOD-04 y la definición 11.5 dejan la apelación <b>fuera</b> de la
 * aplicación: no hay formulario, ni buzón interno, ni adjuntos. Lo único que Moica hace es
 * <em>mostrar</em> a dónde escribir, y quien lo recibe registra después lo que llegó desde el área
 * administrativa.
 *
 * <p>Es configuración y no una constante en el código porque el canal cambia con el despliegue y no
 * con el producto: un entorno de demostración y uno real no atienden en la misma dirección. Tampoco
 * es un secreto —se publica a quien esté sancionado—, así que tiene valor por omisión y la
 * aplicación arranca sin definirlo.
 *
 * <p>La definición funcional habla de «por ejemplo un correo de soporte», sin fijar cuál. El valor
 * por omisión es un marcador de posición del proyecto, no una dirección atendida: el equipo debe
 * sustituirlo por la real antes de publicar.
 *
 * @param canal a dónde escribir para apelar. Viaja al frontend en la respuesta de la sesión y
 *     aparece también en el mensaje con el que se rechaza el acceso a una cuenta suspendida, que es
 *     lo único que esa persona llega a leer
 */
@ConfigurationProperties("moica.soporte")
public record PropiedadesDeSoporte(String canal) {

  public PropiedadesDeSoporte {
    if (canal == null || canal.isBlank()) {
      throw new IllegalStateException(
          "moica.soporte.canal no puede quedar vacío: es el único camino que le queda a una"
              + " cuenta sancionada para apelar. Defínelo en MOICA_SOPORTE_CANAL.");
    }
    canal = canal.strip();
  }
}
