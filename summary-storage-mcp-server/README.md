# Summary Storage MCP Server (порт 8084)

Сохранение и список резюме. Инструменты: **save_summary(content, original_url)**, **list_saved_summaries()**.

## Переменные окружения

- `OUTPUT_DIR` — папка для файла `saved_summaries.json` (по умолчанию `./output`).
- `PORT` — порт (по умолчанию 8084).

## Запуск

```bash
export PORT=8084
./gradlew :summary-storage-mcp-server:run
```

Для работы раздела «Краткая статья» в приложении запустите все три сервера (в трёх терминалах) на портах 8082, 8083, 8084.
