package eliseev.aiadvent.chat.data.store

import eliseev.aiadvent.chat.data.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatMessageStore {
    
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    fun addMessage(message: ChatMessage) {
        _messages.update { it + message }
    }
    
    fun addMessages(newMessages: List<ChatMessage>) {
        _messages.update { it + newMessages }
    }
    
    fun updateMessages(messages: List<ChatMessage>) {
        _messages.value = messages
    }
    
    fun clear() {
        _messages.value = emptyList()
    }
    
    fun getMessages(): List<ChatMessage> {
        return _messages.value
    }
}
