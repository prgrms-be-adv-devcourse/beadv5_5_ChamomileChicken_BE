# Stage 1: Build
FROM gradle:8-jdk21 AS build
WORKDIR /app

# 멀티모듈 루트 기준 파일 복사
COPY settings.gradle build.gradle gradlew gradlew.bat /app/
COPY gradle /app/gradle

# 공통 모듈 + service 설정 + user 모듈 복사
COPY common /app/common
COPY service/build.gradle /app/service/build.gradle
COPY service/user /app/service/user

# user 모듈 bootJar 빌드
RUN gradle :service:user:bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# build 결과 복사
COPY --from=build /app/service/user/build/libs/*.jar app.jar

# Spring profile
ENV SPRING_PROFILES_ACTIVE=prod

# 로그 디렉토리
VOLUME /app/logs

# user 모듈 포트
EXPOSE 9003

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]