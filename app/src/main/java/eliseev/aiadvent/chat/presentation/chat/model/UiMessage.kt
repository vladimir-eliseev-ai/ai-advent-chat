package eliseev.aiadvent.chat.presentation.chat.model

import eliseev.aiadvent.chat.data.model.MessageRole

data class UiMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val formattedDate: String? = null,
    val body: String? = null,
    val tags: List<String>? = null,
    val urls: List<String>? = null,
    val userQuestionSummary: String? = null
)
