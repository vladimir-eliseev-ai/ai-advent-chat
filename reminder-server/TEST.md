# Тестирование Reminder Server

## Подготовка

### 1. Убедитесь, что запущен newsapi-mcp-server

```bash
cd newsapi-mcp-server
export NEWS_API_KEY="ваш_api_ключ"
java -jar build/libs/newsapi-mcp-server-1.0.0.jar
```

Сервер должен быть доступен на `http://localhost:8080`

### 2. Проверьте доступность MCP сервера

```bash
curl http://localhost:8080
```

Должен вернуться ответ от MCP сервера.

## Запуск Reminder Server

### Вариант 1: С переменными окружения

```bash
cd reminder-server
export DEEPSEEK_API_KEY="ваш_deepseek_ключ"
export NEWS_API_MCP_URL="http://localhost:8080/mcp"
export INTERVAL_HOURS=6
export PORT=8081
export STORAGE_PATH="./summaries.json"

java -jar build/libs/reminder-server-1.0.0.jar
```

### Вариант 2: Одной строкой

```bash
cd reminder-server
DEEPSEEK_API_KEY="ваш_ключ" NEWS_API_MCP_URL="http://localhost:8080/mcp" java -jar build/libs/reminder-server-1.0.0.jar
```

## Тестирование

### 1. Проверка статуса сервера

```bash
curl http://localhost:8081/
```

Ожидаемый ответ:
```json
{
  "service": "Reminder Server",
  "version": "1.0.0",
  "status": "running",
  "interval_hours": 6,
  "mcp_server_url": "http://localhost:8080/mcp"
}
```

### 2. Проверка здоровья

```bash
curl http://localhost:8081/health
```

Ожидаемый ответ:
```json
{
  "status": "ok",
  "is_processing": false
}
```

### 3. Принудительный запуск обновления

```bash
curl -X POST http://localhost:8081/trigger
```

Ожидаемый ответ:
```json
{
  "status": "Обновление запущено"
}
```

### 4. Получение последней сводки

```bash
curl http://localhost:8081/summary
```

Ожидаемый ответ (после первого обновления):
```json
{
  "id": "1706457600000",
  "created_at": "2024-01-28T12:00:00",
  "summary": "Краткая сводка новостей...",
  "news_count": 5
}
```

### 5. Получение всех сводок

```bash
curl http://localhost:8081/summaries
```

## Проверка логов

Сервер выводит подробные логи в консоль:

1. **При запуске:**
   - "Запуск фоновой задачи обновления новостей..."
   - "Интервал: 10 секунд (тестовый режим)"

2. **При каждом обновлении:**
   - "Начинаем обновление новостей..."
   - "Подключение к MCP серверу: http://localhost:8080"
   - "Инициализация MCP сервера..."
   - "MCP сервер инициализирован успешно"
   - "Получение новостей..."
   - "Получено новостей: X символов"
   - Полный текст новостей с разделителями
   - "Сводка создана и сохранена: ID"

3. **При ошибках:**
   - Детальное описание ошибки
   - "Отключение от MCP сервера..."

## Ожидаемое поведение

1. **Первые 5 секунд:** Сервер запускается, HTTP сервер стартует
2. **Через 5 секунд:** Первое обновление новостей
3. **Каждые 10 секунд:** Автоматическое обновление новостей
4. **При успехе:** Новости выводятся в логи, создается сводка, сохраняется в файл
5. **При ошибке:** Клиент отключается, при следующей попытке переподключается

## Устранение проблем

### Ошибка "DEEPSEEK_API_KEY не установлен"
- Убедитесь, что переменная окружения установлена: `echo $DEEPSEEK_API_KEY`

### Ошибка "HTTP 404" или "Server not initialized"
- Проверьте, что `newsapi-mcp-server` запущен: `curl http://localhost:8080`
- Проверьте URL в переменной `NEWS_API_MCP_URL`

### Ошибка "MCP error" или "Empty response"
- Проверьте логи `newsapi-mcp-server`
- Убедитесь, что API ключ NewsAPI установлен и действителен

### Сервер не отвечает на HTTP запросы
- Проверьте, что порт 8081 свободен: `lsof -i :8081`
- Проверьте логи на наличие ошибок запуска

## Остановка сервера

Нажмите `Ctrl+C` в терминале, где запущен сервер.
