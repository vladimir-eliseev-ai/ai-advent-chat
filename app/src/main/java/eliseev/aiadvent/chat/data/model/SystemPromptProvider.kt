package eliseev.aiadvent.chat.data.model

import android.content.Context
import java.io.IOException

class SystemPromptProvider(
    private val context: Context
) {
    fun getSystemPrompt(): String {
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

    fun getSystemMessage(): ChatMessage {
        return ChatMessage(
            role = MessageRole.SYSTEM,
            content = getSystemPrompt()
        )
    }
}
