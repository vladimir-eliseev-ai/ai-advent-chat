package eliseev.aiadvent.chat.presentation.chat.model

import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.model.MovieInfo

data class UiMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val formattedDate: String? = null,
    val body: String? = null,
    val tags: List<String>? = null,
    val urls: List<String>? = null,
    val userQuestionSummary: String? = null,
    val type: String? = null, // "movie_recommendation" или "clarification_needed"
    val movie: MovieInfo? = null,
    val questions: List<String>? = null,
    val metrics: UiMetrics? = null
)

data class UiMetrics(
    val responseTimeMs: Long,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val costUSD: Double,
    val modelName: String,
    val providerName: String
)
