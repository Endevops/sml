# syntax=docker/dockerfile:1.4

# Build-time metadata
ARG BUILDKIT_SBOM_SCAN_CONTEXT=true

# --------------------
# Builder: compile application with Gradle
# --------------------
FROM --platform=$BUILDPLATFORM gradle:8.5-jdk17 AS builder
WORKDIR /home/gradle/project

# Copy Gradle wrapper, build files and settings first to maximize cache hits
COPY settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat ./
COPY gradle/ ./gradle/

# Copy sources
COPY src/ ./src/

# Use cache mounts for Gradle caches to speed subsequent builds
# Build the fat/executable jar (project provides buildFatJar task per AGENTS.md)
RUN --mount=type=cache,target=/home/gradle/.gradle \
    --mount=type=cache,target=/root/.gradle \
    chmod +x ./gradlew && \
    ./gradlew --no-daemon buildFatJar -x test

# --------------------
# Runtime: small, secure image
# --------------------
FROM gcr.io/distroless/java17-debian11:nonroot AS runtime

ENV DB_FILE=/app/data/data.db
VOLUME /app/data

# Basic metadata labels (adjust source/maintainer as needed)
LABEL org.opencontainers.image.title="sml-develop" \
      org.opencontainers.image.description="Ktor SML service" \
      org.opencontainers.image.licenses="MIT" \
      org.opencontainers.image.authors="TheYoxy <floryan.simar@endevops.be>" \
      org.opencontainers.image.source="https://github.com/Endevops/sml-develop" \
      org.opencontainers.image.vendor="Endevops"

WORKDIR /app

# Copy the fat jar from the builder stage. Set ownership to distroless nonroot (65532)
# Note: --chown requires BuildKit (Docker Buildx uses BuildKit by default)
COPY --from=builder --chown=65532:65532 /home/gradle/project/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
