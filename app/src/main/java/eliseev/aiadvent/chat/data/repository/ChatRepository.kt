package eliseev.aiadvent.chat.data.repository

import eliseev.aiadvent.chat.data.api.DeepSeekApi
import eliseev.aiadvent.chat.data.api.dto.ChatRequestDto
import eliseev.aiadvent.chat.data.api.dto.MessageDto
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.domain.model.ChatResult
import retrofit2.HttpException
import timber.log.Timber
import java.net.UnknownHostException
import java.io.IOException

class ChatRepository(
    private val api: DeepSeekApi
) {
    suspend fun sendMessage(
        messages: List<ChatMessage>
    ): ChatResult<List<ChatMessage>> {
        return try {
            val requestMessages = messages.map { message ->
                MessageDto(
                    role = message.role.name.lowercase(),
                    content = message.content
                )
            }
            
            val response = api.chatCompletions(
                ChatRequestDto(
                    model = "deepseek-chat",
                    messages = requestMessages,
                    stream = false
                )
            )
            
            val assistantMessage = response.choices.firstOrNull()?.message
            if (assistantMessage != null) {
                val newMessages = messages + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = assistantMessage.content
                )
                ChatResult.Success(newMessages)
            } else {
                ChatResult.Error("Пустой ответ от API")
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "Configuration error")
            ChatResult.Error(e.message ?: "Ошибка конфигурации")
        } catch (e: UnknownHostException) {
            Timber.e(e, "Network error: Unable to resolve host")
            ChatResult.Error("Ошибка подключения. Проверьте интернет-соединение")
        } catch (e: IOException) {
            Timber.e(e, "Network error: IO exception")
            ChatResult.Error("Ошибка сети. Проверьте подключение к интернету")
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error: ${e.code()}")
            val errorMessage = when (e.code()) {
                401 -> "Неверный API ключ. Проверьте настройки"
                429 -> "Превышен лимит запросов. Попробуйте позже"
                500, 502, 503 -> "Ошибка сервера. Попробуйте позже"
                else -> "Ошибка сервера: ${e.code()}"
            }
            ChatResult.Error(errorMessage)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error sending message")
            ChatResult.Error("Произошла ошибка: ${e.message ?: "Неизвестная ошибка"}")
        }
    }
}

