FROM alpine/java:21-jre

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY mcp/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=neo4j", "-Dspring.neo4j.uri=embedded", "-jar", "app.jar"]