# syntax=docker/dockerfile:1
FROM node:22-alpine@sha256:c610fcdfb1d5b4740dd70c284ed3cb16bb857e0f7166196e36a5501df7a3aa32 AS build
WORKDIR /build
COPY package.json package-lock.json ./
# Sin cache montada, por el mismo motivo que en `backend/Dockerfile`: el builder
# de Railway exige un `id` con su prefijo de servicio y fijarlo ataria la imagen
# a un proveedor. `npm ci` va en su capa, antes de copiar el resto, asi que el
# cache de capas de Docker la reutiliza mientras el lockfile no cambie.
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.28-alpine@sha256:a8b39bd9cf0f83869a2162827a0caf6137ddf759d50a171451b335cecc87d236
ENV PORT=8080 MOICA_PUBLIC_SCHEME=https MOICA_PUBLIC_PORT=443
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY nginx/proxy-headers.conf /etc/nginx/proxy-headers.conf
COPY --chmod=755 nginx/15-runtime-config.envsh /docker-entrypoint.d/15-runtime-config.envsh
COPY --from=build /build/dist/ /usr/share/nginx/html/
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s CMD wget -q -O /dev/null http://127.0.0.1:${PORT}/healthz || exit 1
