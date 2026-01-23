package eliseev.aiadvent.chat.data.repository

import eliseev.aiadvent.chat.data.api.DeepSeekApi
import eliseev.aiadvent.chat.data.api.OllamaApi
import eliseev.aiadvent.chat.data.api.dto.ChatRequestDto
import eliseev.aiadvent.chat.data.api.dto.MessageDto
import eliseev.aiadvent.chat.data.api.dto.OllamaChatRequestDto
import eliseev.aiadvent.chat.data.api.dto.OllamaMessageDto
import eliseev.aiadvent.chat.data.api.dto.OllamaOptions
import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageMetrics
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
    private val deepSeekApi: DeepSeekApi,
    private val ollamaApi: OllamaApi,
    private val systemPromptProvider: SystemPromptProvider
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    companion object {
        private const val COMPRESSION_THRESHOLD = 6 // Сжимать каждые 6 сообщения
    }

    suspend fun sendMessage(
        messages: List<ChatMessage>,
        mode: AnswerMode
    ): ChatResult<List<ChatMessage>> {
        val isCompressionEnabled = systemPromptProvider.isHistoryCompressionEnabled()
        Timber.d("sendMessage: compression enabled=$isCompressionEnabled, messages count=${messages.size}")
        
        // Проверяем необходимость сжатия истории (только если включено)
        val compressedMessages = if (isCompressionEnabled) {
            compressHistoryIfNeeded(messages)
        } else {
            Timber.d("Compression disabled, skipping")
            messages
        }
        
        val provider = systemPromptProvider.getApiProvider()
        
        return when (provider) {
            ApiProvider.DEEPSEEK -> sendMessageDeepSeek(compressedMessages, mode)
            ApiProvider.OLLAMA -> sendMessageOllama(compressedMessages, mode)
        }
    }
    
    /**
     * Сжимает историю диалога, если количество сообщений достигло порога
     */
    private suspend fun compressHistoryIfNeeded(messages: List<ChatMessage>): List<ChatMessage> {
        // Фильтруем только USER и ASSISTANT сообщения (без SYSTEM)
        val conversationMessages = messages.filter { 
            it.role != MessageRole.SYSTEM 
        }
        
        Timber.d("compressHistoryIfNeeded: total messages=${messages.size}, conversation messages=${conversationMessages.size}")
        
        // Находим последний summary (если есть)
        val lastSummaryIndex = messages.indexOfLast { it.isSummary }
        
        // Определяем сообщения после последнего summary (или все, если summary нет)
        val messagesAfterSummary = if (lastSummaryIndex >= 0) {
            messages.subList(lastSummaryIndex + 1, messages.size)
                .filter { it.role != MessageRole.SYSTEM }
        } else {
            conversationMessages
        }
        
        Timber.d("Messages after last summary: ${messagesAfterSummary.size}")
        
        // Если сообщений после summary меньше порога, возвращаем как есть
        if (messagesAfterSummary.size < COMPRESSION_THRESHOLD) {
            Timber.d("Not enough messages for compression: ${messagesAfterSummary.size} < $COMPRESSION_THRESHOLD")
            return messages
        }
        
        Timber.d("Compression needed: ${messagesAfterSummary.size} messages after summary, threshold=$COMPRESSION_THRESHOLD")
        
        // Берем последние COMPRESSION_THRESHOLD сообщений для сжатия
        val messagesToCompress = messagesAfterSummary.takeLast(COMPRESSION_THRESHOLD)
        
        // Если сообщений для сжатия меньше порога, не сжимаем
        if (messagesToCompress.size < COMPRESSION_THRESHOLD) {
            Timber.d("Not enough messages to compress: ${messagesToCompress.size} < $COMPRESSION_THRESHOLD")
            return messages
        }

        // Находим индексы сообщений для сжатия в оригинальном списке
        val firstMessageToCompress = messagesToCompress.first()
        val startIndex = messages.indexOfFirst { 
            it.role == firstMessageToCompress.role && 
            it.content == firstMessageToCompress.content &&
            it.timestamp == firstMessageToCompress.timestamp
        }

        if (startIndex < 0) {
            Timber.w("Could not find start index for compression")
            return messages
        }
        
        // Создаем summary через API
        Timber.d("Compressing ${messagesToCompress.size} messages into summary")
        val summaryResult = createSummary(messagesToCompress)
        
        return when (summaryResult) {
            is ChatResult.Success -> {
                val summaryMessage = summaryResult.data
                Timber.d("Summary created successfully, replacing ${messagesToCompress.size} messages")
                Timber.d("Summary message: isSummary=${summaryMessage.isSummary}, originalCount=${summaryMessage.originalMessageCount}, content=${summaryMessage.content.take(100)}...")
                // Заменяем сжатые сообщения на summary
                val messagesBefore = messages.subList(0, startIndex).toList()
                val messagesAfter = messages.subList(startIndex + messagesToCompress.size, messages.size).toList()
                val resultMessages = messagesBefore + summaryMessage + messagesAfter
                Timber.d("Result messages count: ${resultMessages.size}, summary messages count: ${resultMessages.count { it.isSummary }}")
                resultMessages
            }
            else -> {
                // Если не удалось создать summary, возвращаем оригинальные сообщения
                Timber.w("Failed to create summary, using original messages")
                messages
            }
        }
    }
    
    /**
     * Создает summary для списка сообщений
     */
    private suspend fun createSummary(messagesToCompress: List<ChatMessage>): ChatResult<ChatMessage> {
        if (messagesToCompress.isEmpty()) {
            return ChatResult.Error("Нет сообщений для сжатия")
        }
        
        // Формируем промпт для создания summary
        val conversationText = messagesToCompress.joinToString("\n\n") { message ->
            "${message.role.name}: ${message.content}"
        }
        
        val summaryPrompt = """
            Создай краткое резюме следующего диалога, сохраняя ключевые факты, контекст и важные детали.
            Резюме должно быть достаточно подробным, чтобы можно было продолжить разговор с пониманием контекста.
            
            Диалог:
            $conversationText
            
            Резюме:
        """.trimIndent()
        
        // Создаем временный список сообщений для запроса summary
        val summaryRequestMessages = listOf(
            ChatMessage(
                role = MessageRole.USER,
                content = summaryPrompt
            )
        )
        
        // Отправляем запрос на создание summary
        val result = sendSimpleMessage(
            messages = summaryRequestMessages,
            systemPrompt = "Ты помощник, который создает краткие и информативные резюме диалогов. Сохраняй все важные детали и контекст."
        )
        
        return when (result) {
            is ChatResult.Success -> {
                val summaryMessage = result.data.lastOrNull()
                if (summaryMessage != null && summaryMessage.role == MessageRole.ASSISTANT) {
                    ChatResult.Success(
                        summaryMessage.copy(
                            isSummary = true,
                            originalMessageCount = messagesToCompress.size
                        )
                    )
                } else {
                    ChatResult.Error("Не удалось создать резюме")
                }
            }
            is ChatResult.Error -> ChatResult.Error(result.message)
            is ChatResult.Loading -> ChatResult.Error("Ожидание ответа...")
        }
    }
    
    private suspend fun sendMessageDeepSeek(
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
            
            val temperature = systemPromptProvider.getTemperature().toDouble()
            val model = systemPromptProvider.getDeepSeekModel()
            
            val startTime = System.currentTimeMillis()
            val response = deepSeekApi.chatCompletions(
                ChatRequestDto(
                    model = model,
                    messages = requestMessages,
                    stream = false,
                    temperature = temperature
                )
            )
            val responseTime = System.currentTimeMillis() - startTime
            
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
                
                // Создаем метрики
                val usage = response.usage
                val metrics = if (usage != null) {
                    MessageMetrics(
                        responseTimeMs = responseTime,
                        promptTokens = usage.promptTokens,
                        completionTokens = usage.completionTokens,
                        totalTokens = usage.totalTokens,
                        costUSD = calculateCost(
                            ApiProvider.DEEPSEEK,
                            model,
                            usage.promptTokens,
                            usage.completionTokens
                        ),
                        modelName = model,
                        providerName = "DeepSeek"
                    )
                } else {
                    null
                }
                
                // Возвращаем только USER и ASSISTANT сообщения (без SYSTEM)
                val newMessages = messages + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = structuredResponse?.body ?: assistantMessage.content,
                    structuredResponse = structuredResponse,
                    metrics = metrics
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
    
    private suspend fun sendMessageOllama(
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
                OllamaMessageDto(
                    role = message.role.name.lowercase(),
                    content = contentForApi
                )
            }
            
            val temperature = systemPromptProvider.getTemperature().toDouble()
            val model = systemPromptProvider.getOllamaModel()
            
            val startTime = System.currentTimeMillis()
            val response = ollamaApi.chat(
                OllamaChatRequestDto(
                    model = model,
                    messages = requestMessages,
                    stream = false,
                    options = OllamaOptions(temperature = temperature)
                )
            )
            val responseTime = System.currentTimeMillis() - startTime
            
            val assistantMessage = response.message
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
                
                // Создаем метрики
                val metrics = MessageMetrics(
                    responseTimeMs = responseTime,
                    promptTokens = response.promptEvalCount ?: 0,
                    completionTokens = response.evalCount ?: 0,
                    totalTokens = (response.promptEvalCount ?: 0) + (response.evalCount ?: 0),
                    costUSD = 0.0, // Ollama бесплатный
                    modelName = model,
                    providerName = "Ollama"
                )
                
                // Возвращаем только USER и ASSISTANT сообщения (без SYSTEM)
                val newMessages = messages + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = structuredResponse?.body ?: assistantMessage.content,
                    structuredResponse = structuredResponse,
                    metrics = metrics
                )
                ChatResult.Success(newMessages)
            } else {
                ChatResult.Error("Пустой ответ от Ollama API")
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "Configuration error")
            ChatResult.Error(e.message ?: "Ошибка конфигурации")
        } catch (e: UnknownHostException) {
            Timber.e(e, "Network error: Unable to resolve host")
            ChatResult.Error("Ошибка подключения к Ollama. Убедитесь, что Ollama запущена")
        } catch (e: IOException) {
            Timber.e(e, "Network error: IO exception")
            ChatResult.Error("Ошибка подключения к Ollama. Проверьте настройки")
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error: ${e.code()}")
            val errorMessage = when (e.code()) {
                404 -> "Модель ${systemPromptProvider.getOllamaModel()} не найдена. Загрузите её командой: ollama pull ${systemPromptProvider.getOllamaModel()}"
                500, 502, 503 -> "Ошибка Ollama сервера. Попробуйте позже"
                else -> "Ошибка Ollama: ${e.code()}"
            }
            ChatResult.Error(errorMessage)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error sending message to Ollama")
            ChatResult.Error("Произошла ошибка Ollama: ${e.message ?: "Неизвестная ошибка"}")
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
        // Проверяем необходимость сжатия истории (только если включено)
        val compressedMessages = if (systemPromptProvider.isHistoryCompressionEnabled()) {
            compressHistoryIfNeeded(messages)
        } else {
            messages
        }
        
        val provider = systemPromptProvider.getApiProvider()
        
        return when (provider) {
            ApiProvider.DEEPSEEK -> sendSimpleMessageDeepSeek(compressedMessages, systemPrompt)
            ApiProvider.OLLAMA -> sendSimpleMessageOllama(compressedMessages, systemPrompt)
        }
    }
    
    private suspend fun sendSimpleMessageDeepSeek(
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
            
            val temperature = systemPromptProvider.getTemperature().toDouble()
            val model = systemPromptProvider.getDeepSeekModel()
            
            val startTime = System.currentTimeMillis()
            val response = deepSeekApi.chatCompletions(
                ChatRequestDto(
                    model = model,
                    messages = requestMessages,
                    stream = false,
                    temperature = temperature
                )
            )
            val responseTime = System.currentTimeMillis() - startTime
            
            val assistantMessage = response.choices.firstOrNull()?.message
            if (assistantMessage != null) {
                // Создаем метрики
                val usage = response.usage
                val metrics = if (usage != null) {
                    MessageMetrics(
                        responseTimeMs = responseTime,
                        promptTokens = usage.promptTokens,
                        completionTokens = usage.completionTokens,
                        totalTokens = usage.totalTokens,
                        costUSD = calculateCost(
                            ApiProvider.DEEPSEEK,
                            model,
                            usage.promptTokens,
                            usage.completionTokens
                        ),
                        modelName = model,
                        providerName = "DeepSeek"
                    )
                } else {
                    null
                }
                
                // Возвращаем только USER и ASSISTANT сообщения (без SYSTEM)
                val newMessages = messages + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = assistantMessage.content,
                    metrics = metrics
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
    
    private suspend fun sendSimpleMessageOllama(
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
                OllamaMessageDto(
                    role = message.role.name.lowercase(),
                    content = message.content
                )
            }
            
            val temperature = systemPromptProvider.getTemperature().toDouble()
            val model = systemPromptProvider.getOllamaModel()
            
            val startTime = System.currentTimeMillis()
            val response = ollamaApi.chat(
                OllamaChatRequestDto(
                    model = model,
                    messages = requestMessages,
                    stream = false,
                    options = OllamaOptions(temperature = temperature)
                )
            )
            val responseTime = System.currentTimeMillis() - startTime
            
            val assistantMessage = response.message
            if (assistantMessage != null) {
                // Создаем метрики
                val metrics = MessageMetrics(
                    responseTimeMs = responseTime,
                    promptTokens = response.promptEvalCount ?: 0,
                    completionTokens = response.evalCount ?: 0,
                    totalTokens = (response.promptEvalCount ?: 0) + (response.evalCount ?: 0),
                    costUSD = 0.0, // Ollama бесплатный
                    modelName = model,
                    providerName = "Ollama"
                )
                
                // Возвращаем только USER и ASSISTANT сообщения (без SYSTEM)
                val newMessages = messages + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = assistantMessage.content,
                    metrics = metrics
                )
                ChatResult.Success(newMessages)
            } else {
                ChatResult.Error("Пустой ответ от Ollama API")
            }
        } catch (e: IllegalStateException) {
            Timber.e(e, "Configuration error")
            ChatResult.Error(e.message ?: "Ошибка конфигурации")
        } catch (e: UnknownHostException) {
            Timber.e(e, "Network error: Unable to resolve host")
            ChatResult.Error("Ошибка подключения к Ollama. Убедитесь, что Ollama запущена")
        } catch (e: IOException) {
            Timber.e(e, "Network error: IO exception")
            ChatResult.Error("Ошибка подключения к Ollama. Проверьте настройки")
        } catch (e: HttpException) {
            Timber.e(e, "HTTP error: ${e.code()}")
            val errorMessage = when (e.code()) {
                404 -> "Модель ${systemPromptProvider.getOllamaModel()} не найдена. Загрузите её командой: ollama pull ${systemPromptProvider.getOllamaModel()}"
                500, 502, 503 -> "Ошибка Ollama сервера. Попробуйте позже"
                else -> "Ошибка Ollama: ${e.code()}"
            }
            ChatResult.Error(errorMessage)
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error sending message to Ollama")
            ChatResult.Error("Произошла ошибка Ollama: ${e.message ?: "Неизвестная ошибка"}")
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
    
    private fun calculateCost(
        provider: ApiProvider,
        modelName: String,
        promptTokens: Int,
        completionTokens: Int
    ): Double {
        return when (provider) {
            ApiProvider.DEEPSEEK -> {
                // DeepSeek pricing (по состоянию на 2024)
                // Input: $0.14 / 1M tokens
                // Output: $0.28 / 1M tokens
                val inputCost = (promptTokens / 1_000_000.0) * 0.14
                val outputCost = (completionTokens / 1_000_000.0) * 0.28
                inputCost + outputCost
            }
            ApiProvider.OLLAMA -> {
                // Ollama бесплатный
                0.0
            }
        }
    }
}

