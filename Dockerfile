# syntax=docker/dockerfile:1.7

FROM node:22-bookworm AS node

FROM maven:3.9.11-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY --from=node /usr/local /usr/local
COPY . .
RUN --mount=type=secret,id=maven_settings,target=/root/.m2/settings.xml,required=true \
    mvn -B package -P production -DskipTests

FROM eclipse-temurin:25-jdk

WORKDIR /opt/kassandra
COPY --from=build /workspace/target/kassandra.jar app.jar
COPY test-database-snapshots/Demo-1.zip demo/Demo-1.zip

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/opt/kassandra/app.jar"]
