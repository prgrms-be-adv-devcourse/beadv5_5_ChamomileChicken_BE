# Stage 1: Build
FROM gradle:8-jdk21 AS build
WORKDIR /app

COPY settings.gradle build.gradle gradlew gradlew.bat /app/
COPY gradle /app/gradle
COPY common /app/common
COPY service/build.gradle /app/service/build.gradle
COPY service/user /app/service/user

RUN gradle :service:user:bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/service/user/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod

VOLUME /app/logs
EXPOSE 9003

ENTRYPOINT ["java", "-jar", "app.jar"]