package eliseev.aiadvent.chat.presentation.logictasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.domain.model.ChatResult
import eliseev.aiadvent.chat.domain.model.UserSettings
import eliseev.aiadvent.chat.domain.usecase.GetMessagesUseCase
import eliseev.aiadvent.chat.domain.usecase.GetUserSettingsUseCase
import eliseev.aiadvent.chat.domain.usecase.SaveUserSettingsUseCase
import eliseev.aiadvent.chat.domain.usecase.SendMessageUseCase
import eliseev.aiadvent.chat.presentation.chat.mapper.ChatMessageMapper
import eliseev.aiadvent.chat.presentation.chat.model.UiMessage
import eliseev.aiadvent.chat.data.utils.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LogicTasksUiState(
    val messages: List<UiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val inputText: String = "",
    val selectedMode: AnswerMode = AnswerMode.BRIEF,
    val settings: UserSettings = UserSettings()
)

class LogicTasksViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val saveUserSettingsUseCase: SaveUserSettingsUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _inputText = MutableStateFlow("")
    private val _selectedMode = MutableStateFlow(AnswerMode.BRIEF)
    private val _settings = MutableStateFlow(getUserSettingsUseCase.executeForLogic())

    val uiState: StateFlow<LogicTasksUiState> = combine(
        getMessagesUseCase.execute().map { messages ->
            ChatMessageMapper.toUiMessages(messages)
        },
        _isLoading,
        _errorMessage,
        _inputText,
        _selectedMode,
        _settings
    ) { messages, isLoading, errorMessage, inputText, selectedMode, settings ->
        LogicTasksUiState(
            messages = messages,
            isLoading = isLoading,
            errorMessage = errorMessage,
            inputText = inputText,
            selectedMode = selectedMode,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LogicTasksUiState()
    )

    fun onUiEvent(event: LogicTasksUiEvent) {
        when (event) {
            is LogicTasksUiEvent.UpdateInputText -> onInputTextUpdated(event.text)
            is LogicTasksUiEvent.UpdateAnswerMode -> onAnswerModeUpdated(event.mode)
            is LogicTasksUiEvent.SendMessage -> onSendMessage()
            is LogicTasksUiEvent.DismissError -> onErrorDismissed()
            is LogicTasksUiEvent.UpdateSettings -> onSettingsUpdated(event.settings)
            is LogicTasksUiEvent.UpdateUserPrompt -> onUserPromptUpdated(event.prompt)
            is LogicTasksUiEvent.UpdateTemperature -> onTemperatureUpdated(event.temperature)
            is LogicTasksUiEvent.UpdateApiSettings -> onApiSettingsUpdated(event.provider, event.ollamaModel, event.deepSeekModel)
            is LogicTasksUiEvent.UpdateHistoryCompression -> onHistoryCompressionUpdated(event.enabled)
            is LogicTasksUiEvent.QuickSwitchModel -> onModelQuickSwitched(event.modelName)
        }
    }

    private fun onInputTextUpdated(text: String) {
        _inputText.value = text
    }

    private fun onAnswerModeUpdated(mode: AnswerMode) {
        _selectedMode.value = mode
    }

    private fun onSendMessage() {
        val messageText = _inputText.value.trim()
        if (messageText.isBlank() || _isLoading.value) {
            return
        }

        clearInput()
        setLoadingState(true)

        viewModelScope.launch {
            val result = sendMessageUseCase.execute(
                userMessage = messageText,
                mode = _selectedMode.value
            )
            handleSendMessageResult(result)
        }
    }

    private fun onErrorDismissed() {
        _errorMessage.value = null
    }

    private fun onSettingsUpdated(settings: UserSettings) {
        saveUserSettingsUseCase.executeForLogic(settings)
        _settings.value = getUserSettingsUseCase.executeForLogic()
    }

    private fun onUserPromptUpdated(prompt: String) {
        saveUserSettingsUseCase.updateUserPromptForLogic(prompt)
        _settings.value = getUserSettingsUseCase.executeForLogic()
    }

    private fun onTemperatureUpdated(temperature: Float) {
        saveUserSettingsUseCase.updateTemperature(temperature)
        _settings.value = getUserSettingsUseCase.executeForLogic()
    }

    private fun onApiSettingsUpdated(provider: ApiProvider, ollamaModel: String, deepSeekModel: String) {
        saveUserSettingsUseCase.updateApiSettings(provider, ollamaModel, deepSeekModel)
        _settings.value = getUserSettingsUseCase.executeForLogic()
    }

    private fun onHistoryCompressionUpdated(enabled: Boolean) {
        saveUserSettingsUseCase.updateHistoryCompression(enabled)
        _settings.value = getUserSettingsUseCase.executeForLogic()
    }

    private fun onModelQuickSwitched(modelName: String) {
        val currentProvider = _settings.value.apiProvider
        saveUserSettingsUseCase.quickSwitchModel(modelName, currentProvider)
        _settings.value = getUserSettingsUseCase.executeForLogic()
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

    private fun handleSendMessageResult(result: ChatResult<Unit>) {
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
