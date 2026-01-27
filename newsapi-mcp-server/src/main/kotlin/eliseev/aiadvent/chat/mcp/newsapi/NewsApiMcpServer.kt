package eliseev.aiadvent.chat.mcp.newsapi

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class NewsItemDto(
    val title: String,
    val description: String,
    val source: String,
    val publishedAt: String,
    val url: String
)

fun main() {
    val apiKey = System.getenv("NEWS_API_KEY") ?: ""
    
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    val server = Server(
        serverInfo = Implementation(
            name = "newsapi-mcp-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(
                    listChanged = null
                )
            )
        )
    ) {
        "MCP сервер для получения новостей из NewsAPI"
    }
    
    server.addTool(
        name = "get_latest_news",
        description = "Получить список из 5 последних новостей из различных источников. Возвращает заголовки, описания, ссылки и даты публикации.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", "string")
                    put("description", "Поисковый запрос (опционально). Если не указан, вернутся общие новости.")
                })
                put("language", buildJsonObject {
                    put("type", "string")
                    put("description", "Язык новостей (например, 'ru', 'en'). По умолчанию 'ru'.")
                    put("default", "ru")
                })
            }
        )
    ) { request: CallToolRequest ->
        try {
            val params = request.arguments as? JsonObject ?: buildJsonObject {}
            val query = params["query"]?.toString()?.trim('"')?.trim() ?: ""
            val language = params["language"]?.toString()?.trim('"')?.trim() ?: "ru"
            
            val url = if (query.isNotEmpty()) {
                "https://newsapi.org/v2/everything?q=$query&language=$language&pageSize=5&sortBy=publishedAt&apiKey=$apiKey"
            } else {
                "https://newsapi.org/v2/top-headlines?language=$language&pageSize=5&apiKey=$apiKey"
            }
            
            var response: NewsApiResponse = httpClient.get(url).body()
            
            // Если нет новостей на выбранном языке, пробуем английский
            if (response.status == "ok" && response.articles.isEmpty() && language == "ru") {
                val fallbackUrl = if (query.isNotEmpty()) {
                    "https://newsapi.org/v2/everything?q=$query&language=en&pageSize=5&sortBy=publishedAt&apiKey=$apiKey"
                } else {
                    "https://newsapi.org/v2/top-headlines?language=en&pageSize=5&apiKey=$apiKey"
                }
                response = httpClient.get(fallbackUrl).body()
            }
            
            if (response.status == "ok" && response.articles.isNotEmpty()) {
                val newsList = response.articles.take(5).map { article ->
                    NewsItemDto(
                        title = article.title ?: "Без названия",
                        description = article.description ?: "Нет описания",
                        source = article.source?.name ?: "Неизвестно",
                        publishedAt = article.publishedAt ?: "",
                        url = article.url ?: ""
                    )
                }
                val newsJson = Json { ignoreUnknownKeys = true }.encodeToString(newsList)
                
                CallToolResult(
                    content = listOf(
                        TextContent(text = newsJson)
                    ),
                    isError = false
                )
            } else if (response.status == "ok" && response.articles.isEmpty()) {
                CallToolResult(
                    content = listOf(
                        TextContent(text = "Новости не найдены. Попробуйте другой язык или поисковый запрос.")
                    ),
                    isError = false
                )
            } else {
                CallToolResult(
                    content = listOf(
                        TextContent(text = "Не удалось получить новости. Статус: ${response.status}. Проверьте API ключ или попробуйте позже.")
                    ),
                    isError = true
                )
            }
        } catch (e: Exception) {
            CallToolResult(
                content = listOf(
                    TextContent(text = "Ошибка при получении новостей: ${e.message}")
                ),
                isError = true
            )
        }
    }
    
    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )
    
    runBlocking {
        val session = server.createSession(transport)
        val done = Job()
        session.onClose { done.complete() }
        done.join()
    }
}

@Serializable
data class NewsApiResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

@Serializable
data class Article(
    val title: String,
    val description: String?,
    val url: String,
    val publishedAt: String,
    val source: Source?
)

@Serializable
data class Source(
    val name: String?
)

