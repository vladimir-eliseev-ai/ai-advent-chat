package eliseev.aiadvent.chat.data.mcp

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequestParams
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.serialization.json.JsonObject
import timber.log.Timber

class McpClientManager {
    private var client: Client? = null
    private var httpClient: HttpClient? = null
    private var connectedUrl: String? = null

    private companion object {
        const val SEP = "==============="
    }

    private fun stageLabel(url: String): String = when {
        url.contains(":8082") -> "Этап 1 — чтение статьи"
        url.contains(":8083") -> "Этап 2 — создание резюме"
        url.contains(":8084") -> "Этап 3 — сохранение / список"
        else -> url
    }

    private fun mcpLog(message: String) {
        Timber.tag("MCP").d("$SEP $message")
    }

    suspend fun connect(url: String, authToken: String? = null): Result<Unit> {
        return try {
            if (client != null && connectedUrl == url) {
                mcpLog("[${stageLabel(url)}] переиспользовано соединение")
                return Result.success(Unit)
            }
            disconnect()

            mcpLog("[${stageLabel(url)}] подключение...")
            httpClient = HttpClient(Android) {
                engine {
                    connectTimeout = 30_000
                    socketTimeout = 60_000
                }
                install(SSE)
                install(HttpTimeout) {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                }
                if (authToken != null && authToken.isNotBlank()) {
                    defaultRequest {
                        header("Authorization", "Bearer $authToken")
                    }
                }
            }
            val transport = StreamableHttpClientTransport(
                client = httpClient!!,
                url = url
            )

            client = Client(
                clientInfo = Implementation(
                    name = "aiadvent-chat",
                    version = "1.0.0"
                )
            )

            client!!.connect(transport)
            connectedUrl = url
            mcpLog("[${stageLabel(url)}] подключено")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.tag("MCP").e(e, "$SEP [${stageLabel(url)}] ошибка подключения")
            Result.failure(e)
        }
    }

    suspend fun listTools(): Result<List<Tool>> {
        return try {
            val mcpClient = client ?: return Result.failure(IllegalStateException("Client not connected"))
            mcpLog("запрос списка инструментов...")
            val tools = mcpClient.listTools()
            mcpLog("получено инструментов: ${tools.tools.size}")
            Result.success(tools.tools)
        } catch (e: Exception) {
            Timber.tag("MCP").e(e, "$SEP ошибка получения списка инструментов")
            Result.failure(e)
        }
    }

    suspend fun disconnect() {
        val wasUrl = connectedUrl
        try {
            client?.close()
            httpClient?.close()
            if (wasUrl != null) {
                mcpLog("[${stageLabel(wasUrl)}] отключено")
            }
        } catch (e: Exception) {
            Timber.tag("MCP").e(e, "$SEP ошибка при отключении")
        } finally {
            client = null
            httpClient = null
            connectedUrl = null
        }
    }

    suspend fun callTool(toolName: String, arguments: JsonObject? = null): Result<CallToolResult> {
        return try {
            val mcpClient = client ?: return Result.failure(IllegalStateException("Client not connected"))

            val request = CallToolRequest(
                params = CallToolRequestParams(
                    name = toolName,
                    arguments = arguments
                )
            )
            mcpLog("вызов инструмента «$toolName»...")
            val result = mcpClient.callTool(request)
            mcpLog("инструмент «$toolName» — OK")
            Result.success(result)
        } catch (e: Exception) {
            Timber.tag("MCP").e(e, "$SEP ошибка вызова инструмента «$toolName»")
            Result.failure(e)
        }
    }

    fun isConnected(): Boolean {
        return client != null
    }
}
