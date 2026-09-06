package com.moica.moderacion.service;

import com.moica.auth.seguridad.UsuarioAutenticado;
import com.moica.comun.error.ErrorDeAplicacion;
import com.moica.moderacion.entity.CasoModeracion;
import org.springframework.http.HttpStatus;

/**
 * Las comprobaciones que todo servicio administrativo de moderación repite.
 *
 * <p>Están aquí y no copiadas en cada clase porque P10B suma dos escritores más a la capacidad —las
 * medidas y las apelaciones—, y una comprobación de seguridad repetida tres veces es la que alguien
 * acabará olvidando en la cuarta.
 */
final class GuardiasDeCaso {

  private GuardiasDeCaso() {}

  /**
   * La última red del área administrativa.
   *
   * <p>Toda ruta de {@code /api/admin/**} pasa antes por la cadena de seguridad, que exige rol
   * administrativo y segundo factor verificado <b>en esa misma sesión</b>. Esta comprobación la
   * repite dentro del servicio, igual que hace la revisión de verificaciones de P4V: si alguien
   * alcanzara un servicio por otra vía, tampoco obtendría nada.
   *
   * <p>Vale también para las lecturas. Un catálogo de sanciones o el expediente de una persona
   * reportada no son información pública.
   */
  static void exigirPermisoAdministrativo(UsuarioAutenticado sujeto) {
    if (sujeto == null || !sujeto.puedeAdministrar()) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "ACCESO_DENEGADO",
          "Esta cuenta no tiene permisos administrativos.");
    }
  }

  /**
   * Exige que quien decide sea quien tiene el caso asignado.
   *
   * <p>Asignar y reasignar las puede hacer cualquier administrador, porque repartir trabajo es
   * coordinación. Decidir no: una resolución, una medida o una apelación las firma quien estudió el
   * expediente, igual que aprobar una verificación exige haberla tomado.
   */
  static void exigirQueSeaElResponsable(CasoModeracion caso, UsuarioAutenticado sujeto) {
    if (caso.getIdAdministradorResponsable() == null) {
      throw new ErrorDeAplicacion(
          HttpStatus.CONFLICT,
          "CASO_SIN_RESPONSABLE",
          "Asigna primero una persona responsable del caso.");
    }
    if (!sujeto.idUsuario().equals(caso.getIdAdministradorResponsable())) {
      throw new ErrorDeAplicacion(
          HttpStatus.FORBIDDEN,
          "CASO_DE_OTRO_ADMINISTRADOR",
          "Este caso lo lleva otra persona administradora. Solo quien lo tiene asignado decide"
              + " sobre él.");
    }
  }

  /**
   * El conflicto de una operación que el caso no admite en su estado actual.
   *
   * <p>El mensaje nombra el estado real porque es información administrativa que quien revisa
   * necesita para entender qué pasó mientras tenía la pantalla abierta.
   */
  static ErrorDeAplicacion transicionNoPermitida(CasoModeracion caso, String explicacion) {
    return new ErrorDeAplicacion(
        HttpStatus.CONFLICT,
        "TRANSICION_NO_PERMITIDA",
        explicacion + " El caso está en estado " + caso.getEstadoActual() + ".");
  }

  static ErrorDeAplicacion casoNoEncontrado() {
    return new ErrorDeAplicacion(
        HttpStatus.NOT_FOUND, "CASO_NO_ENCONTRADO", "Ese caso de moderación no existe.");
  }
}
