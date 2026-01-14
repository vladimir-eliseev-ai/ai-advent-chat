package eliseev.aiadvent.chat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StructuredResponse(
    val type: String? = null, // "movie_recommendation" или "clarification_needed"
    val date: String? = null,
    val body: String,
    val movie: MovieInfo? = null,
    val questions: List<String>? = null,
    // Старые поля для обратной совместимости
    val tags: List<String>? = null,
    val urls: List<String>? = null
)

@Serializable
data class MovieInfo(
    val title: String,
    val year: Int,
    val genre: String,
    val description: String,
    @SerialName("whyMatch")
    val whyMatch: String? = null
)
