package eliseev.aiadvent.chat.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import eliseev.aiadvent.chat.data.model.MessageRole

@Entity(tableName = "chat_messages")
@TypeConverters(Converters::class)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val storeName: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long,
    val structuredResponseJson: String? = null,
    val metricsJson: String? = null,
    val isSummary: Boolean = false,
    val originalMessageCount: Int = 0
)
