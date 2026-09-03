import { describe, expect, it } from 'vitest';

import {
  apareceEnBandejaDeMensajes,
  conversacionesDeBandeja,
  estadoVisibleDeConversacion,
  filtrarConversaciones,
  horaDeMensaje,
  inicialesDeNombre,
  instanteDeLista,
  nombreDeContraparte,
} from './presentacion';
import type { ResumenDeSolicitudServicio } from './tipos';

function resumen(cambios: Partial<ResumenDeSolicitudServicio> = {}): ResumenDeSolicitudServicio {
  return {
    idSolicitudServicio: 21,
    idServicioPublicado: 10,
    nombreServicio: 'Reparación de fugas',
    idCliente: 2,
    nombreCliente: 'Ana Cliente',
    idPrestador: 1,
    nombrePublicoPrestador: 'Taller La Esperanza',
    idMunicipio: 3,
    nombreMunicipio: 'Managua',
    estadoActual: 'ACEPTADA',
    fechaPreferida: null,
    fechaCreacion: '2026-08-29T10:00:00-06:00',
    ...cambios,
  };
}

describe('presentación de la bandeja de mensajes', () => {
  it('incluye aceptadas y las cerradas, y deja fuera pendientes y rechazadas', () => {
    expect(apareceEnBandejaDeMensajes('ACEPTADA')).toBe(true);
    expect(apareceEnBandejaDeMensajes('COMPLETADA')).toBe(true);
    expect(apareceEnBandejaDeMensajes('CANCELADA')).toBe(true);
    expect(apareceEnBandejaDeMensajes('PENDIENTE')).toBe(false);
    expect(apareceEnBandejaDeMensajes('RECHAZADA')).toBe(false);
  });

  it('une enviadas y recibidas, descarta duplicados y ordena por la más reciente', () => {
    const antiguas = [
      resumen({
        idSolicitudServicio: 10,
        estadoActual: 'ACEPTADA',
        fechaCreacion: '2026-08-01T10:00:00-06:00',
      }),
      resumen({ idSolicitudServicio: 11, estadoActual: 'PENDIENTE' }),
    ];
    const recientes = [
      resumen({
        idSolicitudServicio: 10,
        estadoActual: 'ACEPTADA',
        fechaCreacion: '2026-08-01T10:00:00-06:00',
      }),
      resumen({
        idSolicitudServicio: 30,
        estadoActual: 'COMPLETADA',
        fechaCreacion: '2026-08-30T10:00:00-06:00',
      }),
    ];

    const bandeja = conversacionesDeBandeja(antiguas, recientes);

    expect(bandeja.map((item) => item.idSolicitudServicio)).toEqual([30, 10]);
  });

  it('nombra a la contraparte según el rol de la sesión', () => {
    const item = resumen();
    expect(nombreDeContraparte(item, 1)).toBe('Ana Cliente');
    expect(nombreDeContraparte(item, 2)).toBe('Taller La Esperanza');
  });

  it('filtra por nombre de la contraparte o del servicio', () => {
    const items = [
      resumen(),
      resumen({
        idSolicitudServicio: 22,
        nombreServicio: 'Electricidad',
        nombreCliente: 'Bruno Pérez',
        nombrePublicoPrestador: 'Electro Sur',
      }),
    ];

    expect(filtrarConversaciones(items, 'esperanza', 2)).toHaveLength(1);
    expect(filtrarConversaciones(items, 'electric', 1)[0]?.nombreServicio).toBe('Electricidad');
    expect(filtrarConversaciones(items, 'no-existe', 1)).toHaveLength(0);
    expect(filtrarConversaciones(items, '  ', 1)).toHaveLength(2);
  });

  it('lee como en línea una conversación viva y con el estado si ya cerró', () => {
    expect(estadoVisibleDeConversacion('ACEPTADA')).toBe('En línea');
    expect(estadoVisibleDeConversacion('COMPLETADA')).toBe('Completada');
    expect(estadoVisibleDeConversacion('CANCELADA')).toBe('Cancelada');
  });

  it('saca iniciales del nombre público', () => {
    expect(inicialesDeNombre('Ana Cliente')).toBe('AC');
    expect(inicialesDeNombre('Erving')).toBe('ER');
    expect(inicialesDeNombre('   ')).toBe('');
  });

  it('formatea instantes y conserva el texto si la fecha no es válida', () => {
    expect(horaDeMensaje('no-es-fecha')).toBe('no-es-fecha');
    expect(horaDeMensaje('2026-08-29T11:05:00-06:00')).not.toBe('2026-08-29T11:05:00-06:00');
    expect(instanteDeLista('no-es-fecha')).toBe('no-es-fecha');
    expect(instanteDeLista('2026-08-29T11:05:00-06:00')).not.toBe('2026-08-29T11:05:00-06:00');
  });
});
