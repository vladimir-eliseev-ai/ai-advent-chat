package eliseev.aiadvent.chat.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OllamaChatRequestDto(
    @SerialName("model")
    val model: String,
    @SerialName("messages")
    val messages: List<OllamaMessageDto>,
    @SerialName("stream")
    val stream: Boolean = false,
    @SerialName("options")
    val options: OllamaOptions? = null
)

@Serializable
data class OllamaMessageDto(
    @SerialName("role")
    val role: String,
    @SerialName("content")
    val content: String
)

@Serializable
data class OllamaOptions(
    @SerialName("temperature")
    val temperature: Double? = null,
    @SerialName("num_predict")
    val numPredict: Int? = null
)
