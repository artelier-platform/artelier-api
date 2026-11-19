# =============================================================================
# Multi-stage Dockerfile for Artelier API (Spring Boot on Java 21)
# Stage 1 compiles and packages the application JAR using Maven.
# Stage 2 creates a minimal runtime image running as a non-root user.
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Download Maven dependencies in an earlier layer so they remain cached
# across builds whenever pom.xml has not changed.
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Copy application source code and build the executable JAR without running tests.
COPY src ./src
RUN mvn clean package -DskipTests -B -q

# -----------------------------------------------------------------------------
# Stage 2: Runtime
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Create a dedicated non-root group and user to execute the container safely.
RUN groupadd --system appgroup && useradd --system --gid appgroup appuser

# Copy only the packaged artifact from the build stage with non-root ownership.
COPY --from=build --chown=appuser:appgroup /app/target/artelier-api-*.jar app.jar

# Switch to the non-root user before starting the JVM process.
USER appuser

# Document the default HTTP port exposed by the Spring Boot application.
EXPOSE 8080

# Start the application JAR with Java preview features enabled.
ENTRYPOINT ["java", "--enable-preview", "-jar", "app.jar"]