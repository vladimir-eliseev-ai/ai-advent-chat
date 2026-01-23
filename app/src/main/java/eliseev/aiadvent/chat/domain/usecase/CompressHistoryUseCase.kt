package eliseev.aiadvent.chat.domain.usecase

import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.repository.ChatRepository
import eliseev.aiadvent.chat.domain.model.ChatResult

class CompressHistoryUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        messagesToCompress: List<ChatMessage>
    ): ChatResult<ChatMessage> {
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
        val result = repository.sendSimpleMessage(
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
