package eliseev.aiadvent.chat.domain.usecase

import eliseev.aiadvent.chat.data.model.SystemPromptProvider
import eliseev.aiadvent.chat.data.repository.ChatRepository
import eliseev.aiadvent.chat.domain.model.ChatResult

class SendSimpleMessageUseCase(
    private val chatRepository: ChatRepository,
    private val systemPromptProvider: SystemPromptProvider
) {
    suspend fun execute(
        userMessage: String
    ): ChatResult<Unit> {
        val systemPrompt = systemPromptProvider.getSimpleChatPrompt()
        return chatRepository.sendSimpleMessage(userMessage, systemPrompt)
    }
}
