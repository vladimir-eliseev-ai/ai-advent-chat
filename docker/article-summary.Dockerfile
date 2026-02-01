FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
RUN ./gradlew :article-summary-mcp-server:shadowJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/article-summary-mcp-server/build/libs/article-summary-mcp-server-1.0.0.jar ./app.jar
ENV PORT=8081
EXPOSE 8081
CMD ["sh", "-c", "java -jar app.jar"]
