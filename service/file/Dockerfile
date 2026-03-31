# Stage 1: Build
# Gradle JDK 21 이미지를 사용해서 빌드 환경 구성
FROM gradle:8-jdk21 AS build
WORKDIR /app

# root 프로젝트 설정과 product 모듈 복사
COPY settings.gradle build.gradle /app/
COPY file /app/file

# Gradle 빌드 (테스트 제외)
RUN gradle :file:bootJar -x test --no-daemon

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Build 단계에서 생성된 product JAR 복사
COPY --from=build /app/file/build/libs/*.jar app.jar

# Spring 프로파일 지정
ENV SPRING_PROFILES_ACTIVE=prod

# 로그 디렉토리
VOLUME /app/logs

# 컨테이너 포트
EXPOSE 9004

# 애플리케이션 실행
ENTRYPOINT ["java", "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}", "-jar", "app.jar"]