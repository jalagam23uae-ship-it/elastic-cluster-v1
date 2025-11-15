# ========================================
# Stage 1: Build Stage
# ========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

LABEL stage=builder

WORKDIR /build

# Copy pom.xml and download dependencies (layer caching optimization)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for Docker build - run tests in CI/CD)
RUN mvn clean package -DskipTests -B && \
    mv target/*.jar target/app.jar

# ========================================
# Stage 2: Runtime Stage
# ========================================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="FedNow Development Team"
LABEL description="ISO 20022 Message Converter for FedNow"
LABEL version="1.0.0"

# Create application directory
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy the built JAR from builder stage
COPY --from=builder /build/target/app.jar app.jar

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring && \
    chown -R spring:spring /app

# Switch to non-root user
USER spring:spring

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM options for containerized environments with optimal performance
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
