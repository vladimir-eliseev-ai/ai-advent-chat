package eliseev.aiadvent.chat.presentation.simplechat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.domain.model.ChatResult
import eliseev.aiadvent.chat.domain.model.UserSettings
import eliseev.aiadvent.chat.domain.usecase.GetMessagesUseCase
import eliseev.aiadvent.chat.domain.usecase.GetUserSettingsUseCase
import eliseev.aiadvent.chat.domain.usecase.SaveUserSettingsUseCase
import eliseev.aiadvent.chat.domain.usecase.SendSimpleMessageUseCase
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
    val inputText: String = "",
    val settings: UserSettings = UserSettings()
)

class SimpleChatViewModel(
    private val sendSimpleMessageUseCase: SendSimpleMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val saveUserSettingsUseCase: SaveUserSettingsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _inputText = MutableStateFlow("")
    private val _settings = MutableStateFlow(getUserSettingsUseCase.executeForSimple())

    val uiState: StateFlow<SimpleChatUiState> = combine(
        getMessagesUseCase.execute().map { messages ->
            ChatMessageMapper.toUiMessages(messages)
        },
        _isLoading,
        _errorMessage,
        _inputText,
        _settings
    ) { messages, isLoading, errorMessage, inputText, settings ->
        SimpleChatUiState(
            messages = messages,
            isLoading = isLoading,
            errorMessage = errorMessage,
            inputText = inputText,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SimpleChatUiState()
    )

    fun onUiEvent(event: SimpleChatUiEvent) {
        when (event) {
            is SimpleChatUiEvent.UpdateInputText -> onInputTextUpdated(event.text)
            is SimpleChatUiEvent.SendMessage -> onSendMessage()
            is SimpleChatUiEvent.DismissError -> onErrorDismissed()
            is SimpleChatUiEvent.UpdateSettings -> onSettingsUpdated(event.settings)
            is SimpleChatUiEvent.UpdateUserPrompt -> onUserPromptUpdated(event.prompt)
            is SimpleChatUiEvent.UpdateTemperature -> onTemperatureUpdated(event.temperature)
            is SimpleChatUiEvent.UpdateApiSettings -> onApiSettingsUpdated(event.provider, event.ollamaModel, event.deepSeekModel)
            is SimpleChatUiEvent.UpdateHistoryCompression -> onHistoryCompressionUpdated(event.enabled)
            is SimpleChatUiEvent.QuickSwitchModel -> onModelQuickSwitched(event.modelName)
        }
    }

    private fun onInputTextUpdated(text: String) {
        _inputText.value = text
    }

    private fun onSendMessage() {
        val messageText = _inputText.value.trim()
        if (messageText.isBlank() || _isLoading.value) {
            return
        }

        clearInput()
        setLoadingState(true)

        viewModelScope.launch {
            val result = sendSimpleMessageUseCase.execute(
                userMessage = messageText
            )

            when (result) {
                is ChatResult.Success -> {
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
    }

    private fun onErrorDismissed() {
        _errorMessage.value = null
    }

    private fun onSettingsUpdated(settings: UserSettings) {
        saveUserSettingsUseCase.executeForSimple(settings)
        _settings.value = getUserSettingsUseCase.executeForSimple()
    }

    private fun onUserPromptUpdated(prompt: String) {
        saveUserSettingsUseCase.updateUserPromptForSimple(prompt)
        _settings.value = getUserSettingsUseCase.executeForSimple()
    }

    private fun onTemperatureUpdated(temperature: Float) {
        saveUserSettingsUseCase.updateTemperature(temperature)
        _settings.value = getUserSettingsUseCase.executeForSimple()
    }

    private fun onApiSettingsUpdated(provider: ApiProvider, ollamaModel: String, deepSeekModel: String) {
        saveUserSettingsUseCase.updateApiSettings(provider, ollamaModel, deepSeekModel)
        _settings.value = getUserSettingsUseCase.executeForSimple()
    }

    private fun onHistoryCompressionUpdated(enabled: Boolean) {
        saveUserSettingsUseCase.updateHistoryCompression(enabled)
        _settings.value = getUserSettingsUseCase.executeForSimple()
    }

    private fun onModelQuickSwitched(modelName: String) {
        val currentProvider = _settings.value.apiProvider
        saveUserSettingsUseCase.quickSwitchModel(modelName, currentProvider)
        _settings.value = getUserSettingsUseCase.executeForSimple()
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
}
