# ============================================================
# Stage 1: Build
# ============================================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy the Maven wrapper and POM first to leverage layer caching.
COPY mvnw ./
COPY .mvn .mvn
COPY pom.xml ./

# Make the Maven wrapper executable (Linux).
RUN chmod +x mvnw

# Download dependencies trước
# Giúp tận dụng Docker layer cache
# Exclude test dependencies (not needed for build) and increase HTTP timeouts for large artifacts
RUN ./mvnw dependency:go-offline -B \
    -DexcludeTestDeps=true \
    -Daether.connector.http.connectTimeout=120000 \
    -Daether.connector.http.requestTimeout=300000

# Copy source code.
COPY src src

# Build a production JAR:
#   -Pprod skips tests (run separately in CI) and excludes
#         test-only resources from the packaged artifact.
RUN ./mvnw clean package -Pprod -B


# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install mysql-client for database backup and restore operations
RUN apt-get update && apt-get install -y --no-install-recommends mysql-client && rm -rf /var/lib/apt/lists/*

# Create runtime directories for file storage and backups.
RUN mkdir -p /app/uploads /app/files/qr /app/backups

# Copy the packaged JAR from the build stage.
COPY --from=builder /app/target/*.jar app.jar

# Run as a non-root user for security.
RUN addgroup --system spring && adduser --system spring --ingroup spring
RUN chown -R spring:spring /app
USER spring:spring

EXPOSE 8080

# JVM options can be overridden at runtime (e.g. -Xmx512m).
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
