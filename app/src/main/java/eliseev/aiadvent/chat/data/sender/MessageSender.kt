package eliseev.aiadvent.chat.data.sender

import eliseev.aiadvent.chat.data.api.DeepSeekApi
import eliseev.aiadvent.chat.data.api.OllamaApi
import eliseev.aiadvent.chat.data.api.dto.ChatRequestDto
import eliseev.aiadvent.chat.data.api.dto.MessageDto
import eliseev.aiadvent.chat.data.api.dto.OllamaChatRequestDto
import eliseev.aiadvent.chat.data.api.dto.OllamaMessageDto
import eliseev.aiadvent.chat.data.api.dto.OllamaOptions
import eliseev.aiadvent.chat.data.api.dto.OllamaResponseDto
import eliseev.aiadvent.chat.data.api.dto.UsageDto
import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageMetrics
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.model.StructuredResponse
import eliseev.aiadvent.chat.data.model.AppSettings
import eliseev.aiadvent.chat.data.model.SystemPromptProvider
import eliseev.aiadvent.chat.data.util.CostCalculator
import eliseev.aiadvent.chat.data.util.JsonExtractor
import eliseev.aiadvent.chat.domain.model.ChatResult
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.UnknownHostException

class MessageSender(
    private val deepSeekApi: DeepSeekApi,
    private val ollamaApi: OllamaApi,
    private val systemPromptProvider: SystemPromptProvider,
    private val appSettings: AppSettings,
    private val json: Json
) {
    
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        mode: AnswerMode
    ): ChatResult<List<ChatMessage>> {
        val provider = appSettings.getApiProvider()
        
        return when (provider) {
            ApiProvider.DEEPSEEK -> sendMessageDeepSeek(messages, mode)
            ApiProvider.OLLAMA -> sendMessageOllama(messages, mode)
        }
    }
    
    suspend fun sendSimpleMessage(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): ChatResult<List<ChatMessage>> {
        val provider = appSettings.getApiProvider()
        
        return when (provider) {
            ApiProvider.DEEPSEEK -> sendSimpleMessageDeepSeek(messages, systemPrompt)
            ApiProvider.OLLAMA -> sendSimpleMessageOllama(messages, systemPrompt)
        }
    }
    
    private suspend fun sendMessageDeepSeek(
        messages: List<ChatMessage>,
        mode: AnswerMode
    ): ChatResult<List<ChatMessage>> {
        return try {
            val systemMessage = systemPromptProvider.getSystemMessage()
            val messagesWithSystem = prepareMessagesWithSystem(messages, systemMessage)
            val requestMessages = prepareRequestMessagesWithMode(messagesWithSystem, mode) { message ->
                MessageDto(
                    role = message.role.name.lowercase(),
                    content = message.content
                )
            }
            
            val temperature = appSettings.getTemperature().toDouble()
            val model = appSettings.getDeepSeekModel()
            
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
                val structuredResponse = parseStructuredResponse(assistantMessage.content)
                val metrics = createDeepSeekMetrics(response.usage, responseTime, model)
                
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
        } catch (e: Exception) {
            handleDeepSeekError(e)
        }
    }
    
    private suspend fun sendMessageOllama(
        messages: List<ChatMessage>,
        mode: AnswerMode
    ): ChatResult<List<ChatMessage>> {
        return try {
            val systemMessage = systemPromptProvider.getSystemMessage()
            val messagesWithSystem = prepareMessagesWithSystem(messages, systemMessage)
            val requestMessages = prepareRequestMessagesWithMode(messagesWithSystem, mode) { message ->
                OllamaMessageDto(
                    role = message.role.name.lowercase(),
                    content = message.content
                )
            }
            
            val temperature = appSettings.getTemperature().toDouble()
            val model = appSettings.getOllamaModel()
            
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
                val structuredResponse = parseStructuredResponse(assistantMessage.content)
                val metrics = createOllamaMetrics(response, responseTime, model)
                
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
        } catch (e: Exception) {
            handleOllamaError(e)
        }
    }
    
    private suspend fun sendSimpleMessageDeepSeek(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): ChatResult<List<ChatMessage>> {
        return try {
            val systemMessage = ChatMessage(
                role = MessageRole.SYSTEM,
                content = systemPrompt
            )
            val messagesWithSystem = prepareMessagesWithSystem(messages, systemMessage)
            val requestMessages = messagesWithSystem.map { message ->
                MessageDto(
                    role = message.role.name.lowercase(),
                    content = message.content
                )
            }
            
            val temperature = appSettings.getTemperature().toDouble()
            val model = appSettings.getDeepSeekModel()
            
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
                val metrics = createDeepSeekMetrics(response.usage, responseTime, model)
                
                val newMessages = messages + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = assistantMessage.content,
                    metrics = metrics
                )
                ChatResult.Success(newMessages)
            } else {
                ChatResult.Error("Пустой ответ от API")
            }
        } catch (e: Exception) {
            handleDeepSeekError(e)
        }
    }
    
    private suspend fun sendSimpleMessageOllama(
        messages: List<ChatMessage>,
        systemPrompt: String
    ): ChatResult<List<ChatMessage>> {
        return try {
            val systemMessage = ChatMessage(
                role = MessageRole.SYSTEM,
                content = systemPrompt
            )
            val messagesWithSystem = prepareMessagesWithSystem(messages, systemMessage)
            val requestMessages = messagesWithSystem.map { message ->
                OllamaMessageDto(
                    role = message.role.name.lowercase(),
                    content = message.content
                )
            }
            
            val temperature = appSettings.getTemperature().toDouble()
            val model = appSettings.getOllamaModel()
            
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
                val metrics = createOllamaMetrics(response, responseTime, model)
                
                val newMessages = messages + ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = assistantMessage.content,
                    metrics = metrics
                )
                ChatResult.Success(newMessages)
            } else {
                ChatResult.Error("Пустой ответ от Ollama API")
            }
        } catch (e: Exception) {
            handleOllamaError(e)
        }
    }
    
    private fun prepareMessagesWithSystem(
        messages: List<ChatMessage>,
        systemMessage: ChatMessage
    ): List<ChatMessage> {
        val messagesWithoutSystem = messages.filter { it.role != MessageRole.SYSTEM }
        return listOf(systemMessage) + messagesWithoutSystem
    }
    
    private inline fun <T> prepareRequestMessagesWithMode(
        messagesWithSystem: List<ChatMessage>,
        mode: AnswerMode?,
        transform: (ChatMessage) -> T
    ): List<T> {
        return if (mode != null) {
            val lastUserIndex = messagesWithSystem.indexOfLast { it.role == MessageRole.USER }
            messagesWithSystem.mapIndexed { index, message ->
                val contentForApi = if (index == lastUserIndex) {
                    addModeTagToUserMessage(message.content, mode)
                } else {
                    message.content
                }
                transform(message.copy(content = contentForApi))
            }
        } else {
            messagesWithSystem.map(transform)
        }
    }
    
    private fun parseStructuredResponse(content: String): StructuredResponse? {
        return try {
            val candidateJson = JsonExtractor.extractJsonObject(content) ?: content
            json.decodeFromString<StructuredResponse>(candidateJson)
        } catch (e: Exception) {
            Timber.d(e, "Failed to parse JSON response, using plain text")
            null
        }
    }
    
    private fun createDeepSeekMetrics(
        usage: UsageDto?,
        responseTime: Long,
        model: String
    ): MessageMetrics? {
        return if (usage != null) {
            MessageMetrics(
                responseTimeMs = responseTime,
                promptTokens = usage.promptTokens,
                completionTokens = usage.completionTokens,
                totalTokens = usage.totalTokens,
                costUSD = CostCalculator.calculateCost(
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
    }
    
    private fun createOllamaMetrics(
        response: OllamaResponseDto,
        responseTime: Long,
        model: String
    ): MessageMetrics {
        return MessageMetrics(
            responseTimeMs = responseTime,
            promptTokens = response.promptEvalCount ?: 0,
            completionTokens = response.evalCount ?: 0,
            totalTokens = (response.promptEvalCount ?: 0) + (response.evalCount ?: 0),
            costUSD = 0.0,
            modelName = model,
            providerName = "Ollama"
        )
    }
    
    private fun handleDeepSeekError(e: Exception): ChatResult<List<ChatMessage>> {
        return when (e) {
            is IllegalStateException -> {
                Timber.e(e, "Configuration error")
                ChatResult.Error(e.message ?: "Ошибка конфигурации")
            }
            is UnknownHostException -> {
                Timber.e(e, "Network error: Unable to resolve host")
                ChatResult.Error("Ошибка подключения. Проверьте интернет-соединение")
            }
            is IOException -> {
                Timber.e(e, "Network error: IO exception")
                ChatResult.Error("Ошибка сети. Проверьте подключение к интернету")
            }
            is HttpException -> {
                Timber.e(e, "HTTP error: ${e.code()}")
                val errorMessage = when (e.code()) {
                    401 -> "Неверный API ключ. Проверьте настройки"
                    429 -> "Превышен лимит запросов. Попробуйте позже"
                    500, 502, 503 -> "Ошибка сервера. Попробуйте позже"
                    else -> "Ошибка сервера: ${e.code()}"
                }
                ChatResult.Error(errorMessage)
            }
            else -> {
                Timber.e(e, "Unexpected error sending message")
                ChatResult.Error("Произошла ошибка: ${e.message ?: "Неизвестная ошибка"}")
            }
        }
    }
    
    private fun handleOllamaError(e: Exception): ChatResult<List<ChatMessage>> {
        return when (e) {
            is IllegalStateException -> {
                Timber.e(e, "Configuration error")
                ChatResult.Error(e.message ?: "Ошибка конфигурации")
            }
            is UnknownHostException -> {
                Timber.e(e, "Network error: Unable to resolve host")
                ChatResult.Error("Ошибка подключения к Ollama. Убедитесь, что Ollama запущена")
            }
            is IOException -> {
                Timber.e(e, "Network error: IO exception")
                ChatResult.Error("Ошибка подключения к Ollama. Проверьте настройки")
            }
            is HttpException -> {
                Timber.e(e, "HTTP error: ${e.code()}")
                val errorMessage = when (e.code()) {
                    404 -> "Модель ${appSettings.getOllamaModel()} не найдена. Загрузите её командой: ollama pull ${appSettings.getOllamaModel()}"
                    500, 502, 503 -> "Ошибка Ollama сервера. Попробуйте позже"
                    else -> "Ошибка Ollama: ${e.code()}"
                }
                ChatResult.Error(errorMessage)
            }
            else -> {
                Timber.e(e, "Unexpected error sending message to Ollama")
                ChatResult.Error("Произошла ошибка Ollama: ${e.message ?: "Неизвестная ошибка"}")
            }
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
}
