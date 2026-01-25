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
        
        // Находим последний summary (если есть) в оригинальном списке
        val lastSummaryIndex = messages.indexOfLast { it.isSummary }
        
        // Определяем диапазон сообщений после последнего summary (или все, если summary нет)
        val startSearchIndex = if (lastSummaryIndex >= 0) {
            lastSummaryIndex + 1
        } else {
            0
        }
        
        // Находим последнее сообщение пользователя в оригинальном списке (после summary, если есть)
        val lastUserMessageIndex = messages.indexOfLast { 
            it.role == MessageRole.USER && 
            messages.indexOf(it) >= startSearchIndex
        }
        
        if (lastUserMessageIndex < startSearchIndex) {
            Timber.d("No user messages found after summary, skipping compression")
            return messages
        }
        
        // Собираем индексы USER и ASSISTANT сообщений между startSearchIndex и lastUserMessageIndex
        val conversationMessageIndices = mutableListOf<Int>()
        for (i in startSearchIndex until lastUserMessageIndex) {
            if (messages[i].role != MessageRole.SYSTEM) {
                conversationMessageIndices.add(i)
            }
        }
        
        // Если сообщений для сжатия меньше порога, возвращаем как есть
        if (conversationMessageIndices.size < COMPRESSION_THRESHOLD) {
            Timber.d("Not enough messages for compression: ${conversationMessageIndices.size} < $COMPRESSION_THRESHOLD")
            return messages
        }
        
        Timber.d("Compression needed: ${conversationMessageIndices.size} messages before last user message, threshold=$COMPRESSION_THRESHOLD")
        
        // Берем последние COMPRESSION_THRESHOLD индексов для сжатия
        val indicesToCompress = conversationMessageIndices.takeLast(COMPRESSION_THRESHOLD)
        val startIndex = indicesToCompress.first()
        val endIndex = indicesToCompress.last() + 1
        
        // Получаем сообщения для сжатия по индексам
        val messagesToCompressFinal = indicesToCompress.map { messages[it] }
        
        // Создаем summary через API
        Timber.d("Compressing ${messagesToCompressFinal.size} messages into summary")
        val summaryResult = createSummary(messagesToCompressFinal)
        
        return when (summaryResult) {
            is ChatResult.Success -> {
                val summaryMessage = summaryResult.data
                Timber.d("Summary created successfully, replacing ${messagesToCompressFinal.size} messages")
                Timber.d("Summary message: isSummary=${summaryMessage.isSummary}, originalCount=${summaryMessage.originalMessageCount}, content=${summaryMessage.content.take(100)}...")
                
                // Создаем новый список, удаляя только сообщения по индексам из indicesToCompress
                // SYSTEM сообщения между ними сохраняем
                val resultMessages = mutableListOf<ChatMessage>()
                val indicesToCompressSet = indicesToCompress.toSet()
                var summaryAdded = false
                
                for (i in messages.indices) {
                    if (i in indicesToCompressSet) {
                        // Пропускаем сжимаемые сообщения, заменяем первое на summary
                        if (!summaryAdded) {
                            resultMessages.add(summaryMessage)
                            summaryAdded = true
                        }
                    } else {
                        // Все остальные сообщения (включая SYSTEM и последнее сообщение пользователя) сохраняем
                        resultMessages.add(messages[i])
                    }
                }
                
                Timber.d("Result messages count: ${resultMessages.size}, summary messages count: ${resultMessages.count { it.isSummary }}")
                Timber.d("Last message in result: role=${resultMessages.lastOrNull()?.role}, content=${resultMessages.lastOrNull()?.content?.take(50)}")
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
