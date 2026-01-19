package eliseev.aiadvent.chat.data.repository

import eliseev.aiadvent.chat.data.api.DeepSeekApi
import eliseev.aiadvent.chat.data.api.dto.ChatRequestDto
import eliseev.aiadvent.chat.data.api.dto.MessageDto
import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.model.StructuredResponse
import eliseev.aiadvent.chat.data.model.SystemPromptProvider
import eliseev.aiadvent.chat.domain.model.ChatResult
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import timber.log.Timber
import java.net.UnknownHostException
import java.io.IOException

class ChatRepository(
    private val api: DeepSeekApi,
    private val systemPromptProvider: SystemPromptProvider
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun sendMessage(
        messages: List<ChatMessage>,
        mode: AnswerMode
    ): ChatResult<List<ChatMessage>> {
        return try {
            // Всегда получаем актуальное системное сообщение
            val systemMessage = systemPromptProvider.getSystemMessage()

            // Удаляем старое системное сообщение (если есть) и добавляем новое
            val messagesWithoutSystem = messages.filter { it.role != MessageRole.SYSTEM }
            val messagesWithSystem = listOf(systemMessage) + messagesWithoutSystem
            
            val lastUserIndex = messagesWithSystem.indexOfLast { it.role == MessageRole.USER }
            val requestMessages = messagesWithSystem.mapIndexed { index, message ->
                val contentForApi = if (index == lastUserIndex) {
                    addModeTagToUserMessage(message.content, mode)
                } else {
                    message.content
                }
                MessageDto(
                    role = message.role.name.lowercase(),
                    content = contentForApi
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
                // Пытаемся распарсить JSON ответ
                val structuredResponse = try {
                    val candidateJson = extractJsonObject(assistantMessage.content) ?: assistantMessage.content
                    json.decodeFromString<StructuredResponse>(candidateJson)
                } catch (e: Exception) {
                    // Если не JSON, используем обычный текст
                    Timber.d(e, "Failed to parse JSON response, using plain text")
                    null
                }
                
                // Возвращаем только USER и ASSISTANT сообщения (без SYSTEM)
                val newMessages = messages + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = structuredResponse?.body ?: assistantMessage.content,
                    structuredResponse = structuredResponse
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

    private fun addModeTagToUserMessage(userMessage: String, mode: AnswerMode): String {
        val trimmed = userMessage.trim()
        if (trimmed.isEmpty()) return userMessage
        val tag = when (mode) {
            AnswerMode.BRIEF -> "[mode=BRIEF]"
            AnswerMode.STEP_BY_STEP -> "[mode=STEP_BY_STEP]"
            AnswerMode.EXPERTS -> "[mode=EXPERTS]"
        }
        return "$tag\n$trimmed"
    }

    suspend fun sendSimpleMessage(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): ChatResult<List<ChatMessage>> {
        return try {
            // Всегда создаем актуальное системное сообщение
            val systemMessage = ChatMessage(
                role = MessageRole.SYSTEM,
                content = systemPrompt
            )

            // Удаляем старое системное сообщение (если есть) и добавляем новое
            val messagesWithoutSystem = messages.filter { it.role != MessageRole.SYSTEM }
            val messagesWithSystem = listOf(systemMessage) + messagesWithoutSystem
            
            val requestMessages = messagesWithSystem.map { message ->
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
                // Возвращаем только USER и ASSISTANT сообщения (без SYSTEM)
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

    private fun extractJsonObject(text: String): String? {
        val trimmed = text.trim()

        // Частый случай: модель заворачивает JSON в fenced code block
        val fenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```", RegexOption.IGNORE_CASE)
        val fenced = fenceRegex.find(trimmed)?.groupValues?.getOrNull(1)?.trim()
        val candidate = if (!fenced.isNullOrBlank()) fenced else trimmed

        val start = candidate.indexOf('{')
        val end = candidate.lastIndexOf('}')
        if (start == -1 || end == -1 || end <= start) return null
        return candidate.substring(start, end + 1).trim()
    }
}

