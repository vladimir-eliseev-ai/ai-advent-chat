package eliseev.aiadvent.chat.reminder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class Summary(
    @SerialName("id")
    val id: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("summary")
    val summary: String,
    @SerialName("news_count")
    val newsCount: Int = 0
)

@Serializable
data class SummariesData(
    @SerialName("summaries")
    val summaries: MutableList<Summary> = mutableListOf()
)

class SummaryStorage(private val storageFile: File) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    init {
        if (!storageFile.exists()) {
            storageFile.parentFile?.mkdirs()
            saveData(SummariesData())
        }
    }
    
    fun saveSummary(summaryText: String, newsCount: Int = 0): Summary {
        val data = loadData()
        val summary = Summary(
            id = System.currentTimeMillis().toString(),
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            summary = summaryText,
            newsCount = newsCount
        )
        
        data.summaries.add(0, summary)
        
        if (data.summaries.size > 100) {
            data.summaries.removeAt(data.summaries.size - 1)
        }
        
        saveData(data)
        return summary
    }
    
    fun getLastSummary(): Summary? {
        val data = loadData()
        return data.summaries.firstOrNull()
    }
    
    fun getAllSummaries(): List<Summary> {
        val data = loadData()
        return data.summaries.toList()
    }
    
    fun getSummaryById(id: String): Summary? {
        val data = loadData()
        return data.summaries.find { it.id == id }
    }
    
    private fun loadData(): SummariesData {
        return try {
            if (storageFile.exists()) {
                val content = storageFile.readText()
                json.decodeFromString<SummariesData>(content)
            } else {
                SummariesData()
            }
        } catch (e: Exception) {
            SummariesData()
        }
    }
    
    private fun saveData(data: SummariesData) {
        try {
            val content = json.encodeToString(SummariesData.serializer(), data)
            storageFile.writeText(content)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
