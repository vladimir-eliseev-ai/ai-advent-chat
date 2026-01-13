package eliseev.aiadvent.chat.data.model

data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val structuredResponse: StructuredResponse? = null
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

