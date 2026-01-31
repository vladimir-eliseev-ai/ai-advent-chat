# Article Reader MCP Server (порт 8082)

Чтение статьи по URL. Инструмент: **fetch_article(url)**.

## Запуск

```bash
export PORT=8082
./gradlew :article-reader-mcp-server:run
```

Из корня проекта. Для Android-эмулятора в приложении укажите `MCP_BASE_URL=http://10.0.2.2` (порты 8082–8084 подставляются автоматически).
