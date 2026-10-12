# =============================================================================
# FlyPro Analytics – Backend Dockerfile (multi-stage)
# Build Maven → runtime JRE 21 Alpine (non-root)
# =============================================================================

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package \
    && cp target/*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache curl \
    && addgroup -S flypro \
    && adduser -S flypro -G flypro \
    && mkdir -p /var/log/flypro \
    && chown -R flypro:flypro /var/log/flypro /app

USER flypro

COPY --from=build /workspace/app.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    LOG_FILE=/var/log/flypro/backend.log

EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=90s --retries=10 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
