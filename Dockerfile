# Multi-stage build for production
FROM eclipse-temurin:17-jdk as builder

WORKDIR /app
COPY gradle gradle
COPY build.gradle settings.gradle gradlew ./
COPY src src

# Build the application
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# Production stage
FROM eclipse-temurin:17-jre

# Install additional packages for production
RUN apt-get update && apt-get install -y \
    curl \
    dumb-init \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN addgroup --system --gid 1001 carden && \
    adduser --system --uid 1001 --gid 1001 carden

# Set working directory
WORKDIR /app

# Copy jar file from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Create logs directory
RUN mkdir -p /var/log/carden && \
    chown -R carden:carden /app /var/log/carden

# Switch to non-root user
USER carden

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/public/health || exit 1

# Expose port
EXPOSE 8080

# Use dumb-init to handle signals properly
ENTRYPOINT ["dumb-init", "--"]

# Run the application with production profile
CMD ["java", \
     "-server", \
     "-Xms512m", \
     "-Xmx1024m", \
     "-XX:+UseG1GC", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=80.0", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-Dspring.profiles.active=prod", \
     "-jar", \
     "app.jar"]
