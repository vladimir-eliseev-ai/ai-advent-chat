FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
RUN ./gradlew :article-reader-mcp-server:shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/article-reader-mcp-server/build/libs/article-reader-mcp-server.jar ./app.jar
ENV PORT=8082
EXPOSE 8082
CMD ["sh", "-c", "java -jar app.jar"]
