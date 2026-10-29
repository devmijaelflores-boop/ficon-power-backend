FROM maven:3.9.11-eclipse-temurin-21-alpine AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/ficon-power-backend-1.0.0.jar app.jar
COPY certs/tuya-pulsar-ca-bundle.pem /app/certs/tuya-pulsar-ca-bundle.pem
ENV TUYA_PULSAR_TLS_TRUST_CERTS_PATH=/app/certs/tuya-pulsar-ca-bundle.pem
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
