FROM eclipse-temurin:21-jre-alpine-3.23

WORKDIR /app

# Default data directory for LadybugDB persistence
ENV LADYBUGDB_DATA_DIR=/data/ladybugdb
# Default JVM memory settings
ENV INITIAL_MEMORY=256m
ENV MAX_MEMORY=512m
ENV MAX_RAM_PERCENTAGE=75.0

RUN addgroup -S spring && adduser -S spring -G spring

# Create data directory with proper permissions before switching user
RUN mkdir -p /data/ladybugdb && chown -R spring:spring /data

USER spring:spring

COPY mcp/target/*.jar app.jar

# HTTP server port
EXPOSE 8080

# Volume mount point for LadybugDB data
VOLUME ["/data/ladybugdb"]

ENTRYPOINT ["sh", "-c", "java \
    -Xms${INITIAL_MEMORY} \
    -Xmx${MAX_MEMORY} \
    -XX:MaxRAMPercentage=${MAX_RAM_PERCENTAGE} \
    -Dspring.profiles.active=ladybugdb \
    -Dladybugdb.data-dir=${LADYBUGDB_DATA_DIR} \
    -jar \
    app.jar"]
