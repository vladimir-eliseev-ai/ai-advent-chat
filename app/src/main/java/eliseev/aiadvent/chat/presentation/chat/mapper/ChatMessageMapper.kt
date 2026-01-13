package eliseev.aiadvent.chat.presentation.chat.mapper

import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.presentation.chat.model.UiMessage

object ChatMessageMapper {
    
    private const val QUESTION_SUMMARY_MAX_LENGTH = 100
    
    fun toUiMessage(message: ChatMessage): UiMessage {
        val structuredResponse = message.structuredResponse
        
        // Форматируем дату из timestamp для всех сообщений
        val formattedTimestamp = formatTimestamp(message.timestamp)
        
        return if (structuredResponse != null) {
            // Если есть дата в структурированном ответе, используем её
            val formattedDate = structuredResponse.date?.let { dateStr ->
                parseAndFormatDate(dateStr)
            } ?: formattedTimestamp
            
            UiMessage(
                role = message.role,
                content = message.content,
                timestamp = message.timestamp,
                formattedDate = formattedDate,
                body = structuredResponse.body,
                tags = structuredResponse.tags,
                urls = structuredResponse.urls
            )
        } else {
            UiMessage(
                role = message.role,
                content = message.content,
                timestamp = message.timestamp,
                formattedDate = formattedTimestamp
            )
        }
    }
    
    fun toUiMessages(messages: List<ChatMessage>): List<UiMessage> {
        return messages.mapIndexed { index, message ->
            val uiMessage = toUiMessage(message)
            
            // Если это сообщение ассистента, добавляем краткое содержание предыдущего вопроса
            if (message.role == MessageRole.ASSISTANT && index > 0) {
                val previousMessage = messages[index - 1]
                if (previousMessage.role == MessageRole.USER) {
                    val summary = createQuestionSummary(previousMessage.content)
                    uiMessage.copy(userQuestionSummary = summary)
                } else {
                    uiMessage
                }
            } else {
                uiMessage
            }
        }
    }
    
    private fun createQuestionSummary(question: String): String {
        return if (question.length > QUESTION_SUMMARY_MAX_LENGTH) {
            question.take(QUESTION_SUMMARY_MAX_LENGTH) + "..."
        } else {
            question
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val date = java.util.Date(timestamp)
            val outputFormat = DateFormatHelper.createOutputFormat()
            outputFormat.format(date)
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun parseAndFormatDate(dateStr: String): String? {
        return try {
            // Парсим ISO 8601 формат
            val isoFormat = DateFormatHelper.createIso8601Format()
            val date = isoFormat.parse(dateStr) ?: 
                DateFormatHelper.createIso8601FormatWithoutMillis().parse(dateStr)
            
            date?.let {
                val outputFormat = DateFormatHelper.createOutputFormat()
                outputFormat.format(it)
            }
        } catch (e: Exception) {
            // Если не удалось распарсить дату, возвращаем null
            null
        }
    }
}
