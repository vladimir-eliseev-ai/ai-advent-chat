package eliseev.aiadvent.chat.reminder

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Serializable
data class JsonRpcRequest(
    @SerialName("jsonrpc")
    val jsonrpc: String = "2.0",
    @SerialName("id")
    val id: Int,
    @SerialName("method")
    val method: String,
    @SerialName("params")
    val params: JsonObject? = null
)

@Serializable
data class JsonRpcResponse(
    @SerialName("jsonrpc")
    val jsonrpc: String,
    @SerialName("id")
    val id: Int?,
    @SerialName("result")
    val result: JsonObject? = null,
    @SerialName("error")
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    @SerialName("code")
    val code: Int,
    @SerialName("message")
    val message: String
)

class McpNewsClient(private val mcpServerUrl: String) {
    private var httpClient: HttpClient = createHttpClient()
    private var requestId = 1
    private var sessionId: String = ""
    
    private fun createHttpClient(): HttpClient {
        return HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }
    
    suspend fun connect() {
        println("Подключение к MCP серверу: $mcpServerUrl")
        
        // Сервер использует JSON режим (enableJsonResponse = true), поэтому
        // SDK Client с SSE транспортом не подходит. Используем прямые HTTP запросы.
        // sessionId будет получен от сервера при первом запросе
    }
    
    private suspend fun sendDirectRequest(method: String, params: JsonObject? = null, includeSessionId: Boolean = true): Result<JsonRpcResponse> {
        return try {
            val request = JsonRpcRequest(
                id = requestId++,
                method = method,
                params = params
            )
            
            val httpResponse = httpClient.post(mcpServerUrl) {
                contentType(ContentType.Application.Json)
                header("Accept", "application/json, text/event-stream")
                // Отправляем sessionId только если он есть и includeSessionId = true
                if (includeSessionId && sessionId.isNotEmpty()) {
                    header("Mcp-Session-Id", sessionId)
                }
                setBody(request)
            }
            
            // Читаем sessionId из заголовков ответа
            val responseSessionId = httpResponse.headers["Mcp-Session-Id"]
            if (responseSessionId != null && responseSessionId.isNotEmpty()) {
                sessionId = responseSessionId
                println("✓ Получен sessionId от сервера: $sessionId")
            }
            
            if (httpResponse.status.value != 200) {
                val errorBody = httpResponse.bodyAsText()
                return Result.failure(Exception("HTTP ${httpResponse.status.value}: $errorBody"))
            }
            
            val responseBody = httpResponse.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val response = json.decodeFromString<JsonRpcResponse>(responseBody)
            
            if (response.error != null) {
                // Если ошибка "Session not found", пробуем отправить запрос без sessionId
                if (response.error.message.contains("Session not found", ignoreCase = true) && includeSessionId) {
                    println("⚠ Session not found, пробуем запрос без sessionId...")
                    return sendDirectRequest(method, params, includeSessionId = false)
                }
                
                // Если ошибка "Server already initialized", это нормально
                if (response.error.message.contains("already initialized", ignoreCase = true)) {
                    println("⚠ Сервер уже инициализирован")
                } else {
                    return Result.failure(Exception("MCP error (code ${response.error.code}): ${response.error.message}"))
                }
            }
            
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getLatestNews(query: String? = null, language: String = "ru"): Result<String> {
        return try {
            // Используем прямые HTTP запросы (сервер работает в JSON режиме)
            println("Использование прямых HTTP запросов...")
            
            // Сначала инициализируем сервер
            val initParams = buildJsonObject {
                put("protocolVersion", "2024-11-05")
                put("capabilities", buildJsonObject {})
                put("clientInfo", buildJsonObject {
                    put("name", "reminder-server")
                    put("version", "1.0.0")
                })
            }
            
            println("Инициализация MCP сервера...")
            val initResult = sendDirectRequest("initialize", initParams)
            
            // Обрабатываем результат инициализации
            if (initResult.isFailure) {
                val error = initResult.exceptionOrNull()?.message ?: ""
                if (error.contains("already initialized", ignoreCase = true)) {
                    println("✓ Сервер уже инициализирован, продолжаем работу...")
                } else {
                    println("⚠ Ошибка инициализации: $error")
                }
            } else {
                println("✓ Инициализация успешна")
            }
            
            // Вызываем инструмент
            val arguments = buildJsonObject {
                if (query != null && query.isNotBlank()) {
                    put("query", query)
                }
                put("language", language)
            }
            
            val toolParams = buildJsonObject {
                put("name", "get_latest_news")
                put("arguments", arguments)
            }
            
            println("Вызов инструмента get_latest_news...")
            val responseResult = sendDirectRequest("tools/call", toolParams)
            
            if (responseResult.isFailure) {
                return Result.failure(responseResult.exceptionOrNull() ?: Exception("Unknown error"))
            }
            
            val response = responseResult.getOrNull() ?: return Result.failure(Exception("Empty response"))
            
            if (response.error != null) {
                return Result.failure(Exception("MCP error (code ${response.error.code}): ${response.error.message}"))
            }
            
            val result = response.result ?: return Result.failure(Exception("Empty result"))
            
            val newsText = extractNewsText(result)
            if (newsText == null || newsText.isBlank()) {
                return Result.failure(Exception("No text content in response"))
            }
            
            println("✓ Новости получены: ${newsText.length} символов")
            Result.success(newsText)
        } catch (e: Exception) {
            println("✗ Ошибка при получении новостей: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun extractNewsText(result: JsonObject?): String? {
        if (result == null) return null
        
        val contentArray = result["content"]
        return if (contentArray != null) {
            try {
                val contentList = contentArray.jsonArray
                val firstContent = contentList.firstOrNull()?.jsonObject
                firstContent?.get("text")?.toString()?.trim('"')?.replace("\\n", "\n")
            } catch (e: Exception) {
                val textMatch = Regex(""""text"\s*:\s*"([^"]+)"""").find(contentArray.toString())
                textMatch?.groupValues?.get(1)?.replace("\\n", "\n")
            }
        } else {
            null
        }
    }
    
    suspend fun disconnect() {
        try {
            println("Отключение от MCP сервера...")
            // НЕ сбрасываем sessionId - он может быть нужен для следующего запроса
            // sessionId = ""
            requestId = 1
            println("Отключено от MCP сервера")
        } catch (e: Exception) {
            println("Ошибка при отключении: ${e.message}")
        }
    }
}
