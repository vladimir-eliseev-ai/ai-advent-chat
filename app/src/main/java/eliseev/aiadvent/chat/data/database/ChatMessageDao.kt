package eliseev.aiadvent.chat.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE storeName = :storeName ORDER BY timestamp ASC")
    fun getMessages(storeName: String): Flow<List<ChatMessageEntity>>
    
    @Query("SELECT * FROM chat_messages WHERE storeName = :storeName ORDER BY timestamp ASC")
    suspend fun getMessagesSync(storeName: String): List<ChatMessageEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    @Query("DELETE FROM chat_messages WHERE storeName = :storeName")
    suspend fun deleteByStoreName(storeName: String)
    
    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Transaction
    suspend fun replaceMessages(storeName: String, messages: List<ChatMessageEntity>) {
        deleteByStoreName(storeName)
        if (messages.isNotEmpty()) {
            insertMessages(messages)
        }
    }
}
