package eliseev.aiadvent.chat.mcp.storage

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("summary-storage-mcp-server")

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8084
    val outputDir = System.getenv("OUTPUT_DIR")?.trim()?.ifBlank { null } ?: "./output"
    val dataFile = File(outputDir, "saved_summaries.json").also { it.parentFile?.mkdirs() }
    log.info("Сервер [Этап 3] сохранение резюме запущен, порт={}, outputDir={}", port, dataFile.parent)
    val mutex = Mutex()

    fun stringArg(obj: JsonObject?, key: String): String =
        obj?.get(key)?.toString()?.trim('"')?.trim() ?: ""

    fun loadSummaries(): List<JsonObject> {
        if (!dataFile.exists()) return emptyList()
        return runCatching {
            val content = dataFile.readText(Charsets.UTF_8).trim()
            if (content.isBlank()) emptyList()
            else Json.parseToJsonElement(content).jsonArray.map { it as JsonObject }
        }.getOrElse { emptyList() }
    }

    fun saveSummaries(items: List<JsonObject>) {
        dataFile.parentFile?.mkdirs()
        val arr = buildJsonArray { items.forEach { add(it) } }
        dataFile.writeText(arr.toString(), Charsets.UTF_8)
    }

    val server = Server(
        serverInfo = Implementation(name = "summary-storage-mcp-server", version = "1.0.0"),
        options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = null)))
    ) { "MCP сервер: сохранение и список резюме статей. Инструменты save_summary, list_saved_summaries." }

    server.addTool(
        name = "save_summary",
        description = "Сохранить резюме статьи с ссылкой на оригинал. Возвращает id сохранённой записи.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("content", buildJsonObject {
                    put("type", "string")
                    put("description", "Текст резюме")
                })
                put("original_url", buildJsonObject {
                    put("type", "string")
                    put("description", "URL оригинальной статьи")
                })
            }
        )
    ) { request: CallToolRequest ->
        runBlocking {
            mutex.withLock {
                try {
                    log.info("[Этап 3] Сохранение резюме")
                    val params = request.arguments as? JsonObject ?: buildJsonObject { }
                    val content = stringArg(params, "content")
                    val originalUrl = stringArg(params, "original_url")
                    if (content.isBlank()) {
                        log.warn("[Этап 3] Текст резюме не передан")
                        return@withLock CallToolResult(
                            content = listOf(TextContent(text = "Текст резюме не передан.")),
                            isError = true
                        )
                    }
                    val list = loadSummaries().toMutableList()
                    val id = "summary_${System.currentTimeMillis()}_${list.size}"
                    val item = buildJsonObject {
                        put("id", id)
                        put("original_url", originalUrl)
                        put("content", content)
                        put("saved_at", System.currentTimeMillis())
                    }
                    list.add(item)
                    saveSummaries(list)
                    log.info("[Этап 3] Сохранено id={}, url={}", id, originalUrl.take(60).let { if (originalUrl.length > 60) "$it..." else it })
                    CallToolResult(
                        content = listOf(TextContent(text = "Сохранено. id: $id")),
                        isError = false
                    )
                } catch (e: Exception) {
                    log.error("[Этап 3] Ошибка сохранения: {}", e.message)
                    CallToolResult(
                        content = listOf(TextContent(text = "Ошибка сохранения: ${e.message}")),
                        isError = true
                    )
                }
            }
        }
    }

    server.addTool(
        name = "list_saved_summaries",
        description = "Вернуть список сохранённых резюме с ссылками на оригиналы. Каждый элемент: id, original_url, content (кратко), saved_at.",
        inputSchema = ToolSchema(properties = buildJsonObject { })
    ) { _: CallToolRequest ->
        runBlocking {
            mutex.withLock {
                try {
                    log.info("[Этап 3] Запрос списка сохранённых резюме")
                    val list = loadSummaries()
                    val previewLen = 200
                    val arr = buildJsonArray {
                        list.reversed().forEach { obj ->
                            val content = (obj["content"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                            val preview = if (content.length <= previewLen) content else content.take(previewLen) + "..."
                            add(buildJsonObject {
                                put("id", obj["id"]?.toString()?.trim('"') ?: "")
                                put("original_url", obj["original_url"]?.toString()?.trim('"') ?: "")
                                put("content_preview", preview)
                                put("content", content)
                                put("saved_at", obj["saved_at"]?.toString() ?: "0")
                            })
                        }
                    }
                    log.info("[Этап 3] Список выдан, записей={}", list.size)
                    CallToolResult(
                        content = listOf(TextContent(text = arr.toString())),
                        isError = false
                    )
                } catch (e: Exception) {
                    log.error("[Этап 3] Ошибка чтения списка: {}", e.message)
                    CallToolResult(
                        content = listOf(TextContent(text = "Ошибка чтения списка: ${e.message}")),
                        isError = true
                    )
                }
            }
        }
    }

    val transport = StreamableHttpServerTransport(enableJsonResponse = true).apply {
        setSessionIdGenerator(null)
    }
    runBlocking { server.connect(transport) }
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) { serverJson() }
        install(SSE)
        routing {
            get { transport.handleRequest(null, call) }
            post { transport.handleRequest(null, call) }
        }
    }.start(wait = true)
}
