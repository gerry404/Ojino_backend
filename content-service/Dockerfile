# syntax=docker/dockerfile:1

# Image de content-service.
#
# Seul service multi-module du depot : content-core porte le referentiel, les
# cinq modules de cycle portent leur contenu, et content-app est l'application
# Spring Boot qui les assemble. On construit donc l'agregat entier, mais on
# n'embarque que le jar de content-app.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Tout le projet d'un coup : les modules dependent les uns des autres, un
# decoupage par pom.xml n'apporterait rien qu'un cache Maven ne donne deja.
COPY . .

RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S ojino && adduser -S -G ojino ojino

WORKDIR /app
COPY --from=build /build/content-app/target/*.jar app.jar
USER ojino

EXPOSE 8083

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8083/actuator/health | grep -q '"status":"UP"'

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
