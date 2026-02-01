FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
RUN ./gradlew :summary-storage-mcp-server:shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/summary-storage-mcp-server/build/libs/summary-storage-mcp-server.jar ./app.jar
ENV PORT=8084
EXPOSE 8084
CMD ["sh", "-c", "java -jar app.jar"]
