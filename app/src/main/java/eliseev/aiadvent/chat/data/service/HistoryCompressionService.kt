package eliseev.aiadvent.chat.data.service

import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.sender.MessageSender
import eliseev.aiadvent.chat.domain.model.ChatResult
import timber.log.Timber

/**
 * Сервис для сжатия истории диалога.
 * Техническая деталь реализации, находится в data слое.
 */
class HistoryCompressionService(
    private val messageSender: MessageSender
) {
    companion object {
        private const val COMPRESSION_THRESHOLD = 6 // Сжимать каждые 6 сообщения
    }

    suspend fun compressIfNeeded(
        messages: List<ChatMessage>,
        isCompressionEnabled: Boolean
    ): List<ChatMessage> {
        if (!isCompressionEnabled) {
            Timber.d("Compression disabled, skipping")
            return messages
        }
        
        Timber.d("compressHistoryIfNeeded: compression enabled=$isCompressionEnabled, messages count=${messages.size}")
        
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
        val result = messageSender.sendSimpleMessage(
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
}
