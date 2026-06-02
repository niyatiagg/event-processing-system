# syntax=docker/dockerfile:1
# Multi-stage build: compile the whole reactor, then ship only the payment-service jar.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY notification-lambda/pom.xml notification-lambda/pom.xml
RUN mvn -q -pl common,payment-service -am dependency:go-offline || true
COPY common common
COPY payment-service payment-service
RUN mvn -q -pl payment-service -am -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /workspace/payment-service/target/payment-service.jar app.jar
USER app
EXPOSE 8081
HEALTHCHECK --interval=15s --timeout=3s --retries=10 \
  CMD wget -qO- http://localhost:8081/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
