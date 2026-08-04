# syntax=docker/dockerfile:1

# --- 1. aşama: build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
COPY src ./src

# BuildKit cache mount: ~/.m2 katmanlar arasında saklanır, her build'de
# bağımlılıklar yeniden indirilmez.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# --- 2. aşama: runtime ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --from=build /build/target/*.jar app.jar

# Attachment modülünün yazacağı dizin (compose'da named volume bağlanıyor)
RUN mkdir -p /app/uploads && chown -R app:app /app

USER app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
