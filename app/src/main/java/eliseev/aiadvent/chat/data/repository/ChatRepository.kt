package eliseev.aiadvent.chat.data.repository

import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.model.AppSettings
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.sender.MessageSender
import eliseev.aiadvent.chat.data.store.ChatMessageStore
import eliseev.aiadvent.chat.domain.model.ChatResult
import eliseev.aiadvent.chat.data.service.HistoryCompressionService
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Репозиторий для работы с сообщениями.
 * Управляет хранилищем сообщений и отправкой.
 */
class ChatRepository(
    private val messageSender: MessageSender,
    private val messageStore: ChatMessageStore,
    private val historyCompressionService: HistoryCompressionService,
    private val appSettings: AppSettings
) {
    /**
     * Flow сообщений для подписки в ViewModel
     */
    val messages: StateFlow<List<ChatMessage>> = messageStore.messages
    
    /**
     * Отправляет сообщение и обновляет хранилище
     */
    suspend fun sendMessage(
        userMessage: String,
        mode: AnswerMode
    ): ChatResult<Unit> {
        return sendMessageInternal(
            userMessage = userMessage,
            mode = mode,
            systemPrompt = null
        ) { messages, modeParam ->
            requireNotNull(modeParam) { "Mode must not be null for sendMessage" }
            messageSender.sendMessage(messages, modeParam)
        }
    }
    
    /**
     * Отправляет простое сообщение и обновляет хранилище
     */
    suspend fun sendSimpleMessage(
        userMessage: String,
        systemPrompt: String
    ): ChatResult<Unit> {
        return sendMessageInternal(
            userMessage = userMessage,
            mode = null,
            systemPrompt = systemPrompt
        ) { messages, _ ->
            messageSender.sendSimpleMessage(messages, systemPrompt)
        }
    }
    
    private suspend fun sendMessageInternal(
        userMessage: String,
        mode: AnswerMode?,
        systemPrompt: String?,
        sendAction: suspend (List<ChatMessage>, AnswerMode?) -> ChatResult<List<ChatMessage>>
    ): ChatResult<Unit> {
        if (userMessage.isBlank()) {
            return ChatResult.Error("Message cannot be empty")
        }
        
        // Получаем текущие сообщения перед добавлением
        var currentMessages = messageStore.getMessages()
        Timber.d("Before adding message, current messages count: ${currentMessages.size}")
        
        // Добавляем сообщение пользователя в хранилище
        val userMessageObj = ChatMessage(
            role = MessageRole.USER,
            content = userMessage.trim()
        )
        messageStore.addMessage(userMessageObj)
        
        // Получаем обновленные сообщения после добавления
        currentMessages = messageStore.getMessages()
        Timber.d("After adding message, current messages count: ${currentMessages.size}")
        
        // Сжимаем историю перед отправкой (если включено)
        val isCompressionEnabled = appSettings.isHistoryCompressionEnabled()
        val compressedMessages = historyCompressionService.compressIfNeeded(
            messages = currentMessages,
            isCompressionEnabled = isCompressionEnabled
        )
        
        // Обновляем хранилище сжатыми сообщениями (если было сжатие)
        if (compressedMessages !== currentMessages && compressedMessages.size != currentMessages.size) {
            messageStore.updateMessages(compressedMessages)
        }
        
        // Отправляем сообщение
        val messageType = if (systemPrompt != null) "simple" else "regular"
        Timber.d("Sending $messageType message with ${compressedMessages.size} messages (current: ${currentMessages.size})")
        val result = sendAction(compressedMessages, mode)
        
        return when (result) {
            is ChatResult.Success -> {
                Timber.d("$messageType message sent successfully, updating store with ${result.data.size} messages")
                messageStore.updateMessages(result.data)
                ChatResult.Success(Unit)
            }
            is ChatResult.Error -> {
                // Удаляем сообщение пользователя при ошибке
                val messagesWithoutUser = currentMessages.filter { 
                    !(it.role == MessageRole.USER && it.content == userMessageObj.content)
                }
                messageStore.updateMessages(messagesWithoutUser)
                result
            }
            is ChatResult.Loading -> result
        }
    }
    
    /**
     * Очищает хранилище сообщений
     */
    suspend fun clearMessages() {
        messageStore.clear()
    }
}

