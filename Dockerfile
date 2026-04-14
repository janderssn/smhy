FROM eclipse-temurin:25-jdk-alpine

# Install cron and nginx for serving reports
RUN apk add --no-cache dcron nginx

# Set working directory
WORKDIR /app

# Create directories
RUN mkdir -p lib public_html

# Copy application jar to lib directory with expected name
COPY target/*.jar lib/console-0.0.1-SNAPSHOT.jar

# Copy scripts
COPY run.sh /app/run.sh
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

# Copy nginx config
COPY nginx.conf /etc/nginx/http.d/default.conf

# Make scripts executable
RUN chmod +x /app/run.sh /app/docker-entrypoint.sh

# Expose HTTP port
EXPOSE 8080

# Set entrypoint
ENTRYPOINT ["/app/docker-entrypoint.sh"]
