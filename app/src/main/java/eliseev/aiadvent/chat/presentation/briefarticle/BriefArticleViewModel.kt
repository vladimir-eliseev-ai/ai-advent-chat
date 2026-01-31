package eliseev.aiadvent.chat.presentation.briefarticle

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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

enum class StepStatus { PENDING, RUNNING, OK, FAILED }

data class StepState(
    val label: String,
    val status: StepStatus
)

data class BriefArticleUiState(
    val urlInput: String = "",
    val step1: StepState = StepState("", StepStatus.PENDING),
    val step2: StepState = StepState("", StepStatus.PENDING),
    val step3: StepState = StepState("", StepStatus.PENDING),
    val summaryResult: String? = null,
    val error: String? = null
)

private val MCP_BASE_URL: String
    get() = BuildConfig.MCP_BASE_URL.ifBlank { "http://10.0.2.2" }
private const val PORT_READER = 8082
private const val PORT_SUMMARIZER = 8083
private const val PORT_STORAGE = 8084

class BriefArticleViewModel(
    private val mcpClientManager: McpClientManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BriefArticleUiState())
    val uiState: StateFlow<BriefArticleUiState> = _uiState.asStateFlow()

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url, error = null)
    }

    fun runBriefSummary() {
        val url = _uiState.value.urlInput.trim()
        if (url.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Введите ссылку на статью")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                error = null,
                summaryResult = null,
                step1 = StepState("Загрузка статьи", StepStatus.RUNNING),
                step2 = StepState("Создание резюме", StepStatus.PENDING),
                step3 = StepState("Сохранение", StepStatus.PENDING)
            )
            var articleText: String? = null
            var summary: String? = null

            mcpClientManager.disconnect()
            val readerUrl = "$MCP_BASE_URL:$PORT_READER"
            val connect1 = mcpClientManager.connect(readerUrl)
            if (connect1.isFailure) {
                _uiState.value = _uiState.value.copy(
                    step1 = StepState("Загрузка статьи", StepStatus.FAILED),
                    error = "Не удалось подключиться к MCP (чтение): ${connect1.exceptionOrNull()?.message}"
                )
                return@launch
            }
            val fetchResult = mcpClientManager.callTool("fetch_article", buildJsonObject { put("url", url) })
            mcpClientManager.disconnect()
            if (fetchResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    step1 = StepState("Загрузка статьи", StepStatus.FAILED),
                    error = "Ошибка загрузки статьи: ${fetchResult.exceptionOrNull()?.message}"
                )
                return@launch
            }
            articleText = textFromResult(fetchResult.getOrNull())
            if (articleText.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    step1 = StepState("Загрузка статьи", StepStatus.FAILED),
                    error = "Текст статьи пуст или не получен"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                step1 = StepState("Загрузка статьи", StepStatus.OK),
                step2 = StepState("Создание резюме", StepStatus.RUNNING)
            )

            val connect2 = mcpClientManager.connect("$MCP_BASE_URL:$PORT_SUMMARIZER")
            if (connect2.isFailure) {
                _uiState.value = _uiState.value.copy(
                    step2 = StepState("Создание резюме", StepStatus.FAILED),
                    error = "Не удалось подключиться к MCP (резюме): ${connect2.exceptionOrNull()?.message}"
                )
                return@launch
            }
            val summarizeResult = mcpClientManager.callTool("summarize", buildJsonObject { put("text", articleText) })
            mcpClientManager.disconnect()
            if (summarizeResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    step2 = StepState("Создание резюме", StepStatus.FAILED),
                    error = "Ошибка суммаризации: ${summarizeResult.exceptionOrNull()?.message}"
                )
                return@launch
            }
            summary = textFromResult(summarizeResult.getOrNull())
            if (summary.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    step2 = StepState("Создание резюме", StepStatus.FAILED),
                    error = "Резюме не получено"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                step2 = StepState("Создание резюме", StepStatus.OK),
                step3 = StepState("Сохранение", StepStatus.RUNNING)
            )

            val connect3 = mcpClientManager.connect("$MCP_BASE_URL:$PORT_STORAGE")
            if (connect3.isFailure) {
                _uiState.value = _uiState.value.copy(
                    step3 = StepState("Сохранение", StepStatus.FAILED),
                    error = "Не удалось подключиться к MCP (сохранение): ${connect3.exceptionOrNull()?.message}"
                )
                return@launch
            }
            val saveResult = mcpClientManager.callTool(
                "save_summary",
                buildJsonObject {
                    put("content", summary)
                    put("original_url", url)
                }
            )
            if (saveResult.isFailure || (saveResult.getOrNull()?.isError == true)) {
                _uiState.value = _uiState.value.copy(
                    step3 = StepState("Сохранение", StepStatus.FAILED),
                    error = "Ошибка сохранения: ${saveResult.exceptionOrNull()?.message ?: textFromResult(saveResult.getOrNull())}"
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(
                step3 = StepState("Сохранение", StepStatus.OK),
                summaryResult = summary,
                error = null
            )
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
}
