package eliseev.aiadvent.chat.data.store

import eliseev.aiadvent.chat.data.model.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

class ChatMessageStore(
    private val persistentStore: PersistentChatMessageStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    val messages: StateFlow<List<ChatMessage>> = persistentStore.getMessagesFlow()
        .stateIn(
            scope = scope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    init {
        // Предзагрузка для быстрого доступа
        scope.launch {
            val loadedMessages = persistentStore.loadMessages()
            Timber.d("Preloaded ${loadedMessages.size} messages from Room")
        }
    }
    
    suspend fun addMessage(message: ChatMessage) {
        // Всегда загружаем из базы для гарантии актуальности данных
        val currentMessages = persistentStore.loadMessages()
        val newMessages = currentMessages + message
        Timber.d("Adding message. Current count: ${currentMessages.size}, new count: ${newMessages.size}")
        persistentStore.saveMessages(newMessages)
    }
    
    suspend fun addMessages(newMessages: List<ChatMessage>) {
        // Всегда загружаем из базы для гарантии актуальности данных
        val currentMessages = persistentStore.loadMessages()
        persistentStore.saveMessages(currentMessages + newMessages)
    }
    
    suspend fun updateMessages(messages: List<ChatMessage>) {
        persistentStore.saveMessages(messages)
    }
    
    suspend fun clear() {
        persistentStore.clear()
    }
    
    suspend fun getMessages(): List<ChatMessage> {
        // Всегда загружаем из базы для гарантии актуальности данных
        return persistentStore.loadMessages()
    }
}
