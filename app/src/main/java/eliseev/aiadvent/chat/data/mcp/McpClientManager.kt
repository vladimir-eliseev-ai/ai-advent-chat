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

    suspend fun connect(url: String, authToken: String? = null): Result<Unit> {
        return try {
            disconnect()

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
            Timber.d("MCP client connected to: $url")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect to MCP server: $url")
            Result.failure(e)
        }
    }

    suspend fun listTools(): Result<List<Tool>> {
        return try {
            val mcpClient = client ?: return Result.failure(IllegalStateException("Client not connected"))

            val tools = mcpClient.listTools()
            Timber.d("Retrieved ${tools.tools.size} tools from MCP server")
            Result.success(tools.tools)
        } catch (e: Exception) {
            Timber.e(e, "Failed to list tools")
            Result.failure(e)
        }
    }

    suspend fun disconnect() {
        try {
            client?.close()
            httpClient?.close()
            Timber.d("MCP client disconnected")
        } catch (e: Exception) {
            Timber.e(e, "Error disconnecting MCP client")
        } finally {
            client = null
            httpClient = null
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
            val result = mcpClient.callTool(request)
            Timber.d("Tool '$toolName' called successfully")
            Result.success(result)
        } catch (e: Exception) {
            Timber.e(e, "Failed to call tool: $toolName")
            Result.failure(e)
        }
    }

    fun isConnected(): Boolean {
        return client != null
    }
}
