package eliseev.aiadvent.chat.data.store

import eliseev.aiadvent.chat.data.database.ChatDatabase
import eliseev.aiadvent.chat.data.database.ChatMessageDao
import eliseev.aiadvent.chat.data.database.ChatMessageEntity
import eliseev.aiadvent.chat.data.model.ChatMessage
import eliseev.aiadvent.chat.data.model.MessageMetrics
import eliseev.aiadvent.chat.data.model.StructuredResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class PersistentChatMessageStore(
    private val database: ChatDatabase,
    private val storeName: String
) {
    private val dao: ChatMessageDao = database.chatMessageDao()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    fun getMessagesFlow(): Flow<List<ChatMessage>> {
        return dao.getMessages(storeName).map { entities ->
            entities.map { it.toChatMessage() }
        }
    }
    
    suspend fun loadMessages(): List<ChatMessage> = withContext(Dispatchers.IO) {
        try {
            val entities = dao.getMessagesSync(storeName)
            val messages = entities.map { it.toChatMessage() }
            Timber.d("Loaded ${messages.size} messages from Room for store: $storeName")
            return@withContext messages
        } catch (e: Exception) {
            Timber.e(e, "Error loading messages from Room for store: $storeName")
            return@withContext emptyList()
        }
    }
    
    suspend fun saveMessages(messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        try {
            // Используем транзакцию для атомарной замены сообщений
            val entities = messages.map { it.toEntity(storeName) }
            dao.replaceMessages(storeName, entities)
            Timber.d("Saved ${messages.size} messages to Room for store: $storeName")
        } catch (e: Exception) {
            Timber.e(e, "Error saving messages to Room for store: $storeName")
        }
    }
    
    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            dao.deleteByStoreName(storeName)
            Timber.d("Cleared messages from Room for store: $storeName")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing messages from Room for store: $storeName")
        }
    }
    
    private fun ChatMessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            role = role,
            content = content,
            timestamp = timestamp,
            structuredResponse = structuredResponseJson?.let { jsonString ->
                try {
                    json.decodeFromString<StructuredResponse>(jsonString)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse structured response")
                    null
                }
            },
            metrics = metricsJson?.let { jsonString ->
                try {
                    json.decodeFromString<MessageMetrics>(jsonString)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse metrics")
                    null
                }
            },
            isSummary = isSummary,
            originalMessageCount = originalMessageCount
        )
    }
    
    private fun ChatMessage.toEntity(storeName: String): ChatMessageEntity {
        return ChatMessageEntity(
            storeName = storeName,
            role = role,
            content = content,
            timestamp = timestamp,
            structuredResponseJson = structuredResponse?.let { json.encodeToString(it) },
            metricsJson = metrics?.let { json.encodeToString(it) },
            isSummary = isSummary,
            originalMessageCount = originalMessageCount
        )
    }
}
