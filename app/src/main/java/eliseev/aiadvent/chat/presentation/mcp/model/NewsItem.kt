package eliseev.aiadvent.chat.presentation.mcp.model

import kotlinx.serialization.Serializable

@Serializable
data class NewsItem(
    val title: String,
    val description: String,
    val source: String,
    val publishedAt: String,
    val url: String
)
