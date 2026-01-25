package eliseev.aiadvent.chat.domain.usecase

import eliseev.aiadvent.chat.data.model.AppSettings
import eliseev.aiadvent.chat.data.model.SystemPromptProvider
import eliseev.aiadvent.chat.domain.model.UserSettings

class GetUserSettingsUseCase(
    private val systemPromptProvider: SystemPromptProvider,
    private val appSettings: AppSettings
) {
    fun executeForLogic(): UserSettings {
        return UserSettings(
            userPrompt = systemPromptProvider.getUserPromptLogic(),
            temperature = appSettings.getTemperature(),
            apiProvider = appSettings.getApiProvider(),
            ollamaModel = appSettings.getOllamaModel(),
            deepSeekModel = appSettings.getDeepSeekModel(),
            isHistoryCompressionEnabled = appSettings.isHistoryCompressionEnabled()
        )
    }
    
    fun executeForSimple(): UserSettings {
        return UserSettings(
            userPrompt = systemPromptProvider.getUserPromptSimple(),
            temperature = appSettings.getTemperature(),
            apiProvider = appSettings.getApiProvider(),
            ollamaModel = appSettings.getOllamaModel(),
            deepSeekModel = appSettings.getDeepSeekModel(),
            isHistoryCompressionEnabled = appSettings.isHistoryCompressionEnabled()
        )
    }
}
