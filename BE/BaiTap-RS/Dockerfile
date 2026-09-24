FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY --from=build --chown=spring:spring /workspace/build/libs/*.jar app.jar
RUN mkdir -p /app/build/logs && chown -R spring:spring /app

USER spring:spring

ENV APP_SEED_DEMO_ENABLED=false
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
