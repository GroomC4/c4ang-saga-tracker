FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 비-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring

# JAR 파일 복사
COPY saga-tracker-api/build/libs/saga-tracker-api-*.jar app.jar

# 권한 변경
RUN chown -R spring:spring /app
USER spring:spring

# 환경 변수
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV SPRING_PROFILES_ACTIVE="prod"

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1

# 포트 노출
EXPOSE 8086

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
