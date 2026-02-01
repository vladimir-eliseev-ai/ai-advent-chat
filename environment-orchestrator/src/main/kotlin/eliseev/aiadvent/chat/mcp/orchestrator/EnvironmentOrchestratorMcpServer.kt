package eliseev.aiadvent.chat.mcp.orchestrator

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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger("environment-orchestrator")

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8090
    val composeDir = System.getenv("COMPOSE_DIR")?.trim()?.ifBlank { null }
        ?: System.getProperty("user.dir")
    val dir = File(composeDir)
    log.info("Оркестратор окружения запущен, порт={}, COMPOSE_DIR={}", port, dir.absolutePath)

    fun runDocker(vararg args: String): Pair<Int, String> {
        val proc = ProcessBuilder("docker", "compose", *args)
            .directory(dir)
            .redirectErrorStream(true)
            .start()
        val output = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        return proc.exitValue() to output
    }

    val server = Server(
        serverInfo = Implementation(name = "environment-orchestrator", version = "1.0.0"),
        options = ServerOptions(capabilities = ServerCapabilities(tools = ServerCapabilities.Tools(listChanged = null)))
    ) { "MCP сервер: подъём и остановка Docker Compose окружения статей. Инструменты start_articles_environment, stop_articles_environment, status_articles_environment." }

    server.addTool(
        name = "start_articles_environment",
        description = "Поднять окружение MCP-серверов статей: выполнить docker compose up -d в директории проекта. Запускает в фоне, чтобы не упираться в таймаут MCP.",
        inputSchema = ToolSchema(properties = buildJsonObject { })
    ) { _: CallToolRequest ->
        try {
            log.info("Вызов start_articles_environment (в фоне)")
            Thread {
                try {
                    val (code, out) = runDocker("up", "-d")
                    log.info("docker compose up -d завершён: код={}, вывод={}", code, out.take(500))
                } catch (e: Exception) {
                    log.error("start_articles_environment (фоновый поток): {}", e.message)
                }
            }.start()
            CallToolResult(
                content = listOf(TextContent(text = "Окружение запускается в фоне (docker compose up -d). При первой сборке подождите 2–5 минут, затем проверьте статус (status_articles_environment) или нажмите «Краткое резюме» снова.")),
                isError = false
            )
        } catch (e: Exception) {
            log.error("start_articles_environment: {}", e.message)
            CallToolResult(
                content = listOf(TextContent(text = "Ошибка: ${e.message}")),
                isError = true
            )
        }
    }

    server.addTool(
        name = "stop_articles_environment",
        description = "Остановить окружение: выполнить docker compose down.",
        inputSchema = ToolSchema(properties = buildJsonObject { })
    ) { _: CallToolRequest ->
        try {
            log.info("Вызов stop_articles_environment")
            val (code, out) = runDocker("down")
            val msg = if (code == 0) "Окружение остановлено.\n$out" else "Ошибка (код $code):\n$out"
            CallToolResult(
                content = listOf(TextContent(text = msg)),
                isError = code != 0
            )
        } catch (e: Exception) {
            log.error("stop_articles_environment: {}", e.message)
            CallToolResult(
                content = listOf(TextContent(text = "Ошибка: ${e.message}")),
                isError = true
            )
        }
    }

    server.addTool(
        name = "status_articles_environment",
        description = "Статус контейнеров: выполнить docker compose ps -a.",
        inputSchema = ToolSchema(properties = buildJsonObject { })
    ) { _: CallToolRequest ->
        try {
            val (code, out) = runDocker("ps", "-a")
            val msg = if (code == 0) out else "Ошибка (код $code):\n$out"
            CallToolResult(
                content = listOf(TextContent(text = msg)),
                isError = code != 0
            )
        } catch (e: Exception) {
            log.error("status_articles_environment: {}", e.message)
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
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ContentNegotiation) { serverJson() }
        install(SSE)
        routing {
            get { transport.handleRequest(null, call) }
            post { transport.handleRequest(null, call) }
        }
    }.start(wait = true)
}
