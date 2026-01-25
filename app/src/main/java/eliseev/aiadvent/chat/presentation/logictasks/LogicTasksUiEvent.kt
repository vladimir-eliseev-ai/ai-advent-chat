package eliseev.aiadvent.chat.presentation.logictasks

import eliseev.aiadvent.chat.data.model.AnswerMode
import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.domain.model.UserSettings

sealed class LogicTasksUiEvent {
    data class UpdateInputText(val text: String) : LogicTasksUiEvent()
    data class UpdateAnswerMode(val mode: AnswerMode) : LogicTasksUiEvent()
    object SendMessage : LogicTasksUiEvent()
    object DismissError : LogicTasksUiEvent()
    data class UpdateSettings(val settings: UserSettings) : LogicTasksUiEvent()
    data class UpdateUserPrompt(val prompt: String) : LogicTasksUiEvent()
    data class UpdateTemperature(val temperature: Float) : LogicTasksUiEvent()
    data class UpdateApiSettings(
        val provider: ApiProvider,
        val ollamaModel: String,
        val deepSeekModel: String
    ) : LogicTasksUiEvent()
    data class UpdateHistoryCompression(val enabled: Boolean) : LogicTasksUiEvent()
    data class QuickSwitchModel(val modelName: String) : LogicTasksUiEvent()
}
