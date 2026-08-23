# syntax=docker/dockerfile:1

# Image de learning-service.
#
# Deux etapes : on compile avec le JDK complet et Maven, on execute avec un
# simple JRE. L'image finale ne contient ni compilateur, ni sources, ni depot
# Maven — moins de poids a transferer, et surtout moins de surface d'attaque.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

# Le cache BuildKit conserve le depot Maven entre deux constructions. Sans lui,
# chaque build retelechargerait Spring en entier.
#
# Les tests ne tournent pas ici : ils appartiennent a l'integration continue.
# Une image doit se construire a l'identique, hors ligne, sans base de donnees.
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

# Un utilisateur sans privileges. Par defaut un conteneur tourne en root : une
# execution de code arbitraire donnerait alors root sur le conteneur.
RUN addgroup -S ojino && adduser -S -G ojino ojino

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER ojino

EXPOSE 8084

# Sans cette option la JVM ignore la limite memoire du conteneur : elle
# dimensionne son tas sur la RAM de la machine entiere, puis se fait tuer par
# l'orchestrateur des que la limite est franchie.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# start-period genereux : une application Spring met plusieurs dizaines de
# secondes a demarrer, et un echec pendant ce delai ne compte pas comme une
# panne.
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8084/actuator/health | grep -q '"status":"UP"'

# exec pour que java soit le PID 1 : il recoit alors SIGTERM directement et
# s'arrete proprement, au lieu d'etre tue au bout du delai de grace.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
