# === Stage 1: Build Stage ===
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean build -x test --no-daemon

# === Stage 2: Runtime Stage ===
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE ${PORT:-8080}
ENTRYPOINT ["sh", "-c", "java -Duser.timezone=Asia/Seoul -Dserver.port=${PORT:-8080} -jar app.jar"]
