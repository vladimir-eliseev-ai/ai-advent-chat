package eliseev.aiadvent.chat.domain.usecase

import eliseev.aiadvent.chat.data.model.ApiProvider
import eliseev.aiadvent.chat.data.model.AppSettings
import eliseev.aiadvent.chat.data.model.SystemPromptProvider
import eliseev.aiadvent.chat.domain.model.UserSettings

enum class ChatType {
    LOGIC,
    SIMPLE
}

class SaveUserSettingsUseCase(
    private val systemPromptProvider: SystemPromptProvider,
    private val appSettings: AppSettings
) {
    fun executeForLogic(settings: UserSettings) {
        execute(settings, ChatType.LOGIC)
    }
    
    fun executeForSimple(settings: UserSettings) {
        execute(settings, ChatType.SIMPLE)
    }
    
    private fun execute(settings: UserSettings, chatType: ChatType) {
        when (chatType) {
            ChatType.LOGIC -> systemPromptProvider.setUserPromptLogic(settings.userPrompt)
            ChatType.SIMPLE -> systemPromptProvider.setUserPromptSimple(settings.userPrompt)
        }
        updateCommonSettings(settings)
    }
    
    private fun updateCommonSettings(settings: UserSettings) {
        appSettings.setTemperature(settings.temperature)
        appSettings.setApiProvider(settings.apiProvider)
        appSettings.setOllamaModel(settings.ollamaModel)
        appSettings.setDeepSeekModel(settings.deepSeekModel)
        appSettings.setHistoryCompressionEnabled(settings.isHistoryCompressionEnabled)
    }
    
    fun updateTemperature(temperature: Float) {
        appSettings.setTemperature(temperature)
    }
    
    fun updateApiSettings(provider: ApiProvider, ollamaModel: String, deepSeekModel: String) {
        appSettings.setApiProvider(provider)
        appSettings.setOllamaModel(ollamaModel)
        appSettings.setDeepSeekModel(deepSeekModel)
    }
    
    fun updateHistoryCompression(enabled: Boolean) {
        appSettings.setHistoryCompressionEnabled(enabled)
    }
    
    fun updateUserPromptForLogic(prompt: String) {
        systemPromptProvider.setUserPromptLogic(prompt)
    }
    
    fun updateUserPromptForSimple(prompt: String) {
        systemPromptProvider.setUserPromptSimple(prompt)
    }
    
    fun quickSwitchModel(modelName: String, currentProvider: ApiProvider) {
        when (currentProvider) {
            ApiProvider.OLLAMA -> appSettings.setOllamaModel(modelName)
            ApiProvider.DEEPSEEK -> appSettings.setDeepSeekModel(modelName)
        }
    }
}
