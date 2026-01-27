package eliseev.aiadvent.chat.presentation.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eliseev.aiadvent.chat.data.mcp.McpClientManager
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class McpToolsUiState(
    val tools: List<Tool> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val authToken: String = "",
    val isConnected: Boolean = false
)

private const val GITHUB_MCP_URL = "https://api.githubcopilot.com/mcp/"

class McpToolsViewModel(
    private val mcpClientManager: McpClientManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(McpToolsUiState())
    val uiState: StateFlow<McpToolsUiState> = _uiState.asStateFlow()

    fun updateAuthToken(token: String) {
        _uiState.value = _uiState.value.copy(authToken = token)
    }

    fun connectAndLoadTools() {
        val authToken = _uiState.value.authToken.trim()
        if (authToken.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                error = "Введите GitHub Personal Access Token"
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                tools = emptyList()
            )

                val connectResult = mcpClientManager.connect(GITHUB_MCP_URL, authToken)
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

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            mcpClientManager.disconnect()
        }
    }
}
