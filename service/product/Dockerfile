# Stage 1: Build
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .

# 빌드 시 테스트 건너뛰기 및 JAR 파일 생성
RUN gradle clean build -x test

# Stage 2: Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# 빌드 스테이지에서 생성된 JAR 파일만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 로그 디렉토리
VOLUME /app/logs

# 포트 설정
EXPOSE 9004
# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "${JAR_NAME}.jar"]
