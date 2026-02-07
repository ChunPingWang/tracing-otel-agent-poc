# ---- Build Stage ----
FROM maven:3.8-openjdk-8 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# ---- Run Stage ----
FROM openjdk:8-jre-slim

# Download OpenTelemetry Java Agent
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.1/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
RUN chmod 644 /opentelemetry-javaagent.jar

COPY --from=builder /build/target/product-service-0.0.1-SNAPSHOT.jar /app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app.jar"]
