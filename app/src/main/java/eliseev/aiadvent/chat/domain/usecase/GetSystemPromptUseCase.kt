package eliseev.aiadvent.chat.domain.usecase

import eliseev.aiadvent.chat.data.model.SystemPromptProvider

class GetSystemPromptUseCase(
    private val systemPromptProvider: SystemPromptProvider
) {
    fun executeForSimpleChat(): String {
        return systemPromptProvider.getSimpleChatPrompt()
    }
}
