# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:23-jdk AS build

WORKDIR /workspace

COPY gradle ./gradle
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY server ./server
COPY shared ./shared
COPY composeApp ./composeApp

RUN chmod +x ./gradlew
RUN ./gradlew --no-daemon :server:buildFatJar

FROM eclipse-temurin:23-jre

WORKDIR /app

ENV ENVIRONMENT=production
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"

COPY --from=build /workspace/server/build/libs/*-all.jar /app/wakeve-server.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/wakeve-server.jar"]
