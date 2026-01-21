package eliseev.aiadvent.chat.presentation.simplechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageRole
import eliseev.aiadvent.chat.data.model.SystemPromptProvider
import eliseev.aiadvent.chat.data.repository.ChatRepository
import eliseev.aiadvent.chat.data.store.ChatMessageStore
import eliseev.aiadvent.chat.domain.model.ChatResult
import eliseev.aiadvent.chat.presentation.chat.mapper.ChatMessageMapper
import eliseev.aiadvent.chat.presentation.chat.model.UiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SimpleChatUiState(
    val messages: List<UiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val inputText: String = ""
)

class SimpleChatViewModel(
    private val repository: ChatRepository,
    private val messageStore: ChatMessageStore,
    private val systemPromptProvider: SystemPromptProvider
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _inputText = MutableStateFlow("")

    val uiState: StateFlow<SimpleChatUiState> = combine(
        messageStore.messages.map { messages ->
            ChatMessageMapper.toUiMessages(messages)
        },
        _isLoading,
        _errorMessage,
        _inputText
    ) { messages, isLoading, errorMessage, inputText ->
        SimpleChatUiState(
            messages = messages,
            isLoading = isLoading,
            errorMessage = errorMessage,
            inputText = inputText
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SimpleChatUiState()
    )

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val messageText = _inputText.value.trim()
        if (messageText.isBlank() || _isLoading.value) {
            return
        }

        addUserMessage(messageText)
        clearInput()
        setLoadingState(true)

        viewModelScope.launch {
            val result = repository.sendSimpleMessage(
                messages = messageStore.getMessages(),
                systemPrompt = systemPromptProvider.getSimpleChatPrompt()
            )
            handleSendMessageResult(result)
        }
    }

    private fun addUserMessage(text: String) {
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text
        )
        messageStore.addMessage(userMessage)
    }

    private fun clearInput() {
        _inputText.value = ""
    }

    private fun setLoadingState(isLoading: Boolean) {
        _isLoading.value = isLoading
        if (isLoading) {
            _errorMessage.value = null
        }
    }

    private fun handleSendMessageResult(result: ChatResult<List<ChatMessage>>) {
        when (result) {
            is ChatResult.Success -> {
                messageStore.updateMessages(result.data)
                setLoadingState(false)
            }

            is ChatResult.Error -> {
                setLoadingState(false)
                _errorMessage.value = result.message
            }

            is ChatResult.Loading -> {
                // Loading state already set
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun getUserPrompt(): String {
        return systemPromptProvider.getUserPromptSimple()
    }

    fun saveUserPrompt(prompt: String) {
        systemPromptProvider.setUserPromptSimple(prompt)
    }
    
    fun getTemperature(): Float {
        return systemPromptProvider.getTemperature()
    }
    
    fun saveTemperature(temperature: Float) {
        systemPromptProvider.setTemperature(temperature)
    }
    
    fun getApiProvider(): ApiProvider {
        return systemPromptProvider.getApiProvider()
    }
    
    fun getOllamaModel(): String {
        return systemPromptProvider.getOllamaModel()
    }
    
    fun getDeepSeekModel(): String {
        return systemPromptProvider.getDeepSeekModel()
    }
    
    fun saveApiSettings(provider: ApiProvider, ollamaModel: String, deepSeekModel: String) {
        systemPromptProvider.setApiProvider(provider)
        systemPromptProvider.setOllamaModel(ollamaModel)
        systemPromptProvider.setDeepSeekModel(deepSeekModel)
    }
    
    fun getCurrentModel(): String {
        return when (systemPromptProvider.getApiProvider()) {
            ApiProvider.DEEPSEEK -> systemPromptProvider.getDeepSeekModel()
            ApiProvider.OLLAMA -> systemPromptProvider.getOllamaModel()
        }
    }
    
    fun quickSwitchModel(modelName: String) {
        val provider = systemPromptProvider.getApiProvider()
        when (provider) {
            ApiProvider.OLLAMA -> systemPromptProvider.setOllamaModel(modelName)
            ApiProvider.DEEPSEEK -> systemPromptProvider.setDeepSeekModel(modelName)
        }
    }
}
