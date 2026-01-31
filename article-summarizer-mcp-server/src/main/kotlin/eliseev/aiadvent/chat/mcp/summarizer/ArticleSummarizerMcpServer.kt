package eliseev.aiadvent.chat.mcp.summarizer

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory

private const val DEEPSEEK_MAX_INPUT_CHARS = 12_000
private val log = LoggerFactory.getLogger("article-summarizer-mcp-server")
private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com"
private const val DEFAULT_SUMMARY_CHARS = 500

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8083
    val deepSeekApiKey = System.getenv("DEEPSEEK_API_KEY")?.trim()
    val deepSeekModel = System.getenv("DEEPSEEK_MODEL") ?: "deepseek-chat"
    log.info("Сервер [Этап 2] создание резюме запущен, порт={}, DeepSeek={}", port, !deepSeekApiKey.isNullOrBlank())

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

    fun summarizeWithDeepSeek(text: String): String? {
        if (deepSeekApiKey.isNullOrBlank()) return null
        val input = text.take(DEEPSEEK_MAX_INPUT_CHARS)
        val prompt = "Кратко суммаризируй следующий текст на том же языке. Только суть, без вводных слов.\n\n$input"
        return runCatching {
            val body = buildJsonObject {
                put("model", deepSeekModel)
                put("stream", false)
                put("temperature", 0.3)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }
            val response: JsonObject = runBlocking {
                httpClient.post("$DEEPSEEK_BASE_URL/chat/completions") {
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $deepSeekApiKey")
                    setBody(body.toString())
                }.body()
            }
            val choices = response["choices"] as? JsonArray ?: return@runCatching null
            val first = choices.getOrNull(0) as? JsonObject ?: return@runCatching null
            val message = first["message"] as? JsonObject ?: return@runCatching null
            (message["content"] as? JsonPrimitive)?.content?.trim('"')?.replace("\\n", "\n")
        }.getOrNull()
    }

    fun simpleSummarize(text: String, maxLength: Int): String {
        val len = maxLength.coerceIn(100, 2000)
        return if (text.length <= len) text else text.take(len) + "..."
    }

    val server = Server(
        serverInfo = Implementation(name = "article-summarizer-mcp-server", version = "1.0.0"),
        options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = null)))
    ) { "MCP сервер: создание резюме статьи. Инструмент summarize(text). Использует DeepSeek API при наличии DEEPSEEK_API_KEY." }

    server.addTool(
        name = "summarize",
        description = "Создать краткое резюме переданного текста статьи. При DEEPSEEK_API_KEY используется DeepSeek, иначе обрезка по длине.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("text", buildJsonObject {
                    put("type", "string")
                    put("description", "Текст статьи для суммаризации")
                })
                put("max_length", buildJsonObject {
                    put("type", "number")
                    put("description", "Макс. длина резюме при простой обрезке (по умолчанию 500)")
                })
            }
        )
    ) { request: CallToolRequest ->
        try {
            val params = request.arguments as? JsonObject ?: buildJsonObject { }
            val text = stringArg(params, "text")
            val maxLength = (params["max_length"]?.toString()?.toIntOrNull() ?: DEFAULT_SUMMARY_CHARS).coerceIn(100, 2000)
            if (text.isBlank()) {
                log.warn("[Этап 2] Текст не передан")
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Текст не передан.")),
                    isError = true
                )
            }
            log.info("[Этап 2] Создание резюме, длина входа={}", text.length)
            val summary = summarizeWithDeepSeek(text) ?: simpleSummarize(text, maxLength)
            log.info("[Этап 2] Резюме готово, длина={}", summary.length)
            CallToolResult(
                content = listOf(TextContent(text = summary)),
                isError = false
            )
        } catch (e: Exception) {
            log.error("[Этап 2] Ошибка суммаризации: {}", e.message)
            CallToolResult(
                content = listOf(TextContent(text = "Ошибка суммаризации: ${e.message}")),
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
