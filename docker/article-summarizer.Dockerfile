FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
RUN ./gradlew :article-summarizer-mcp-server:shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/article-summarizer-mcp-server/build/libs/article-summarizer-mcp-server.jar ./app.jar
ENV PORT=8083
EXPOSE 8083
CMD ["sh", "-c", "java -jar app.jar"]
