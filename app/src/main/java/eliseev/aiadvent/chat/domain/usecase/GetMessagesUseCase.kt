package eliseev.aiadvent.chat.domain.usecase

import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.repository.ChatRepository
import kotlinx.coroutines.flow.StateFlow

class GetMessagesUseCase(
    private val chatRepository: ChatRepository
) {
    fun execute(): StateFlow<List<ChatMessage>> {
        return chatRepository.messages
    }
}
