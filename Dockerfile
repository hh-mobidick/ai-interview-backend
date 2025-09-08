# syntax=docker/dockerfile:1

# --- Build stage ---
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# Copy Gradle wrapper and build files first (better layer caching)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew

# Pre-fetch dependencies to leverage Docker layer cache
RUN ./gradlew --no-daemon dependencies

# Copy sources
COPY src src

# Build Spring Boot fat jar (skip tests during image build)
RUN ./gradlew --no-daemon clean bootJar -x test \
    && mkdir -p /app \
    && cp $(ls build/libs/*.jar | grep -v plain | head -n1) /app/app.jar


# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring

# Copy application
COPY --from=builder /app/app.jar /app/app.jar

# Expose Render default port
EXPOSE 10000

# Optional JVM options can be passed via JAVA_OPTS env var
ENV JAVA_OPTS=""

USER spring:spring

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
