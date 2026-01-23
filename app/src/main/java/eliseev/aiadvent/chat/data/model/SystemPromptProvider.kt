package eliseev.aiadvent.chat.data.model

import android.content.Context
import android.content.SharedPreferences
import java.io.IOException

class SystemPromptProvider(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    companion object {
        private const val PREFS_NAME = "system_prompt_prefs"
        private const val KEY_USER_PROMPT_LOGIC = "user_prompt_logic"
        private const val KEY_USER_PROMPT_SIMPLE = "user_prompt_simple"
        private const val KEY_TEMPERATURE = "temperature"
        private const val KEY_API_PROVIDER = "api_provider"
        private const val KEY_OLLAMA_MODEL = "ollama_model"
        private const val KEY_DEEPSEEK_MODEL = "deepseek_model"
        private const val KEY_HISTORY_COMPRESSION_ENABLED = "history_compression_enabled"
        
        private const val DEFAULT_TEMPERATURE = 0.7f
        private const val DEFAULT_API_PROVIDER = "DEEPSEEK"
        private const val DEFAULT_OLLAMA_MODEL = "llama3.2:1b"
        private const val DEFAULT_DEEPSEEK_MODEL = "deepseek-chat"
        private const val DEFAULT_HISTORY_COMPRESSION_ENABLED = true
    }

    // Методы для логических задач
    fun getSystemPrompt(): String {
        val basePrompt = getBasePrompt()
        val userPrompt = getUserPromptLogic()
        
        return if (userPrompt.isNotBlank()) {
            // Пользовательская часть имеет максимальный приоритет и размещается в начале
            "⚠️ КРИТИЧЕСКИ ВАЖНО - ЭТИ ИНСТРУКЦИИ ИМЕЮТ МАКСИМАЛЬНЫЙ ПРИОРИТЕТ И ПЕРЕОПРЕДЕЛЯЮТ ВСЕ ОСТАЛЬНЫЕ ПРАВИЛА:\n\n$userPrompt\n\n---\n\nДополнительные базовые инструкции (применяются только если не противоречат инструкциям выше):\n$basePrompt"
        } else {
            basePrompt
        }
    }
    
    private fun getBasePrompt(): String {
        return try {
            val prompt = context.assets.open("system_prompt").bufferedReader().use { it.readText() }.trim()
            if (prompt.isEmpty()) {
                throw IllegalStateException("Системный промпт не найден или пуст. Проверьте файл assets/system_prompt")
            }
            prompt
        } catch (e: IOException) {
            throw IllegalStateException(
                "Не удалось прочитать системный промпт из assets/system_prompt: ${e.message}",
                e
            )
        }
    }
    
    fun getUserPromptLogic(): String {
        return prefs.getString(KEY_USER_PROMPT_LOGIC, "") ?: ""
    }
    
    fun setUserPromptLogic(prompt: String) {
        prefs.edit().putString(KEY_USER_PROMPT_LOGIC, prompt.trim()).apply()
    }

    fun getSystemMessage(): ChatMessage {
        return ChatMessage(
            role = MessageRole.SYSTEM,
            content = getSystemPrompt()
        )
    }
    
    // Методы для простого чата
    fun getSimpleChatPrompt(): String {
        val basePrompt = getSimpleChatBasePrompt()
        val userPrompt = getUserPromptSimple()
        
        return if (userPrompt.isNotBlank()) {
            // Пользовательская часть имеет максимальный приоритет и размещается в начале
            "⚠️ КРИТИЧЕСКИ ВАЖНО - ЭТИ ИНСТРУКЦИИ ИМЕЮТ МАКСИМАЛЬНЫЙ ПРИОРИТЕТ И ПЕРЕОПРЕДЕЛЯЮТ ВСЕ ОСТАЛЬНЫЕ ПРАВИЛА:\n\n$userPrompt\n\n---\n\nДополнительные базовые инструкции (применяются только если не противоречат инструкциям выше):\n$basePrompt"
        } else {
            basePrompt
        }
    }
    
    private fun getSimpleChatBasePrompt(): String {
        return try {
            val prompt = context.assets.open("simple_chat_prompt").bufferedReader().use { it.readText() }.trim()
            if (prompt.isEmpty()) {
                throw IllegalStateException("Системный промпт для простого чата не найден или пуст. Проверьте файл assets/simple_chat_prompt")
            }
            prompt
        } catch (e: IOException) {
            throw IllegalStateException(
                "Не удалось прочитать системный промпт из assets/simple_chat_prompt: ${e.message}",
                e
            )
        }
    }
    
    fun getUserPromptSimple(): String {
        return prefs.getString(KEY_USER_PROMPT_SIMPLE, "") ?: ""
    }
    
    fun setUserPromptSimple(prompt: String) {
        prefs.edit().putString(KEY_USER_PROMPT_SIMPLE, prompt.trim()).apply()
    }
    
    // Методы для температуры
    fun getTemperature(): Float {
        return prefs.getFloat(KEY_TEMPERATURE, DEFAULT_TEMPERATURE)
    }
    
    fun setTemperature(temperature: Float) {
        prefs.edit().putFloat(KEY_TEMPERATURE, temperature).apply()
    }
    
    // Методы для API провайдера
    fun getApiProvider(): ApiProvider {
        val providerName = prefs.getString(KEY_API_PROVIDER, DEFAULT_API_PROVIDER) ?: DEFAULT_API_PROVIDER
        return try {
            ApiProvider.valueOf(providerName)
        } catch (e: IllegalArgumentException) {
            ApiProvider.DEEPSEEK
        }
    }
    
    fun setApiProvider(provider: ApiProvider) {
        prefs.edit().putString(KEY_API_PROVIDER, provider.name).apply()
    }
    
    // Методы для Ollama модели
    fun getOllamaModel(): String {
        return prefs.getString(KEY_OLLAMA_MODEL, DEFAULT_OLLAMA_MODEL) ?: DEFAULT_OLLAMA_MODEL
    }
    
    fun setOllamaModel(model: String) {
        prefs.edit().putString(KEY_OLLAMA_MODEL, model).apply()
    }
    
    // Методы для DeepSeek модели
    fun getDeepSeekModel(): String {
        return prefs.getString(KEY_DEEPSEEK_MODEL, DEFAULT_DEEPSEEK_MODEL) ?: DEFAULT_DEEPSEEK_MODEL
    }
    
    fun setDeepSeekModel(model: String) {
        prefs.edit().putString(KEY_DEEPSEEK_MODEL, model).apply()
    }
    
    // Методы для компрессии истории
    fun isHistoryCompressionEnabled(): Boolean {
        return prefs.getBoolean(KEY_HISTORY_COMPRESSION_ENABLED, DEFAULT_HISTORY_COMPRESSION_ENABLED)
    }
    
    fun setHistoryCompressionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HISTORY_COMPRESSION_ENABLED, enabled).apply()
    }
}
