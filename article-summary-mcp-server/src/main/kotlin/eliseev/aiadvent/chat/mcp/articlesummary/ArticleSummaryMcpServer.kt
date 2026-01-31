package eliseev.aiadvent.chat.mcp.articlesummary

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jsoup.Jsoup
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

private const val MAX_ARTICLE_CHARS = 100_000
private const val DEFAULT_SUMMARY_CHARS = 500
private const val DEEPSEEK_MAX_INPUT_CHARS = 12_000
private const val DEEPSEEK_BASE_URL = "https://api.deepseek.com"

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    val deepSeekApiKey = System.getenv("DEEPSEEK_API_KEY")?.trim()
    val deepSeekModel = System.getenv("DEEPSEEK_MODEL") ?: "deepseek-chat"
    val outputDir = System.getenv("OUTPUT_DIR") ?: "./output"

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
        val html = httpClient.get(url).body<String>()
        val text = Jsoup.parse(html).text().replace(Regex("\\s+"), " ").trim()
        if (text.length > MAX_ARTICLE_CHARS) text.take(MAX_ARTICLE_CHARS) + "\n[... обрезано ...]" else text
    }

    fun summarizeWithDeepSeek(text: String): String? {
        if (deepSeekApiKey.isNullOrBlank()) return null
        val input = text.take(DEEPSEEK_MAX_INPUT_CHARS)
        val prompt = "Кратко суммаризируй следующий текст на том же языке. Только суть, без вводных слов.\n\n$input"
        return runCatching {
            val body = buildJsonObject {
                put("model", deepSeekModel)
                put("stream", false)
                put("temperature", 0.3)
                put("messages", listOf(buildJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }))
            }
            val response: JsonObject = httpClient.post("$DEEPSEEK_BASE_URL/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $deepSeekApiKey")
                setBody(body.toString())
            }.body()
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
        serverInfo = Implementation(name = "article-summary-mcp-server", version = "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = null)
            )
        )
    ) { "MCP сервер: загрузка статьи по ссылке, суммаризация, сохранение в файл." }

    server.addTool(
        name = "fetch_article",
        description = "Загрузить статью по URL и вернуть её текст (HTML убирается).",
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
            val result = fetchArticleContent(url)
            result.fold(
                onSuccess = { text ->
                    CallToolResult(
                        content = listOf(TextContent(text = text.ifEmpty { "Текст не найден или страница пуста." })),
                        isError = false
                    )
                },
                onFailure = { e ->
                    CallToolResult(
                        content = listOf(TextContent(text = "Ошибка загрузки: ${e.message}")),
                        isError = true
                    )
                }
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "Ошибка: ${e.message}")),
                isError = true
            )
        }
    }

    server.addTool(
        name = "summarize",
        description = "Сделать краткое резюме текста. Если задан DEEPSEEK_API_KEY — используется DeepSeek API, иначе обрезка по длине.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("text", buildJsonObject {
                    put("type", "string")
                    put("description", "Текст для суммаризации")
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
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Текст не передан.")),
                    isError = true
                )
            }
            val summary = summarizeWithDeepSeek(text) ?: simpleSummarize(text, maxLength)
            CallToolResult(
                content = listOf(TextContent(text = summary)),
                isError = false
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "Ошибка суммаризации: ${e.message}")),
                isError = true
            )
        }
    }

    server.addTool(
        name = "save_to_file",
        description = "Сохранить текст в файл в директории OUTPUT_DIR (по умолчанию ./output).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("content", buildJsonObject {
                    put("type", "string")
                    put("description", "Содержимое файла")
                })
                put("filename", buildJsonObject {
                    put("type", "string")
                    put("description", "Имя файла (без пути)")
                })
            }
        )
    ) { request: CallToolRequest ->
        try {
            val params = request.arguments as? JsonObject ?: buildJsonObject { }
            val content = stringArg(params, "content")
            var filename = stringArg(params, "filename")
            if (filename.isBlank()) filename = "summary_${System.currentTimeMillis()}.txt"
            if (!filename.all { it.isLetterOrDigit() || it in "._-" }) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Недопустимое имя файла.")),
                    isError = true
                )
            }
            val dir = java.io.File(outputDir)
            dir.mkdirs()
            val file = java.io.File(dir, filename)
            file.writeText(content, Charsets.UTF_8)
            CallToolResult(
                content = listOf(TextContent(text = "Сохранено: ${file.absolutePath}")),
                isError = false
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "Ошибка сохранения: ${e.message}")),
                isError = true
            )
        }
    }

    server.addTool(
        name = "summarize_article",
        description = "Загрузить статью по ссылке и вернуть краткое резюме (цепочка: fetch_article → summarize).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("url", buildJsonObject {
                    put("type", "string")
                    put("description", "URL статьи")
                })
                put("max_length", buildJsonObject {
                    put("type", "number")
                    put("description", "Макс. длина резюме при обрезке без LLM")
                })
            }
        )
    ) { request: CallToolRequest ->
        try {
            val params = request.arguments as? JsonObject ?: buildJsonObject { }
            val url = stringArg(params, "url")
            val maxLength = params["max_length"]?.toString()?.toIntOrNull() ?: DEFAULT_SUMMARY_CHARS
            if (url.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "URL не указан.")),
                    isError = true
                )
            }
            val fetchResult = fetchArticleContent(url)
            val text = fetchResult.getOrElse {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Ошибка загрузки: ${it.message}")),
                    isError = true
                )
            }
            if (text.isBlank()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "Текст статьи пуст.")),
                    isError = false
                )
            }
            val summary = summarizeWithDeepSeek(text) ?: simpleSummarize(text, maxLength)
            CallToolResult(
                content = listOf(TextContent(text = summary)),
                isError = false
            )
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(TextContent(text = "Ошибка: ${e.message}")),
                isError = true
            )
        }
    }

    val transport = StreamableHttpServerTransport(enableJsonResponse = true)
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
