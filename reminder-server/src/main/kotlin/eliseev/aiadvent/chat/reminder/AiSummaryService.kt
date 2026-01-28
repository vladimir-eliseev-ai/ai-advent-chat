package eliseev.aiadvent.chat.reminder

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

@Serializable
data class ChatRequest(
    @SerialName("model")
    val model: String = "deepseek-chat",
    @SerialName("messages")
    val messages: List<MessageDto>,
    @SerialName("stream")
    val stream: Boolean = false,
    @SerialName("temperature")
    val temperature: Double = 0.7
)

@Serializable
data class MessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)

@Serializable
data class ChatResponse(
    @SerialName("choices")
    val choices: List<ChoiceDto>? = null,
    @SerialName("error")
    val error: ApiError? = null
)

@Serializable
data class ApiError(
    @SerialName("message")
    val message: String? = null,
    @SerialName("type")
    val type: String? = null
)

@Serializable
data class ChoiceDto(
    @SerialName("message")
    val message: MessageDto
)

class AiSummaryService(private val apiKey: String) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@AiSummaryService.json)
        }
    }
    
    suspend fun createSummary(newsText: String): Result<String> {
        return try {
            val prompt = """
                Проанализируй следующие новости и создай краткую сводку (не более 300 слов).
                Выдели основные темы и самые важные события.
                Форматируй сводку структурированно с заголовками.
                
                Новости:
                $newsText
            """.trimIndent()
            
            val request = ChatRequest(
                model = "deepseek-chat",
                messages = listOf(
                    MessageDto(role = "user", content = prompt)
                ),
                temperature = 0.7
            )
            
            val httpResponse = httpClient.post("https://api.deepseek.com/chat/completions") {
                header("Authorization", "Bearer $apiKey")
                header("Content-Type", "application/json")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            val responseBody = httpResponse.bodyAsText()
            
            if (httpResponse.status.value != 200) {
                println("✗ Ошибка API DeepSeek: HTTP ${httpResponse.status.value}")
                println("Ответ: $responseBody")
                println("Отправленный запрос: ${json.encodeToString(request)}")
                return Result.failure(Exception("API error: HTTP ${httpResponse.status.value} - $responseBody"))
            }
            
            val response = json.decodeFromString<ChatResponse>(responseBody)
            
            if (response.error != null) {
                val errorMsg = response.error.message ?: "Unknown error"
                println("✗ Ошибка API DeepSeek: $errorMsg")
                return Result.failure(Exception("API error: $errorMsg"))
            }
            
            val summary = response.choices?.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Empty response from AI"))
            
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun close() {
        httpClient.close()
    }
}
