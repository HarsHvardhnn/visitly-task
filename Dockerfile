# Use Eclipse Temurin JDK 17 as base image
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install curl for health checks and wait-for-it script
RUN apt-get update && apt-get install -y curl netcat-openbsd && rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml first (for better layer caching)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x ./mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Copy wait-for-it script
COPY wait-for-it.sh /wait-for-it.sh
RUN chmod +x /wait-for-it.sh

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with wait-for-it for dependencies
CMD ["/wait-for-it.sh", "mysql:3306", "--", "rabbitmq:5672", "--", "java", "-jar", "-Dspring.profiles.active=docker", "target/rbac-0.0.1-SNAPSHOT.jar"]


