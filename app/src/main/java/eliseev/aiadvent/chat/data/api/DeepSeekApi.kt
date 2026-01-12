package eliseev.aiadvent.chat.data.api

import eliseev.aiadvent.chat.data.api.dto.ChatRequestDto
import eliseev.aiadvent.chat.data.api.dto.ChatResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chatCompletions(
        @Body request: ChatRequestDto
    ): ChatResponseDto
}

