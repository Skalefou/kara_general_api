# syntax=docker/dockerfile:1

##############################
# Stage 1 — build the boot jar
##############################
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# 1) Copy only the wrapper + build config first so the dependency layer is
#    cached and reused as long as these files don't change.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon --version

# 2) Warm the dependency cache without the sources (best-effort: this layer is
#    invalidated only when build.gradle.kts / settings.gradle.kts change).
RUN ./gradlew --no-daemon dependencies --no-configuration-cache > /dev/null 2>&1 || true

# 3) Now bring in the sources and build the Spring Boot fat jar.
#    `bootJar` does NOT run the test suite (which needs Docker/Testcontainers);
#    tests are expected to run in CI, not in the image build.
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

##############################
# Stage 2 — slim runtime image
##############################
# Alpine (musl libc) JRE — smallest published Temurin 25 runtime.
FROM eclipse-temurin:25-jre-alpine AS runtime

# Run as a non-root system user (Alpine BusyBox tools: addgroup/adduser).
RUN addgroup -S app \
 && adduser -S -G app -h /app -s /sbin/nologin app
WORKDIR /app

# Copy the Spring Boot fat jar owned by the runtime user in a single layer
# (--chown avoids a second full-size layer from a separate chown of the 150+MB jar).
# The plain jar is *-SNAPSHOT-plain.jar and is not matched by this glob.
COPY --from=build --chown=app:app /workspace/build/libs/*-SNAPSHOT.jar app.jar
USER app

# prod profile by default (schema is NOT created by the app — see application-prod.yml).
# JVM is container-memory aware (UseContainerSupport is on by default on JDK 25);
# MaxRAMPercentage caps heap relative to the container limit.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# /actuator/health is exposed by Spring Boot by default and permitted anonymously
# in SecurityConfig.kt, so this probe reflects real application readiness.
# Uses BusyBox wget (already present on Alpine) instead of pulling in curl.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# exec form + `exec` so the JVM becomes PID 1 and receives SIGTERM for graceful shutdown.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
