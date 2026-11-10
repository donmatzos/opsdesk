# syntax=docker/dockerfile:1
# opsdesk image: build the jar, split it into Spring Boot layers, run on a Red Hat UBI JRE.
# OpenShift-ready: runs as UID 1001 / GID 0 and also under an arbitrary UID (restricted SCC), read-only root FS ok.

FROM eclipse-temurin:25-jdk-noble AS build
WORKDIR /src
COPY . .
# Caches survive between builds (Maven repo, npm cache, the Node download of frontend-maven-plugin).
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    --mount=type=cache,target=/src/frontend/node \
    ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-ubi10-minimal AS extract
WORKDIR /extract
COPY --from=build /src/backend/target/opsdesk.jar .
RUN java -Djarmode=tools -jar opsdesk.jar extract --layers --destination layers \
 # Group 0 gets the owner's rights here, so the runtime stage needs no RUN (a chmod there would copy every file
 # into one more layer). OpenShift runs with a random UID that is always in group 0.
 && chmod -R g=u layers

FROM eclipse-temurin:25-jre-ubi10-minimal
LABEL org.opencontainers.image.title="opsdesk" \
      org.opencontainers.image.source="https://github.com/donmatzos/opsdesk"
WORKDIR /app
# Least to most frequently changing, so a code change only rebuilds the last layer.
COPY --from=extract --chown=1001:0 /extract/layers/dependencies/ ./
COPY --from=extract --chown=1001:0 /extract/layers/spring-boot-loader/ ./
COPY --from=extract --chown=1001:0 /extract/layers/snapshot-dependencies/ ./
COPY --from=extract --chown=1001:0 /extract/layers/application/ ./
USER 1001
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs
# Docker/compose only; Kubernetes uses the probes from the Helm chart.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fs http://localhost:8080/readyz || exit 1
ENTRYPOINT ["java", "-jar", "opsdesk.jar"]
