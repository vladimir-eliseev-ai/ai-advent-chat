package eliseev.aiadvent.chat.presentation.logictasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eliseev.aiadvent.chat.data.model.AnswerMode
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LogicTasksUiState(
    val messages: List<UiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val inputText: String = "",
    val selectedMode: AnswerMode = AnswerMode.BRIEF
)

class LogicTasksViewModel(
    private val repository: ChatRepository,
    private val messageStore: ChatMessageStore,
    private val systemPromptProvider: SystemPromptProvider
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _inputText = MutableStateFlow("")
    private val _selectedMode = MutableStateFlow(AnswerMode.BRIEF)

    val uiState: StateFlow<LogicTasksUiState> = combine(
        messageStore.messages.map { messages ->
            ChatMessageMapper.toUiMessages(messages)
        },
        _isLoading,
        _errorMessage,
        _inputText,
        _selectedMode
    ) { messages, isLoading, errorMessage, inputText, selectedMode ->
        LogicTasksUiState(
            messages = messages,
            isLoading = isLoading,
            errorMessage = errorMessage,
            inputText = inputText,
            selectedMode = selectedMode
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LogicTasksUiState()
    )

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun updateAnswerMode(mode: AnswerMode) {
        _selectedMode.value = mode
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
            val result = repository.sendMessage(
                messages = messageStore.getMessages(),
                mode = _selectedMode.value
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
        return systemPromptProvider.getUserPromptLogic()
    }
    
    fun saveUserPrompt(prompt: String) {
        systemPromptProvider.setUserPromptLogic(prompt)
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
    
    fun isHistoryCompressionEnabled(): Boolean {
        return systemPromptProvider.isHistoryCompressionEnabled()
    }
    
    fun setHistoryCompressionEnabled(enabled: Boolean) {
        systemPromptProvider.setHistoryCompressionEnabled(enabled)
    }
}
