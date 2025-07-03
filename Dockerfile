FROM docker.io/gradle:8.13-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle shadowJar --no-daemon
RUN ls -la /app/build/libs/

FROM gcr.io/distroless/java21-debian12:nonroot
COPY --from=build /app/build/libs/*-all.jar /app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]