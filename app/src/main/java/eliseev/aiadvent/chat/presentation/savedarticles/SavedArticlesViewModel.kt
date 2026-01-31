package eliseev.aiadvent.chat.presentation.savedarticles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eliseev.aiadvent.chat.BuildConfig
import eliseev.aiadvent.chat.data.mcp.McpClientManager
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class SavedArticlesUiState(
    val list: List<SavedSummaryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

private val MCP_BASE_URL: String
    get() = BuildConfig.MCP_BASE_URL.ifBlank { "http://10.0.2.2" }
private const val PORT_STORAGE = 8084

class SavedArticlesViewModel(
    private val mcpClientManager: McpClientManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedArticlesUiState())
    val uiState: StateFlow<SavedArticlesUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val storageUrl = "$MCP_BASE_URL:$PORT_STORAGE"
            val connectStorage = mcpClientManager.connect(storageUrl)
            if (connectStorage.isFailure) {
                val msg = connectStorage.exceptionOrNull()?.message ?: "неизвестная ошибка"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Не удалось подключиться к MCP. URL: $storageUrl. Ошибка: $msg"
                )
                return@launch
            }
            val result = mcpClientManager.callTool("list_saved_summaries", null)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка загрузки списка: ${result.exceptionOrNull()?.message}"
                )
                return@launch
            }
            val text = textFromResult(result.getOrNull())
            val list = parseSavedSummaries(text)
            _uiState.value = _uiState.value.copy(list = list, isLoading = false, error = null)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { mcpClientManager.disconnect() }
    }

    private fun textFromResult(result: CallToolResult?): String? {
        if (result == null) return null
        return result.content?.joinToString("\n") { content ->
            when (content) {
                is TextContent -> content.text
                else -> content.toString()
            }
        }
    }

    private fun parseSavedSummaries(jsonText: String?): List<SavedSummaryItem> {
        if (jsonText.isNullOrBlank()) return emptyList()
        return try {
            val arr = Json.parseToJsonElement(jsonText) as? JsonArray ?: return emptyList()
            arr.map { el ->
                val obj = el as? JsonObject ?: return@map null
                fun str(key: String): String =
                    (obj[key] as? JsonPrimitive)?.content?.trim('"')?.replace("\\n", "\n") ?: ""
                SavedSummaryItem(
                    id = str("id"),
                    originalUrl = str("original_url"),
                    contentPreview = str("content_preview"),
                    content = str("content"),
                    savedAt = (obj["saved_at"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 0L
                )
            }.filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
