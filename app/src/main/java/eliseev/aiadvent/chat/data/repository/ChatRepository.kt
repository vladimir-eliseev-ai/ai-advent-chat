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

    suspend fun sendMessage(
        messages: List<ChatMessage>,
        mode: AnswerMode
    ): ChatResult<List<ChatMessage>> {
        val provider = systemPromptProvider.getApiProvider()
        
        return when (provider) {
            ApiProvider.DEEPSEEK -> sendMessageDeepSeek(messages, mode)
            ApiProvider.OLLAMA -> sendMessageOllama(messages, mode)
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
        val provider = systemPromptProvider.getApiProvider()
        
        return when (provider) {
            ApiProvider.DEEPSEEK -> sendSimpleMessageDeepSeek(messages, systemPrompt)
            ApiProvider.OLLAMA -> sendSimpleMessageOllama(messages, systemPrompt)
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

