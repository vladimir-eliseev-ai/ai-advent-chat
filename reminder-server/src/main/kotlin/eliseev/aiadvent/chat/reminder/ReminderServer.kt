package eliseev.aiadvent.chat.reminder

import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.net.BindException
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

fun main() {
    val mcpServerUrl = System.getenv("NEWS_API_MCP_URL") ?: "http://localhost:8080/mcp"
    val deepseekApiKey = System.getenv("DEEPSEEK_API_KEY") ?: ""
    val intervalHours = System.getenv("INTERVAL_HOURS")?.toLongOrNull() ?: 6L
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8081
    val storagePath = System.getenv("STORAGE_PATH") ?: "./summaries.json"
    
    if (deepseekApiKey.isBlank()) {
        println("ОШИБКА: DEEPSEEK_API_KEY не установлен!")
        return
    }
    
    val storageFile = File(storagePath)
    val storage = SummaryStorage(storageFile)
    val mcpClient = McpNewsClient(mcpServerUrl)
    val aiService = AiSummaryService(deepseekApiKey)
    
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    var isProcessing = false
    
    suspend fun processNewsUpdate() {
        if (isProcessing) {
            println("Обновление уже выполняется, пропускаем...")
            return
        }
        
        isProcessing = true
        try {
            println("Начинаем обновление новостей...")
            
            mcpClient.connect()
            val newsResult = mcpClient.getLatestNews()
            
            if (newsResult.isFailure) {
                println("Ошибка при получении новостей: ${newsResult.exceptionOrNull()?.message}")
            } else {
                val newsText = newsResult.getOrNull()
                if (newsText != null) {
                    println("Получено новостей: ${newsText.length} символов")
                    println("=".repeat(80))
                    println("НОВОСТИ:")
                    println("=".repeat(80))
                    println(newsText)
                    println("=".repeat(80))
                    
                    println("⏳ Формирование сводки новостей...")
                    val summaryResult = aiService.createSummary(newsText)
                    
                    if (summaryResult.isFailure) {
                        println("Ошибка при создании сводки: ${summaryResult.exceptionOrNull()?.message}")
                    } else {
                        val summaryText = summaryResult.getOrNull()
                        if (summaryText != null) {
                            val newsCount = newsText.split("\n\n").size
                            val summary = storage.saveSummary(summaryText, newsCount)
                            
                            println("Сводка создана и сохранена: ${summary.id}")
                            println("Время создания: ${summary.createdAt}")
                            println("=".repeat(80))
                            println("СВОДКА:")
                            println("=".repeat(80))
                            println(summaryText)
                            println("=".repeat(80))
                        } else {
                            println("Сводка не создана")
                        }
                    }
                } else {
                    println("Новости не получены")
                }
            }
        } catch (e: Exception) {
            println("Критическая ошибка при обновлении: ${e.message}")
            e.printStackTrace()
        } finally {
            isProcessing = false
        }
    }
    
    scope.launch {
        println("Запуск фоновой задачи обновления новостей...")
        println("Интервал: 10 секунд (тестовый режим)")
        
        delay(TimeUnit.SECONDS.toMillis(5))
        
        while (true) {
            processNewsUpdate()
            delay(10 * 1000)
        }
    }
    
    // Функция для проверки доступности порта
    fun isPortAvailable(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    // Пытаемся найти свободный порт, начиная с указанного
    var actualPort = port
    var attempts = 0
    while (!isPortAvailable(actualPort) && attempts < 10) {
        println("⚠ Порт $actualPort занят, пробуем порт ${actualPort + 1}...")
        actualPort++
        attempts++
    }
    
    if (!isPortAvailable(actualPort)) {
        println("✗ ОШИБКА: Не удалось найти свободный порт (пробовали порты $port-${port + attempts})")
        println("Остановите процесс, занимающий порт $port, или укажите другой порт через переменную PORT:")
        println("  PORT=8082 java -jar build/libs/reminder-server-1.0.0.jar")
        return
    }
    
    if (actualPort != port) {
        println("✓ Используется порт $actualPort (вместо $port)")
    } else {
        println("✓ Используется порт $actualPort")
    }
    
    try {
        embeddedServer(Netty, port = actualPort) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            })
        }
        
        routing {
            get("/") {
                call.respond(mapOf(
                    "service" to "Reminder Server",
                    "version" to "1.0.0",
                    "status" to "running",
                    "interval_hours" to intervalHours,
                    "mcp_server_url" to mcpServerUrl
                ))
            }
            
            get("/summary") {
                val summary = storage.getLastSummary()
                if (summary != null) {
                    call.respond(summary)
                } else {
                    call.respond(mapOf("error" to "Сводки пока нет"))
                }
            }
            
            get("/summaries") {
                val summaries = storage.getAllSummaries()
                call.respond(mapOf("summaries" to summaries, "count" to summaries.size))
            }
            
            get("/summary/{id}") {
                val id = call.parameters["id"]
                if (id != null) {
                    val summary = storage.getSummaryById(id)
                    if (summary != null) {
                        call.respond(summary)
                    } else {
                        call.respond(mapOf("error" to "Сводка не найдена"))
                    }
                } else {
                    call.respond(mapOf("error" to "ID не указан"))
                }
            }
            
            post("/trigger") {
                scope.launch {
                    processNewsUpdate()
                }
                call.respond(mapOf("status" to "Обновление запущено"))
            }
            
            get("/health") {
                call.respond(mapOf(
                    "status" to "ok",
                    "is_processing" to isProcessing
                ))
            }
        }
        }.start(wait = true)
        println("✓ Сервер запущен на порту $actualPort")
    } catch (e: BindException) {
        println("✗ ОШИБКА: Порт $actualPort занят!")
        println("Остановите процесс, занимающий порт, или укажите другой порт:")
        println("  PORT=${actualPort + 1} java -jar build/libs/reminder-server-1.0.0.jar")
        println("\nДля поиска процесса, занимающего порт:")
        println("  lsof -i :$actualPort")
        System.exit(1)
    } catch (e: Exception) {
        println("✗ ОШИБКА при запуске сервера: ${e.message}")
        e.printStackTrace()
        System.exit(1)
    }
}
