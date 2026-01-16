package eliseev.aiadvent.chat.domain.usecase

import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.repository.ChatRepository
import eliseev.aiadvent.chat.domain.model.ChatResult

class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        currentMessages: List<ChatMessage>,
        userMessage: String,
        mode: AnswerMode = AnswerMode.BRIEF
    ): ChatResult<List<ChatMessage>> {
        if (userMessage.isBlank()) {
            return ChatResult.Error("Message cannot be empty")
        }
        
        val newUserMessage = ChatMessage(
            role = MessageRole.USER,
            content = userMessage.trim()
        )
        
        val messagesWithUser = currentMessages + newUserMessage
        
        return repository.sendMessage(messagesWithUser, mode)
    }
}

