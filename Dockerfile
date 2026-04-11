# Stage 1: Build
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and POM first for better layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable (for Windows filesystem compatibility)
RUN chmod +x mvnw

# Set Maven memory limits
ENV MAVEN_OPTS="-Xmx512m -XX:MaxMetaspaceSize=256m"

# Download dependencies (cached if pom.xml unchanged)
RUN ./mvnw dependency:go-offline

# Copy source and build
COPY src ./src

# Build the application (tests run in CI, not during image build)
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy JAR from builder stage (use specific pattern to avoid issues with multiple JARs)
COPY --from=builder /app/target/*[-.]jar app.jar

# Set permissions
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
