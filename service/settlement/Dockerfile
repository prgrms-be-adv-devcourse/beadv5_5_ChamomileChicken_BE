FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

VOLUME /app/logs

EXPOSE 9002

ENTRYPOINT ["java", "-jar", "app.jar"]
