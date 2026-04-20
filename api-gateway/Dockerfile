FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY build/libs/api-gateway-0.0.1-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

VOLUME /app/logs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
