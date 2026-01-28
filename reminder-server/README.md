# Reminder Server

Автоматический сервер для получения новостей и создания сводок через AI.

## Возможности

- ✅ Автоматическое получение новостей через MCP инструмент `get_latest_news`
- ✅ Создание кратких сводок через DeepSeek API
- ✅ Сохранение истории сводок (до 100 последних)
- ✅ HTTP API для получения сводок
- ✅ Работает 24/7 в фоновом режиме

## Требования

- Java 21+
- Запущенный `newsapi-mcp-server` (по умолчанию на `http://localhost:8080/mcp`)
- API ключ DeepSeek

## Установка и запуск

### 1. Соберите проект

```bash
cd reminder-server
./gradlew shadowJar
```

Это создаст JAR файл: `build/libs/reminder-server-1.0.0.jar`

### 2. Установите переменные окружения

```bash
export NEWS_API_MCP_URL="http://localhost:8080/mcp"
export DEEPSEEK_API_KEY="ваш_api_ключ_deepseek"
export INTERVAL_HOURS=6
export PORT=8081
export STORAGE_PATH="./summaries.json"
```

**Обязательные переменные:**
- `DEEPSEEK_API_KEY` - API ключ DeepSeek (обязательно!)

**Опциональные переменные:**
- `NEWS_API_MCP_URL` - URL MCP сервера новостей (по умолчанию: `http://localhost:8080/mcp`)
- `INTERVAL_HOURS` - интервал обновления в часах (по умолчанию: 6)
- `PORT` - порт HTTP сервера (по умолчанию: 8081)
- `STORAGE_PATH` - путь к файлу хранения сводок (по умолчанию: `./summaries.json`)

### 3. Запустите сервер

#### Вариант 1: Автоматическая сборка и запуск (рекомендуется)

```bash
export DEEPSEEK_API_KEY="ваш_api_ключ_deepseek"
./run.sh
```

#### Вариант 2: Запуск в фоновом режиме

```bash
export DEEPSEEK_API_KEY="ваш_api_ключ_deepseek"
./run-background.sh
```

Логи будут в файле `reminder-server.log`. Для остановки:
```bash
./stop.sh
```

#### Вариант 3: Ручной запуск

```bash
java -jar build/libs/reminder-server-1.0.0.jar
```

Сервер запустится на **http://localhost:8081**

## Использование

### Автоматическое обновление

Сервер автоматически:
1. Каждые N часов (по умолчанию 6) подключается к MCP серверу новостей
2. Получает последние новости через инструмент `get_latest_news`
3. Отправляет новости в DeepSeek API для создания сводки
4. Сохраняет сводку в файл

### HTTP API

#### Получить последнюю сводку

```bash
curl http://localhost:8081/summary
```

#### Получить все сводки

```bash
curl http://localhost:8081/summaries
```

#### Получить сводку по ID

```bash
curl http://localhost:8081/summary/1234567890
```

#### Принудительно запустить обновление

```bash
curl -X POST http://localhost:8081/trigger
```

#### Проверить статус

```bash
curl http://localhost:8081/health
```

#### Информация о сервере

```bash
curl http://localhost:8081/
```

## Пример ответа

```json
{
  "id": "1706457600000",
  "created_at": "2024-01-28T12:00:00",
  "summary": "Краткая сводка новостей...",
  "news_count": 5
}
```

## Структура проекта

```
reminder-server/
├── build.gradle.kts          # Конфигурация Gradle
├── gradlew                   # Gradle wrapper
├── README.md                 # Этот файл
└── src/main/kotlin/
    └── eliseev/aiadvent/chat/reminder/
        ├── ReminderServer.kt      # Основной сервер
        ├── McpNewsClient.kt       # Клиент MCP
        ├── AiSummaryService.kt    # Сервис AI сводок
        └── SummaryStorage.kt      # Хранение сводок
```

## Решение проблем

### Ошибка "DEEPSEEK_API_KEY не установлен"
- Проверьте, что переменная окружения установлена: `echo $DEEPSEEK_API_KEY`
- Убедитесь, что API ключ действителен

### Ошибка подключения к MCP серверу
- Убедитесь, что `newsapi-mcp-server` запущен и доступен
- Проверьте URL в переменной `NEWS_API_MCP_URL`
- Для эмулятора используйте `http://10.0.2.2:8080/mcp`

### Сводки не создаются
- Проверьте логи сервера на наличие ошибок
- Убедитесь, что MCP сервер возвращает новости
- Проверьте баланс/лимиты DeepSeek API

### Сервер не запускается
- Убедитесь, что Java 21+ установлена: `java -version`
- Проверьте, что порт свободен: `lsof -i :8081`

## Запуск как служба (systemd)

Создайте файл `/etc/systemd/system/reminder-server.service`:

```ini
[Unit]
Description=Reminder Server
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/reminder-server
Environment="NEWS_API_MCP_URL=http://localhost:8080/mcp"
Environment="DEEPSEEK_API_KEY=your-key"
Environment="INTERVAL_HOURS=6"
Environment="PORT=8081"
Environment="STORAGE_PATH=/path/to/summaries.json"
ExecStart=/usr/bin/java -jar /path/to/reminder-server/build/libs/reminder-server-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Затем:
```bash
sudo systemctl daemon-reload
sudo systemctl enable reminder-server
sudo systemctl start reminder-server
sudo systemctl status reminder-server
```
