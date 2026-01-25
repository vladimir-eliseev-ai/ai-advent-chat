package eliseev.aiadvent.chat.data.model

import android.content.Context
import android.content.SharedPreferences

class AppSettings(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    companion object {
        private const val PREFS_NAME = "app_settings"
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
