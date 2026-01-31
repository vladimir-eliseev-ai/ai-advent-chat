# Article Summarizer MCP Server (порт 8083)

Создание резюме текста. Инструмент: **summarize(text, max_length?)**. Использует DeepSeek API при наличии ключа.

## Переменные окружения

- `DEEPSEEK_API_KEY` — ключ DeepSeek (без него — обрезка по длине).
- `DEEPSEEK_MODEL` — модель (по умолчанию `deepseek-chat`).
- `PORT` — порт (по умолчанию 8083).

## Запуск

```bash
export DEEPSEEK_API_KEY=sk-...
export PORT=8083
./gradlew :article-summarizer-mcp-server:run
```
