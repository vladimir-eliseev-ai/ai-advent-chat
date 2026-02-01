package eliseev.aiadvent.chat.mcp.newsapi

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.http.HttpMethod
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.serialization.kotlinx.json.json as clientJson
import io.ktor.serialization.kotlinx.json.json as serverJson
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

fun main() {
    val apiKey = System.getenv("NEWS_API_KEY") ?: ""
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    val httpClient = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            clientJson(Json {
                ignoreUnknownKeys = true
            })
        }
    }
    
    // Вспомогательная функция для обработки успешного ответа
    fun processNewsResponse(response: NewsApiResponse): CallToolResult {
        return if (response.status == "ok" && !response.articles.isNullOrEmpty()) {
            val newsList = response.articles!!.take(5).mapIndexed { index, article ->
                """
                ${index + 1}. ${article.title}
                   Описание: ${article.description ?: "Нет описания"}
                   Источник: ${article.source?.name ?: "Неизвестно"}
                   Дата: ${article.publishedAt}
                   Ссылка: ${article.url}
                """.trimIndent()
            }.joinToString("\n\n")

            CallToolResult(
                content = listOf(
                    TextContent(text = "Последние 5 новостей:\n\n$newsList")
                ),
                isError = false
            )
        } else if (response.status == "ok" && response.articles.isNullOrEmpty()) {
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

            // Проверяем на ошибку от NewsAPI
            if (response.status != "ok" || response.articles == null) {
                val errorMsg = response.message ?: "Неизвестная ошибка"
                val errorCode = response.code ?: response.status
                CallToolResult(
                    content = listOf(
                        TextContent(text = "Ошибка NewsAPI (код: $errorCode): $errorMsg. Проверьте API ключ или попробуйте позже.")
                    ),
                    isError = true
                )
            } else {
                // Если нет новостей на выбранном языке, пробуем английский
                if (response.status == "ok" && response.articles.isNullOrEmpty() && language == "ru") {
                    val fallbackUrl = if (query.isNotEmpty()) {
                        "https://newsapi.org/v2/everything?q=$query&language=en&pageSize=5&sortBy=publishedAt&apiKey=$apiKey"
                    } else {
                        "https://newsapi.org/v2/top-headlines?language=en&pageSize=5&apiKey=$apiKey"
                    }
                    response = httpClient.get(fallbackUrl).body()
                    
                    // Проверяем ошибку после fallback
                    if (response.status != "ok" || response.articles == null) {
                        val errorMsg = response.message ?: "Неизвестная ошибка"
                        val errorCode = response.code ?: response.status
                        CallToolResult(
                            content = listOf(
                                TextContent(text = "Ошибка NewsAPI при fallback (код: $errorCode): $errorMsg")
                            ),
                            isError = true
                        )
                    } else {
                        processNewsResponse(response)
                    }
                } else {
                    processNewsResponse(response)
                }
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

    val transport = StreamableHttpServerTransport(
        enableJsonResponse = true  // Используем JSON ответы (без SSE)
    )
    
    runBlocking {
        server.connect(transport)
    }
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(ServerContentNegotiation) {
            serverJson()
        }
        install(SSE)

        routing {
            // Обрабатываем все HTTP методы (GET, POST)
            get {
                transport.handleRequest(null, call)
            }
            
            post {
                transport.handleRequest(null, call)
            }
        }
    }.start(wait = true)
}
