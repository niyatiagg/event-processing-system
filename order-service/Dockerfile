# syntax=docker/dockerfile:1
# Multi-stage build: compile the whole reactor, then ship only the order-service jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-lambda/pom.xml notification-lambda/pom.xml
# Warm the dependency cache (best-effort; ignores the fact that sources aren't present yet).
RUN mvn -q -pl common,order-service -am dependency:go-offline || true
COPY common common
COPY order-service order-service
RUN mvn -q -pl order-service -am -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /workspace/order-service/target/order-service.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --retries=10 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
