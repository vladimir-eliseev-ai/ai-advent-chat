package eliseev.aiadvent.chat.data.api.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("object")
    val `object`: String = "",
    @SerialName("created")
    val created: Long,
    @SerialName("model")
    val model: String,
    @SerialName("choices")
    val choices: List<ChoiceDto>,
    @SerialName("usage")
    val usage: UsageDto?
)

@Serializable
data class ChoiceDto(
    @SerialName("index")
    val index: Int,
    @SerialName("message")
    val message: MessageDto,
    @SerialName("logprobs")
    val logprobs: String? = null,
    @SerialName("finish_reason")
    val finishReason: String?
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)
