package eliseev.aiadvent.chat.mcp.articlereader

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.serialization.kotlinx.json.json as serverJson
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory

private const val MAX_ARTICLE_CHARS = 100_000
private val log = LoggerFactory.getLogger("article-reader-mcp-server")

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8082
    log.info("Сервер [Этап 1] чтение статьи запущен, порт={}", port)
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
        }
    }

    fun stringArg(obj: JsonObject?, key: String): String =
        obj?.get(key)?.toString()?.trim('"')?.trim() ?: ""

    fun fetchArticleContent(url: String): Result<String> = runCatching {
        if (url.isBlank()) return@runCatching error("URL не указан")
        val html = runBlocking { httpClient.get(url).body<String>() }
        val text = Jsoup.parse(html).text().replace(Regex("\\s+"), " ").trim()
        if (text.length > MAX_ARTICLE_CHARS) text.take(MAX_ARTICLE_CHARS) + "\n[... обрезано ...]" else text
    }

    val server = Server(
        serverInfo = Implementation(name = "article-reader-mcp-server", version = "1.0.0"),
        options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = null)))
    ) { "MCP сервер: чтение статьи по URL. Инструмент fetch_article(url)." }

    server.addTool(
        name = "fetch_article",
        description = "Загрузить статью по URL и вернуть её полный текст (HTML убирается).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("url", buildJsonObject {
                    put("type", "string")
                    put("description", "URL страницы статьи")
                })
            }
        )
    ) { request: CallToolRequest ->
        try {
            val params = request.arguments as? JsonObject ?: buildJsonObject { }
            val url = stringArg(params, "url")
            log.info("[Этап 1] Загрузка статьи: url={}", url.take(80).let { if (url.length > 80) "$it..." else it })
            val result = fetchArticleContent(url)
            result.fold(
                onSuccess = { text ->
                    log.info("[Этап 1] Загрузка завершена, символов={}", text.length)
                    CallToolResult(
                        content = listOf(TextContent(text = text.ifEmpty { "Текст не найден или страница пуста." })),
                        isError = false
                    )
                },
                onFailure = { e ->
                    log.warn("[Этап 1] Ошибка загрузки: {}", e.message)
                    CallToolResult(
                        content = listOf(TextContent(text = "Ошибка загрузки: ${e.message}")),
                        isError = true
                    )
                }
            )
        } catch (e: Exception) {
            log.error("[Этап 1] Исключение: {}", e.message)
            CallToolResult(
                content = listOf(TextContent(text = "Ошибка: ${e.message}")),
                isError = true
            )
        }
    }

    val transport = StreamableHttpServerTransport(enableJsonResponse = true).apply {
        setSessionIdGenerator(null)
    }
    runBlocking { server.connect(transport) }
    embeddedServer(Netty, port = port) {
        install(ServerContentNegotiation) { serverJson() }
        install(SSE)
        routing {
            get { transport.handleRequest(null, call) }
            post { transport.handleRequest(null, call) }
        }
    }.start(wait = true)
}
