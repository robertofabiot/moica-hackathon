#!/usr/bin/env bash
#
# Comprueba que un encabezado siga Conventional Commits con los tipos que admite
# Docs/Core/GIT_WORKFLOW.md. El scope es opcional y su lista es abierta.
#
# Uso:
#   validar-convenciones.sh titulo "<encabezado del pull request>"
#   validar-convenciones.sh commits <sha-base> <sha-cabeza>
#
# Los mensajes llegan por argumento o por variable de entorno, nunca
# interpolados dentro del script: un titulo de PR es texto de una persona ajena
# y no debe poder ejecutar nada.

set -uo pipefail

TIPOS='feat|fix|docs|refactor|test|chore|ci|build|style|perf|revert'
FORMATO="^(${TIPOS})(\([a-z0-9][a-z0-9._/-]*\))?!?: [a-z0-9]"

fallos=0

validar_encabezado() {
  local etiqueta="$1"
  local encabezado="$2"

  if [[ -z "${encabezado// /}" ]]; then
    printf 'FALLA %s: el encabezado está vacío\n' "$etiqueta"
    fallos=$((fallos + 1))
    return
  fi

  if [[ ! "$encabezado" =~ $FORMATO ]]; then
    printf 'FALLA %s: %s\n' "$etiqueta" "$encabezado"
    printf '      Formato esperado: <tipo>(<scope opcional>): <descripción en minúscula>\n'
    printf '      Tipos admitidos: %s\n' "${TIPOS//|/, }"
    fallos=$((fallos + 1))
    return
  fi

  if [[ "$encabezado" =~ \.$ ]]; then
    printf 'FALLA %s: %s\n' "$etiqueta" "$encabezado"
    printf '      La descripción no lleva punto final.\n'
    fallos=$((fallos + 1))
    return
  fi

  printf 'OK    %s: %s\n' "$etiqueta" "$encabezado"
}

case "${1:-}" in
  titulo)
    validar_encabezado 'título del PR' "${2:-}"
    ;;

  commits)
    base="${2:-}"
    cabeza="${3:-}"

    if [[ -z "$base" || -z "$cabeza" ]]; then
      printf 'Uso: %s commits <sha-base> <sha-cabeza>\n' "$0" >&2
      exit 2
    fi

    # Solo los commits que aporta este Pull Request. Los merges se omiten:
    # su mensaje lo genera GitHub y el historial previo a la validación
    # automática no se reescribe para simular cumplimiento.
    mapfile -t encabezados < <(git log --no-merges --format=%s "${base}..${cabeza}")

    if [[ ${#encabezados[@]} -eq 0 ]]; then
      printf 'No hay commits propios que validar en %s..%s\n' "$base" "$cabeza"
      exit 0
    fi

    for encabezado in "${encabezados[@]}"; do
      validar_encabezado 'commit' "$encabezado"
    done
    ;;

  *)
    printf 'Uso: %s {titulo <encabezado> | commits <sha-base> <sha-cabeza>}\n' "$0" >&2
    exit 2
    ;;
esac

if [[ $fallos -gt 0 ]]; then
  printf '\n%d encabezado(s) no cumplen las convenciones de GIT_WORKFLOW.md\n' "$fallos" >&2
  exit 1
fi
