# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-noble@sha256:75ce56643243c3db632be2ef259625fb42ee3be1334389659f7a1a61acb78783 AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-noble@sha256:96975602e131485862eb8cd32927face8a06d7591a5e865944b634a701d9df72
WORKDIR /app
RUN groupadd --gid 10001 moica && useradd --uid 10001 --gid moica --no-create-home moica
COPY --from=build --chown=moica:moica /build/target/moica-backend-*.jar app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
