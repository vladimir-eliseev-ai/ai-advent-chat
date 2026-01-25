package eliseev.aiadvent.chat.presentation.simplechat

import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.domain.model.UserSettings

sealed class SimpleChatUiEvent {
    data class UpdateInputText(val text: String) : SimpleChatUiEvent()
    object SendMessage : SimpleChatUiEvent()
    object DismissError : SimpleChatUiEvent()
    data class UpdateSettings(val settings: UserSettings) : SimpleChatUiEvent()
    data class UpdateUserPrompt(val prompt: String) : SimpleChatUiEvent()
    data class UpdateTemperature(val temperature: Float) : SimpleChatUiEvent()
    data class UpdateApiSettings(
        val provider: ApiProvider,
        val ollamaModel: String,
        val deepSeekModel: String
    ) : SimpleChatUiEvent()
    data class UpdateHistoryCompression(val enabled: Boolean) : SimpleChatUiEvent()
    data class QuickSwitchModel(val modelName: String) : SimpleChatUiEvent()
}
