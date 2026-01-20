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
        private const val DEFAULT_TEMPERATURE = 0.7f
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
}
