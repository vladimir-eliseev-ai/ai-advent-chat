package eliseev.aiadvent.chat.presentation.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eliseev.aiadvent.chat.BuildConfig
import eliseev.aiadvent.chat.data.mcp.McpClientManager
import eliseev.aiadvent.chat.presentation.mcp.model.NewsItem
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

enum class McpServerType {
    GITHUB,
    NEWSAPI,
    ORCHESTRATOR
}

data class McpToolsUiState(
    val tools: List<Tool> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val authToken: String = "",
    val isConnected: Boolean = false,
    val toolResult: String? = null,
    val callingTool: String? = null,
    val serverType: McpServerType = McpServerType.GITHUB,
    val newsList: List<NewsItem> = emptyList()
)

private const val GITHUB_MCP_URL = "https://api.githubcopilot.com/mcp/"
private const val NEWSAPI_MCP_URL = "http://10.0.2.2:8080"
private val ORCHESTRATOR_MCP_URL: String
    get() = BuildConfig.MCP_BASE_URL.ifBlank { "http://10.0.2.2" }.trimEnd('/') + ":8090"

class McpToolsViewModel(
    private val mcpClientManager: McpClientManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(McpToolsUiState())
    val uiState: StateFlow<McpToolsUiState> = _uiState.asStateFlow()

    fun updateAuthToken(token: String) {
        _uiState.value = _uiState.value.copy(authToken = token)
    }

    fun setServerType(serverType: McpServerType) {
        _uiState.value = _uiState.value.copy(serverType = serverType)
    }

    fun connectAndLoadTools() {
        viewModelScope.launch {
            // Если уже подключены, сначала отключаемся
            if (_uiState.value.isConnected) {
                mcpClientManager.disconnect()
                _uiState.value = _uiState.value.copy(
                    isConnected = false,
                    tools = emptyList()
                )
            }

            val serverType = _uiState.value.serverType
            val url = when (serverType) {
                McpServerType.GITHUB -> GITHUB_MCP_URL
                McpServerType.NEWSAPI -> NEWSAPI_MCP_URL
                McpServerType.ORCHESTRATOR -> ORCHESTRATOR_MCP_URL
            }

            val authToken = if (serverType == McpServerType.GITHUB) {
                val token = _uiState.value.authToken.trim()
                if (token.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Введите GitHub Personal Access Token"
                    )
                    return@launch
                }
                token
            } else {
                null // NewsAPI не требует токен в заголовке, он в env переменной
            }

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                tools = emptyList()
            )

                val connectResult = mcpClientManager.connect(url, authToken)
                if (connectResult.isFailure) {
                    val errorMessage = connectResult.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                    val userFriendlyError = when {
                        errorMessage.contains("forbidden", ignoreCase = true) || 
                        errorMessage.contains("access denied", ignoreCase = true) ->
                            "Доступ запрещен. Проверьте, что токен имеет scope 'repo'"
                        errorMessage.contains("unauthorized", ignoreCase = true) ->
                            "Неверный токен. Проверьте правильность GitHub PAT"
                        errorMessage.contains("not found", ignoreCase = true) ->
                            "Сервер не найден. Проверьте подключение к интернету"
                        else -> "Ошибка подключения: $errorMessage"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = userFriendlyError,
                        isConnected = false
                    )
                    return@launch
                }

            val toolsResult = mcpClientManager.listTools()
            if (toolsResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Ошибка получения инструментов: ${toolsResult.exceptionOrNull()?.message ?: "Неизвестная ошибка"}",
                    isConnected = true
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                tools = toolsResult.getOrNull() ?: emptyList(),
                isConnected = true,
                error = null
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            mcpClientManager.disconnect()
            _uiState.value = _uiState.value.copy(
                isConnected = false,
                tools = emptyList()
            )
        }
    }

    fun callTool(toolName: String, arguments: JsonObject? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                callingTool = toolName,
                toolResult = null,
                newsList = emptyList(),
                error = null
            )

            val result = mcpClientManager.callTool(toolName, arguments)
            if (result.isSuccess) {
                val toolResult = result.getOrNull()
                val resultText = toolResult?.content?.joinToString("\n") { content ->
                    when {
                        content is io.modelcontextprotocol.kotlin.sdk.types.TextContent -> content.text
                        else -> content.toString()
                    }
                } ?: "Результат получен"
                
                // Пытаемся распарсить как JSON с новостями
                val newsList = try {
                    if (toolName == "get_latest_news") {
                        Json.decodeFromString<List<NewsItem>>(resultText)
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("McpToolsViewModel", "Ошибка парсинга новостей: ${e.message}", e)
                    android.util.Log.d("McpToolsViewModel", "Текст для парсинга: $resultText")
                    emptyList()
                }
                
                _uiState.value = _uiState.value.copy(
                    toolResult = if (newsList.isNotEmpty()) null else resultText,
                    newsList = newsList,
                    callingTool = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Ошибка вызова инструмента: ${result.exceptionOrNull()?.message}",
                    callingTool = null
                )
            }
        }
    }

    fun clearToolResult() {
        _uiState.value = _uiState.value.copy(
            toolResult = null,
            newsList = emptyList()
        )
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            mcpClientManager.disconnect()
        }
    }
}
