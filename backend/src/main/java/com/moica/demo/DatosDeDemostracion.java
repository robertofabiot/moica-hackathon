package com.moica.demo;

import java.math.BigDecimal;
import java.util.List;

/** Catálogo cerrado de contenido ficticio; sus claves reservadas no deben renombrarse. */
public final class DatosDeDemostracion {

  private DatosDeDemostracion() {}

  public record Prestador(
      String clave,
      String nombre,
      String tipo,
      String municipio,
      String cobertura,
      String descripcion,
      String verificacion) {
    public String correo() {
      return "moica-demo-v1-" + clave + "@demo.moica.invalid";
    }
  }

  public record Servicio(
      String clave,
      String prestador,
      String categoria,
      String subcategoria,
      String nombre,
      String descripcion,
      BigDecimal precio) {}

  public static final List<Prestador> PRESTADORES =
      List.of(
          new Prestador(
              "julio",
              "Julio Mendoza · Soluciones del Hogar",
              "INDEPENDIENTE",
              "Managua",
              "Atiendo en Bello Horizonte, Las Américas y Villa Progreso. Visitas con cita en Managua.",
              "Reparaciones de agua e instalaciones eléctricas domésticas. Explico el trabajo y acordamos materiales antes de empezar. Perfil ficticio de demostración.",
              "PROFESIONAL_VERIFICADO"),
          new Prestador(
              "maderas",
              "Maderas del Patio",
              "EMPRENDIMIENTO",
              "Ciudad Sandino",
              "Ciudad Sandino y zona occidental de Managua. Entregas según tamaño de la pieza.",
              "Pequeño taller dedicado a muebles prácticos y reparaciones de madera para el hogar. Perfil ficticio de demostración.",
              "VERIFICADO_BASICO"),
          new Prestador(
              "camila",
              "Camila Ríos · Belleza a Domicilio",
              "INDEPENDIENTE",
              "Managua",
              "Visitas en Altamira, Villa Fontana y Reparto San Juan, con reserva previa.",
              "Maquillaje social y cuidado de uñas con tiempo dedicado a cada clienta. Acordamos el estilo antes de la cita. Perfil ficticio de demostración.",
              "VERIFICADO_BASICO"),
          new Prestador(
              "barberia",
              "Barbería La Esquina",
              "EMPRENDIMIENTO",
              "Tipitapa",
              "Atención con cita en el casco urbano de Tipitapa; visitas para grupos a coordinar.",
              "Cortes clásicos y modernos, arreglo de barba y atención sin prisas. Perfil ficticio de demostración.",
              "VERIFICADO_BASICO"),
          new Prestador(
              "tecnica",
              "Punto Técnico Managua",
              "PYME",
              "Managua",
              "Managua urbana y Ticuantepe. Diagnóstico a domicilio o recepción de equipos con cita.",
              "Mantenimiento de computadoras y asistencia para pequeñas oficinas. Presentamos un diagnóstico antes de cotizar repuestos. Perfil ficticio de demostración.",
              "PROFESIONAL_VERIFICADO"),
          new Prestador(
              "lucia",
              "Lucía Vega · Diseño Local",
              "INDEPENDIENTE",
              "Ticuantepe",
              "Atención remota en el departamento de Managua y reuniones con cita en Ticuantepe.",
              "Diseño de piezas claras para emprendimientos, menús y redes sociales. Trabajo a partir de tus textos y referencias. Perfil ficticio de demostración.",
              "VERIFICADO_BASICO"));

  private static final String HOGAR = "Hogar y mantenimiento";
  private static final String BELLEZA = "Belleza y cuidado personal";
  private static final String TECNOLOGIA = "Tecnología y servicios digitales";

  public static final List<Servicio> SERVICIOS =
      List.of(
          new Servicio(
              "plomeria",
              "julio",
              HOGAR,
              "Plomería",
              "Reparación de fugas y cambio de grifería",
              "Revisión de fugas visibles en lavamanos, fregaderos y sanitarios. Mano de obra desde C$450; materiales y reparaciones adicionales se cotizan después de revisar. Servicio ficticio para demostración.",
              new BigDecimal("450")),
          new Servicio(
              "electricidad",
              "julio",
              HOGAR,
              "Electricidad",
              "Instalación de luminarias y tomacorrientes",
              "Cambio de luminarias, interruptores y tomacorrientes de uso doméstico. Visita y mano de obra básica desde C$650; materiales por separado. Servicio ficticio para demostración.",
              new BigDecimal("650")),
          new Servicio(
              "carpinteria",
              "maderas",
              HOGAR,
              "Carpintería",
              "Muebles de madera a medida y reparaciones",
              "Reparamos puertas y fabricamos repisas, escritorios y muebles pequeños. El presupuesto depende de medidas, madera y acabado; coordinamos una visita para medir. Servicio ficticio para demostración.",
              null),
          new Servicio(
              "maquillaje",
              "camila",
              BELLEZA,
              "Maquillaje",
              "Maquillaje social para tus ocasiones especiales",
              "Preparación de piel y maquillaje para graduaciones, cumpleaños y eventos. Desde C$900 por persona; pestañas y traslado fuera de cobertura se acuerdan al reservar. Servicio ficticio para demostración.",
              new BigDecimal("900")),
          new Servicio(
              "barberia",
              "barberia",
              BELLEZA,
              "Barbería/peluquería",
              "Corte de cabello y perfilado de barba",
              "Corte a tijera o máquina y perfilado de barba, con asesoría de estilo. Precio de referencia C$250 por el conjunto, atención con cita. Servicio ficticio para demostración.",
              new BigDecimal("250")),
          new Servicio(
              "unas",
              "camila",
              BELLEZA,
              "Uñas",
              "Manicura con esmaltado semipermanente",
              "Limpieza, limado y esmaltado semipermanente en un color desde C$400. Diseños especiales y retiro de producto anterior se cotizan al reservar. Servicio ficticio para demostración.",
              new BigDecimal("400")),
          new Servicio(
              "computadoras",
              "tecnica",
              TECNOLOGIA,
              "Reparación de computadoras",
              "Diagnóstico y mantenimiento de laptops",
              "Revisión de fallas, limpieza interna y mantenimiento preventivo desde C$800. Repuestos y recuperación de información requieren presupuesto previo. Servicio ficticio para demostración.",
              new BigDecimal("800")),
          new Servicio(
              "diseno",
              "lucia",
              TECNOLOGIA,
              "Diseño gráfico",
              "Diseño de menús y piezas para redes sociales",
              "Piezas gráficas para negocios locales con propuesta visual y ajustes acordados. Cotizamos según cantidad de diseños, formatos y contenido disponible. Servicio ficticio para demostración.",
              null),
          new Servicio(
              "soporte",
              "tecnica",
              TECNOLOGIA,
              "Soporte técnico",
              "Soporte de Wi-Fi y equipos para pequeñas oficinas",
              "Revisamos conectividad, impresoras y configuración de equipos. Acordamos alcance y precio según cantidad de dispositivos y visita necesaria. Servicio ficticio para demostración.",
              null));
}
