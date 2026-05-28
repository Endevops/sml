# syntax=docker/dockerfile:1-labs

# Build-time metadata
ARG BUILDKIT_SBOM_SCAN_CONTEXT=true
ARG JAVA_VERSION=25

# --------------------
# Builder: compile application with Gradle
# --------------------
FROM --platform=$BUILDPLATFORM gradle:9-jdk${JAVA_VERSION} AS builder
ENV GRADLE_USER_HOME=/home/gradle/.gradle
WORKDIR /gradle

# Copy Gradle wrapper, build files and settings first to maximize cache hits
COPY --parents settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle/ ./

RUN --mount=type=cache,target=/home/gradle/.gradle \
  ./gradlew --no-daemon dependencies || true

# Copy sources
COPY ./ ./
RUN mkdir -p data

# Use cache mounts for Gradle caches to speed subsequent builds
# Build the fat/executable jar (project provides buildFatJar task per AGENTS.md)
RUN --mount=type=cache,target=/home/gradle/.gradle \
  --mount=type=cache,target=/root/.gradle \
  gradle --no-daemon buildFatJar -x test -Pversion=${VERSION} -Dversion=${VERSION} --parallel --max-workers=4

# Probe builder: build a small static healthcheck probe
FROM --platform=$BUILDPLATFORM golang:1.26-alpine AS probe-builder
WORKDIR /probe
COPY docker/healthcheck.go .
RUN apk add --no-cache git && \
  CGO_ENABLED=0  GOOS=$TARGETOS GOARCH=$TARGETARCH go build -ldflags "-s -w" -o /healthprobe ./healthcheck.go

# --------------------
# Runtime: small, secure image
# --------------------
FROM --platform=$BUILDPLATFORM gcr.io/distroless/java${JAVA_VERSION}-debian13:nonroot AS runtime

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
COPY --from=probe-builder --chown=65532:65532 /healthprobe /healthprobe
COPY --from=builder --chown=65532:65532 /gradle/build/libs/*.jar /app/app.jar
COPY --from=builder --chown=65532:65532 /gradle/data/ /app/data/
HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 CMD ["/healthprobe", "/"]

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints",  "-Xms128m", "-Xmx1g", "-Xmx512m", "-jar", "/app/app.jar"]
