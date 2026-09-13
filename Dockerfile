# ---------------------------------------------------------------------------
#  Chinche Hospital — multi-stage build.
#  Stage 1 compiles with Maven; stage 2 is a slim JRE runtime with a
#  non-root user, a JVM tuned for containers, and a healthcheck.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
# Resolve dependencies first so code changes do not invalidate the layer.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system chinche && useradd --system --gid chinche --home /app chinche \
    && mkdir -p /app/logs && chown -R chinche:chinche /app

COPY --from=build /workspace/target/*.jar app.jar

# Container-aware JVM: honour cgroup memory limits, fail fast on OOM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom -Duser.timezone=Asia/Kolkata"

USER chinche
EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
