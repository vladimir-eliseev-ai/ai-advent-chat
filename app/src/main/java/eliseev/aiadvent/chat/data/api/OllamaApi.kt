package eliseev.aiadvent.chat.data.api

import eliseev.aiadvent.chat.data.api.dto.OllamaChatRequestDto
import eliseev.aiadvent.chat.data.api.dto.OllamaResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface OllamaApi {
    @POST("api/chat")
    suspend fun chat(
        @Body request: OllamaChatRequestDto
    ): OllamaResponseDto
}
