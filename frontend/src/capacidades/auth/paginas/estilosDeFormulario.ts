import estilos from './formulario.module.css';

/**
 * Clase de un campo de texto, con el borde de error cuando corresponde.
 *
 * Vivía repetida en cada formulario de acceso. Con el tercer formulario —el de seguridad de la
 * cuenta— ya era la misma regla escrita tres veces, así que se escribe una sola.
 */
export function claseDeEntrada(conError: boolean): string {
  const clases = [estilos.entrada];
  if (conError) {
    clases.push(estilos.entradaConError);
  }
  return clases.join(' ');
}
