#!/bin/sh
set -eu
: "${MOICA_BACKEND_UPSTREAM:?Define MOICA_BACKEND_UPSTREAM como host:puerto privado}"
case "$MOICA_BACKEND_UPSTREAM" in
  *[!a-zA-Z0-9.:-]*|'') echo 'MOICA_BACKEND_UPSTREAM invalido' >&2; exit 1 ;;
esac
case "$MOICA_PUBLIC_SCHEME" in http|https) ;; *) exit 1 ;; esac
case "$PORT:$MOICA_PUBLIC_PORT" in *[!0-9:]*|:*) exit 1 ;; esac
# El DNS del contenedor funciona tanto en Docker como en la red privada del proveedor.
MOICA_DNS_RESOLVER=$(awk '$1 == "nameserver" { if ($2 ~ /:/) print "[" $2 "]"; else print $2; exit }' /etc/resolv.conf)
export MOICA_DNS_RESOLVER
export NGINX_ENVSUBST_FILTER='^(PORT|MOICA_BACKEND_UPSTREAM|MOICA_PUBLIC_SCHEME|MOICA_PUBLIC_PORT|MOICA_DNS_RESOLVER)$'
