# syntax=docker/dockerfile:1

# Build (JDK)
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
ARG GITHUB_ACTOR
ENV GITHUB_ACTOR=$GITHUB_ACTOR
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY src src
RUN --mount=type=secret,id=github_token \
    export GITHUB_TOKEN="$(cat /run/secrets/github_token)" \
    && chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test \
    && cp build/libs/schedule-svc-*-SNAPSHOT.jar /workspace/app.jar

# Run (JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/app.jar app.jar
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
