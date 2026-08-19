FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

RUN apk add --no-cache unzip

COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./

RUN chmod +x gradlew \
    && ./gradlew --no-daemon dependencies

COPY src ./src
COPY docker ./docker

RUN ./gradlew --no-daemon bootJar \
    && mkdir -p /workspace/docker-libs /workspace/docker-bootstrap \
    && unzip -j build/libs/*.jar 'BOOT-INF/lib/mysql-connector-j-*.jar' -d /workspace/docker-libs \
    && javac -cp /workspace/docker-libs/mysql-connector-j-*.jar \
        -d /workspace/docker-bootstrap docker/DatabaseBootstrap.java

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring \
    && adduser -S spring -G spring

WORKDIR /app

COPY --from=build --chown=spring:spring /workspace/build/libs/*.jar app.jar
COPY --from=build --chown=spring:spring /workspace/docker-bootstrap/DatabaseBootstrap.class db-bootstrap/DatabaseBootstrap.class
COPY --from=build --chown=spring:spring /workspace/docker-libs/mysql-connector-j-*.jar mysql-connector-j.jar
COPY --chown=spring:spring docker-entrypoint.sh docker-entrypoint.sh

RUN mkdir -p /app/build/logs \
    && chmod +x /app/docker-entrypoint.sh \
    && chown -R spring:spring /app

USER spring:spring

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=5 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
