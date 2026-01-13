package eliseev.aiadvent.chat.data.model

import kotlinx.serialization.Serializable

@Serializable
data class StructuredResponse(
    val date: String? = null,
    val body: String,
    val tags: List<String>? = null,
    val urls: List<String>? = null
)
