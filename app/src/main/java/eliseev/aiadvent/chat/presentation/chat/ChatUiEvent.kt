package eliseev.aiadvent.chat.presentation.chat

import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.domain.model.UserSettings

sealed class ChatUiEvent {
    data class UpdateInputText(val text: String) : ChatUiEvent()
    data class UpdateAnswerMode(val mode: AnswerMode) : ChatUiEvent()
    object SendMessage : ChatUiEvent()
    object DismissError : ChatUiEvent()
    data class UpdateSettings(val settings: UserSettings) : ChatUiEvent()
    data class UpdateUserPrompt(val prompt: String) : ChatUiEvent()
    data class UpdateTemperature(val temperature: Float) : ChatUiEvent()
    data class UpdateApiSettings(
        val provider: ApiProvider,
        val ollamaModel: String,
        val deepSeekModel: String
    ) : ChatUiEvent()
    data class UpdateHistoryCompression(val enabled: Boolean) : ChatUiEvent()
    data class QuickSwitchModel(val modelName: String) : ChatUiEvent()
}
