# MCP-серверы в Docker

Сборка и запуск из корня проекта:

```bash
docker compose up -d
```

Остановка:

```bash
docker compose down
```

Порты: 8081 (article-summary), 8082 (reader), 8083 (summarizer), 8084 (storage).

Для суммаризации через DeepSeek передайте ключ: `DEEPSEEK_API_KEY=sk-... docker compose up -d`.
