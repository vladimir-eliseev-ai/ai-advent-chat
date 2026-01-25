package eliseev.aiadvent.chat.domain.model

import eliseev.aiadvent.chat.data.model.ApiProvider

data class UserSettings(
    val userPrompt: String = "",
    val temperature: Float = 0.7f,
    val apiProvider: ApiProvider = ApiProvider.DEEPSEEK,
    val ollamaModel: String = "llama3.2:1b",
    val deepSeekModel: String = "deepseek-chat",
    val isHistoryCompressionEnabled: Boolean = true
) {
    val currentModel: String
        get() = when (apiProvider) {
            ApiProvider.DEEPSEEK -> deepSeekModel
            ApiProvider.OLLAMA -> ollamaModel
        }
}
