# Запуск NewsAPI MCP Server как HTTP сервер

## Быстрый старт

### 1. Соберите проект

```bash
cd newsapi-mcp-server
./gradlew shadowJar
```

### 2. Установите переменную окружения

```bash
export NEWS_API_KEY="ваш_api_ключ"
```

### 3. Запустите HTTP сервер

```bash
java -jar build/libs/newsapi-mcp-server-1.0.0.jar
```

Сервер запустится на **http://localhost:8080**

### 4. Измените порт (опционально)

```bash
PORT=3000 java -jar build/libs/newsapi-mcp-server-1.0.0.jar
```

## Подключение из Android приложения

### Для эмулятора Android:

1. Запустите HTTP сервер на компьютере
2. В Android приложении выберите "NewsAPI MCP Server"
3. Сервер автоматически подключится к `http://10.0.2.2:8080/mcp`
   - `10.0.2.2` - специальный IP для доступа к localhost из эмулятора

### Для реального устройства:

1. Узнайте IP адрес вашего компьютера:
   ```bash
   # macOS/Linux
   ifconfig | grep "inet " | grep -v 127.0.0.1
   
   # Windows
   ipconfig
   ```

2. Запустите сервер на этом IP (или используйте 0.0.0.0):
   ```bash
   # Сервер будет доступен на всех интерфейсах
   PORT=8080 java -jar build/libs/newsapi-mcp-server-1.0.0.jar
   ```

3. В Android приложении измените URL на `http://ВАШ_IP:8080/mcp`

## Проверка работы

После запуска сервера проверьте:

```bash
curl http://localhost:8080/mcp
```

Должен вернуться ответ от MCP сервера.

## Остановка сервера

Нажмите `Ctrl+C` в терминале, где запущен сервер.
