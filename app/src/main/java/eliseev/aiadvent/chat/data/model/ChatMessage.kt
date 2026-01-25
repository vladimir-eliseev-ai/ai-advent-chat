package eliseev.aiadvent.chat.data.model

import kotlinx.serialization.Serializable

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val structuredResponse: StructuredResponse? = null,
    val metrics: MessageMetrics? = null,
    val isSummary: Boolean = false,
    val originalMessageCount: Int = 0
)

@Serializable
data class MessageMetrics(
    val responseTimeMs: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUSD: Double,
    val modelName: String,
    val providerName: String
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

