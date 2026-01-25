package eliseev.aiadvent.chat.data.database

import androidx.room.TypeConverter
import eliseev.aiadvent.chat.data.model.MessageRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    @TypeConverter
    fun fromMessageRole(role: MessageRole): String {
        return role.name
    }
    
    @TypeConverter
    fun toMessageRole(role: String): MessageRole {
        return MessageRole.valueOf(role)
    }
}
