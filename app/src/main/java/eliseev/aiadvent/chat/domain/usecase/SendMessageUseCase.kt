package eliseev.aiadvent.chat.domain.usecase

import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.repository.ChatRepository
import eliseev.aiadvent.chat.domain.model.ChatResult

class SendMessageUseCase(
    private val chatRepository: ChatRepository
) {
    suspend fun execute(
        userMessage: String,
        mode: AnswerMode = AnswerMode.BRIEF
    ): ChatResult<Unit> {
        return chatRepository.sendMessage(userMessage, mode)
    }
}

