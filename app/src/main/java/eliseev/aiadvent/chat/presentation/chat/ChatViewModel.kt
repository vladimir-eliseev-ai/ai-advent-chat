package eliseev.aiadvent.chat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.domain.model.ChatResult
import eliseev.aiadvent.chat.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val inputText: String = ""
)

class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    role = MessageRole.SYSTEM,
                    content = "You are a helpful assistant."
                )
            )
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInputText(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val currentText = _uiState.value.inputText
        if (currentText.isBlank() || _uiState.value.isLoading) {
            return
        }

        val currentMessages = _uiState.value.messages
        
        // Сразу добавляем сообщение пользователя в список
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = currentText.trim()
        )
        val messagesWithUser = currentMessages + userMessage
        
        _uiState.update { 
            it.copy(
                messages = messagesWithUser,
                inputText = "",
                isLoading = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            when (val result = sendMessageUseCase(currentMessages, currentText)) {
                is ChatResult.Success -> {
                    _uiState.update {
                        it.copy(
                            messages = result.data,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is ChatResult.Error -> {
                    // При ошибке оставляем сообщение пользователя, но показываем ошибку
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is ChatResult.Loading -> {
                    // Already set loading state
                }
            }
        }
    }

    fun clearChat() {
        _uiState.update { 
            ChatUiState(
                messages = listOf(
                    ChatMessage(
                        role = MessageRole.SYSTEM,
                        content = "You are a helpful assistant."
                    )
                )
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

